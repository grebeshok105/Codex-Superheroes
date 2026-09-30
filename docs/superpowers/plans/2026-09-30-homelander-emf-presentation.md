# Homelander EMF Presentation: Audit and Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan stage by stage. Steps use checkbox (`- [ ]`) syntax. Each stage is one PR. Do not start a stage before the previous stage's PR is merged or explicitly approved by the user.

**Goal:** Move Homelander's player-model presentation onto Entity Model Features (EMF) using the authored `Homelander_All_Animations.bbmodel` (HOVER, TAKEOFF, BOOST, HAND CLAP, MILK DRINK), and fix the surrounding presentation (directional flight transforms, F5 framing, continuous laser audio, laser damage cadence, smaller impact, persistent burn marks, soft contrail, landing audio) without changing other heroes and without changing gameplay except where this plan names an explicit, justified change.

**Architecture (one paragraph):** Server gameplay stays authoritative and unchanged in shape (`FlightController`, abilities, `VfxFx` payloads). A new Homelander-only client state (`HomelanderPoseState`) turns already-synced data (flight state, VFX events, render-interpolated velocity) into a handful of smoothed scalars (clip clocks and blend weights). Codex registers those scalars as EMF animation variables through `EMFAnimationApi`. A generated `player.jem` / `player_slim.jem` (shipped in the mod jar under the EMF-only path) contains the authored geometry and the authored clips as sampled `keyframe(...)` tables and blends them over the vanilla pose with those weights. The legacy `PlayerAnimator` pipeline stays for other heroes only; once Homelander is EMF-owned, Homelander never uses `PlayerAnimator` again. All legacy Homelander animation resources and all Homelander `PlayerAnimator.play/stop` calls are removed in Stage 1. Abilities that do not yet have a new authored EMF animation (laser, iron fists, roar, sun charge, landing) temporarily have no player animation rather than falling back to the old clips. Whole-body pitch/bank stays in `PlayerRendererMixin` (single owner). VFX anchors read a per-entity snapshot of the EMF-animated vanilla parts.

**Tech stack:** Java 21, Minecraft 1.21.1 (Mojang mappings), Fabric Loader 0.19.2, Fabric API 0.116.12+1.21.1, Veil 4.1.2 (Quasar), EMF `3.3.9-fabric-1.21`, ETF `7.2.4-fabric-1.21`, JUnit 5, Fabric GameTest, ArchUnit, Python 3 (offline bake script only).

**Baseline:** PR #138 head (`2dc02edc`, branch `devin/1790691307-homelander-vfx-fix`). PR #140 (branch `devin/1790767119-homelander-emf`) is a failed experiment: do not merge from it, cherry-pick from it, or read it for design.

---

## 0. Global constraints (apply to every stage)

- `AGENTS.md` rules: server-authoritative gameplay; client-only code under `src/client`; typed payloads (`CustomPacketPayload` + `StreamCodec`) registered in `core/net/CoreNetworking`; `en_us.json` and `ru_ru.json` edited together; never hand-edit `src/main/generated/`; `SESSION.md` updated at the end of each PR; `mod_version` in `gradle.properties` bumped deliberately for every delivered jar.
- Hero-agnostic core: nothing under `client/core/**`, `core/**`, `mechanic/**` may reference `hero.homelander` (ArchUnit enforces it). All EMF-API calls live in two places only: `client/core/emf/**` (hero-agnostic bridge) and `client/hero/homelander/emf/**` (Homelander variables). Both only run when `FabricLoader.getInstance().isModLoaded("entity_model_features")`.
- No global migration: other heroes keep `PlayerAnimator`, `FlightPresentation`, `FlightPoseTracker` exactly as today. Diffs to shared classes must be additive (new method, new optional parameter via overload) and must keep existing behaviour byte-for-byte for non-EMF entities.
- No in-game verification by agents (repo rule `codex-superheroes-no-ingame-testing`). Every "in-game verification" block below is a checklist for the user; the PR must list it as "unverified: user will check".
- Finishing gate for every PR: `./gradlew qualityGate --no-daemon` (fast loop: `./gradlew test --no-daemon`, `./gradlew assemble --no-daemon`).
- Homelander must not use `PlayerAnimator` after Stage 1. Delete every legacy Homelander `.animation.json` resource and remove every Homelander `PlayerAnimator.play/stop/sample` dependency that exists only to drive those clips. There is no legacy Homelander animation fallback. The `.bbmodel` is the only source of truth for the new Homelander motion. Other heroes may continue using `PlayerAnimator`.
- Performance: no per-frame allocation in the variable suppliers, in the pose-state update, in the anchor cache or in trail/burn-mark rendering; every collection bounded.

---

## 1. Architecture audit (state at baseline `2dc02edc`)

### 1.1 Shared client animation pipeline (legacy, six-bone)

| Class (`src/client/java/io/github/grebeshok105/codex/client/...`) | Role | Used by other heroes | Fate |
|---|---|---|---|
| `core/anim/AnimationLibrary` | Reload listener, loads `assets/<ns>/player_animations/**/*.animation.json` | yes | keep, untouched |
| `core/anim/BedrockAnimationParser` | Bedrock JSON → `AnimationClip` | yes | keep, untouched |
| `core/anim/AnimationClip`, `BoneTrack`, `PoseSample` | Data types (six vanilla bones, deg + px) | yes | keep, untouched |
| `core/anim/PlayerAnimator` | Per-entity `BASE`/`ACTION` lanes, crossfade, `play/stop/sample/tick/reset` | yes | keep for other heroes; after Stage 1 Homelander must never play/sample any legacy clip through it |
| `core/anim/PlayerPoseApplier` | Applies `PoseSample` to `HumanoidModel` parts, `resetDrivenLimbs`, `renderedRotationDeg` | yes | keep, untouched |
| `mixin/PlayerModelPoseMixin` | `setupAnim` tail: restore, static flight legs, Think Mark pose, apply `PlayerAnimator.sample`, copy to overlay parts | yes (global) | add one early-return gate for EMF-owned entities (Stage 1) |
| `mixin/PlayerRendererMixin` | Whole-body flight pitch/roll on the `PoseStack` (`Axis.XP -pitch`, `Axis.ZP -roll`) | yes | keep as the only owner of whole-body rotation; add anchor snapshot hook (Stage 1) |
| `core/flight/FlightPresentation` (record), `FlightPresentations` (registry) | Hero id → clips, trail/boost effect ids, sounds | Homelander is the only registrant today, but the seam is shared | keep the record shape; Homelander keeps registering it for sounds/trail; clip playback is skipped for EMF owners (Stage 1) |
| `core/flight/FlightPoseTracker` | Per-entity render velocity estimate, clip crossfade, loop sound, trail/boost start, smoothed `FlightBodyTransform` | shared | keep; add EMF-owner skip for clip playback and a directional transform path (Stage 5) |
| `core/flight/FlightPoseMath` | `target(phase, velocity, yawRate, params)` and `step(...)` (half-life smoothing, frame-rate independent) | shared | keep `target`; add a pure `directional(...)` overload (Stage 5). PR #138 removed the descent pitch inversion: do not reintroduce it |
| `ClientFlightState` | Synced `active/mode/phase/horizontalSpeed` + presentation ownership | shared | keep, untouched |
| `fx/FlightTrailManager` | Legacy END_ROD/CLOUD trail, bypassed when presentation-owned | yes | keep, untouched |
| `core/vfx/anchor/HumanoidAnchors` | Eye/body points from vanilla offsets + body yaw + head rot + flight tilt | shared | keep; add overload taking head pivot offset from the anchor snapshot (Stage 1) |
| `core/vfx/pattern/TrailPattern`, `ShockwavePattern`, `ImpactPattern`, `BeamPattern` | Reusable VFX patterns | shared | `TrailPattern` gets an additive soft-profile mode (Stage 13) |

### 1.2 Homelander presentation paths that touch the model today

