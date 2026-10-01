#!/usr/bin/env python3
"""Bake Homelander_All_Animations.bbmodel into EMF jem files.

Plan reference: docs/superpowers/plans/2026-09-30-homelander-emf-presentation.md
§4.3-§4.5 (channels, expression shapes, variables) and §7 Stage 1.

Outputs ``player.jem`` and ``player_slim.jem`` under
``src/main/resources/assets/minecraft/emf/cem/`` and prints the per-clip
root-mean rotations for the ``flight.json`` ``emfBoostRoot*`` keys.

Channels are emitted at the smallest uniform rate whose EMF catmull-rom
reproduction stays within the plan's fidelity bound (0.5 deg / 0.05 px) —
20 Hz for smooth channels, higher for the authored impact snaps.

Usage:
    python3 bake_jem.py Homelander_All_Animations.bbmodel             # both jems
    python3 bake_jem.py Homelander_All_Animations.bbmodel --slim      # slim only
    python3 bake_jem.py Homelander_All_Animations.bbmodel --classic   # classic only
"""

import argparse
import base64
import json
import math
import os

TICKS = 20.0  # EMF keyframe()/keyframeloop() frame index = time_seconds * rate

# clip detection: name prefix -> short id used in superheroes_hl_* vars
CLIP_IDS = {
    "HOVER": "hover",
    "TAKEOFF": "takeoff",
    "HAND CLAP": "clap",
    "BOOST": "boost",
    "MILK DRINK": "milk",
}
EXCLUDED_CLIPS = ("SHOWCASE",)

# authored groups that become carrier parts on the vanilla bone of the same name
CARRIER_PARTS = ("body", "head", "right_arm", "left_arm", "right_leg", "left_leg")

# vanilla overlay parts hidden forever (overlays are baked into the carriers)
HIDDEN_PARTS = ("headwear", "jacket", "left_sleeve", "right_sleeve",
                "left_pants", "right_pants")

# parts rendered on the milk texture instead of the entity texture
MILK_TEXTURE = "superheroes:textures/homelander/milk"
MILK_TEXTURE_PARTS = ("mouth_open", "milk_bottle", "milk_cap")

# arm-subtree parts get is_first_person_hand weight gating on clap/milk
FP_GATED = ("right_arm", "right_forearm", "left_arm", "left_forearm",
            "milk_bottle", "milk_cap")

PROP_VISIBLE = "superheroes_hl_milk_w > 0.01 && !is_first_person_hand"

ATTACHMENTS = {
    "right_forearm": {"right_handheld_item": [0, 4, 0]},
    "left_forearm": {"left_handheld_item": [0, 4, 0]},
}

CHANNELS = ("tx", "ty", "tz", "rx", "ry", "rz", "sx", "sy", "sz")
ROT_CHANNELS = ("rx", "ry", "rz")
# candidate sample rates; the smallest rate inside the fidelity bound wins
RATES = (20.0, 40.0, 100.0, 200.0, 400.0)
TOL_RAD = math.radians(0.5)   # plan bound: 0.5 deg on rotation channels
TOL_PX = 0.05                 # pixels on translation channels
TOL_SCALE = 0.005

_HERE = os.path.dirname(os.path.abspath(__file__))
JEM_DIR = os.path.normpath(os.path.join(
    _HERE, "..", "..", "..", "src", "main", "resources", "assets",
    "minecraft", "emf", "cem"))
TEX_OUT = os.path.normpath(os.path.join(
    _HERE, "..", "..", "..", "src", "main", "resources", "assets",
    "superheroes", "textures", "homelander", "milk.png"))


# ---------------------------------------------------------------------------
# minimal 3x3 + affine math (no numpy dependency)
# ---------------------------------------------------------------------------

def v_add(a, b):
    return (a[0] + b[0], a[1] + b[1], a[2] + b[2])


def v_sub(a, b):
    return (a[0] - b[0], a[1] - b[1], a[2] - b[2])


def m_vec(A, v):
    return (
        A[0][0] * v[0] + A[0][1] * v[1] + A[0][2] * v[2],
        A[1][0] * v[0] + A[1][1] * v[1] + A[1][2] * v[2],
        A[2][0] * v[0] + A[2][1] * v[1] + A[2][2] * v[2],
    )


def m_mul(A, B):
    return tuple(
        tuple(sum(A[i][k] * B[k][j] for k in range(3)) for j in range(3))
        for i in range(3)
    )


def m_transpose(A):
    return tuple(tuple(A[j][i] for j in range(3)) for i in range(3))


def m_identity():
    return ((1.0, 0.0, 0.0), (0.0, 1.0, 0.0), (0.0, 0.0, 1.0))


def rot_x(a):
    c, s = math.cos(a), math.sin(a)
    return ((1.0, 0.0, 0.0), (0.0, c, -s), (0.0, s, c))


