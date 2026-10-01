#!/usr/bin/env python3
"""Merges the Regulus rig into the shared EMF player model.

Consumes:
  - Regulus_All_Animations.bbmodel  (rig geometry: groups, pivots, elements, UVs)
  - Regulus_EMF_Authoring.json      (per-clip molang expressions)

Writes (in place):
  - src/main/resources/assets/minecraft/emf/cem/player.jem
  - src/main/resources/assets/minecraft/emf/cem/player_slim.jem

Coordinate conventions (verified against art-source/homelander/emf/bake_jem.py):
  bb -> mc point map:  C(p) = (x, 24 - y, z)          (bb y-up -> mc y-down)
  bb -> mc delta vec:  V(v) = (vx, -vy, vz)
  rotation deltas:     (rx, ry, rz) -> (-rx, +ry, -rz)   [reflection conjugation L R L, L = diag(1,-1,1)]
  scale:               unchanged (absolute multipliers)

Authoring expressions are literal Blockbench channel deltas (keyframe values
from the bbmodel), so translate channels are emitted as rest + V(delta) and
rotation channels as the sign-converted delta.

Blend shape mirrors the homelander carrier format:
  lerp(rg_w, <self rest>, lerp(max(one-shot w...), loopBlend, sum(w_i*E_i)/max(wsum,0.0001)))
  loopBlend = lerp(rg_evangelium_active_idle_w, combat_idle, evangelium_active_idle)
Arm-subtree one-shot weights are gated by *if(is_first_person_hand,0,1).

UNVERIFIED IN-GAME: axis sign conversion and first-person gating are implemented
per the homelander baker's math; visual orientation must be checked by a human.
"""

import json
import os
import re
import sys

_HERE = os.path.dirname(os.path.abspath(__file__))
_REPO = os.path.normpath(os.path.join(_HERE, "..", ".."))
BBMODEL = os.path.join(_HERE, "Regulus_All_Animations.bbmodel")
AUTHORING = os.path.join(_HERE, "Regulus_EMF_Authoring.json")
JEM_DIR = os.path.join(
    _REPO, "src", "main", "resources", "assets", "minecraft", "emf", "cem")
JEM_FILES = [os.path.join(JEM_DIR, "player.jem"),
             os.path.join(JEM_DIR, "player_slim.jem")]

RG_TEXTURE = "superheroes:textures/entity/hero/regulus.png"
TEX_SIZE = [64, 64]

# one-shot clips (order: weight var order in the blend sums)
ONESHOTS = [
    "lion_heart_activation",
    "lion_roar",
    "mania_of_greed_cast",
    "greeds_embrace_cast",
    "counter_attack",
    "evangelium_activation",
    "evangelium_deactivation",
    "debris_kick",
]
LOOP_COMBAT = "combat_idle"
LOOP_EVANGELIUM = "evangelium_active_idle"

W_MASTER = "superheroes_rg_w"
W_LOOP_EV = "superheroes_rg_evangelium_active_idle_w"


def weight(clip):
    return f"superheroes_rg_{clip}_w"


# parts whose one-shot weights are suppressed in first person (held arms)
FP_GATED = {
    "right_shoulder", "right_arm", "right_forearm", "right_hand",
    "left_shoulder", "left_arm", "left_forearm", "left_hand",
}

CHANNELS = ("tx", "ty", "tz", "rx", "ry", "rz", "sx", "sy", "sz")
_UV_FIELDS = {"north": "uvNorth", "south": "uvSouth", "east": "uvEast",
              "west": "uvWest", "up": "uvUp", "down": "uvDown"}

# vanilla overlay inflation per part name (head hat is +0.5, rest +0.25)
LAYER_INFLATE = {"hat_layer": 0.5}


def _sub(a, b):
    return tuple(x - y for x, y in zip(a, b))


def vec_bb_to_mc(v):
    return (v[0], -v[1], v[2])


def bb_to_mc(p):
    return (p[0], 24.0 - p[1], p[2])


def fmt(v):
    f = float(v)
    return int(f) if f == int(f) else round(f, 6)


def fmt_num(v):
    f = float(v)
    return str(int(f)) if f == int(f) else f"{f:.6f}".rstrip("0").rstrip(".")


class Group:
    def __init__(self, uuid_, name, origin, rotation, parent=None):
        self.uuid = uuid_
        self.name = name
        self.origin = tuple(origin)
        self.rotation = tuple(rotation or (0.0, 0.0, 0.0))
        self.parent = parent
        self.children = []
        self.elements = []