| File (`client/hero/homelander/fx/`) | Legacy call | Stage that changes it |
|---|---|---|
| `HomelanderFx.register` | registers `FlightPresentation(clip("flight_takeoff"), clip("flight_hover"), clip("flight_cruise"), clip("flight_boost"), clip("flight_land"), ...)` | Stage 1 (clips no longer played for EMF owner) |
| `HomelanderFx.milkDrink` | `PlayerAnimator.play(id, homelander/milk_drink, ACTION, 4)` + milk sound | Stage 8 |
| `ClapFx.create` | plays legacy `homelander/clap`, reads contact time from `AnimationLibrary` | Stage 7 |
| `EyeLaserChannel` | plays `laser_charge → laser_hold → laser_release`; sounds `LASER_CHARGE`, `LASER_LOOP` (`LaserLoopSound`), `LASER_RELEASE`; head anim from `PlayerAnimator.sample` for eye anchors | Stage 1 (anchors), Stage 9 (sound), Stage 11 (impact) |
| `IronFistsFx`, `RoarFx`, `SunChargeFx` | legacy clips `iron_fists_*`, `roar`, `sun_charge` | not migrated; routed into the jem through legacy-pose variables (Stage 1) |
| `FlightFx` (`TrailFx`, `boost`, `landing`) | chest/fist anchors via `HumanoidAnchors.tiltedPoint`, tilt via `FlightPoseTracker.transform` | Stage 13 (trail), Stage 14 (landing) |

Legacy resources under `src/main/resources/assets/superheroes/player_animations/homelander/`: `clap`, `flight_boost`, `flight_cruise`, `flight_hover`, `flight_land`, `flight_takeoff`, `iron_fists_activate`, `iron_fists_strike`, `laser_charge`, `laser_hold`, `laser_release`, `milk_drink`, `roar`, `sun_charge` (`.animation.json`). None is deleted by this plan: they remain the non-EMF fallback (EMF missing on a client) and `sun_charge`/`roar`/`iron_fists_*`/`laser_*` remain active sources. Deleting flight/clap/milk files is out of scope until the user decides the fallback is unwanted.

### 1.3 Server gameplay seams (keep)

- Flight: `mechanic/flight/FlightController` (sets `FlightPhase.TAKEOFF` at start), `FlightPhaseResolver` (`TAKEOFF_TICKS = 8`, hover < 0.08 b/t, BOOST at `SUPERSONIC` or ≥ 1.1 b/t, absolute speed), `FlightMotionMath`. Client receives `ClientFlightState.update(entityId, active, mode, phase, horizontalSpeed)` for local and tracked players.
- Clap: `hero/homelander/ability/HandClapAbility` (`RANGE 18`, cone 45°, `DAMAGE 20`, `KNOCKBACK 3.5`, `COOLDOWN_TICKS 240`), damage applied instantly in `tryActivate`, then `VfxFx.event(player, CLAP, origin, origin + forward*18, 1)`.
- Milk: `hero/homelander/item/MilkBottleItem` (`DRINK_TICKS = 32`, `UseAnim.DRINK`, MADNESS 15 s in `finishUsingItem`), `VfxFx.event(MILK_DRINK)` at use start.
- Laser: `EyeLasersAbility` (`RANGE 64`, `MIN_DPS 56`, `MAX_DPS 120` scaled by mana fraction, `/20` per tick, `MADNESS_DAMAGE_MUL 3`, uranium pulse cycle 80 ticks), `target.hurt(...)` called every firing tick; `VfxFx.channel` START / UPDATE every `CHANNEL_UPDATE_INTERVAL_TICKS = 2` / STOP. Because vanilla `LivingEntity.hurt` blocks equal-or-lower damage while `invulnerableTime > 10`, only about 2 hits per second actually land; the other 18 calls per second are wasted and the effective DPS is roughly `2 × dps/20` (normal 5.6 to 12 HP/s, madness 16.8 to 36 HP/s). Stage 10 makes the cadence explicit.
- Landing: `mechanic/falls/HeroLandingTracker` → `LandingImpact(fallDistance, verticalSpeed, horizontalSpeed, intensity, tier)` → `HomelanderHero.onLanded` → `VfxFx.event(LANDING, scale = 0.30 + intensity*1.20)` → `FlightFx.landing` (sound `homelander.flight.land`).
- `VfxFx.event` broadcasts to tracking players and self (`FxBroadcast.trackingAndSelf`), so every one-shot presentation already reaches remote observers.

### 1.4 Camera

`client/mixin/CameraMixin` only applies `ScreenShakeManager` offsets at `Camera.setup` TAIL. No third-person flight framing exists.

---

## 2. Current animation/flight pipeline (data flow)

```
server FlightController ──(state payload)──► ClientFlightState(phase, mode, hSpeed)
                                                   │
client tick: FlightPoseTracker.tick ──► PlayerAnimator.play(flight_* clip)   [legacy]
                                   ──► flight loop sound, TrailFx/boost start
                                   ──► FlightBodyTransform (pitch, roll) smoothed
render: PlayerModelPoseMixin(setupAnim TAIL) ──► PlayerAnimator.sample → PlayerPoseApplier (6 bones)
        PlayerRendererMixin ──► PoseStack rotate(-pitch X, -roll Z)
VFX:    HumanoidAnchors.eyes(player, partial, tilt, headAnimDeg from PlayerAnimator.sample)
```

Old static poses: `PlayerModelPoseMixin` applies a static flight leg pose to heroes *without* a `FlightPresentation`, and the Think Mark pose. Homelander has a presentation, so it does not get the static legs; nothing to remove.

---

## 3. `.bbmodel` analysis (facts, parsed from JSON)

Source: user attachment `Homelander_All_Animations.bbmodel`; Stage 1 copies it to `art-source/homelander/emf/Homelander_All_Animations.bbmodel` (source of truth).

- Format `free`, `format_version 5.0`, `box_uv false` (per-face UV), texture 64×64 (Homelander skin) + 32×32 milk palette. 40 elements.
- Hierarchy and pivots (px, Blockbench space):

```
root [0,12,0]
├─ body [0,12,0]                    torso boxes (+ jacket layer)
│  ├─ head [0,24,0]                 head, headwear, face_skin
│  │  └─ mouth_open                 hidden by scale 0 except in MILK
│  ├─ right_arm [-5,22,0] → right_forearm [-6,18,0] → milk_bottle → milk_cap
│  └─ left_arm  [5,22,0]  → left_forearm  [6,18,0]
├─ right_leg [-2,12,0] → right_shin [-2,6,0] → right_foot [-2,1.8,0]; right_knee [-2,6,0]
└─ left_leg  [2,12,0]  → left_shin  [2,6,0]  → left_foot  [2,1.8,0];  left_knee  [2,6,0]
```

- Clips (all baked at about 60 samples/s by the author):

| Clip | Length | Loop | Interp. | Keyframes | Facts that matter for runtime |
|---|---:|---|---|---:|---|
| HOVER · EMF expressions preview | 3.2 s | loop | catmullrom | 3478 | root ty 6.47 to 6.78 px (body floats ~0.41 block), asymmetric legs (one forward), forearm/shin micro-motion |
| TAKEOFF · controlled vertical launch | 0.8 s | hold | bezier | 238 | root ty dips to -0.5 px at 0.20 s (crouch) then rises to 6.6 px at 0.8 s = HOVER start height; arms swing back to -11° at 0.28 s |
| SHOWCASE · ground → takeoff → 4 hover | 14.05 s | hold | bezier+catmullrom | 7168 | reference only, not shipped |
| HAND CLAP · windup → impact | 1.92 s | hold | linear | 2440 | windup peak 1.02 to 1.37 s (right_arm rz -71°, left_arm rz +65°); impact (peak angular velocity) at **1.50 s** |
| BOOST FLIGHT · asymmetric speed flight | 2.6 s | loop | linear | 2983 | root rx constant -86.0 ±0.15°, root rz -2.8 to -3.2°, root ty 9 px; right_arm rx 176° (fist forward), left_arm back, legs asymmetric |
| MILK DRINK · unscrew → sip → lower | 6.3 s | hold | linear | 7980 | cap starts moving 1.17 s; `mouth_open` scale > 0 from 2.22 s to 5.68 s; bottle/cap under right_forearm |