def rot_y(a):
    c, s = math.cos(a), math.sin(a)
    return ((c, 0.0, s), (0.0, 1.0, 0.0), (-s, 0.0, c))


def rot_z(a):
    c, s = math.cos(a), math.sin(a)
    return ((c, -s, 0.0), (s, c, 0.0), (0.0, 0.0, 1.0))


def euler_deg_to_rot_zyx(rx, ry, rz):
    """Blockbench extrinsic XYZ == JOML rotationZYX: R = Rz * Ry * Rx."""
    return m_mul(rot_z(math.radians(rz)),
                 m_mul(rot_y(math.radians(ry)), rot_x(math.radians(rx))))


def euler_rad_to_rot_zyx(rx, ry, rz):
    return m_mul(rot_z(rz), m_mul(rot_y(ry), rot_x(rx)))


def rot_to_euler_zyx(R):
    """Extract (rx, ry, rz) radians from R = Rz*Ry*Rx."""
    sy = max(-1.0, min(1.0, -R[2][0]))
    ry = math.asin(sy)
    cy = math.cos(ry)
    if abs(cy) > 1e-6:
        rx = math.atan2(R[2][1], R[2][2])
        rz = math.atan2(R[1][0], R[0][0])
    else:
        # gimbal lock: rz absorbed into rx
        rx = math.atan2(-R[0][1], R[1][1])
        rz = 0.0
    return rx, ry, rz


def rot_angle_deg(R):
    tr = R[0][0] + R[1][1] + R[2][2]
    c = max(-1.0, min(1.0, (tr - 1.0) / 2.0))
    return math.degrees(math.acos(c))


L_FLIP = ((1.0, 0.0, 0.0), (0.0, -1.0, 0.0), (0.0, 0.0, 1.0))
C_OFF = (0.0, 24.0, 0.0)  # bb->mc origin shift: C(p) = L*p + c


def conj_bb_to_mc_mat(A):
    return m_mul(L_FLIP, m_mul(A, L_FLIP))


def bb_to_mc(p):
    return (p[0], 24.0 - p[1], p[2])


def vec_bb_to_mc(v):
    return (v[0], -v[1], v[2])


class Affine:
    """Affine ``p -> A*p + w``."""

    __slots__ = ("a", "w")

    def __init__(self, a=None, w=(0.0, 0.0, 0.0)):
        self.a = a if a is not None else m_identity()
        self.w = w

    @classmethod
    def identity(cls):
        return cls()

    @classmethod
    def translation(cls, x, y, z):
        return cls(m_identity(), (x, y, z))

    @classmethod
    def from_pivot(cls, R, S, origin, offset):
        """Blockbench group transform: q -> o + p + R*S*(q - o)."""
        rs = m_mul(R, S)
        w = v_add(v_add(origin, offset), m_vec(rs, v_sub((0.0, 0.0, 0.0), origin)))
        return cls(rs, w)

    def compose(self, other):
        """self o other (other applies first)."""
        return Affine(m_mul(self.a, other.a),
                      v_add(m_vec(self.a, other.w), self.w))

    def apply(self, p):
        return v_add(m_vec(self.a, p), self.w)

    def almost_equal(self, other, tol):
        for i in range(3):
            if abs(self.w[i] - other.w[i]) > tol:
                return False
            for j in range(3):
                if abs(self.a[i][j] - other.a[i][j]) > tol:
                    return False
        return True

    def to_mc(self):
        """Conjugate into mc coords: F_mc = C o F_bb o C^-1.
        With C(v) = L*v + c: A_mc = L*A*L and w_mc = c + L*(w - A*L*c)."""
        a_mc = conj_bb_to_mc_mat(self.a)
        w_mc = v_add(C_OFF, m_vec(
            L_FLIP, v_sub(self.w, m_vec(self.a, m_vec(L_FLIP, C_OFF)))))
        return Affine(a_mc, w_mc)


def rot_about_point(R, pivot):
    return Affine(R, v_sub(pivot, m_vec(R, pivot)))


# ---------------------------------------------------------------------------
# bbmodel loading
# ---------------------------------------------------------------------------

def parse_number(v):
    """Parse a bbmodel scalar; raises on Molang expressions."""
    if isinstance(v, (int, float)):
        return float(v)
    s = str(v).strip()
    try:
        return float(s)
    except ValueError:
        raise ValueError(f"Molang-valued keyframe data is unsupported: {v!r}")


