"""Contract tests for the Homelander EMF jem bake (plan §7 stage 1 step 2).

Every assertion the plan names lives here; the bake itself lives in
``bake_jem.py`` beside this file. Run with
``python3 -m unittest art-source/homelander/emf/test_bake_jem.py``.
"""

import json
import math
import os
import re
import unittest

import bake_jem

MODEL_PATH = os.path.join(os.path.dirname(__file__), "Homelander_All_Animations.bbmodel")

_CLIP_TERM = re.compile(
    r"(keyframe|keyframeloop)\(superheroes_hl_([a-z]+)_t\*([0-9.]+),\s*([^)]*)\)")


def _num(s):
    return float(s)


class EmfExpression:
    """EMF keyframe()/keyframeloop() semantics, mirrored for assertions."""

    @staticmethod
    def keyframe_args(expr):
        """Extract the FIRST clip term inside a blend expression."""
        m = _CLIP_TERM.search(expr)
        assert m is not None, f"no keyframe term in {expr[:80]}..."
        fn, clip_id, rate, body = m.groups()
        vals = [_num(x) for x in body.split(",")]
        return fn, clip_id, float(rate), vals

    @staticmethod
    def catmull(d, p0, p1, p2, p3):
        return 0.5 * ((2 * p1) + (p2 - p0) * d + (2 * p0 - 5 * p1 + 4 * p2 - p3) * d * d
                      + (-p0 + 3 * p1 - 3 * p2 + p3) * d * d * d)

    @classmethod
    def eval(cls, fn, values, frame):
        if fn == "keyframeloop":
            n = len(values)
            idx = int(math.floor(frame)) % n
            d = frame - math.floor(frame)
            p0 = values[(idx - 1) % n]
            p3 = values[(idx + 2) % n]
            return cls.catmull(d, p0, values[idx], values[(idx + 1) % n], p3)
        idx = int(math.floor(frame))
        if idx < 0:
            return values[0]
        if idx >= len(values) - 1:
            return values[-1]
        d = frame - idx
        p0 = values[max(0, idx - 1)]
        p3 = values[min(len(values) - 1, idx + 2)]
        return cls.catmull(d, p0, values[idx], values[idx + 1], p3)


def _clip_terms(expr):
    """All clip terms inside a blend expression: {clip_id: (fn, rate, vals)}."""
    out = {}
    for m in _CLIP_TERM.finditer(expr):
        fn, clip_id, rate, body = m.groups()
        out[clip_id] = (fn, float(rate), [_num(x) for x in body.split(",")])
    return out


def _truth_fn(model, clip_means, part_id, channel):
    """Dense-eval reference for one emitted channel."""
    carrier = part_id if part_id in bake_jem.CARRIER_PARTS else None
    group = model.group(part_id[3:] if part_id.startswith("hl_") else part_id)

    def fn(clip, t):
        if carrier:
            return bake_jem.carrier_channel_value(
                model, clip, carrier, channel, t,
                bake_jem.m_transpose(clip_means[clip.clip_id]))
        return bake_jem.submodel_channel_value(model, clip, group, channel, t)

    return fn


class BakeContractTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.model = bake_jem.load(MODEL_PATH)
        cls.jem = bake_jem.build_jem(cls.model, slim=False)
        cls.jem_slim = bake_jem.build_jem(cls.model, slim=True)
        cls.clip_means = {
            cid: bake_jem.mean_rotation(bake_jem.sample_root_rotation(cls.model, clip))
            for cid, clip in cls.model.clips.items()
        }

    # -- composition ------------------------------------------------------

    def test_ancestor_chains_never_leg_through_body(self):
        for side in ("right", "left"):
            chain = bake_jem.ancestor_chain(self.model, self.model.group(f"{side}_leg"))
            self.assertEqual([g.name for g in chain], ["root", f"{side}_leg"])
        self.assertEqual(
            [g.name for g in bake_jem.ancestor_chain(self.model, self.model.group("head"))],
            ["root", "body", "head"])
        self.assertEqual(
            [g.name for g in bake_jem.ancestor_chain(self.model, self.model.group("right_arm"))],
            ["root", "body", "right_arm"])

    def test_identity_parents_leave_child_transform(self):
        child = bake_jem.Affine.translation(1.0, -2.0, 3.0)
        composed = bake_jem.Affine.identity().compose(child)
        self.assertTrue(composed.almost_equal(child, 1e-9))

    def test_mean_rotation_removal_leaves_residual(self):
        # the residual rotation stays in the carrier channels: bounded but
        # nonzero for clips whose root rotation varies over time
        for clip_id, clip in self.model.clips.items():
            samples = bake_jem.sample_root_rotation(self.model, clip)
            mean_r = self.clip_means[clip_id]
            residuals = [
                bake_jem.rot_angle_deg(bake_jem.m_mul(bake_jem.m_transpose(mean_r), r))
                for r in samples
            ]
            self.assertLessEqual(max(residuals), 5.0,
                                 f"{clip_id}: residual {max(residuals)} too large")
            self.assertTrue(math.isfinite(bake_jem.rot_angle_deg(mean_r)))

    def test_pivot_compensation_reproduces_authored(self):
        # Re-applying RotAboutFeet(meanR) to a baked carrier transform must
        # reproduce the authored world transform. In bb space the feet are the
        # world origin, so the re-application is a plain rotation about 0.
        for clip_id, clip in self.model.clips.items():
            mean_r = self.clip_means[clip_id]
            mean_r_inv = bake_jem.m_transpose(mean_r)
            reapply = bake_jem.rot_about_point(mean_r, (0.0, 0.0, 0.0))
            for t in (0.0, clip.length_seconds * 0.5, clip.length_seconds * 0.9):
                authored = bake_jem.carrier_affine(self.model, clip, "head", t)
                baked = bake_jem.baked_carrier_affine(self.model, clip, "head", t,
                                                      mean_r_inv)
                reapplied = reapply.compose(baked)
                self.assertTrue(
                    authored.almost_equal(reapplied, 0.02),
                    f"{clip_id}@{t}: pivot compensation off",
                )

    def test_zyx_euler_roundtrip(self):
        for rx, ry, rz in [(10.0, -20.0, 30.0), (-86.0, 5.0, 0.0), (0.1, 89.0, -0.4),
                           (45.0, 12.0, -170.0)]:
            r = bake_jem.euler_deg_to_rot_zyx(rx, ry, rz)
            ex, ey, ez = bake_jem.rot_to_euler_zyx(r)
            r2 = bake_jem.euler_rad_to_rot_zyx(ex, ey, ez)
            self.assertLess(
                bake_jem.rot_angle_deg(bake_jem.m_mul(bake_jem.m_transpose(r), r2)),
                0.01, f"roundtrip {rx},{ry},{rz}")

    # -- resample fidelity against the shipped expressions -------------------

    def test_resample_error_every_shipped_channel(self):
        """Every keyframe term in the jems reproduces the authored curve within
        the plan bound (0.5 deg rotation / 0.05 px translation / 0.005 scale)."""
        tol = {"r": math.radians(0.5), "t": 0.05, "s": 0.005}
        for jem in (self.jem, self.jem_slim):
            for part in bake_jem.all_parts(jem):
                part_id = part.get("part") or part.get("id", "")[3:]
                name = part.get("part") or part.get("id")
                owner = part.get("part") if part.get("part") in bake_jem.CARRIER_PARTS \
                    else part.get("id", "")[3:]
                for anim in part.get("animations", []):
                    for key, expr in anim.items():
                        chan = key.rsplit(".", 1)[-1]
                        if chan not in bake_jem.CHANNELS:
                            continue
                        for clip_id, (fn, rate, vals) in _clip_terms(expr).items():
                            clip = self.model.clips[clip_id]
                            truth = _truth_fn(self.model, self.clip_means,
                                              owner, chan)
                            steps = int(clip.length_seconds * rate * 4)
                            worst = 0.0
                            for i in range(steps + 1):
                                t = i / (rate * 4)
                                got = EmfExpression.eval(fn, vals, t * rate)
                                worst = max(worst, abs(truth(clip, t) - got))
                            self.assertLessEqual(
                                worst, tol[chan[0]],
                                f"{name}.{chan} clip={clip_id} rate={rate} "
                                f"err={worst}")

    def test_keyframe_uses_frame_index_not_seconds(self):
        expr = bake_jem.channel_expression(self.jem, "head", "rx")
        self.assertIsNotNone(expr)
        self.assertRegex(expr, r"superheroes_hl_\w+_t\*[0-9.]+")

    # -- units -------------------------------------------------------------

    def test_radians_in_rx_channels_degrees_in_rotate(self):
        expr = bake_jem.channel_expression(self.jem, "right_leg", "rx")
        _, _, _, values = EmfExpression.keyframe_args(expr)
        # leg rx reaches ~-86deg in boost => emitted values are radians (< 2pi)
        self.assertTrue(all(abs(v) < math.pi * 2 for v in values))
        part = bake_jem.jem_part(self.jem, "right_leg")
        self.assertTrue(all(abs(v) < 360.0 for v in part.get("rotate", [0])))

    def test_visible_expressions_are_boolean_form(self):
        for part in bake_jem.all_parts(self.jem):
            for anim in part.get("animations", []):
                for key, expr in anim.items():
                    if key.split(".")[-1] not in ("visible", "visible_boxes"):
                        continue
                    stripped = expr.replace(" ", "")
                    self.assertNotIn(stripped, ("0", "1", "0.0", "1.0"),
                                     f"{part.get('id')}.{key} numeric literal: {expr}")
                    self.assertTrue(
                        any(tok in stripped for tok in
                            ("=0", "=1", "is_first_person_hand", "&&", "||",
                             "true", "false")),
                        f"{part.get('id')}.{key} not a boolean expression: {expr}")

    # -- jem contract -------------------------------------------------------

    def test_jem_structure(self):
        for jem in (self.jem, self.jem_slim):
            parts = {p.get("part"): p for p in jem["models"] if p.get("part")}
            for vanilla in ("head", "body", "right_arm", "left_arm",
                            "right_leg", "left_leg"):
                self.assertIn(vanilla, parts, f"missing carrier {vanilla}")
            self.assertNotIn("cloak", parts)
            self.assertNotIn("texture", jem, "player.jem must not pin a texture")
            text = json.dumps(jem)
            self.assertNotIn("emf_lab", text)
            self.assertNotIn("nan", text.lower())
            for clip in ("hover", "takeoff", "boost", "clap", "milk"):
                self.assertIn(f"superheroes_hl_{clip}_t", text,
                              f"{clip} time var missing")
            for wvar in ("w", "takeoff_w", "clap_w", "milk_w", "boost_w"):
                self.assertIn(f"superheroes_hl_{wvar}", text,
                              f"var {wvar} missing")

    def test_custom_ids_present(self):
        ids = {p.get("id") for p in bake_jem.all_parts(self.jem)}
        for expected in ("hl_head", "hl_body", "hl_right_arm", "hl_left_arm",
                         "hl_right_leg", "hl_left_leg", "hl_mouth_open",
                         "hl_right_forearm", "hl_left_forearm",
                         "hl_right_shin", "hl_right_foot", "hl_right_knee",
                         "hl_left_shin", "hl_left_foot", "hl_left_knee",
                         "hl_milk_bottle", "hl_milk_cap"):
            self.assertIn(expected, ids)

    def test_hidden_overlay_parts_use_boolean_visible(self):
        parts = {p.get("part"): p for p in self.jem["models"]}
        for hide in ("headwear", "jacket", "left_sleeve", "right_sleeve",
                     "left_pants", "right_pants"):
            self.assertIn(hide, parts)
            anims = parts[hide]["animations"]
            self.assertTrue(any(f"{hide}.visible" in a and a[f"{hide}.visible"] == "1=0"
                                for a in anims))

    def test_slim_arm_boxes_match_vanilla_slim(self):
        # vanilla slim arm is 3px wide; classic is 4px. Every arm box must be
        # shaved to width 3 (right side shaves the x- edge, left the x+).
        slim_arm = bake_jem.jem_part(self.jem_slim, "right_arm")
        widths = [b["coordinates"][3] for b in slim_arm["boxes"]]
        self.assertTrue(widths and all(w == 3.0 for w in widths),
                        f"slim right_arm widths {widths}")
        fore = next(p for p in slim_arm.get("submodels", [])
                    if p.get("id") == "hl_right_forearm")
        widths = [b["coordinates"][3] for b in fore["boxes"]]
        self.assertTrue(widths and all(w == 3.0 for w in widths),
                        f"slim right_forearm widths {widths}")

    def test_no_molang_keyframes(self):
        for clip in self.model.clips.values():
            for animator in clip.animators:
                for kf in animator.get("keyframes", []):
                    for point in kf["data_points"]:
                        for v in point.values():
                            bake_jem.parse_number(v)  # raises on Molang


if __name__ == "__main__":
    unittest.main()