- `emf_lab` block: `target "Minecraft 1.21.1 Fabric / EMF 3.3.9"`, `runtime_verified: false`, `preview_only: true`, expressions using `keyframeloop`, `cubicbezier`, `clamp`, `torad`, `sin`, `if`, custom `var.*`. These expressions assume the Blockbench parent chain (root → body → head/arms), which does not exist in a player CEM (see 4.3). They are not used at runtime; the bake script regenerates equivalent expressions from the native keyframes.

---

## 4. EMF integration analysis (verified)

Sources checked: EMF repository `Traben-0/Entity_Model_Features` (cloned, `mod_version=3.3.9`, `etf_version=7.2.2`, `EMFAnimationApi` API version 11), `.github/emf_animation.txt`, `.github/emf_part.txt`, `FEATURES.md`, Modrinth API.

### 4.1 Versions and packaging
- EMF `3.3.9-fabric-1.21` (Modrinth: game versions 1.21, 1.21.1; loaders fabric, quilt), client-only per its `fabric.mod.json`, required dependency ETF (`BVzZfTc1`). Available as `maven.modrinth:entity-model-features:3.3.9-fabric-1.21` (POM returns 200).
- ETF `7.2.4-fabric-1.21` (1.21/1.21.1) as `maven.modrinth:entitytexturefeatures:7.2.4-fabric-1.21` (POM returns 200). EMF 3.3.9 builds against ETF 7.2.2, so 7.2.4 satisfies it.
- EMF mod id `entity_model_features`, ETF mod id `entity_texture_features`. Both are client-only, so Stage 1 must verify the correct Fabric packaging/metadata strategy that keeps the dedicated server valid while making EMF+ETF mandatory for the supported client configuration. **Do not implement a Homelander legacy animation fallback when EMF is absent.**

### 4.2 Player CEM
- Files: `player.jem`, `player_slim.jem`, `player_cape.jem`. Parts: `head, headwear, body, left_arm, right_arm, left_leg, right_leg, ear, left_sleeve, right_sleeve, left_pants, right_pants, jacket, cloak`.
- Lookup folders: `assets/minecraft/emf/cem/` (EMF-only, preferred) and `assets/minecraft/optifine/cem/`. Mod jar assets are a resource pack, so the jem ships inside the jar. User resource packs with their own player jem win over it (documented risk).
- Custom parts: submodels with `id` under a vanilla part; animatable as `<id>.rx` etc.; `visible`, `visible_boxes`, `sx/sy/sz`, `tx/ty/tz`, `rx/ry/rz`.
- Attachments for player: `right_handheld_item`, `left_handheld_item`, `head_item` can be declared on any part, including custom forearms.
- Armor: separate `player_inner_armor.jem`/`player_outer_armor.jem` style models that copy vanilla part transforms, so armor follows the six vanilla top-level parts only.

### 4.3 Expressions and variables
- Built-ins used by this plan: `frame_time`, `is_first_person_hand`, `is_player_first_person`, `head_pitch`, `head_yaw`, `limb_swing`, `limb_speed`, functions `keyframe(k, …)` (Catmull-Rom between integer frames, source `KeyframeMethod` uses `Mth.catmullrom`), `keyframeloop`, `lerp(k, x, y)`, `clamp`, `if`, `torad`. Entity variables `var.*`/`varb.*` are per rendered entity.
- Mod API: `EMFAnimationApi.registerSingletonAnimationVariable(String modId, String name, String explanation, Supplier<Float>)` (and `BooleanSupplier`), `EMFAnimationApi.getCurrentEntity()` → `EMFEntity` (`etf$getUuid()`), `registerAnimationHook`, `registerVanillaModelCondition(Function<EMFEntity,Boolean>)`, `lockEntityToVanillaModel`, `pauseAllCustomAnimationsForEntity`. Singleton variables are evaluated during the animation of the current entity, so a supplier can look up per-entity state by `getCurrentEntity()`.
- Part names inside a player CEM are flat top-level vanilla parts. There is no `root` and a vanilla part cannot be parented under another vanilla part, so the Blockbench chain root → body → head/arms cannot be reproduced directly.