class KeyframeTrack:
    """One animated channel of one group in one clip.

    keys: sorted list of (time, (x,y,z), interp, rc_time, rc_value, lc_time,
    lc_value) where rc_* are this key's right bezier controls and lc_* the
    next key's left controls.
    """

    def __init__(self, keyframes):
        self.keys = sorted(keyframes, key=lambda k: k[0])

    def eval(self, t):
        if not self.keys:
            return (0.0, 0.0, 0.0)
        if t <= self.keys[0][0]:
            return self.keys[0][1]
        if t >= self.keys[-1][0]:
            return self.keys[-1][1]
        for i in range(len(self.keys) - 1):
            t0, v0, interp, rc_t0, rc_v0, _lc_t0, _lc_v0 = self.keys[i]
            t1, v1, _i1, _rc_t1, _rc_v1, lc_t1, lc_v1 = self.keys[i + 1]
            if t0 <= t <= t1:
                d = (t - t0) / (t1 - t0) if t1 > t0 else 0.0
                return tuple(
                    self._interp(i, interp, d, c, rc_t0, rc_v0, lc_t1, lc_v1)
                    for c in range(3))
        return self.keys[-1][1]

    def _interp(self, i, interp, d, c, rc_t0, rc_v0, lc_t1, lc_v1):
        keys = self.keys
        t0 = keys[i][0]
        t1 = keys[i + 1][0]
        v0 = keys[i][1][c]
        v1 = keys[i + 1][1][c]
        if interp == "step":
            return v0
        if interp == "linear":
            return v0 + (v1 - v0) * d
        if interp == "bezier":
            x0, x1, x2, x3 = t0, t0 + rc_t0[c], t1 + lc_t1[c], t1
            y0, y1, y2, y3 = v0, v0 + rc_v0[c], v1 + lc_v1[c], v1
            u = _solve_bezier_u(t0 + (t1 - t0) * d, x0, x1, x2, x3)
            return _bezier(u, y0, y1, y2, y3)
        if interp == "catmullrom":
            i0 = max(0, i - 1)
            i3 = min(len(keys) - 1, i + 2)
            p0, p1, p2, p3 = (keys[i0][1][c], keys[i][1][c],
                              keys[i + 1][1][c], keys[i3][1][c])
            return _catmull(d, p0, p1, p2, p3)
        raise ValueError(f"unknown interpolation {interp!r}")


def _bezier(u, a, b, c, d):
    mt = 1 - u
    return mt ** 3 * a + 3 * mt * mt * u * b + 3 * mt * u * u * c + u ** 3 * d


def _solve_bezier_u(target, x0, x1, x2, x3):
    lo, hi = 0.0, 1.0
    for _ in range(40):
        mid = (lo + hi) / 2
        if _bezier(mid, x0, x1, x2, x3) < target:
            lo = mid
        else:
            hi = mid
    return (lo + hi) / 2


def _catmull(d, p0, p1, p2, p3):
    return 0.5 * ((2 * p1) + (p2 - p0) * d
                  + (2 * p0 - 5 * p1 + 4 * p2 - p3) * d * d
                  + (-p0 + 3 * p1 - 3 * p2 + p3) * d * d * d)


class Group:
    def __init__(self, raw, parent):
        self.name = raw["name"]
        self.uuid = raw["uuid"]
        self.origin = tuple(float(x) for x in raw["origin"])
        self.rotation = tuple(float(x) for x in raw.get("rotation", [0, 0, 0]))
        self.parent = parent
        self.children = []
        self.elements = []  # element uuids


class Clip:
    def __init__(self, raw):
        self.name = raw["name"]
        self.clip_id = None
        for prefix, cid in CLIP_IDS.items():
            if raw["name"].startswith(prefix):
                self.clip_id = cid
                break
        if self.clip_id is None:
            raise ValueError(f"unmapped clip {self.name!r}")
        self.loop = raw.get("loop") == "loop"
        self.length_seconds = float(raw.get("length", 0))
        self.tracks = {}      # group name -> {channel: KeyframeTrack}
        self.animators = []   # raw animator dicts (for the molang audit)