def load_model():
    bb = json.load(open(BBMODEL))
    meta = {g["uuid"]: g for g in bb["groups"]}
    elements = {e["uuid"]: e for e in bb["elements"]}
    groups = {}

    def build(node, parent):
        g = groups.get(node["uuid"])
        if g is None:
            m = meta[node["uuid"]]
            g = Group(node["uuid"], m["name"], m["origin"],
                      m.get("rotation"), parent)
            groups[node["uuid"]] = g
        for child in node.get("children", []):
            if isinstance(child, dict):
                g.children.append(build(child, g))
            else:
                g.elements.append(elements[child])
        return g

    roots = [build(n, None) for n in bb["outliner"]]
    assert len(roots) == 1 and roots[0].name == "root", (
        f"expected single 'root' group, got {[r.name for r in roots]}")
    return roots[0]


def box_from_element(el, group, slim):
    fr, to = el["from"], el["to"]
    o = group.origin
    fx, fy, fz = fr[0] - o[0], fr[1] - o[1], fr[2] - o[2]
    tx, ty, tz = to[0] - o[0], to[1] - o[1], to[2] - o[2]
    inflate = LAYER_INFLATE.get(el["name"], 0.25 if el["name"].endswith("_layer") else 0.0)
    if inflate:
        fx -= inflate
        fy -= inflate
        fz -= inflate
        tx += inflate
        ty += inflate
        tz += inflate
    if slim and group.name in ("right_arm", "right_forearm"):
        fx += 1.0
    if slim and group.name in ("left_arm", "left_forearm"):
        tx -= 1.0
    box = {"coordinates": [fmt(v) for v in (fx, -ty, fz, tx - fx, ty - fy, tz - fz)]}
    for face, field in _UV_FIELDS.items():
        f = el.get("faces", {}).get(face)
        if f and "uv" in f:
            box[field] = [fmt(x) for x in f["uv"]]
    return box


# ---------------------------------------------------------------------------
# expression pipeline
# ---------------------------------------------------------------------------

def resolve_vars(clip):
    """Inline rg_t / rg_u_* into part_expressions so only var.regulus_* survives."""
    ov = clip["ordered_variables"]
    rg_t = ov.get("var.rg_t")
    resolved = dict(ov)
    if rg_t:
        for k in list(resolved):
            resolved[k] = resolved[k].replace("var.rg_t", f"({rg_t})")
    exprs = {}
    for key, expr in clip["part_expressions"].items():
        for name in sorted(resolved, key=len, reverse=True):
            expr = expr.replace(name, f"({resolved[name]})")
        exprs[key] = expr
    return exprs


def convert_channel(channel, rest_translate, expr):
    """bb delta -> mc absolute channel value."""
    idx = {"tx": 0, "ty": 1, "tz": 2}
    if channel in ("tx", "tz"):
        return f"({fmt_num(rest_translate[idx[channel]])} + ({expr}))"
    if channel == "ty":
        return f"({fmt_num(rest_translate[idx[channel]])} - ({expr}))"
    if channel in ("rx", "rz"):
        return f"(0 - ({expr}))"
    return f"({expr})"  # ry, sx, sy, sz


def channel_expr(part_name, channel, rest, per_clip, fp_gate):
    """Full blend expression for one channel of one part."""
    ws = {}
    for clip in ONESHOTS:
        w = weight(clip)
        if fp_gate:
            w = f"({w}*if(is_first_person_hand,0,1))"
        ws[clip] = w
    wsum = "(" + "+".join(ws[c] for c in ONESHOTS) + ")"
    terms = [f"{ws[c]}*{per_clip[c]}" for c in ONESHOTS]
    oneshot = f"({' + '.join(terms)})/max({wsum},0.0001)"
    oneshot = f"({oneshot})"
    max_w = ws[ONESHOTS[-1]]
    for c in reversed(ONESHOTS[:-1]):
        max_w = f"max({ws[c]},{max_w})"

    loop = (f"lerp({W_LOOP_EV},{per_clip[LOOP_COMBAT]},"
            f"{per_clip[LOOP_EVANGELIUM]})")
    blended = f"lerp({max_w},{loop},{oneshot})"
    return f"lerp({W_MASTER},{rest},{blended})"


def rest_for(channel, translate):
    if channel == "tx":
        return fmt_num(translate[0])
    if channel == "ty":
        return fmt_num(translate[1])
    if channel == "tz":
        return fmt_num(translate[2])
    if channel in ("rx", "ry", "rz"):
        return "0"
    return "1"