### 4.4 Model hierarchy decision
Chosen: **baked flat hierarchy on vanilla carriers**. The bake script composes `root × body × part` into each top-level vanilla part per sample (rotation composed in ModelPart's `ZYX` order, pivot-corrected translation), and keeps the sub-bones (`*_forearm`, `*_shin`, `*_foot`, `*_knee`, `mouth_open`, `milk_bottle`, `milk_cap`) as custom submodels of their carrier with their authored local tracks. Result: exact authored world pose, armor/held items/sleeves follow the six carriers, eye anchors stay on `model.head`.

Rejected: (a) everything as custom submodels under `body` with vanilla boxes hidden (exact hierarchy, but armor, cape, held items, first-person arms and `HumanoidAnchors` detach from the visible model); (b) reduce to six vanilla bones (destroys forearm/shin/foot/prop motion, forbidden by the brief); (c) use `emf_lab` preview expressions (unverified, assume the Blockbench chain).

Root rotation split: authored root **rotation** in BOOST (-86° rx) is whole-body orientation, and `PlayerRendererMixin` already owns whole-body rotation. The bake script removes the clip-mean root rotation from the baked parts and writes it to `vfx/homelander/flight.json` as `emfBoostRootPitch = 86.0` / `emfBoostRootRoll`; `FlightPoseMath` then uses it as the BOOST pitch target. The residual (±0.15°, ±0.2°) stays baked. Root **translation** (hover +6.6 px, boost +9 px, takeoff dip/rise) stays baked.

### 4.5 Where state lives
- Codex client state (`HomelanderPoseState`): clip clocks and blend weights, directional pitch/bank, computed once per client tick + partial-tick interpolated at read time.
- EMF variables (singletons, Codex-registered): expose those numbers to the jem.
- EMF expressions (jem): only sampling (`keyframe`/`keyframeloop`) and blending (`lerp`). No state machines in the jem.

### 4.6 Anchor seam
There is no public EMF API that returns transformed part matrices. After EMF animation the vanilla `ModelPart` fields of the shared `PlayerModel` hold the animated values, but the model instance is shared by all players, so values must be copied per entity during render. Minimal adapter: `RenderedPoseCache` (Stage 1) copies `head/body/arms` `x,y,z,xRot,yRot,zRot` into a fixed `float[]` per entity id at the point right before the model is drawn.

### 4.7 Open points the Stage 1 spike must close (with the exact check)
1. Where EMF 3.3.9 runs `animate()` for 1.21.1 (source is preprocessor-driven; the `//#else` branch for `< 1.21.2` must be read in the published jar: `unzip entity_model_features-3.3.9-1.21-fabric.jar 'traben/entity_model_features/mixin/**'` and `javap -c -p` the renderer and model-part mixins). Required outcome: EMF animates after `setupAnim` (so `PlayerModelPoseMixin` output is overwritten for assigned parts) and before `renderToBuffer`.
2. Whether the singleton variable name may contain `_` without a prefix collision (follow `registerSimpleFloatVariable` naming in `VariableRegistry`).
3. Whether CEM `ty` on the vanilla `head` part is inverted relative to `ModelPart.y` (bake script must match; verify with EMF export: the user runs EMF config → models → player → export once and attaches the file).

---

## 5. Key conflicts and prevention

| Conflict | Risk | Prevention |
|---|---|---|
| EMF vs `PlayerAnimator` | Both write `head/body/arms/legs` → jitter, double poses | Homelander is cut over completely in Stage 1: `PlayerModelPoseMixin` does not apply `PlayerAnimator` to EMF-owned Homelander, every Homelander legacy play/stop/sample call is removed, and all Homelander legacy animation resources are deleted. No bridge between the two animation engines. |
| EMF root vs procedural tilt | BOOST -86° baked + `boostPitch 80` in renderer → 166°, belly-up | Mean root rotation stripped at bake time and moved to `flight.json`; renderer remains the only whole-body rotation |
| Flight presentation vs eye anchors | Beams leave the vanilla head position while the EMF head moved | Anchors take head offsets/rotation from `RenderedPoseCache` (captured after EMF) plus renderer tilt |
| Body pitch/bank vs F5 camera | Camera follows eye position that swings with tilt → nausea | Camera anchor = smoothed body centre in world space, independent of tilt; tilt never feeds camera rotation |
| Custom arm hierarchy vs held items | Items float at the upper-arm pivot while forearm bends | `right_handheld_item`/`left_handheld_item` attachments declared on the forearm submodels; held item hidden during MILK (authored bottle replaces it) |
| Custom model vs armor and skin layers | Armor ignores forearm/shin bends; overlays desync | Carriers keep armor aligned at the shoulder/hip; overlay boxes baked into the same custom parts with `sizeAdd 0.25`; `headwear/jacket/sleeves/pants` vanilla parts hidden in the jem to avoid double layers; forearm armor bend is a documented limitation |
| Remote animation vs local prediction | Local uses input, remote only sees positions → different poses | All pose inputs come from the same sources for every player: synced `ClientFlightState`, `VfxFx` events, render-interpolated position delta. No keyboard input in `HomelanderPoseState` |
| Trail orientation vs velocity source | Server velocity is zero for remote players; pos delta jitters at low speed | One smoothed velocity per entity in `HomelanderPoseState` (half-life 3 ticks) feeds pose, rings and trail; rings require speed ≥ threshold for N consecutive ticks |
| Non-Homelander players and the shipped `player.jem` | Every player gets the Homelander jem | Jem geometry equals the vanilla player shape when all weights are 0 and every expression is `lerp(superheroes_hl_w, <vanilla value>, <authored>)`; texture is the entity's own skin; plus `EMFAnimationApi.registerVanillaModelCondition(e -> !isHomelanderPlayer(e))` so non-Homelander players render the untouched vanilla model |
| Other resource packs with a player jem | Pack overrides Homelander | Documented in README; out of scope |

---

## 6. Target Homelander presentation architecture

New files (all client unless stated):

- `client/core/emf/EmfBridge.java`: `isAvailable()`, `registerFloatVariable(String name, String explanation, Supplier<Float>)`, `currentEntityUuid()` (nullable), `registerVanillaModelCondition(Predicate<UUID>)`. The only class under `client/core` that imports `traben.*`. Every method no-ops when EMF is absent.
- `client/core/emf/EmfPresentationOwnership.java`: `static void register(Predicate<AbstractClientPlayer> owner)`, `static boolean isOwned(AbstractClientPlayer)`. Hero-agnostic; `false` when EMF is absent.
- `client/core/emf/RenderedPoseCache.java`: `capture(int entityId, PlayerModel<?> model)`, `headOffsetPx(int id, Vector3f out)`, `headRotationRad(int id, Vector3f out)`, `clear(int id)`, `clearAll()`; fixed arrays, capacity 64 entities, LRU by last frame.
- `client/hero/homelander/emf/HomelanderPoseState.java`: per-entity state (`ClipClock` for HOVER/BOOST loops and TAKEOFF/CLAP/MILK one-shots; weights `active`, `boost`, `takeoff`, `clap`, `milk`); `tick()` from client tick, `read(UUID, float partial)`; directional values `forward`, `strafe`, `pitchDeg`, `bankDeg`. Pure logic in `HomelanderPoseMath` (unit-tested).
- `client/hero/homelander/emf/HomelanderEmfVariables.java`: registers only the variables required by the new EMF-authored HOVER/BOOST/TAKEOFF/CLAP/MILK presentation. It must not expose legacy `PlayerAnimator` bone channels.
- `art-source/homelander/emf/bake_jem.py` + `art-source/homelander/emf/test_bake_jem.py`: offline generator; outputs `src/main/resources/assets/minecraft/emf/cem/player.jem` and `player_slim.jem` (committed), and prints the root-mean values for `flight.json`.
- Server (Stage 12): `hero/homelander/scorch/LaserScorchData.java` (`SavedData`), `core/net/ScorchMarksS2CPayload.java`.

Jem expression shape (per channel, generated):

```
"right_arm.rx": "lerp(superheroes_hl_w, right_arm.rx,
    lerp(max(superheroes_hl_takeoff_w, max(superheroes_hl_clap_w, superheroes_hl_milk_w)),
         lerp(superheroes_hl_boost_w,
              keyframeloop(superheroes_hl_hover_t*20, ...),
              keyframeloop(superheroes_hl_boost_t*20, ...)),
         <active one-shot keyframe(...)>))"
```

Only one one-shot weight is non-zero at a time (`HomelanderPoseState` enforces it), so the generator emits `superheroes_hl_takeoff_w*keyframe(takeoff) + superheroes_hl_clap_w*keyframe(clap) + superheroes_hl_milk_w*keyframe(milk)` normalised by their sum. Sample rate: 20 Hz (`keyframe` interpolates with Catmull-Rom, matching the author's 60 Hz curves within 0.5° on these clips; the bake test asserts the max error).

---

## 7. Staged implementation plan

Each stage: goal, current state, files, symbols, steps, constraints, automated verification, user in-game verification, rollback boundary.

### Stage 1: EMF foundation and Homelander model integration (PR 1)

**Goal:** EMF/ETF integrated for the supported client configuration; generated Homelander jem in the jar; Homelander (and only Homelander) owned by EMF; every legacy Homelander animation resource and every Homelander `PlayerAnimator` animation call removed; anchor cache in place. Abilities without a new EMF-authored clip temporarily have no player animation.

**Current state:** no EMF; `PlayerModelPoseMixin` applies `PlayerAnimator` to everybody.

**Files:**
- Modify `build.gradle`: in `dependencies`, `modCompileOnly "maven.modrinth:entity-model-features:3.3.9-fabric-1.21"`, `modCompileOnly "maven.modrinth:entitytexturefeatures:7.2.4-fabric-1.21"`, `modLocalRuntime` for both; extend the existing `filter { !it.path.contains('veil') }` server/gametest filters to also drop paths containing `entity-model-features` and `entitytexturefeatures`. Modrinth maven repo block already exists (Iris).
- Modify dependency metadata after verifying the correct Fabric strategy for a universal mod with client-only EMF/ETF: the supported client configuration must require EMF+ETF, while dedicated-server startup must remain valid. Do not solve this by keeping a legacy Homelander animation fallback.
- Add `art-source/homelander/emf/Homelander_All_Animations.bbmodel`, `bake_jem.py`, `test_bake_jem.py`.
- Add `src/main/resources/assets/minecraft/emf/cem/player.jem`, `player_slim.jem` (generated; slim differs only in arm widths 3 px and arm pivots per vanilla slim).
- Add `client/core/emf/EmfBridge.java`, `EmfPresentationOwnership.java`, `RenderedPoseCache.java`.
- Add `client/hero/homelander/emf/HomelanderPoseState.java`, `HomelanderPoseMath.java`, `HomelanderEmfVariables.java`, `HomelanderEmf.java` (`static void register(HeroClientContext ctx)`: ownership predicate = player has Homelander hero id in `ClientHeroState`, registers variables and the vanilla-model condition).
- Modify `client/hero/homelander/HomelanderClientModule.java`: call `HomelanderEmf.register(ctx)`.
- Modify `client/mixin/PlayerModelPoseMixin.java`: after the restore step, `if (EmfPresentationOwnership.isOwned(player)) return;`.
- Modify `client/mixin/PlayerRendererMixin.java`: after the model is set up and before rendering, `RenderedPoseCache.capture(player.getId(), getModel())` for owned players.
- Modify `client/core/flight/FlightPoseTracker.java`: Homelander must no longer play/stop any legacy flight clip; sounds/trail remain.
- Modify `client/core/vfx/anchor/HumanoidAnchors.java` and `client/hero/homelander/fx/EyeLaserChannel.java` so eye anchors come from the EMF-rendered pose and do not depend on `PlayerAnimator.sample`.
- Remove Homelander legacy animation calls from `HomelanderFx`, `ClapFx`, `EyeLaserChannel`, `IronFistsFx`, `RoarFx`, `SunChargeFx` and any other Homelander call site found by repo-wide search.
- Delete every file under `src/main/resources/assets/superheroes/player_animations/homelander/`: `clap.animation.json`, `flight_boost.animation.json`, `flight_cruise.animation.json`, `flight_hover.animation.json`, `flight_land.animation.json`, `flight_takeoff.animation.json`, `iron_fists_activate.animation.json`, `iron_fists_strike.animation.json`, `laser_charge.animation.json`, `laser_hold.animation.json`, `laser_release.animation.json`, `milk_drink.animation.json`, `roar.animation.json`, `sun_charge.animation.json`.
- Modify `client/ClientSessionState` reset path: `RenderedPoseCache.clearAll()`, `HomelanderPoseState.clearAll()`.
- Tests: `src/test/java/.../client/hero/homelander/emf/HomelanderPoseMathTest.java`, `src/test/java/.../client/core/emf/RenderedPoseCacheTest.java`, `src/test/java/.../HomelanderJemContractTest.java` (parses the shipped jem: required parts/ids present, every custom id from the bbmodel present, no `NaN`, all five clips present, no `emf_lab` expressions); extend the ArchUnit rule so `traben..` is only imported from `client.core.emf..` and `client.hero.homelander.emf..`.

**Steps:**
- [ ] Spike: resolve §4.7 items 1 and 2 by reading the published jar; record results in the PR description.
- [ ] Write `test_bake_jem.py` first: composing identity parents returns the child; composing root rx -86 then removing the mean returns the residual; ZYX Euler round-trip error < 1e-4 rad; 20 Hz Catmull-Rom resample error < 0.5° versus the 60 Hz source for every channel of every shipped clip.
- [ ] Implement `bake_jem.py` (inputs: bbmodel path, `--slim`; outputs jem + printed root means). Run `python3 -m unittest art-source/homelander/emf/test_bake_jem.py` and the bake.
- [ ] Hide vanilla overlay parts in the jem (`headwear`, `jacket`, sleeves, pants: `"visible": "false"` equivalent via animation `visible_boxes`) because the baked parts contain the overlay boxes.
- [ ] Implement bridge, ownership, cache and minimal pose state with the new EMF variables only; gate the legacy model mixin for Homelander. There is no `legacy_w` and no legacy bone-variable bridge.
- [ ] `./gradlew test --no-daemon`, then `./gradlew qualityGate --no-daemon`.

**Constraints:** no new authored motion yet; no gameplay changes. Old Homelander player animations are intentionally gone. Laser, iron fists, roar, sun charge and landing may temporarily have no player animation.

**Automated verification:** unit tests above; `grep -rn "traben\." src/client/java | grep -v "/client/core/emf/\|/client/hero/homelander/emf/"` returns nothing; `grep -rn "traben\." src/main/java` returns nothing; `runServer`-style gametest classpath without EMF passes (`qualityGate`).

**User in-game verification:** with EMF+ETF installed, Homelander stands/walks correctly (same skin, no gaps at elbows/knees, normal and slim skin); non-Homelander players unchanged; flight/laser/iron fists/roar/sun/landing gameplay and VFX still function even where player animation is temporarily absent; F3+T reload does not break. No legacy Homelander animation should play anywhere.

**Rollback boundary:** revert PR 1 completely; nothing else depends on it yet.

### Stage 2: HOVER (PR 2)

**Goal:** Flying Homelander uses authored HOVER as base for all non-boost flight, including slow movement.

**Current state:** legacy `flight_hover`/`flight_cruise` clips (now skipped for the owner in Stage 1, so the owner shows vanilla pose in flight until this PR).

**Files:** `HomelanderPoseState`, `HomelanderPoseMath`, `HomelanderPoseMathTest`.

**Symbols:** `HomelanderPoseMath.activeWeight(float current, boolean flying, float dtTicks)` (half-life 3 ticks), `ClipClock.advance(float dtSeconds)`; `superheroes_hl_w` = active weight, `superheroes_hl_hover_t` = hover loop time.

**Steps:**
- [ ] Test first: weight rises 0 → 1 with half-life 3 ticks when `ClientFlightState.get(id).active()` and returns to 0 after landing; hover clock wraps at 3.2 s; clock advances by real render delta (frame-rate independent: 60 fps and 144 fps produce the same value at the same wall time within 1e-4).
- [ ] Implement; hover clock never resets on phase flips (prevents snaps).
- [ ] `qualityGate`.

**Constraints:** no BOOST yet (boost weight 0).

**Automated verification:** tests; bake contract test asserts HOVER loop continuity (first sample equals last within 0.5°).

**User in-game:** stationary hover shows breathing, one leg forward, micro-motion; slow forward/strafe keeps hover; no pop when starting to move.

**Rollback boundary:** revert PR 2; Stage 1 still valid.

### Stage 3: TAKEOFF (PR 3)

**Goal:** 0.8 s authored takeoff from ground or jump, blending into HOVER without snapping.

**Current state:** server sets `FlightPhase.TAKEOFF` for `FlightPhaseResolver.TAKEOFF_TICKS = 8` (0.4 s); gameplay lift starts immediately.

**Decision:** gameplay unchanged. The presentation clock starts on the first client tick where `ClientFlightState` becomes `active` (same moment for local and remote, since both come from the synced state). Takeoff weight = 1 for 0 to 0.65 s, then eases to 0 by 0.8 s (half-life 2 ticks) while hover weight is already 1 underneath; the takeoff's last frame equals the hover start height (6.6 px), so the crossfade is continuous. If flight becomes active while airborne (takeoff from jump, `onGround == false` at activation), start the clip at 0.28 s (skip the crouch dip, verified from root ty track).

**Files:** `HomelanderPoseState`, `HomelanderPoseMath`, tests.

**Steps:**
- [ ] Tests: activation on ground → `takeoff_t` starts at 0; airborne → 0.28; toggling flight off during takeoff fades takeoff and active weight out without resetting hover; re-activation within 0.8 s restarts cleanly (no two takeoffs).
- [ ] Implement; `qualityGate`.

**User in-game:** takeoff from ground shows crouch and launch; from a jump no crouch; no snap into hover; observer sees the same.

**Rollback boundary:** revert PR 3.

### Stage 4: BOOST (PR 4)

**Goal:** Horizontal, one-fist-forward BOOST loop entered by signed forward speed relative to body yaw.

**Decision:** `forward = v · bodyForward`, `strafe = v · bodyRight` from the smoothed render velocity (half-life 3 ticks) in `HomelanderPoseState`. BOOST weight target = 1 when `forward ≥ boostEnter` (param `emfBoostEnter = 0.9` b/t) or when mode is `SUPERSONIC` and `forward > 0.3`; target 0 when `forward < emfBoostExit = 0.6` (hysteresis). Backward or sideways speed never enters BOOST. Weight half-life 4 ticks. BOOST pitch target in `FlightPoseMath` comes from `emfBoostRootPitch` (86°) for the owner.

**Note on the reverted `5c3be771`:** that commit replaced the synced server phase for gameplay-visible behaviour (trail gating and loop volume) and made gameplay worse. This stage does not touch `ClientFlightState`, `FlightPhaseResolver`, trail gating or loop volume: the directional values only drive pose weights. Any change to trail/sound gating waits for Stage 13 and needs its own test.

**Files:** `HomelanderPoseState`, `HomelanderPoseMath`, `client/core/flight/FlightPoseTracker.java` (owner uses the directional target, see Stage 5 for the method), `src/main/resources/assets/superheroes/vfx/homelander/flight.json` (new keys), tests.

**Steps:** tests first for enter/exit hysteresis, backward never boosts, supersonic forward boosts, weight continuity across 1-tick phase flicker; implement; `qualityGate`.

**User in-game:** fast forward flight goes horizontal with one fist forward and asymmetric legs; no belly-up; releasing speed returns to hover smoothly; flying backward fast stays upright.

**Rollback boundary:** revert PR 4 (hover remains).

### Stage 5: procedural flight transforms and backward flight (PR 5)

**Goal:** forward pitch, small backward lean, bank from strafe and yaw rate, vertical response; frame-rate independent; no oscillation.

**Symbols:** add to `FlightPoseMath`: `public static FlightBodyTransform directional(float forward, float strafe, float vertical, float yawRateDegPerTick, float boostWeight, VfxParams p)`:
- `pitch = lerp(boostWeight, clamp(forward / hoverRefSpeed, -1, 1) * (forward ≥ 0 ? hoverForwardPitch(12) : hoverBackwardPitch(8)), emfBoostRootPitch)`; backward yields negative pitch, clamped to `-hoverBackwardPitch`.
- `pitch += clamp(-vertical / verticalRef(0.6), -1, 1) * verticalPitch(6)` only when `boostWeight < 0.5` and sign keeps the body upright (never inverts, never exceeds ±90 − margin; the PR #138 descent rule stays).
- `roll = clamp(-strafe / strafeRef(0.5) * strafeRoll(14) - yawRateDegPerTick * rollFactor, -rollMax, rollMax)`.
- Smoothing via existing `FlightPoseMath.step` with `halfLifeTicks` param.
`FlightPoseTracker` calls `directional` only when `EmfPresentationOwnership.isOwned`, otherwise the existing `target(...)`.

**Files:** `FlightPoseMath.java`, `FlightPoseTracker.java`, `src/test/java/io/github/grebeshok105/codex/client/core/flight/FlightPoseMathTest.java` (extend), `vfx/homelander/flight.json`.

**Tests:** backward flight pitch ∈ [-8°, 0]; pure strafe gives roll sign opposite for left/right; descent at full speed never produces pitch > 90° or sign flip; step at 30 fps and 240 fps converge to the same value within 0.1° after 1 s; alternating forward/backward input every tick produces bounded output (no growth).

**User in-game:** backward flight leans back slightly and stays readable; strafes bank; diagonals combine; rapid direction changes do not jitter; F5 front/back consistent.

**Rollback boundary:** revert PR 5; owner falls back to `target(...)`.

### Stage 6: F5 camera (PR 6)

**Goal:** third-person framing centred on the body while flying; smooth transitions; no gameplay position change.

**Current state:** `CameraMixin` shake only.

**Decision:** in `CameraMixin`, for a detached camera and a focused player whose `HomelanderPoseState` active weight > 0, inject at `Camera.setup` before the zoom move (`INVOKE` of `Camera.move`/`getMaxZoom` in 1.21.1, confirm target by reading `Camera.setup` in the mapped sources) and replace the base position with `lerp(activeWeight, eyePos, bodyCentre)`, where `bodyCentre = feet + (0, 1.1 + hoverRootTy/16, 0)` smoothed with half-life 3 ticks in world space. Camera rotation is never touched by tilt. Collision (`getMaxZoom`) still runs on the new base, so no clipping into walls. Standing third-person: unchanged (weight 0).

**Files:** `client/mixin/CameraMixin.java`, new hero-agnostic `client/core/camera/ThirdPersonFraming.java` (`static void register(Function<Entity, Vec3> offsetProvider)`, `static @Nullable Vec3 offset(Entity, float partial)`), `HomelanderEmf.register` registers the provider; test `ThirdPersonFramingTest` for smoothing and weight 0 identity.

**User in-game:** F5 back and front while hovering and boosting keep the body centred; no jump when flight starts/ends; walls still block the camera.

**Rollback boundary:** revert PR 6.

### Stage 7: hand clap (PR 7)

**Goal:** authored 1.92 s clap on the model, shockwave/sound/VFX at the authored impact (1.50 s), multiplayer consistent.

**Current state:** damage/knockback applied instantly in `HandClapAbility.tryActivate`, VFX at the same tick.

**Confirmed gameplay timing:** the server keeps range, cone, damage, knockback and cooldown, but schedules the hit `IMPACT_TICKS = 30` ticks after activation so gameplay impact, sound and VFX land on the authored ~1.50 s hand-contact frame. Cancellation: if the player dies, is swapped out of the hero, or is stunned before tick 30, the hit is cancelled and a `CLAP_CANCEL` event is sent; cooldown still applies from activation. Two events: `CLAP` at activation (starts clip for everybody) and `CLAP_IMPACT` at tick 30 (existing `ClapFx` VFX + `HomelanderSounds.HAND_CLAP`). This timing is approved by the user.

**Files:** `hero/homelander/ability/HandClapAbility.java` (pending-clap map using the same `OwnedSessionMap` pattern as `EyeLasersAbility`), `hero/homelander/HomelanderVfxIds.java` (`CLAP_IMPACT`, `CLAP_CANCEL`), `client/hero/homelander/fx/ClapFx.java` (no legacy clip for owner; impact visuals move to the impact event), `HomelanderPoseState` (clap clock/weight), `HomelanderFx.register`, `src/gametest/java/.../HomelanderGameTests.java` (hit happens at tick 30, not tick 0; cancelled on death).

**User in-game:** windup visible, shockwave exactly when hands meet, remote observer sees the same timing, retrigger blocked by cooldown, clap in flight blends over hover.

**Rollback boundary:** revert PR 7 (server and client together).

### Stage 8: milk and props (PR 8)

**Goal:** authored 6.3 s milk sequence with bottle, cap and mouth.

**Current state:** `DRINK_TICKS = 32` (1.6 s), vanilla held-item drinking, legacy `milk_drink` clip.

**Confirmed gameplay timing:** `MilkBottleItem.getUseDuration` = 126 ticks (6.3 s) so the real use duration matches the authored sequence and the gameplay effect lands after the sip. MADNESS is applied in `finishUsingItem` as today. Releasing use early cancels (vanilla `releaseUsing`), sends `MILK_CANCEL`, no effect. While the milk weight > 0, the third-person held item is hidden for the owner because the authored bottle replaces it. First-person drinking stays vanilla (out of scope). This timing is approved by the user.

**Files:** `hero/homelander/item/MilkBottleItem.java`, `HomelanderVfxIds.MILK_CANCEL`, `HomelanderFx.milkDrink` (owner: no legacy clip; sound unchanged), `HomelanderPoseState`, new `client/mixin/ItemInHandLayerMixin.java` + entry in `superheroes.client.mixins.json`, gametest for duration and cancel.

**User in-game:** bottle appears, cap unscrews, mouth opens during sip, bottle lowers; no vanilla bottle doubling; cancel stops cleanly; observer sees it.

**Rollback boundary:** revert PR 8.

### Stage 9: continuous laser sound (PR 9)

**Goal:** one loop starting at activation, fading out on release, never duplicated.

**Current state:** `EyeLaserChannel` plays `LASER_CHARGE` at START, starts `LaserLoopSound` at HOLD, plays `LASER_RELEASE` and hard-stops the loop at release.

**Steps:**
- [ ] Start `LaserLoopSound` in the constructor (START) with volume ramp 0 → `loopVolume` over `CHARGE_TICKS`.
- [ ] Add `fadeOut(int ticks)` to the nested `EyeLaserChannel.LaserLoopSound`; `release()` calls `fadeOut(RELEASE_TICKS)`; the sound stops itself when volume reaches 0; `cancel()` stops immediately.
- [ ] Remove the `LASER_CHARGE` and `LASER_RELEASE` playback calls. Keep the `SoundEvent` registrations and `sounds.json` entries (other code may reference them: `rg -n "LASER_CHARGE|LASER_RELEASE" src` before touching; if unused after this change, leave them registered to avoid breaking resource packs).
- [ ] Dedup: the channel table already keys one channel per source; add a guard that a second START for the same source reuses the existing loop.
- [ ] Tests: pure `LaserLoopVolume.at(age, releasedAt)` curve test.

**User in-game:** one smooth sound from press to release, soft tail, no clicks, no stacking when spamming; observer hears it moving with the caster.

**Rollback boundary:** revert PR 9.

### Stage 10: laser damage cadence (PR 10)

**Goal:** explicit, less frequent damage and lower DPS, beam still continuous.

**Confirmed gameplay balance:** damage every `DAMAGE_INTERVAL_TICKS = 10` ticks (2 hits/s) instead of calling `hurt` 20 times/s; per-hit damage `dps * 10 / 20`. Use `MIN_DPS = 8`, `MAX_DPS = 16`. Madness keeps ×3 and its every-2-tick explosions unchanged. Visual beam, UPDATE cadence and uranium pulse phases unchanged; the damage clock is separate from `ACTIVE_TICK` so pauses do not shift it. These cadence/DPS values are approved by the user.

**Hit feedback consequence:** red flash and hurt sound already only played on landed hits (~2/s); unchanged. Knockback from the damage source also only on landed hits.

**Files:** `EyeLasersAbility.java`, `src/test/java/.../EyeLaserDamageCadenceTest.java` (pure helper `EyeLaserPhases.shouldDamage(int ticks)`), gametest asserting total damage over 40 ticks within the expected band.

**User in-game:** beam feels continuous; targets die noticeably slower; madness still brutal.

**Rollback boundary:** revert PR 10.

### Stage 11: laser impact VFX (PR 11)

**Goal:** small, clear contact point, low bloom.

**Steps (data first, code only if needed):**
- [ ] `quasar/emitters/homelander_laser_impact.json`: `count 14 → 5`, `max_lifetime 6 → 4`.
- [ ] Linked particle_data / render module under `quasar/modules/`: reduce size ~50%, emissive/alpha ~40%.
- [ ] `vfx/homelander/laser.json`: `lightBrightness 0.85 → 0.4`, `lightRadius 7 → 3.5`, `distortionRadius 0.8 → 0.35`; add `impactFlashScale` if a flash quad exists in `ImpactPattern` (read it first).
- [ ] `EyeLaserChannel.IMPACT_INTERVAL_TICKS` stays 3.

**Automated:** JSON parses (existing `VfxParamsLoader` test or a new resource test).

**User in-game:** small bright point with a few sparks, no white blob, readable on dark and bright blocks, with and without shaders.

**Rollback boundary:** revert data files.

### Stage 12: permanent burn marks (PR 12)

**Goal:** laser leaves scorch decals on blocks that stay across relogs and are seen by everyone.

**Design:**
- Representation: `ScorchMark(BlockPos pos, byte face, float u, float v, float size, int rot)`, 16 bytes on the wire.
- Ownership: server-authoritative cosmetic world state. `EyeLasersAbility.fireBeam` (block hit branch) calls `LaserScorchData.tryAdd(level, hit)`; spacing ≥ 0.35 blocks from the previous mark of that caster, max 6 marks/s per caster.
- Persistence: `LaserScorchData extends SavedData` per dimension, ring buffer `MAX_MARKS = 2048` (oldest overwritten), saved in the dimension data folder, so it survives world reload.
- Sync: new mark → `ScorchMarksS2CPayload(List<ScorchMark>)` via `FxBroadcast.around(level, Vec3.atCenterOf(pos), 128, payload)`; on join / dimension change / respawn the server sends all marks of that dimension in chunks of 256.
- Chunk unload/reload: data is per dimension, not per chunk, so unload does not lose marks; the client only renders marks within render distance.
- Cleanup: client skips marks whose block face is no longer solid (`isFaceSturdy`); server prunes such marks lazily when adding.
- Rendering: one textured quad per mark on the face, offset 0.002 along the normal plus polygon offset, alpha-cutout, lit by block light, `RenderType.entityTranslucent` with texture `superheroes:textures/effect/homelander/laser_scorch.png` (32×32, dark radial falloff with a faint ember rim; generate once with a small script in `art-source/homelander/emf/` if no asset exists in `art-source`). Client storage: fixed arrays sized to `MAX_MARKS`, no per-frame allocation; batch into one buffer per frame.
- Size: 0.25 to 0.4 blocks, random rotation seeded by position.

**Files:** new `hero/homelander/scorch/LaserScorchData.java`, `core/net/ScorchMarksS2CPayload.java` (+ `CoreNetworking` registration), `client/hero/homelander/fx/ScorchMarkRenderer.java` (registered from `HomelanderClientModule` via `WorldRenderEvents.AFTER_TRANSLUCENT`), `EyeLasersAbility.java`, texture, tests (ring buffer, spacing, codec round-trip), gametest (marks persist through `SavedData` save/load).

**User in-game:** burn marks stay after 10 minutes, relog, and server restart; observer sees them; no z-fighting; breaking the block removes the mark.

**Rollback boundary:** revert PR 12; saved data file is ignored by older versions.

### Stage 13: trail and sonic rings (PR 13)

**Goal:** soft continuous contrail instead of hard triangular ribbons; rings only at high speed, spaced, oriented by motion.

**Current state:** `FlightFx.TrailFx` uses `List<TrailPattern>` ribbons (bounded) and `ShockwavePattern` rings with interval and motion orientation.

**Steps:**
- [ ] Read `core/vfx/pattern/TrailPattern.java`; add a soft profile mode (additive): Catmull-Rom subdivision between stored points (×3), width taper from head to tail, alpha fade by age with ease-out, cross-section rendered as two crossed quads with a soft gaussian texture (`textures/effect/soft_trail.png`) instead of a flat triangle strip.
- [ ] Keep point capacity fixed (existing cap); no new list allocations per frame.
- [ ] Rings: emit only when smoothed speed ≥ `ringSpeed` for 5 consecutive ticks and at most every `ringInterval` ticks (existing param, raise to ≥ 12); orientation from the smoothed velocity in `HomelanderPoseState` (same source as pose).
- [ ] Tune `vfx/homelander/flight.json`.
- [ ] Unit test for subdivision and fade curve (pure helper).

**User in-game:** smooth contrail following the path, fading gradually, rings only at supersonic speed, no triangle artefacts, good FPS.

**Rollback boundary:** revert PR 13 (soft mode is opt-in per trail).

### Stage 14: landing audio and presentation (PR 14)

**Goal:** landing sound scaled by impact, new non-Warden sound.

**Steps:**
- [ ] Asset: convert `art-source/sounds/homelander/cUsersstravvberyDownloadsuniversfield-ground-impact-352053.mp3` with `ffmpeg -i <mp3> -c:a libvorbis -qscale:a 5 src/main/resources/assets/superheroes/sounds/homelander/flight_land.ogg` (replaces the current file; ask the user to confirm the asset in the PR).
- [ ] `FlightFx.landing`: volume `0.4 + 0.8 * s`, pitch `1.15 - 0.3 * s` where `s = clamp((scale - 0.30) / 1.20, 0, 1)` (inverse of `HomelanderHero.onLanded` mapping), screen shake and particle count scaled by `s`.
- [ ] Unit test for the mapping helper.

**User in-game:** small drops thud softly, big drops boom, no Warden sound.

**Rollback boundary:** revert PR 14 (restore old ogg).

### Stage 15: multiplayer, performance and integration pass (PR 15)

**Goal:** close gaps across stages.

**Steps:**
- [ ] Late tracking: observer entering range mid-flight/mid-milk/mid-laser: pose state starts from `ClientFlightState` (hover), laser via existing channel UPDATE-opens-channel behaviour, milk/clap one-shots are skipped if the start event was missed (acceptable, documented).
- [ ] Session reset: `ClientSessionState` clears all new caches; dimension change clears `RenderedPoseCache`.
- [ ] Profiling hooks: count variable supplier calls per frame in a debug flag; ensure `HomelanderPoseState.read` is O(1) (cache last UUID → state).
- [ ] README: dependency note that EMF+ETF are required for the supported client configuration.
- [ ] `qualityGate`, update `SESSION.md`, bump `mod_version`.

**Rollback boundary:** revert PR 15 only.

---

## 8. PR breakdown

| PR | Depends on | Scope | Explicit exclusions | Acceptance |
|---|---|---|---|---|
| 1 EMF foundation | baseline | deps, jem, bridge, ownership, anchor cache, full removal of legacy Homelander animation resources/calls | any new authored motion, gameplay | §7 Stage 1 checks green, no legacy Homelander animation path remains |
| 2 HOVER | 1 | hover loop + active weight | boost, takeoff | hover visible, frame-rate independent |
| 3 TAKEOFF | 2 | takeoff one-shot | gameplay lift | no snap into hover |
| 4 BOOST | 2 | directional boost weight | trail/sound gating | no belly-up, hysteresis |
| 5 Transforms | 4 | `FlightPoseMath.directional` | camera | backward lean, bank, bounded |
| 6 Camera | 5 | framing offset | rotation changes | body centred, no jumps |
| 7 Clap | 1 | clap clip + approved delayed hit | damage/range/cooldown values | impact at 1.50 s |
| 8 Milk | 1 | milk clip, props, approved 126-tick duration | first-person | full sequence |
| 9 Laser sound | baseline | single loop + fade | laser visuals | no stacking |
| 10 Laser damage | baseline | approved 10-tick cadence + 8–16 DPS | madness explosions | 2 hits/s, lower DPS |
| 11 Laser impact | baseline | data tuning | new VFX system | small impact |
| 12 Burn marks | 10 | persistent decals | block damage | persist + sync |
| 13 Trail | 4 | soft trail, rings | new graphics framework | smooth, bounded |
| 14 Landing | baseline | sound + mapping | detection changes | scaled audio |
| 15 Integration | all | MP/perf pass | new features | full test matrix |

Order change vs the brief: none, except PRs 9 to 11 and 14 have no EMF dependency and may run in parallel with 2 to 8.

---

## 9. Test matrix (user-owned in-game; agents only run automated checks)

Flight: idle standing; takeoff from ground; takeoff from jump; stationary hover; slow forward; fast forward; backward; left strafe; right strafe; diagonal; vertical ascent; descent; rapid direction changes; start/stop spam; F5 front; F5 back.

Multiplayer (dedicated server, two clients): observer watches takeoff, hover, boost, backward, clap, laser, milk, trail, burn marks; observer joins mid-flight.

Transitions: takeoff → hover; hover → boost; boost → hover; hover → backward; backward → forward; flight → laser; flight → clap; flight cancel (toggle, damage, landing).

Visual regressions: normal skin; slim skin; armor (all tiers); cape; held items (sword, shield, bow draw); first-person arms; spectator; death animation; relog; non-Homelander players unchanged. Supported client testing always includes EMF+ETF.

Automated (agents): `python3 -m unittest art-source/homelander/emf/test_bake_jem.py`; `./gradlew test --no-daemon`; `./gradlew qualityGate --no-daemon` (includes gametests and ArchUnit).

---

## 10. Performance risks

- Jem expression size: about 70 channels × 5 clips × up to 127 samples. EMF compiles expressions; still, Stage 1 measures frame time with 4 Homelanders on screen (user) and falls back to 10 Hz sampling if needed (the bake test re-checks error).
- Variable suppliers are called per expression evaluation: `HomelanderPoseState.read` caches the last UUID and returns preallocated floats; no boxing beyond EMF's `Supplier<Float>` (use cached `Float` instances only if profiling shows GC pressure; otherwise the `BooleanSupplier`/float factory path via `UniqueVariableFactory` is the fallback).
- `RenderedPoseCache`, burn marks, trail points: fixed-size arrays.
- Networking: no new per-tick packets; clap/milk add one or two events per use; burn marks at most 6/s per caster, batched.
- No render-frame raycasts added; burn marks come from the server hit.

## 11. Multiplayer risks

- Remote velocity comes from interpolated positions (jitter at low speed): smoothing + hysteresis; boost needs sustained forward speed.
- Missed one-shot events on late tracking: accepted, documented.
- Clock drift between clients: one-shot clocks start from event receipt, loops are cosmetic; acceptable.
- Server-delayed clap: hit timing identical for everybody because the server applies it.
- There is no no-EMF Homelander animation fallback. Dedicated-server compatibility must be preserved without reintroducing legacy player animations.

## 12. Acceptance criteria (whole plan)

1. Homelander rendered by EMF from the shipped jem with the authored hierarchy (forearms, shins, feet, knees, mouth, bottle, cap) on normal and slim skins; other heroes and players unchanged. No legacy Homelander `.animation.json` or Homelander `PlayerAnimator` animation path remains.
2. No frame where both `PlayerAnimator` and EMF drive the same Homelander part (code gate + ArchUnit).
3. HOVER, TAKEOFF, BOOST, HAND CLAP, MILK DRINK play with authored timing (tests assert lengths 3.2/0.8/2.6/1.92/6.3 s and impact 1.50 s).
4. BOOST only on forward motion relative to body yaw; backward stays upright with ≤ 8° lean; no belly-up in any tested direction (unit tests on `directional`).
5. F5 framing centred on the body in flight, unchanged on the ground.
6. Laser: one continuous loop with fade; damage every 10 ticks with the user-approved DPS; small impact.
7. Burn marks persist across relog and server restart, synced, capped at 2048 per dimension.
8. Trail soft and bounded; rings only above ring speed and at least 12 ticks apart.
9. Landing sound scales with `LandingImpact` intensity.
10. `./gradlew qualityGate --no-daemon` green on every PR; `mod_version` bumped per delivered jar; `SESSION.md` updated.
11. User confirms the in-game matrix (§9); ~100 FPS on the normal test scene.

---

## implementation handoff

- **Start from:** commit `2dc02edc` (head of PR #138, branch `origin/devin/1790691307-homelander-vfx-fix`). If PR #138 has been merged into `main` by then, start from `origin/main` after that merge. Never start from, merge, or cherry-pick `origin/devin/1790767119-homelander-emf` (PR #140).
- **Implement first:** Stage 1 (EMF foundation). Begin with its spike (§4.7) and the bake script tests; report spike results in the PR before wiring mixins.
- **Do not touch:** other heroes' presentation, `PlayerAnimator`/`AnimationLibrary`/`BedrockAnimationParser`/`PoseSample`/`PlayerPoseApplier` internals for other heroes, `ClientFlightState`, `FlightPhaseResolver`, `FlightTrailManager`, `src/main/generated/`, gameplay constants outside the already-approved changes in Stages 7, 8, 10, the reverted presentation-phase approach (`5c3be771`). **Stage 1 must delete all legacy Homelander `.animation.json` files and remove their Homelander call sites.**
- **Sources of truth:** this plan; `art-source/homelander/emf/Homelander_All_Animations.bbmodel` (after Stage 1 copies it); `AGENTS.md`; `SESSION.md`; EMF docs `.github/emf_animation.txt`, `.github/emf_part.txt`, `FEATURES.md` and `EMFAnimationApi.java` in `Traben-0/Entity_Model_Features` at `mod_version=3.3.9`.
- **After Stage 1 is verified by the user:** Stage 2 (HOVER). Stages 9, 10, 11, 14 may run in parallel at any time because they do not depend on EMF. Clap timing, milk duration and laser damage cadence/DPS are already approved by the user; no further decision gate is needed for Stages 7, 8 or 10.