class Model:
    def __init__(self, path):
        with open(path) as f:
            self.raw = json.load(f)
        self.groups = {}
        self.elements_by_uuid = {e["uuid"]: e for e in self.raw.get("elements", [])}
        self._build_groups()
        self.clips = {}
        for a in self.raw.get("animations", []):
            if any(a["name"].startswith(x) for x in EXCLUDED_CLIPS):
                continue
            clip = Clip(a)
            self._load_animators(a, clip)
            self.clips[clip.clip_id] = clip
        self.milk_png = self._extract_milk_png()

    def _build_groups(self):
        raw_groups = {g["uuid"]: g for g in self.raw.get("groups", [])}

        def walk(nodes, parent):
            for node in nodes:
                if isinstance(node, str):
                    if parent is not None:
                        parent.elements.append(node)
                    continue
                raw = raw_groups[node["uuid"]]
                g = Group(raw, parent)
                self.groups[g.name] = g
                if parent is not None:
                    parent.children.append(g)
                walk(node.get("children", []), g)

        walk(self.raw.get("outliner", []), None)

    def _load_animators(self, raw_clip, clip):
        uuid_to_name = {g.uuid: g.name for g in self.groups.values()}
        for uuid, an in raw_clip.get("animators", {}).items():
            clip.animators.append(an)
            name = uuid_to_name.get(uuid)
            if name is None:
                continue
            tracks = {}
            for kf in an.get("keyframes", []):
                chan = kf["channel"]
                t = float(kf["time"])
                vals = tuple(parse_number(v)
                             for v in kf["data_points"][0].values())[:3]
                interp = kf.get("interpolation", "linear")
                rc_t = [parse_number(x)
                        for x in kf.get("bezier_right_time", [0, 0, 0])]
                rc_v = [parse_number(x)
                        for x in kf.get("bezier_right_value", [0, 0, 0])]
                lc_t = [parse_number(x)
                        for x in kf.get("bezier_left_time", [0, 0, 0])]
                lc_v = [parse_number(x)
                        for x in kf.get("bezier_left_value", [0, 0, 0])]
                tracks.setdefault(chan, []).append(
                    (t, vals, interp, rc_t, rc_v, lc_t, lc_v))
            clip.tracks[name] = {c: KeyframeTrack(v) for c, v in tracks.items()}

    def _extract_milk_png(self):
        for tex in self.raw.get("textures", []):
            if "Milk" in tex.get("name", ""):
                src = tex.get("source", "")
                if src.startswith("data:"):
                    return base64.b64decode(src.split(",", 1)[1])
        return None

    def group(self, name):
        return self.groups[name]

    def chain(self, group):
        """Root-first ancestor chain (inclusive)."""
        chain = []
        g = group
        while g is not None:
            chain.append(g)
            g = g.parent
        return list(reversed(chain))

    def local_affine(self, clip, group, t):
        """The group's own transform at time t (channels only, bb space)."""
        tracks = clip.tracks.get(group.name, {})
        pos = tracks["position"].eval(t) if "position" in tracks else (0.0, 0.0, 0.0)
        rot = tracks["rotation"].eval(t) if "rotation" in tracks else (0.0, 0.0, 0.0)
        scl = tracks["scale"].eval(t) if "scale" in tracks else (1.0, 1.0, 1.0)
        r = euler_deg_to_rot_zyx(*rot)
        s = ((scl[0], 0.0, 0.0), (0.0, scl[1], 0.0), (0.0, 0.0, scl[2]))
        return Affine.from_pivot(r, s, group.origin, pos)


def load(path):
    return Model(path)


# ---------------------------------------------------------------------------
# sampling API used by the tests and the bake
# ---------------------------------------------------------------------------

def ancestor_chain(model, group):
    return model.chain(group)


def channel_curve(model, clip, group_name, channel):
    """Authored curve evaluator f(t)->vec3, or None when unkeyed."""
    tracks = clip.tracks.get(group_name, {})
    if channel not in tracks:
        return None
    return tracks[channel].eval


def sample_root_rotation(model, clip, n=64):
    """Root group rotation matrices sampled across the clip."""
    rot_track = clip.tracks.get("root", {}).get("rotation")
    out = []
    for i in range(n):
        t = clip.length_seconds * i / max(1, n - 1)
        rot = rot_track.eval(t) if rot_track else (0.0, 0.0, 0.0)
        out.append(euler_deg_to_rot_zyx(*rot))
    return out


def mean_rotation(rotations):
    """Quaternion mean (Markley + power iteration)."""
    m = [[0.0] * 4 for _ in range(4)]
    for r in rotations:
        q = _rot_to_quat(r)
        for i in range(4):
            for j in range(4):
                m[i][j] += q[i] * q[j]
    n = len(rotations)
    for i in range(4):
        for j in range(4):
            m[i][j] /= n
    v = [1.0, 0.0, 0.0, 0.0]
    for _ in range(300):
        w = [sum(m[i][j] * v[j] for j in range(4)) for i in range(4)]
        norm = math.sqrt(sum(x * x for x in w))
        if norm < 1e-12:
            return m_identity()
        v = [x / norm for x in w]
    return _quat_to_rot(v)


def _rot_to_quat(r):
    t = r[0][0] + r[1][1] + r[2][2]
    if t > 0:
        s = math.sqrt(t + 1.0) * 2
        return ((r[2][1] - r[1][2]) / s, (r[0][2] - r[2][0]) / s,
                (r[1][0] - r[0][1]) / s, s / 4)
    if r[0][0] > r[1][1] and r[0][0] > r[2][2]:
        s = math.sqrt(1.0 + r[0][0] - r[1][1] - r[2][2]) * 2
        return (s / 4, (r[0][1] + r[1][0]) / s, (r[0][2] + r[2][0]) / s,
                (r[2][1] - r[1][2]) / s)
    if r[1][1] > r[2][2]:
        s = math.sqrt(1.0 + r[1][1] - r[0][0] - r[2][2]) * 2
        return ((r[0][1] + r[1][0]) / s, s / 4, (r[1][2] + r[2][1]) / s,
                (r[0][2] - r[2][0]) / s)
    s = math.sqrt(1.0 + r[2][2] - r[0][0] - r[1][1]) * 2
    return ((r[0][2] + r[2][0]) / s, (r[1][2] + r[2][1]) / s, s / 4,
            (r[1][0] - r[0][1]) / s)


