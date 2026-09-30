# Homelander EMF animation core — design spec

Source of truth for the "Homelander Visual Core v2" work: the player model and
all Homelander player animations move to EMF (Entity Model Features) as a real
dependency, driven by the converted bbmodel assets.

## Assets (generated, do not hand-edit)

`src/main/resources/assets/superheroes/emf/homelander/` — produced by
`art-source/homelander/convert_bbmodel.py` from
`art-source/homelander/Homelander_All_Animations.bbmodel` (authoritative source).

- `model.json` — `codex-emf-model/1`: bone tree (name, parent, pivot in model
  pixels), cubes (origin/size in px, per-face uv + texture index).
  Textures: index 0 = `entity/hero/homelander.png`, index 1 =
  `entity/hero/homelander_milk.png`.
- `<clip>.json` — `emf-keyframe-clip/1`: uniform 60 fps tracks per bone and
  channel (`rotation` deg, `position` px, `scale` mul), resampled with
  catmull-rom. `loop`, `length_seconds`, `frame_count`.
- `emf_lab_expressions.json` — the author's original EMF expressions (reference
  for semantic intent; hover especially).
- `showcase.json` — reference only, MUST NOT be registered as a runtime state.

### Bone tree

```
root
├── body
│   ├── head
│   │   └── mouth_open            (prop: hidden by default)
│   ├── right_arm
│   │   └── right_forearm
│   │       └── milk_bottle       (prop)
│   │           └── milk_cap      (prop)
│   └── left_arm
│       └── left_forearm
├── right_leg
│   ├── right_shin
│   │   └── right_foot
│   └── right_knee
└── left_leg
    ├── left_shin
    │   └── left_foot
    └── left_knee
```

`prop_bones` (`milk_bottle`, `milk_cap`, `mouth_open`) render at scale 0 unless a
playing clip animates them (the milk clip carries explicit scale channels).

## Runtime contract

- EMF (`entity-model-features` + its required `entitytexturefeatures`) is a
  normal mod dependency for Fabric 1.21.1 (`maven.modrinth:entity-model-features`).
- EMF is the animation engine: clip tracks become `keyframe()/keyframeloop()`
  expressions, compiled/evaluated by EMF (`ASMAnimationHandler`,
  `EMFAnimationHandler.AnimLineData`, `EMFAnimationEntityContext`,
  `EMFModelPart*`, `EMFAnimationApi`). We do NOT write a second sampler.
- Per-player state = registered EMF variables (`var.<clip>_w` blend weight 0..1,
  `var.<clip>_t` clip time in frames, lean vars) supplied by our code; each
  channel's expression is the weighted sum over the states that drive it, so
  EMF does the crossfade math.
- `PlayerAnimator`/six-bone `PlayerPoseApplier` path is replaced for Homelander;
  other heroes unaffected. Legacy `player_animations/homelander/*` clips die
  with the migration (laser/roar/etc. port mechanically or are dropped per spec).
- Gameplay flight physics (`mechanic/flight/*`) is untouched — this is
  presentation only. `ClientFlightState`/`FlightStateS2CPayload` keep driving
  phase; direction-vs-look math happens client-side from entity velocity/yaw.

## Flight state machine

- `TAKEOFF` (0.8 s, one-shot, hold) plays fully on ground launch → crossfades
  into `HOVER`. Never snaps.
- `HOVER` (3.2 s, loop) is the flight idle — full body micro-motion, all bones.
- `BOOST` (2.6 s, loop) engages only on fast FORWARD movement (vs look yaw);
  HOVER↔BOOST crossfades smoothly.
- Procedural lean on top of HOVER/BOOST via EMF vars: forward → pitch,
  left/right → roll bank, up/down → small pitch, all half-life smoothed.
- BACKWARD RULE: moving backward vs look → never BOOST, stays HOVER with a
  small smoothed backward lean regardless of speed.

## Wave ownership

| Wave | Scope | Owns |
|------|-------|------|
| A | EMF dep+model+runtime, flight states, lean, old-anim removal | `client/core/anim`, `client/core/flight`, `client/mixin/Player*`, `hero/homelander` model/skin wiring, `emf/homelander/*` semantics |
| C | laser loop sound/fade-out, damage cadence, smaller impact | `EyeLasersAbility`, `EyeLaserChannel`, `vfx/homelander/laser.json`, `homelander_laser_impact` quasar jsons, `HomelanderSounds` laser entries |
| B | hand clap (contact-frame sync) + milk drink prop bones | `HandClapAbility`, `ClapFx`, `MilkBottleItem`, `HomelanderFx` milk/clap paths, clip triggers on top of A's API |
| D | trail smoothing, speed rings, landing sound, F5 camera, burn marks | `FlightFx`/`TrailPattern`, `HomelanderSounds.FLIGHT_LAND` + landing trigger, camera mixin, `LaserBurnMarks` (block-hit hook in `EyeLasersAbility`) |

C and D both touch `EyeLasersAbility` — coordinate via the integration branch
(C lands first; D builds burns in a separate class with a narrow call site).