def convert_or_rest(channel, rest_translate, authored):
    if authored is None:
        return rest_for(channel, rest_translate)
    return convert_channel(channel, rest_translate, authored)


# ---------------------------------------------------------------------------
# jem emission
# ---------------------------------------------------------------------------

def part_entry(group, clip_expr_tables, slim, is_root):
    assert all(v == 0 for v in group.rotation), (
        f"{group.name} has a non-zero rest rotation, unsupported")
    if is_root:
        # EMF "root" part pivot is model-space origin (0,0,0); bb origin maps to
        # (0,24,0) — the feet anchor.
        translate = bb_to_mc(group.origin)
    else:
        translate = vec_bb_to_mc(_sub(group.origin, group.parent.origin))
    entry = {
        "id": f"rg_{group.name}",
        "translate": [fmt(v) for v in translate],
        "rotate": [0, 0, 0],
        "boxes": [box_from_element(el, group, slim) for el in group.elements],
    }
    animations = []
    for ch in CHANNELS:
        # mouth_open's authored rest is hidden (sx=0 in every non-roar clip)
        if group.name == "mouth_open" and ch in ("sx", "sy", "sz"):
            rest = "0"
        else:
            rest = rest_for(ch, translate)
        per_clip = {}
        for clip_name, table in clip_expr_tables.items():
            authored = table.get(f"{group.name}.{ch}")
            per_clip[clip_name] = convert_or_rest(ch, translate, authored)
        animations.append({
            f"rg_{group.name}.{ch}": channel_expr(
                group.name, ch, rest, per_clip,
                group.name in FP_GATED),
        })
    animations.append({f"rg_{group.name}.visible": f"!({W_MASTER}<=0.01)"})
    entry["animations"] = animations
    if entry["boxes"]:
        entry["texture"] = RG_TEXTURE
        entry["textureSize"] = TEX_SIZE
    subs = [part_entry(c, clip_expr_tables, slim, False)
            for c in group.children]
    if subs:
        entry["submodels"] = subs
    return entry


def _anim_keys(entry):
    return {k for a in entry.get("animations", []) for k in a}


HL_GATE = "!(superheroes_hl_w<=0.01)"


def _set_visible(entry, key):
    """Idempotently (re)write a .visible gate: a prior merge run's form is
    replaced, authored forms (1=0, is_first_person_hand) are kept."""
    anims = entry.setdefault("animations", [])
    for a in anims:
        if key in a and a[key] in ("superheroes_hl_w > 0.01", HL_GATE):
            a[key] = HL_GATE
            return
        if key in a:
            return
    anims.append({key: HL_GATE})


def gate_homelander(entry):
    """Every hl part renders only while its hero owns the model."""
    pid = entry.get("part")
    if pid in ("body", "head", "right_arm", "left_arm", "right_leg", "left_leg"):
        _set_visible(entry, f"{pid}.visible")
    for sub in entry.get("submodels", []):
        sid = sub.get("id")
        if sid and sid.startswith("hl_"):
            _set_visible(sub, f"{sid}.visible")
        gate_homelander(sub)


def build(path, slim):
    with open(path) as f:
        jem = json.load(f)
    root = load_model()
    authoring = json.load(open(AUTHORING))
    clip_tables = {
        name.split(".")[-1]: resolve_vars(clip)
        for name, clip in authoring["clips"].items()
    }
    expected = set(ONESHOTS) | {LOOP_COMBAT, LOOP_EVANGELIUM}
    missing = expected - set(clip_tables)
    assert not missing, f"clips missing from authoring: {missing}"

    jem["models"] = [m for m in jem["models"]
                     if not str(m.get("id", "")).startswith("rg_")]
    for m in jem["models"]:
        gate_homelander(m)
    rg_root = part_entry(root, clip_tables, slim, True)
    rg_root["part"] = "root"
    rg_root["attach"] = True
    jem["models"].append(rg_root)
    with open(path, "w") as f:
        json.dump(jem, f, indent=1, ensure_ascii=False)
        f.write("\n")
    print(f"wrote {path} ({len(jem['models'])} top-level model entries)")


if __name__ == "__main__":
    slim_flag = "--slim" in sys.argv
    targets = JEM_FILES if not slim_flag else [JEM_FILES[1]]
    for p in targets:
        build(p, slim=p.endswith("slim.jem"))