def _quat_to_rot(q):
    x, y, z, w = q
    return (
        (1 - 2 * (y * y + z * z), 2 * (x * y - z * w), 2 * (x * z + y * w)),
        (2 * (x * y + z * w), 1 - 2 * (x * x + z * z), 2 * (y * z - x * w)),
        (2 * (x * z - y * w), 2 * (y * z + x * w), 1 - 2 * (x * x + y * y)),
    )


def carrier_affine(model, clip, carrier_name, t):
    """Authored world affine of a carrier group at time t (bb space)."""
    w = Affine.identity()
    for g in model.chain(model.group(carrier_name)):
        w = w.compose(model.local_affine(clip, g, t))
    return w


def baked_carrier_affine(model, clip, carrier_name, t, mean_r_inv):
    """Carrier affine with the clip's mean root rotation stripped.

    Left-multiplies the root affine by mean_r_inv before composing, which
    folds the pivot compensation ``meanR^-1 * p - p`` into every carrier's
    position channel automatically.
    """
    strip = Affine(mean_r_inv, (0.0, 0.0, 0.0))
    w = Affine.identity()
    for g in model.chain(model.group(carrier_name)):
        local = model.local_affine(clip, g, t)
        if g.parent is None:  # the root group
            local = strip.compose(local)
        w = w.compose(local)
    return w


def jem_part(jem, part_name):
    for p in jem["models"]:
        if p.get("part") == part_name or p.get("id") == "hl_" + part_name:
            return p
    return None


def all_parts(jem):
    out = []

    def walk(parts):
        for p in parts:
            out.append(p)
            walk(p.get("submodels", []))

    walk(jem["models"])
    return out


def channel_expression(jem, part_name, channel):
    for part in all_parts(jem):
        for anim in part.get("animations", []):
            for key, expr in anim.items():
                prefix, _, chan = key.rpartition(".")
                if chan == channel and (prefix == part_name
                                        or prefix == "hl_" + part_name):
                    return expr
    return None


# ---------------------------------------------------------------------------
# emitted-space channel values (mc flavor: rad rotations, px translations)
# ---------------------------------------------------------------------------

def _decompose_mc_affine(aff):
    mc = aff.to_mc()
    rx, ry, rz = rot_to_euler_zyx(mc.a)
    sx = math.sqrt(mc.a[0][0] ** 2 + mc.a[1][0] ** 2 + mc.a[2][0] ** 2)
    sy = math.sqrt(mc.a[0][1] ** 2 + mc.a[1][1] ** 2 + mc.a[2][1] ** 2)
    sz = math.sqrt(mc.a[0][2] ** 2 + mc.a[1][2] ** 2 + mc.a[2][2] ** 2)
    return {"tx": mc.w[0], "ty": mc.w[1], "tz": mc.w[2],
            "rx": rx, "ry": ry, "rz": rz,
            "sx": sx, "sy": sy, "sz": sz}


def carrier_channel_value(model, clip, carrier_name, channel, t, mean_r_inv):
    return _decompose_mc_affine(
        baked_carrier_affine(model, clip, carrier_name, t, mean_r_inv))[channel]


def carrier_channel_triplet(model, clip, carrier_name, t, mean_r_inv):
    d = _decompose_mc_affine(
        baked_carrier_affine(model, clip, carrier_name, t, mean_r_inv))
    return d["rx"], d["ry"], d["rz"]


def submodel_channel_value(model, clip, group, channel, t):
    return _submodel_channels_at(model, clip, group, t)[channel]


def submodel_channel_triplet(model, clip, group, t):
    d = _submodel_channels_at(model, clip, group, t)
    return d["rx"], d["ry"], d["rz"]


def _submodel_channels_at(model, clip, group, t):
    local = model.local_affine(clip, group, t)
    a = conj_bb_to_mc_mat(local.a)
    pivot_world = local.apply(group.origin)  # = o_sub + p(t)
    t_vec = vec_bb_to_mc(v_sub(pivot_world, group.parent.origin))
    rx, ry, rz = rot_to_euler_zyx(a)
    sx = math.sqrt(a[0][0] ** 2 + a[1][0] ** 2 + a[2][0] ** 2)
    sy = math.sqrt(a[0][1] ** 2 + a[1][1] ** 2 + a[2][1] ** 2)
    sz = math.sqrt(a[0][2] ** 2 + a[1][2] ** 2 + a[2][2] ** 2)
    return {"tx": t_vec[0], "ty": t_vec[1], "tz": t_vec[2],
            "rx": rx, "ry": ry, "rz": rz,
            "sx": sx, "sy": sy, "sz": sz}


