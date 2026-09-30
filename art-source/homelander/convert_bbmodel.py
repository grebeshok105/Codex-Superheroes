#!/usr/bin/env python3
"""Convert Homelander_All_Animations.bbmodel into Codex runtime assets.

Outputs (into src/main/resources/assets/superheroes/emf/homelander/):
  model.json          - bone hierarchy, pivots, cubes (per-face uv + texture index)
  <clip>.json         - baked per-frame tracks (rotation deg / position px / scale)
                        meant to be wrapped into EMF keyframe()/keyframeloop()
                        expressions at load: keyframe(var.<clip>_frame, v0..vN)
                        at sample_fps granularity, values in Blockbench units
                        (rotation degrees, position model pixels).

Also writes homelander_milk.png next to homelander.png textures.
"""
import base64
import collections
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SRC = Path(__file__).with_name("Homelander_All_Animations.bbmodel")
OUT = ROOT / "src/main/resources/assets/superheroes/emf/homelander"
TEX_OUT = ROOT / "src/main/resources/assets/superheroes/textures/entity/hero/homelander_milk.png"

# clip file name -> bbmodel animation name prefix
CLIPS = {
    "hover": "HOVER",
    "takeoff": "TAKEOFF",
    "boost": "BOOST FLIGHT",
    "hand_clap": "HAND CLAP",
    "milk_drink": "MILK DRINK",
}
REFERENCE_CLIPS = {"showcase": "SHOWCASE"}  # visual reference only, not runtime


def round3(v):
    return round(float(v), 4)


def catmull(p0, p1, p2, p3, u):
    u2, u3 = u * u, u * u * u
    return 0.5 * ((2 * p1) + (-p0 + p2) * u + (2 * p0 - 5 * p1 + 4 * p2 - p3) * u2
                  + (-p0 + 3 * p1 - 3 * p2 + p3) * u3)


def sample_channel(points, fps, length):
    """Uniform-resample sparse (t, [x,y,z]) keys at `fps` over [0, length)
    with catmull-rom interpolation (matches Blockbench playback)."""
    if not points:
        return []
    pts = sorted(points)
    if len(pts) == 1:
        v = pts[0][1]
        n = max(1, int(round(length * fps)))
        return [v] * n
    n = max(1, int(round(length * fps)))
    out = []
    for k in range(n):
        t = k / fps
        # find segment i: t_i <= t < t_{i+1}
        i = 0
        while i < len(pts) - 2 and pts[i + 1][0] <= t:
            i += 1
        t0, v0 = pts[max(0, i - 1)]
        t1, v1 = pts[i]
        t2, v2 = pts[i + 1]
        t3, v3 = pts[min(len(pts) - 1, i + 2)]
        span = t2 - t1
        u = 0.0 if span <= 0 else (t - t1) / span
        out.append([round3(catmull(v0[c], v1[c], v2[c], v3[c], u)) for c in range(3)])
    return out


def main():
    d = json.loads(SRC.read_text())
    groups = {g["uuid"]: g for g in d["groups"]}

    # parent map from the outliner (uuid -> parent uuid or None)
    parent = {}
    element_group = {}

    def walk(node, p):
        for ch in node.get("children", []):
            if isinstance(ch, dict):
                parent[ch["uuid"]] = p
                walk(ch, ch["uuid"])
            else:
                element_group[ch] = p

    for node in d["outliner"]:
        parent[node["uuid"]] = None
        walk(node, node["uuid"])

    elements = {e["uuid"]: e for e in d["elements"]}
    bones = []
    for g in d["groups"]:
        name = g["name"]
        cubes = []
        for el_uuid, gp in element_group.items():
            if gp != g["uuid"]:
                continue
            el = elements[el_uuid]
            origin = el["from"]
            size = [round3(b - a) for a, b in zip(el["from"], el["to"])]
            faces = {}
            for fname, f in el.get("faces", {}).items():
                if f is None:
                    continue
                faces[fname] = {"uv": [round3(u) for u in f["uv"]],
                                "texture": f.get("texture")}
            cubes.append({"origin": [round3(v) for v in origin], "size": size,
                          "faces": faces})
        bones.append({
            "name": name,
            "parent": groups[parent[g["uuid"]]]["name"] if parent.get(g["uuid"]) else None,
            "pivot": [round3(v) for v in g["origin"]],
            "cubes": cubes,
        })
    model = {
        "format": "codex-emf-model/1",
        "source": "art-source/homelander/Homelander_All_Animations.bbmodel",
        "textures": {
            "0": "superheroes:textures/entity/hero/homelander.png",
            "1": "superheroes:textures/entity/hero/homelander_milk.png",
        },
        "texture_size": [64, 64],
        # prop bones are hidden (scale 0) unless a playing clip animates them
        "prop_bones": ["milk_bottle", "milk_cap", "mouth_open"],
        "bones": bones,
    }

    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / "model.json").write_text(json.dumps(model, indent=1, ensure_ascii=False))

    # textures: [0] homelander (same as repo file), [1] milk palette
    milk_src = d["textures"][1]["source"]
    TEX_OUT.write_bytes(base64.b64decode(milk_src.split(",", 1)[1]))

    uuid_to_bone = {u: g["name"] for u, g in groups.items()}

    for clip_name, prefix in {**CLIPS, **REFERENCE_CLIPS}.items():
        anim = next(a for a in d["animations"] if a["name"].startswith(prefix))
        out_bones = {}
        for buuid, animator in anim["animators"].items():
            bone = uuid_to_bone.get(buuid, buuid)
            channels = collections.defaultdict(list)
            for kf in animator.get("keyframes", []):
                dp = kf["data_points"][0]
                channels[kf["channel"]].append(
                    (round3(kf["time"]), [round3(dp["x"]), round3(dp["y"]), round3(dp["z"])]))
            if channels:
                out_bones[bone] = {
                    ch: sample_channel(pts, 60, anim["length"])
                    for ch, pts in channels.items()}
        # Prop bones are shown by scale in the bbmodel; clips that animate a
        # prop without a scale channel mean "fully visible" — bake scale=1 so
        # the runtime can keep props hidden (scale 0) everywhere else.
        frames_hint = max(1, int(round(anim["length"] * 60)))
        for prop in ("milk_bottle", "milk_cap", "mouth_open"):
            if prop in out_bones and "scale" not in out_bones[prop]:
                out_bones[prop]["scale"] = [[1.0, 1.0, 1.0]] * frames_hint
        frames = max((len(v) for chs in out_bones.values() for v in chs.values()), default=0)
        clip = {
            "format": "emf-keyframe-clip/1",
            "source_animation": anim["name"],
            "length_seconds": round3(anim["length"]),
            "loop": anim["loop"] == "loop",
            "reference_only": clip_name in REFERENCE_CLIPS,
            "sample_fps": 60,
            "frame_count": frames,
            "bones": out_bones,
        }
        (OUT / f"{clip_name}.json").write_text(json.dumps(clip, separators=(",", ":"), ensure_ascii=False))
        print(clip_name, anim["name"], "bones:", len(out_bones), "frames:", frames)

    # emf_lab authored expressions (reference for the EMF-expression path)
    lab = d.get("emf_lab") or d.get("unhandled_root_fields", {}).get("emf_lab")
    if lab:
        (OUT / "emf_lab_expressions.json").write_text(
            json.dumps(lab, indent=1, ensure_ascii=False))
    print("done")


if __name__ == "__main__":
    sys.exit(main())
