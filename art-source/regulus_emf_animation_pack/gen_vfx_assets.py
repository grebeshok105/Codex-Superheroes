#!/usr/bin/env python3
"""Regulus VFX asset generator (Task 9).

Emits the quasar emitter recipes and vfx tuning JSONs consumed by
``client/hero/regulus/fx`` (Visual Core seam) and the server-side
``RegulusVfxIds`` sends. Idempotent — writes are deterministic.

Run: ``python3 art-source/regulus_emf_animation_pack/gen_vfx_assets.py``
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
Q = ROOT / "src/main/resources/assets/superheroes/quasar"
VFX = ROOT / "src/main/resources/assets/superheroes/vfx/regulus"
SPRITES = "superheroes:textures/particle/"

GOLD = "superheroes:color/homelander_gold"
DUST = "superheroes:color/homelander_dust"
FROST = "superheroes:color/regulus_frost"
WHITE = "superheroes:color/regulus_white"


def dump(path: Path, obj) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2, sort_keys=False) + "\n")


def emitter(name: str, lifetime: int, count: int, rate: int = 1) -> None:
    dump(Q / "emitters" / f"{name}.json", {
        "max_lifetime": lifetime,
        "loop": False,
        "rate": rate,
        "count": count,
        "emitter_settings": {
            "shape": f"superheroes:{name}",
            "particle_settings": f"superheroes:{name}",
        },
        "particle_data": f"superheroes:{name}",
    })


def shape(name: str, form: str, dims, surface: bool) -> None:
    dump(Q / "modules/emitter/shape" / f"{name}.json", {
        "shape": form,
        "dimensions": dims,
        "rotation": [0, 0, 0],
        "from_surface": surface,
    })


def particle(name: str, direction, speed: float, life: int, life_var: float,
             size: float, size_var: float) -> None:
    dump(Q / "modules/emitter/particle" / f"{name}.json", {
        "random_speed": True,
        "random_size": True,
        "random_lifetime": True,
        "initial_direction": direction,
        "random_initial_direction": True,
        "random_initial_rotation": True,
        "particle_size_variation": size_var,
        "particle_lifetime": life,
        "particle_lifetime_variation": life_var,
        "particle_speed": speed,
        "base_particle_size": size,
    })


def particle_data(name: str, sprite: str, color: str, additive: bool,
                  collide: bool = False) -> None:
    dump(Q / "modules/particle_data" / f"{name}.json", {
        "sprite_data": {"sprite": f"{SPRITES}{sprite}"},
        "render_style": "billboard",
        "additive": additive,
        "init_modules": [],
        "update_modules": [],
        "collision_modules": [],
        "forces": [],
        "render_modules": [color],
        "face_velocity": False,
        "velocity_stretch_factor": 0.0,
        "should_collide": collide,
    })


def color(name: str, rgb_points, alpha_points) -> None:
    dump(Q / "modules/render/color" / f"{name}.json", {
        "module": "color",
        "interpolant": "q.agePercent",
        "gradient": {
            "rgb_points": [{"percent": p, "color": c} for p, c in rgb_points],
            "alpha_points": [{"percent": p, "alpha": a} for p, a in alpha_points],
        },
    })


def recipe(name: str, lifetime: int, count: int, form: str, dims, surface: bool,
           direction, speed: float, life: int, life_var: float,
           size: float, size_var: float, sprite: str, col: str, additive: bool) -> None:
    emitter(name, lifetime, count)
    shape(name, form, dims, surface)
    particle(name, direction, speed, life, life_var, size, size_var)
    particle_data(name, sprite, col, additive)


def main() -> None:
    # Shared regulus palettes.
    color("regulus_frost",
          [(0, [0.92, 0.97, 1.0]), (0.5, [0.65, 0.82, 0.95]), (1, [0.4, 0.55, 0.8])],
          [(0, 0.9), (0.6, 0.6), (1, 0)])
    color("regulus_white",
          [(0, [1.0, 1.0, 1.0]), (1, [0.7, 0.72, 0.8])],
          [(0, 1), (0.5, 0.7), (1, 0)])

    # --- continuous-channel emitters (re-emitted each client UPDATE) ---------
    # Golden thread caster->victim (greed magnet).
    recipe("regulus_greed_thread", 8, 3, "point", [0, 0, 0], False,
           [0.0, 0.3, 0.0], 0.15, 10, 4.0, 0.05, 0.02, "soul_spark.png", GOLD, True)
    # Frost ring at the stasis-dome base.
    recipe("regulus_stasis_ring", 12, 16, "cylinder", [0.5, 0.15, 0.5], True,
           [0.0, 0.5, 0.0], 0.35, 16, 6.0, 0.1, 0.04, "moonveil_particle.png", FROST, True)
    # Lion-heart dome shimmer around the player.
    recipe("regulus_lion_heart_shimmer", 10, 6, "sphere", [2.6, 2.6, 2.6], True,
           [0.0, 0.2, 0.0], 0.12, 14, 6.0, 0.07, 0.03, "soul_spark.png", FROST, True)
    # Weak golden pulse above heart bearers.
    recipe("regulus_heart_pulse", 8, 4, "sphere", [0.4, 0.2, 0.4], False,
           [0.0, 0.4, 0.0], 0.25, 12, 5.0, 0.06, 0.03, "soul_spark.png", GOLD, True)

    # --- one-shot emitters ----------------------------------------------------
    # Evangelium golden glow (ritual start + major + deactivation).
    recipe("regulus_evangelium_gold", 14, 24, "sphere", [0.8, 1.0, 0.8], False,
           [0.0, 0.5, 0.0], 0.45, 18, 7.0, 0.09, 0.04, "soul_spark.png", GOLD, True)
    # Debris lift during the kick cast.
    recipe("regulus_debris_lift", 14, 22, "hemisphere", [2.0, 0.6, 2.0], False,
           [0.0, 0.8, 0.0], 0.8, 16, 6.0, 0.22, 0.12, "white_boom_2.png", DUST, False)
    # Debris volley impact: dust cone + shard burst.
    recipe("regulus_debris_impact", 16, 40, "hemisphere", [1.5, 0.4, 1.5], False,
           [0.0, 0.4, 0.0], 1.4, 14, 6.0, 0.3, 0.16, "white_boom_4.png", DUST, False)
    # Counter-slam speed lines.
    recipe("regulus_slam_lines", 8, 30, "sphere", [0.3, 0.3, 0.3], True,
           [0.0, -0.3, 0.0], 1.8, 8, 3.0, 0.12, 0.05, "white_boom_0.png", WHITE, True)
    # Hard-landing radial dust.
    recipe("regulus_landing_shock", 18, 36, "hemisphere", [2.5, 0.3, 2.5], True,
           [0.0, 0.25, 0.0], 1.2, 18, 7.0, 0.4, 0.18, "white_boom_8.png", DUST, False)

    # --- vfx tuning JSONs (channel params; one-shots carry no params) ---------
    dump(VFX / "greed_magnet.json", {
        "chargeTicks": 2,
        "releaseTicks": 6,
        "coreWidth": 0.02,
        "glowWidth": 0.07,
        "coreColor": "#FFFFE870",
        "glowColor": "#60C89000",
        "noise": 0.4,
        "victimDistortionRadius": 0.5,
        "victimDistortionStrength": 0.25,
    })
    dump(VFX / "greed_stasis.json", {
        "domeRadius": 8.0,
        "chargeTicks": 10,
        "releaseTicks": 8,
        "distortionRadius": 4.4,
        "distortionStrength": 0.35,
        "lightColor": "#A8D8E0",
        "lightRadius": 6.4,
        "lightBrightness": 0.4,
    })
    dump(VFX / "lion_heart_dome.json", {
        "chargeTicks": 6,
        "releaseTicks": 10,
        "domeRadius": 2.6,
        "distortionStrength": 0.3,
    })
    dump(VFX / "heart_pulse.json", {
        "chargeTicks": 8,
        "releaseTicks": 6,
    })

    print("regulus vfx assets written")


if __name__ == "__main__":
    main()