# ---------------------------------------------------------------------------
# emission: adaptive-rate keyframe series
# ---------------------------------------------------------------------------

def _eval_emitted(vals, frame, loop):
    n = len(vals)
    if loop:
        i = int(math.floor(frame)) % n
        d = frame - math.floor(frame)
        return _catmull(d, vals[(i - 1) % n], vals[i], vals[(i + 1) % n],
                        vals[(i + 2) % n])
    i = int(math.floor(frame))
    if i < 0:
        return vals[0]
    if i >= n - 1:
        return vals[-1]
    d = frame - i
    return _catmull(d, vals[max(0, i - 1)], vals[i], vals[i + 1],
                    vals[min(n - 1, i + 2)])


def _series_len(rate, length, loop):
    return (max(2, int(round(length * rate))) if loop
            else int(math.floor(length * rate)) + 2)


def _resample_err(compute_at, vals, rate, loop, length):
    """Worst |emitted_catmull - compute_at| over a dense grid."""
    steps = max(1, int(length * rate * 4))
    worst = 0.0
    for i in range(steps + 1):
        t = i / (rate * 4)
        err = abs(compute_at(t) - _eval_emitted(vals, t * rate, loop))
        worst = max(worst, err)
    return worst


def _unwrap_euler(series):
    """Continuity-adjust a series of euler triplets.

    For each sample, considers the principal triplet and the alternate branch
    (rx+pi, pi-ry, rz+pi), each shifted by +-2pi per axis, and keeps whichever
    is closest to the previously emitted triplet.
    """
    out = [series[0]]
    for rx, ry, rz in series[1:]:
        branches = ((rx, ry, rz),
                    (rx + math.pi, math.pi - ry, rz + math.pi))
        prev = out[-1]
        best = None
        best_d = float("inf")
        for b0, b1, b2 in branches:
            for k0 in (-1, 0, 1):
                for k1 in (-1, 0, 1):
                    for k2 in (-1, 0, 1):
                        c = (b0 + k0 * 2 * math.pi,
                             b1 + k1 * 2 * math.pi,
                             b2 + k2 * 2 * math.pi)
                        d = ((c[0] - prev[0]) ** 2 + (c[1] - prev[1]) ** 2
                             + (c[2] - prev[2]) ** 2)
                        if d < best_d:
                            best_d, best = d, c
        out.append(best)
    return out


def _rot_series_err(triplets, rate, loop, length, rot_at):
    """Worst angular distance (deg) between emitted catmull euler triplet and
    the true rotation at dense times."""
    xs = [t_[0] for t_ in triplets]
    ys = [t_[1] for t_ in triplets]
    zs = [t_[2] for t_ in triplets]
    steps = max(1, int(length * rate * 4))
    worst = 0.0
    for i in range(steps + 1):
        t = i / (rate * 4)
        f = t * rate
        ex = _eval_emitted(xs, f, loop)
        ey = _eval_emitted(ys, f, loop)
        ez = _eval_emitted(zs, f, loop)
        emitted_r = euler_rad_to_rot_zyx(ex, ey, ez)
        true_r = rot_at(t)
        worst = max(worst, rot_angle_deg(
            m_mul(m_transpose(emitted_r), true_r)))
    return worst


def _fmt(v):
    s = f"{v:.6f}"
    return "0" if s == "-0.000000" else s


def _clip_term(clip, rate, values):
    body = ", ".join(_fmt(v) for v in values)
    fn = "keyframeloop" if clip.loop else "keyframe"
    return f"{fn}(superheroes_hl_{clip.clip_id}_t*{rate:.1f}, {body})"


def _tol_for(channel):
    if channel in ROT_CHANNELS:
        return TOL_RAD
    if channel[0] == "t":
        return TOL_PX
    return TOL_SCALE


def _emit_scalar(compute_at, channel, clip):
    for rate in RATES:
        n = _series_len(rate, clip.length_seconds, clip.loop)
        vals = [compute_at(i / rate) for i in range(n)]
        if _resample_err(compute_at, vals, rate, clip.loop,
                         clip.length_seconds) <= _tol_for(channel):
            return rate, vals
    return RATES[-1], vals


def _emit_rotation(rot3_at, rot_mat_at, clip):
    """Emit rx/ry/rz jointly: one shared rate, euler-continuity unwrapped."""
    for rate in RATES:
        n = _series_len(rate, clip.length_seconds, clip.loop)
        triplets = _unwrap_euler([rot3_at(i / rate) for i in range(n)])
        if _rot_series_err(triplets, rate, clip.loop, clip.length_seconds,
                           rot_mat_at) <= math.degrees(TOL_RAD):
            return rate, triplets
    return RATES[-1], triplets


def _emit_channels(model, clip, group, mean_r_inv, is_carrier):
    """All nine emitted channel terms for one part in one clip.

    Returns {channel: term} where term is a keyframe/keyframeloop call or a
    bare rest-pose constant for channels the clip leaves unkeyed.
    """
    keyed = clip.tracks.get(group.name, {})
    terms = {}
    clip_id = clip.clip_id

    def value(ch, t):
        if is_carrier:
            return carrier_channel_value(model, clip, group.name, ch, t,
                                         mean_r_inv)
        return submodel_channel_value(model, clip, group, ch, t)

    # scalar channels: tx/ty/tz (position) and sx/sy/sz (scale)
    channel_of = {"t": "position", "s": "scale"}
    for prefix, bb_channel in channel_of.items():
        for ch in (prefix + "x", prefix + "y", prefix + "z"):
            if is_carrier or bb_channel in keyed:
                compute = lambda t, c=ch: value(c, t)
                rate, vals = _emit_scalar(compute, ch, clip)
                terms[ch] = _clip_term(clip, rate, vals)
            else:
                terms[ch] = _fmt(value(ch, 0.0))

    # rotation triplet
    def rot3(t):
        if is_carrier:
            return carrier_channel_triplet(model, clip, group.name, t,
                                           mean_r_inv)
        return submodel_channel_triplet(model, clip, group, t)

    def rot_mat(t):
        e = rot3(t)
        return euler_rad_to_rot_zyx(*e)

    if is_carrier or "rotation" in keyed:
        rate, triplets = _emit_rotation(rot3, rot_mat, clip)
        for ax, ch in enumerate(ROT_CHANNELS):
            terms[ch] = _clip_term(clip, rate, [tr[ax] for tr in triplets])
    else:
        for ch in ROT_CHANNELS:
            terms[ch] = _fmt(value(ch, 0.0))
    return terms


def blend_expression(self_chan, clip_exprs, fp_gate):
    """lerp(hl_w, <self_chan vanilla value>, <blended authored>)."""
    w_t = "superheroes_hl_takeoff_w"
    w_c = "superheroes_hl_clap_w"
    w_m = "superheroes_hl_milk_w"
    if fp_gate:
        w_c = f"({w_c}*if(is_first_person_hand,0,1))"
        w_m = f"({w_m}*if(is_first_person_hand,0,1))"
    wsum = f"({w_t}+{w_c}+{w_m})"
    terms = []
    if "takeoff" in clip_exprs:
        terms.append(f"{w_t}*{clip_exprs['takeoff']}")
    if "clap" in clip_exprs:
        terms.append(f"{w_c}*{clip_exprs['clap']}")
    if "milk" in clip_exprs:
        terms.append(f"{w_m}*{clip_exprs['milk']}")
    oneshot_sum = "(" + "+".join(terms) + ")" if terms else "0.0"
    oneshot_blend = f"({oneshot_sum}/max({wsum},0.0001))"
    max_w = f"max({w_t},max({w_c},{w_m}))"

    loop_blend = clip_exprs.get("hover", "0.0")
    if "boost" in clip_exprs:
        loop_blend = (f"lerp(superheroes_hl_boost_w,"
                      f"{loop_blend},{clip_exprs['boost']})")

    blended = f"lerp({max_w},{loop_blend},{oneshot_blend})"
    return f"lerp(superheroes_hl_w,{self_chan},{blended})"


def _animations_for_part(model, clip_means, value_owner_name, is_carrier, fp):
    per_channel = {ch: {} for ch in CHANNELS}
    for clip_id, clip in model.clips.items():
        mean_r_inv = m_transpose(clip_means[clip_id])
        terms = _emit_channels(model, clip, model.group(value_owner_name),
                               mean_r_inv, is_carrier)
        for ch, term in terms.items():
            per_channel[ch][clip_id] = term
    return [{f"{value_owner_name if is_carrier else 'hl_' + value_owner_name}.{ch}":
             blend_expression(
                 f"{value_owner_name if is_carrier else 'hl_' + value_owner_name}.{ch}",
                 clips, fp)}
            for ch, clips in per_channel.items()]


# ---------------------------------------------------------------------------
# jem construction
# ---------------------------------------------------------------------------

_UV_FIELDS = {"north": "uvNorth", "south": "uvSouth", "east": "uvEast",
              "west": "uvWest", "up": "uvUp", "down": "uvDown"}


def _mc_box_from_element(el, group, slim):
    """Element -> EMF box dict: pivot-relative mc coords + per-face uv."""
    fr, to = el["from"], el["to"]
    o = group.origin
    fx, fy, fz = fr[0] - o[0], fr[1] - o[1], fr[2] - o[2]
    tx, ty, tz = to[0] - o[0], to[1] - o[1], to[2] - o[2]
    # slim: shave 1px off the OUTER x edge (x- side on the right, x+ on left)
    if slim and group.name in ("right_arm", "right_forearm"):
        fx += 1.0
    if slim and group.name in ("left_arm", "left_forearm"):
        tx -= 1.0
    x0, y0, z0 = fx, -ty, fz
    w, h, d = tx - fx, ty - fy, tz - fz
    box = {"coordinates": [x0, y0, z0, w, h, d]}
    faces = el.get("faces", {})
    for face, field in _UV_FIELDS.items():
        f = faces.get(face)
        if f and "uv" in f:
            box[field] = [float(x) for x in f["uv"]]
    return box


def _group_elements(model, group, slim):
    out = []
    for eid in group.elements:
        el = model.elements_by_uuid.get(eid)
        if el is None or el.get("type", "cube") != "cube":
            continue
        out.append(_mc_box_from_element(el, group, slim))
    return out


def _fmt_val(v):
    return round(v, 6)


def _submodel_entry(model, group, clip_means, slim):
    parent = group.parent
    entry = {
        "id": f"hl_{group.name}",
        "translate": [_fmt_val(v)
                      for v in vec_bb_to_mc(v_sub(group.origin, parent.origin))],
        "rotate": [0.0, 0.0, 0.0],
        "boxes": _group_elements(model, group, slim),
        "animations": _animations_for_part(
            model, clip_means, group.name, is_carrier=False,
            fp=group.name in FP_GATED),
    }
    subs = [_submodel_entry(model, c, clip_means, slim)
            for c in group.children if c.name not in CARRIER_PARTS]
    if subs:
        entry["submodels"] = subs
    if group.name in MILK_TEXTURE_PARTS:
        entry["texture"] = MILK_TEXTURE
        entry["textureSize"] = [64, 64]
        entry["animations"].append({f"hl_{group.name}.visible": PROP_VISIBLE})
    if group.name in ATTACHMENTS:
        entry["attachments"] = ATTACHMENTS[group.name]
    return entry


def build_jem(model, slim=False):
    clip_means = {
        cid: mean_rotation(sample_root_rotation(model, clip))
        for cid, clip in model.clips.items()
    }
    models = []
    for carrier in CARRIER_PARTS:
        group = model.group(carrier)
        entry = {
            "part": carrier,
            "id": f"hl_{carrier}",
            "attach": False,
            "translate": [0.0, 0.0, 0.0],
            "rotate": [0.0, 0.0, 0.0],
            "boxes": _group_elements(model, group, slim),
            "animations": _animations_for_part(
                model, clip_means, carrier, is_carrier=True,
                fp=carrier in FP_GATED),
        }
        subs = [_submodel_entry(model, c, clip_means, slim)
                for c in group.children if c.name not in CARRIER_PARTS]
        if subs:
            entry["submodels"] = subs
        models.append(entry)
    for hide in HIDDEN_PARTS:
        models.append({
            "part": hide,
            "id": f"hl_hide_{hide}",
            "attach": True,
            "animations": [{f"{hide}.visible": "1=0"}],
        })
    return {"textureSize": [64, 64], "models": models}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("bbmodel")
    ap.add_argument("--slim", action="store_true",
                    help="emit only player_slim.jem")
    ap.add_argument("--classic", action="store_true",
                    help="emit only player.jem")
    ap.add_argument("--out", default=JEM_DIR)
    args = ap.parse_args()

    model = Model(args.bbmodel)

    if model.milk_png:
        os.makedirs(os.path.dirname(TEX_OUT), exist_ok=True)
        with open(TEX_OUT, "wb") as f:
            f.write(model.milk_png)
        print(f"wrote {TEX_OUT} ({len(model.milk_png)} bytes)")

    os.makedirs(args.out, exist_ok=True)
    if not args.slim:
        jem = build_jem(model, slim=False)
        with open(os.path.join(args.out, "player.jem"), "w") as f:
            json.dump(jem, f, indent=1)
        print("wrote player.jem")
    if not args.classic:
        jem = build_jem(model, slim=True)
        with open(os.path.join(args.out, "player_slim.jem"), "w") as f:
            json.dump(jem, f, indent=1)
        print("wrote player_slim.jem")

    # per-clip root-mean rotation in mc flavor, for flight.json
    for cid, clip in model.clips.items():
        mean_r = mean_rotation(sample_root_rotation(model, clip))
        rx, ry, rz = rot_to_euler_zyx(conj_bb_to_mc_mat(mean_r))
        print(f"{cid}: root mean rotation (mc deg) "
              f"pitch={math.degrees(rx):.3f} yaw={math.degrees(ry):.3f} "
              f"roll={math.degrees(rz):.3f}")


if __name__ == "__main__":
    main()
