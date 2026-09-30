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
| `mixin/PlayerRendererMixin` | Whole-body flight pitch/roll on the `PoseStack` (`Axis.XP -pitch`, `Axis.ZP -roll`) | yes | keep as the only owner of whole-body rotation; at `PlayerRenderer.render` TAIL capture the EMF-animated parts into `RenderedPoseCache` for owned players (Stage 1, §4.6) |
| `core/flight/FlightPresentation` (record), `FlightPresentations` (registry) | Hero id → clips, trail/boost effect ids, sounds | Homelander is the only registrant today, but the seam is shared | keep the record shape; Homelander keeps registering it for sounds/trail; clip playback is skipped for EMF owners (Stage 1) |
| `core/flight/FlightPoseTracker` | Per-entity render velocity estimate, clip crossfade, loop sound, trail/boost start, smoothed `FlightBodyTransform` | shared | keep; add EMF-owner skip for clip playback and a directional transform path (Stage 5) |
| `core/flight/FlightPoseMath` | `target(phase, velocity, yawRate, params)` and `step(...)` (half-life smoothing, frame-rate independent) | shared | keep `target`; add a pure `directional(...)` overload (Stage 5). PR #138 removed the descent pitch inversion: do not reintroduce it |
| `ClientFlightState` | Synced `active/mode/phase/horizontalSpeed` + presentation ownership | shared | keep, untouched |
| `fx/FlightTrailManager` | Legacy END_ROD/CLOUD trail, bypassed when presentation-owned | yes | keep, untouched |
| `core/vfx/anchor/HumanoidAnchors` | Eye/body points from vanilla offsets + body yaw + head rot + flight tilt | shared | keep; for owned entities consume the captured absolute head transform (pivot offset + rotation in model space) from `RenderedPoseCache` instead of appending to the vanilla yaw/pitch formula (Stage 1, §4.6) |
| `core/vfx/pattern/TrailPattern`, `ShockwavePattern`, `ImpactPattern`, `BeamPattern` | Reusable VFX patterns | shared | `TrailPattern` gets an additive soft-profile mode (Stage 13) |

### 1.2 Homelander presentation paths that touch the model today

| File (`client/hero/homelander/fx/`) | Current baseline behaviour | Stage 1 fate |
|---|---|---|
| `HomelanderFx.register` | registers legacy flight clips `flight_takeoff`, `flight_hover`, `flight_cruise`, `flight_boost`, `flight_land` through `FlightPresentation` | remove Homelander legacy flight clip playback/reference path; keep only the non-animation presentation pieces that are still needed |
| `HomelanderFx.milkDrink` | plays legacy `homelander/milk_drink` | remove legacy animation call; new MILK animation arrives in Stage 8 |
| `ClapFx.create` | plays legacy `homelander/clap` and reads its contact time | remove legacy animation dependency; new authored clap arrives in Stage 7 |
| `EyeLaserChannel` | plays `laser_charge → laser_hold → laser_release` and samples `PlayerAnimator` for eye-anchor compensation | remove all laser player-animation play/stop/sample usage; keep laser gameplay/VFX/audio channel; anchors must use the EMF-rendered pose |
| `IronFistsFx` | plays `iron_fists_activate` and `iron_fists_strike` | remove player-animation calls; gameplay/VFX/sounds remain with no player animation until a new EMF clip is authored |
| `RoarFx` | plays legacy `roar` | remove player-animation call; gameplay/VFX/sounds remain with no player animation until a new EMF clip is authored |
| `SunChargeFx` | plays legacy `sun_charge` | remove player-animation call; gameplay/VFX/sounds remain with no player animation until a new EMF clip is authored |
| `FlightFx` | flight VFX and landing presentation use current anchors/tilt | keep VFX functionality; landing has no player animation after Stage 1 until a new EMF landing clip is authored |

Legacy resources currently present under `src/main/resources/assets/superheroes/player_animations/homelander/`: `clap.animation.json`, `flight_boost.animation.json`, `flight_cruise.animation.json`, `flight_hover.animation.json`, `flight_land.animation.json`, `flight_takeoff.animation.json`, `iron_fists_activate.animation.json`, `iron_fists_strike.animation.json`, `laser_charge.animation.json`, `laser_hold.animation.json`, `laser_release.animation.json`, `milk_drink.animation.json`, `roar.animation.json`, `sun_charge.animation.json`. **Stage 1 deletes every one of them and removes every Homelander code reference to them.** There is no legacy Homelander animation fallback and no old-animation-to-EMF bridge.

New authored replacements in this plan exist only for HOVER, TAKEOFF, BOOST, HAND CLAP and MILK DRINK. Laser, iron fists, roar, sun charge and landing intentionally remain without player animation until new EMF-authored source animations are created later.

### 1.3 Server gameplay seams (keep)

- Flight: `mechanic/flight/FlightController` (sets `FlightPhase.TAKEOFF` at start), `FlightPhaseResolver` (`TAKEOFF_TICKS = 8`, hover < 0.08 b/t, BOOST at `SUPERSONIC` or ≥ 1.1 b/t, absolute speed), `FlightMotionMath`. Client receives `ClientFlightState.update(entityId, active, mode, phase, horizontalSpeed)` for local and tracked players.
- Clap: `hero/homelander/ability/HandClapAbility` (`RANGE 18`, cone 45°, `DAMAGE 20`, `KNOCKBACK 3.5`, `COOLDOWN_TICKS 240`), damage applied instantly in `tryActivate`, then `VfxFx.event(player, CLAP, origin, origin + forward*18, 1)`.
- Milk: `hero/homelander/item/MilkBottleItem` (`DRINK_TICKS = 32`, `UseAnim.DRINK`, MADNESS 15 s in `finishUsingItem`), `VfxFx.event(MILK_DRINK)` at use start.
- Laser: `EyeLasersAbility` (`RANGE 64`, `MIN_DPS 56`, `MAX_DPS 120` scaled by mana fraction, `/20` per tick, `MADNESS_DAMAGE_MUL 3`, uranium pulse cycle 80 ticks), `target.hurt(...)` called every firing tick; `VfxFx.channel` START / UPDATE every `CHANNEL_UPDATE_INTERVAL_TICKS = 2` / STOP. **Every call lands:** `superheroes:eye_laser` is registered with `DamageTypeTags.BYPASSES_COOLDOWN` (`HomelanderDamageTypes` + generated `data/minecraft/tags/damage_type/bypasses_cooldown.json`), so `invulnerableTime` never gates it — there is no 2-hit/s throttling. True effective DPS today is the full 56 to 120 HP/s (madness 168 to 360) with continuous hurt feedback. Stage 10's cadence is therefore a real ~7 to 15× damage nerf plus a feedback change from continuous to pulsed, not merely making an existing cadence explicit.
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
| MILK DRINK · unscrew → sip → lower | 6.3 s | hold | linear | 7980 | cap starts moving ~1.27 s; `mouth_open` scale > 0 from ~2.07 s to ~5.83 s; bottle/cap under right_forearm |

- `emf_lab` block: `target "Minecraft 1.21.1 Fabric / EMF 3.3.9"`, `runtime_verified: false`, `preview_only: true`, expressions using `keyframeloop`, `cubicbezier`, `clamp`, `torad`, `sin`, `if`, custom `var.*`. These expressions assume the Blockbench parent chain (root → body → head/arms), which does not exist in a player CEM (see 4.3). They are not used at runtime; the bake script regenerates equivalent expressions from the native keyframes.

---

## 4. EMF integration analysis (verified)

Sources checked: EMF repository `Traben-0/Entity_Model_Features` (cloned, `mod_version=3.3.9`, `etf_version=7.2.2`, `EMFAnimationApi` API version 11), `.github/emf_animation.txt`, `.github/emf_part.txt`, `FEATURES.md`, Modrinth API.

### 4.1 Versions and packaging
- EMF `3.3.9-fabric-1.21` (Modrinth: game versions 1.21.x, loaders fabric, quilt), `environment: "client"`, `license LGPL-3.0` (jar-in-jar redistribution permitted with the license text), `depends: entity_texture_features >= 7.2`, `breaks: optifabric, cem, entity_texture_features <7.2, entity_sound_features <0.8.2`, `accessWidener` present. Coordinate: `maven.modrinth:entity-model-features:3.3.9-fabric-1.21` (POM 200).
- ETF `7.2.4-fabric-1.21`, `environment: "client"`, LGPL-3.0, no extra depends. Coordinate: `maven.modrinth:entitytexturefeatures:7.2.4-fabric-1.21` (POM 200). EMF compiles against ETF 7.2.2; 7.2.4 satisfies its `>=7.2` range.
- **Mandatory-client packaging (verified against fabric-loader 0.19.5 `ModResolver`/`ModSolver`):** ship EMF+ETF jar-in-jar via `modImplementation` + `include` in `build.gradle` (same mechanism as GeckoLib) AND declare `depends: { "entity_model_features": ">=3.3" }` in `fabric.mod.json`. On the client the nested jars are first-class mods and satisfy the dep (and a user-installed newer EMF also satisfies it). On a dedicated server the nested mods are discovered, land in `envDisabledMods` because their own `fabric.mod.json` says `environment: "client"` (no classpath, no mixins, their deps unevaluated), and a positive dep on a *present-but-env-disabled* mod is silently softened from `depends` to `suggests` before solving — so the server never crashes and the client requirement is still enforced. A `depends` on a completely absent EMF would crash the dedicated server; the softening applies precisely because the jar is bundled. Alternatives considered and rejected: (a) `modCompileOnly` + `isModLoaded` gate — makes EMF optional again, forbidden by the brief; (b) `recommends`/`suggests` — no version enforcement; (c) a nested client-only `fabric.mod.json` carrying the dep — works but adds a second mod descriptor for no gain.
- EMF mod id `entity_model_features`, ETF mod id `entity_texture_features`. The strategy above makes EMF+ETF mandatory for the supported client while keeping the dedicated server valid. **Do not implement a Homelander legacy animation fallback when EMF is absent** — with jar-in-jar it cannot be absent on the client anyway.

### 4.2 Player CEM
- Registered player-model ids (EMFModelMappings `genericPlayerBiped`, 14 parts): `head, headwear, body, left_arm, right_arm, left_leg, right_leg, ear, left_sleeve, right_sleeve, left_pants, right_pants, jacket, cloak`. We ship exactly two files: `player.jem` and `player_slim.jem`. `player_slim` is selected by vanilla from the player profile and falls back to `player.jem` when absent — without it, slim-skinned players would get the wide-arm jem (misaligned arms), so both are required. `player_cape.jem` is intentionally not shipped (vanilla cape path untouched); the jem must NOT declare a `cloak` part — inside `player.jem` it would render a static cape that does not cancel vanilla cape rendering (FEATURES.md caveat). `player_ears.jem` does not exist on 1.21.1 (1.21.2+ only; the `emf_model.txt` part list is stale).
- Lookup folders: `assets/minecraft/emf/cem/` (EMF-only, preferred) and `assets/minecraft/optifine/cem/`; optional subfolder `<name>/<name>.jem`. One winner per model name by pack index; on ties `emf/` beats `optifine/`. Mod jar assets sit at the lowest priority, so a user resource pack's own player jem wins wholesale — no merging (documented risk).
- Custom parts: submodels with `id` under a vanilla part; animatable as `<id>.rx` etc.; `visible`, `visible_boxes`, `sx/sy/sz`, `tx/ty/tz`, `rx/ry/rz`. `part` attaches the custom part under a vanilla parent; `attach: false` clears the vanilla part's cubes (replace), `attach: true` adds. Custom parts CAN nest (`submodel`/`submodels`).
- Attachments for player: `right_handheld_item`, `left_handheld_item`, `head_item` declared per part as `"attachments": {"<type>": [tx,ty,tz]}` (pixels); verified to work on custom submodels — `EMFModelPartCustom.getAttachmentPositioner` composes `translateAndRotate` up the parent chain, so an item attached to `right_forearm` follows the forearm.
- Armor: on 1.21.1 EMF's armor-layer mixins are disabled (`CancelTarget` stubs); armor pose copying is pure vanilla `HumanoidArmorLayer` → `parentModel.copyPropertiesTo(armorModel)` over the six biped parts + headwear. It copies the *post-animation* field values, so armor follows the EMF-animated carriers — but never custom submodels (a forearm bend does not move the armor sleeve — documented limitation). `player_inner_armor.jem`/`player_outer_armor.jem` exist as ids but we do not ship them.

### 4.3 Expressions and variables
- Channel application is an **absolute write**: expression results replace the part's fields (`EMFModelOrRenderVariable.RX.setValue` → `part.xRot = value`), no blending with the vanilla pose. On the ASM path (default `asmMaths=true`) all expressions evaluate into a result array inside `compiledAnimationExecutor.execute(...)` and the channel appliers write afterwards, so `right_arm.rx` referenced inside any expression reads the field as vanilla `setupAnim` left it — the correct vanilla↔authored blend is `lerp(w, right_arm.rx, <baked>)`, and `w = 0` reproduces vanilla exactly. Keep expressions independent of write order: never read a channel that another animation line writes.
- `keyframe(k, v0, v1, …)` / `keyframeloop(k, v0, v1, …)`: first arg is a continuous **frame index** (not seconds — expressions pass `clip_t * 20.0` for a 20 Hz bake); both interpolate with `Mth.catmullrom` over the 4-point neighbourhood — there is no linear variant. `keyframe` clamps at the ends, `keyframeloop` wraps modulo. Constant-index calls fold at parse. The 20 Hz sample table re-curved by Catmull-Rom is covered by the bake test tolerance.
- Units: `rx/ry/rz` are **raw radians** written into `xRot/yRot/zRot`; `tx/ty/tz` are pixels (`+ty` moves the part *down*, vanilla model space); `sx/sy/sz` are scale factors; `head_yaw`/`head_pitch` builtins are **degrees** while `rot_x`/`rot_y` are radians; jem `rotate`/`translate` arrays take degrees/pixels. The bake emits radians into channel tables and degrees into model `rotate` fields — never mix.
- `visible`/`visible_boxes` are boolean-typed channels: the write is `part.visible = toBoolean(value)` / `part.skipDraw = !toBoolean(value)` and `toBoolean` accepts only the boolean sentinels (comparisons, `varb.*`, `is_*` vars, `false`/`true`) — a numeric `0`/`1` throws and disables that animation for the whole model. Emit `1=0`/`1=1`-style expressions (or `is_first_person_hand`-gated forms), never numbers. `visible` hides the subtree, `visible_boxes` only the part's own cubes.
- Mod API (all verified present in the 3.3.9 jar): `EMFAnimationApi.registerSingletonAnimationVariable(String modId, String name, String explanation, Supplier<Float>|BooleanSupplier)` — the name is stored **verbatim** (no mod-id prefix; `var.`/`varb.`/`global_var.` prefixes are for jem-internal vars, not API vars; `enforceOptiFineAnimSyntaxLimits` forbids names starting with a digit or `_` — `superheroes_hl_*` is legal). `getCurrentEntity()` → `EMFEntity` (`etf$getUuid()`) during animation eval. `registerVanillaModelCondition(Function<EMFEntity,Boolean>)` — true → that entity renders variant 0 (the untouched vanilla part set), evaluated per entity per render pass via `EMFState.isEntityForcedToVanillaModel`. `lockEntityToVanillaModel`/`unlockEntityToVanillaModel` (UUID set), `registerAnimationHook(EMFAnimationHook)` (once per animation run; the hook's `AnimationContext.animatingModelRoot` exposes the live part tree), `pauseAllCustomAnimationsForEntity`/`pauseCustomAnimationsForThesePartsOfEntity`, `animateModelForEntity`, `isModelPartAnimatedByEMF`. Singleton suppliers evaluate inside the current entity's `animate()` call on the render thread, so a supplier looks up per-entity state by `getCurrentEntity()`.
- Other built-ins confirmed in `VariableRegistry`: `frame_time` (seconds), `is_first_person_hand` (this render pass is the FP hand), `is_player_first_person`/`is_player_third_person` (camera), `is_in_gui` (paperdoll/inventory render — EMF also zeroes `head_yaw`/`limb_*`/`move_*`/`rot_*` there), `is_in_hand`, `is_on_head`, `is_on_shoulder`, `nan`, `limb_swing`, `limb_speed`, functions `lerp`, `clamp`, `if`, `torad`, `todeg`, `sin`, `cos`, `max`, `min`, `bezier`, `catmullrom`, `ease*`, `random`, `between`, `equals`.
- Part names inside a player CEM are flat top-level vanilla parts. There is no `root` and a vanilla part cannot be parented under another vanilla part, so the Blockbench chain root → body → head/arms cannot be reproduced directly.

### 4.4 Model hierarchy decision
Chosen: **baked flat hierarchy on vanilla carriers**. The bake script composes each part's **actual authored ancestor chain** — `root × body × part` for `head`, `right_arm`, `left_arm`, `body`, but `root × part` for `right_leg`/`left_leg` (legs hang under `root`, **not** `body`, per the §3 hierarchy — composing them through `body` would inject the authored torso lean, ±21° in BOOST, into every leg carrier). Composition walks the parsed group tree; nothing is hand-coded per part. Rotation composed in ModelPart's `ZYX` order with pivot-corrected translation; sub-bones (`*_forearm`, `*_shin`, `*_foot`, `*_knee`, `mouth_open`, `milk_bottle`, `milk_cap`) stay as custom submodels of their carrier with their authored local tracks (`attach: false` on carriers that replace vanilla cubes with authored geometry). Result: exact authored world pose, armor/held items/sleeves follow the six carriers, eye anchors stay on `model.head`.

Rejected: (a) everything as custom submodels under `body` with vanilla boxes hidden (exact hierarchy, but armor, cape, held items, first-person arms and `HumanoidAnchors` detach from the visible model); (b) reduce to six vanilla bones (destroys forearm/shin/foot/prop motion, forbidden by the brief); (c) use `emf_lab` preview expressions (unverified, assume the Blockbench chain).

Root rotation split: authored root **rotation** in BOOST (-86° rx) is whole-body orientation, and `PlayerRendererMixin` already owns whole-body rotation. The bake script removes the clip-mean root rotation `meanR` from the baked parts and writes it to `vfx/homelander/flight.json` as `emfBoostRootPitch = 86.0` / `emfBoostRootRoll`; `FlightPoseMath` then uses it as the BOOST pitch target. The residual (±0.15°, ±0.2°) stays baked. Root **translation** (hover +6.6 px, boost +9 px, takeoff dip/rise) stays baked.

Pivot compensation (required, or BOOST renders displaced): the authored root rotates about its pivot `[0,12,0]`; the renderer re-adds `meanR` about the entity's feet origin. Rendered-vs-authored difference without compensation is `p − meanR·p` ≈ `(0, +11.2 px, +12.0 px)` for `meanR = rotX(-86°)` — the body would float ~0.70 block up and ~0.75 block off in Z. For exactness the bake therefore folds the constant offset `meanR⁻¹·p − p` into the affected carriers' translation channels for that clip (one 3-vector per clip: baked carrier transform `= T(meanR⁻¹·p − p) · T_res · chain`, so `RotAboutFeet(meanR)` applied at render reproduces `RotAboutPivot(meanR)` up to the residual). The bake test asserts each composed carrier equals the authored part transform at every sampled frame within tolerance.

### 4.5 Where state lives
- Codex client state (`HomelanderPoseState`): clip clocks and blend weights, directional pitch/bank, computed once per client tick + partial-tick interpolated at read time.
- EMF variables (singletons, Codex-registered): expose those numbers to the jem. Suppliers evaluate per expression evaluation on the render thread inside the current entity's `animate()` call — same thread as the client tick for an integrated client, so reads are race-free.
- EMF expressions (jem): only sampling (`keyframe`/`keyframeloop`) and blending (`lerp`), plus `is_first_person_hand` branches on arm/prop channels where first-person must stay vanilla. No state machines in the jem.
- First-person: EMF animates FP hands by default (`preventFirstPersonHandAnimating` defaults to false; `MixinPlayerEntityRenderer` mounts a manual `isFirstPersonHand` state for `renderHand`). Channels whose authored pose must not leak into first person (arm/hand carriers during MILK/CLAP, `milk_bottle`/`milk_cap`/`mouth_open` visibility) branch on the built-in `is_first_person_hand` boolean variable — we do not rely on the user-facing config.
- GUI/paperdoll: an owned player's inventory/paperdoll render receives the authored pose too (same entity, `is_in_gui` only zeroes EMF's own `head_yaw`/`limb_*`/`rot_*` builtins). Accepted: the pose shown matches the entity's real state. If it ever needs suppressing, weights can gate on `is_in_gui` — noted, not implemented.

### 4.6 Anchor seam (verified in the 3.3.9 source)
EMF animation timing on 1.21.1: `EMFModelPartWithState.render()` calls `root.animate()` on the first rendered part — deduplicated per render pass by `lastMobCountAnimatedOn != entityRenderCount` — so `animate()` runs **inside `renderToBuffer`, strictly after `setupAnim`**. `model.head`/`body`/arms on a jem'd player model are `EMFModelPartVanilla` instances (EMF swaps the part objects at `EntityModelSet.bakeLayer`, recursively wrapping every vanilla child), so after `renderToBuffer` their `x/y/z/xRot/yRot/zRot` fields hold this entity's animated values. A `setupAnim` TAIL injector sees *pre*-animation values; the cache must capture **after** `renderToBuffer` — concretely a TAIL inject on `PlayerRenderer.render` (the method is overridden in 1.21.1 and is a valid mixin target), after the feature-layers loop. The model is shared across all players on the same renderer, so the cache snapshots per entity id at that point — the values used by same-frame VFX draws (beams render later in the world pass). Iris shadow/outline re-renders re-run `animate()` and re-capture — idempotent. An off-screen entity keeps its last rendered pose (LRU evicts, §6 cap 64).
`RenderedPoseCache` (Stage 1) copies `head/body/arms` `x,y,z,xRot,yRot,zRot` into a fixed `float[]` per entity id at that TAIL point. There is no public EMF API returning transformed part matrices; the jem `attachments` + `EMFModelPart.getAttachmentPositioner` path is a possible alternative but composes the live PoseStack during held-item rendering only — the field snapshot is simpler and independent of render context.
Anchor consequence: under `hl_w = 1` the authored head transform *replaces* both the vanilla `headYaw`/`headPitch` inputs and the `HEAD_PIVOT` placement (the authored pivot is lifted by root translation and tilted by body lean). `HumanoidAnchors.eyes` for owned entities must therefore consume the captured **absolute** head transform — pivot offset plus rotation in model space — from `RenderedPoseCache` and compose body-yaw + renderer tilt around it. Appending `headAnimDeg`/`headOffsetPx` scalars onto the vanilla analytic formula would double-apply head motion and anchor at the wrong pivot. The vanilla path stays for non-owned callers.

### 4.7 Verified answers + remaining open points
Resolved from the published EMF 3.3.9 jar and source (`v3.3.9` tag, effective-1.21.1 tree) and fabric-loader 0.19.5:
- `animate()` runs inside `EMFModelPartWithState.render` during `renderToBuffer`, after `setupAnim` (§4.6) — `PlayerModelPoseMixin`'s TAIL output is overwritten for EMF-assigned channels.
- Channel writes are absolute; expression reads see the post-`setupAnim` vanilla field values (§4.3) — the `lerp(w, part.rx, baked)` shape is correct EMF idiom.
- `keyframe`/`keyframeloop` are always Catmull-Rom, first arg = frame index (§4.3).
- API variable names are literal — `superheroes_hl_*` registered via `registerSingletonAnimationVariable` is referenced bare (§4.3); `_`-containing names are legal (only leading digit/`_` is rejected by `enforceOptiFineAnimSyntaxLimits`).
- `visible`/`visible_boxes` are boolean-typed; numeric `0`/`1` throws and disables the animation (§4.3).
- `TY` writes `ModelPart.y` verbatim — no inversion at the API level; the Blockbench→ModelPart axis conversion is covered by the bake test, not a spike.
- `registerVanillaModelCondition` is evaluated per entity per render (§4.3); the model instance is shared, variant switching swaps *structure* (children/cubes/state), not fields.
- First-person hands animate by default; `is_first_person_hand` exists (§4.5).
- JiJ packaging + softened `depends` semantics verified in `ModResolver`/`ModSolver` (§4.1).
- `player_slim` falls back to `player.jem`; `player_cape`/`cloak`/`player_ears` caveats (§4.2).

Remaining checks for Stage 1 (spike):
1. Jar-in-jar env skip: on the dedicated classpath (`runServer` or gametest smoke) the bundled EMF/ETF mods land in `envDisabledMods` — nothing loads, no `traben.*` class loading, and the `depends` softens. Client side: both mods load from inside our jar.
2. EMF LOD (`EMFLODHandler`): distant entities may skip `animate()` and reuse `lastResultsPerEntity` — check the default LOD setting is off or harmless for a handful of player models.
3. One literal check in the generated jem: `false`/`true` literals must parse as booleans for `visible` channels; if not, use `1=0`/`1=1` forms (contract test covers either).

---

## 5. Key conflicts and prevention

| Conflict | Risk | Prevention |
|---|---|---|
| EMF vs `PlayerAnimator` | Both write `head/body/arms/legs` → jitter, double poses | Homelander is cut over completely in Stage 1: `PlayerModelPoseMixin` does not apply `PlayerAnimator` to EMF-owned Homelander, every Homelander legacy play/stop/sample call is removed, and all Homelander legacy animation resources are deleted. No bridge between the two animation engines. |
| EMF root vs procedural tilt | BOOST -86° baked + `boostPitch 80` in renderer → 166°, belly-up; and authored pivot `[0,12,0]` vs renderer feet origin → ~0.7-block displacement | Mean root rotation stripped at bake time and moved to `flight.json`; bake folds the constant `meanR⁻¹·pivot − pivot` compensation into carrier translations (§4.4); renderer remains the only whole-body rotation |
| Flight presentation vs eye anchors | Beams leave the vanilla head position while the EMF head moved | Anchors take head offsets/rotation from `RenderedPoseCache` (captured after EMF) plus renderer tilt |
| Body pitch/bank vs F5 camera | Camera follows eye position that swings with tilt → nausea | Camera anchor = smoothed body centre in world space, independent of tilt; tilt never feeds camera rotation |
| Custom arm hierarchy vs held items | Items float at the upper-arm pivot while forearm bends | `right_handheld_item`/`left_handheld_item` attachments declared on the forearm submodels; held item hidden during MILK (authored bottle replaces it) |
| Custom model vs armor and skin layers | Armor ignores forearm/shin bends; overlays desync | Carriers keep armor aligned at the shoulder/hip; overlay boxes baked into the same custom parts with `sizeAdd 0.25`; `headwear/jacket/sleeves/pants` vanilla parts hidden in the jem to avoid double layers; forearm armor bend is a documented limitation |
| Remote animation vs local prediction | Local uses input, remote only sees positions → different poses | All pose inputs come from the same sources for every player: synced `ClientFlightState`, `VfxFx` events, render-interpolated position delta. No keyboard input in `HomelanderPoseState` |
| Trail orientation vs velocity source | Server velocity is zero for remote players; pos delta jitters at low speed | One smoothed velocity per entity in `HomelanderPoseState` (half-life 3 ticks) feeds pose, rings and trail; rings require speed ≥ threshold for N consecutive ticks |
| Non-Homelander players and the shipped `player.jem` | Every player gets the Homelander jem | Two-layer mitigation: `EMFAnimationApi.registerVanillaModelCondition(e -> !isOwned(e))` makes non-owners render variant 0 (the untouched vanilla part set — evaluated per entity per render); and every expression is `lerp(superheroes_hl_w, <vanilla value>, <authored>)` so `w = 0` reproduces vanilla exactly. Texture is the entity's own skin (no `texture` field in the jem → `customTexture` null → per-entity skin). Also covers NPC-type entities rendered through the player layers |
| Other resource packs with a player jem | Pack overrides Homelander | Documented in README; out of scope |

---

## 6. Target Homelander presentation architecture

New files (all client unless stated):

- `client/core/emf/EmfBridge.java`: `isAvailable()`, `registerFloatVariable(String name, String explanation, Supplier<Float>)`, `currentEntityUuid()` (nullable), `registerVanillaModelCondition(Predicate<UUID>)` — adapts `EMFAnimationApi.registerVanillaModelCondition(Function<EMFEntity,Boolean>)` by resolving the uuid to a player (`level.getPlayerByUUID`-style; evaluated per entity render). The only class under `client/core` that imports `traben.*`. Every method no-ops when EMF is absent.
- `client/core/emf/EmfPresentationOwnership.java`: `static void register(Predicate<AbstractClientPlayer> owner)`, `static boolean isOwned(AbstractClientPlayer)`. Hero-agnostic; `false` when EMF is absent.
- `client/core/emf/RenderedPoseCache.java`: `capture(int entityId, PlayerModel<?> model)` called at `PlayerRenderer.render` TAIL (post-`renderToBuffer`, so the EMF-animated fields are present — §4.6), `headOffsetPx(int id, Vector3f out)`, `headRotationRad(int id, Vector3f out)`, `clear(int id)`, `clearAll()`; fixed arrays, capacity 64 entities, LRU by last frame.
- `client/hero/homelander/emf/HomelanderPoseState.java`: per-entity state (`ClipClock` for HOVER/BOOST loops and TAKEOFF/CLAP/MILK one-shots; weights `active`, `boost`, `takeoff`, `clap`, `milk`); `tick()` from client tick, `read(UUID, float partial)`; directional values `forward`, `strafe`, `pitchDeg`, `bankDeg`. Pure logic in `HomelanderPoseMath` (unit-tested).
- `client/hero/homelander/emf/HomelanderEmfVariables.java`: registers only the variables required by the new EMF-authored HOVER/BOOST/TAKEOFF/CLAP/MILK presentation. It must not expose legacy `PlayerAnimator` bone channels.
- `client/hero/homelander/emf/HomelanderEmf.java`: `static void register(HeroClientContext ctx)` — wires the ownership predicate (`SkinResolver.heroIdFor(player)` equals Homelander — resolves remote players through `CoreAttachments.PUBLIC_HERO`, unlike `ClientHeroState` which is local-only), registers all variables and the vanilla-model condition (`!isOwned`).
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

Only one one-shot weight is non-zero at a time (`HomelanderPoseState` enforces it), so the generator emits `superheroes_hl_takeoff_w*keyframe(takeoff) + superheroes_hl_clap_w*keyframe(clap) + superheroes_hl_milk_w*keyframe(milk)` normalised by `max(wsum, 1e-4)` — the raw `wsum` denominator would NaN the channel while a one-shot fades to 0. Sample rate: 20 Hz (`keyframe` interpolates with Catmull-Rom, matching the author's 60 Hz curves within 0.5° on these clips; the bake test asserts the max error).

---

## 7. Staged implementation plan

Each stage: goal, current state, files, symbols, steps, constraints, automated verification, user in-game verification, rollback boundary.

### Stage 1: EMF foundation and Homelander model integration (PR 1)

**Goal:** EMF/ETF integrated for the supported client configuration; generated Homelander jem in the jar; Homelander (and only Homelander) owned by EMF; every legacy Homelander animation resource and every Homelander `PlayerAnimator` animation call removed; anchor cache in place. Abilities without a new EMF-authored clip temporarily have no player animation.

**Current state:** no EMF; `PlayerModelPoseMixin` applies `PlayerAnimator` to everybody.

**Files:**
- Modify `build.gradle`: `modImplementation "maven.modrinth:entity-model-features:3.3.9-fabric-1.21"` and `modImplementation "maven.modrinth:entitytexturefeatures:7.2.4-fabric-1.21"`, plus `include` for both (jar-in-jar — same mechanism as GeckoLib). Modrinth maven repo block already exists (Iris). Do NOT use `modCompileOnly`/`modLocalRuntime`: those leave EMF outside the jar, and a `depends` on a completely absent mod hard-crashes the dedicated server. With the jars bundled, the client-side dep resolves and the server-side dep softens to `suggests` (§4.1).
- Modify `fabric.mod.json`: `depends` += `"entity_model_features": ">=3.3"` (range must cover the bundled version; a user-installed newer EMF satisfies it too). Do not add `entity_texture_features` to `depends` — EMF's own `depends` covers it; adding a direct dep on a *completely absent* ETF on a server that somehow lost the nested jars would hard-crash. No `recommends`/`breaks` for EMF/ETF: EMF's own `breaks` (optifabric, cem, old ETF/ESF) only reach the solver when EMF is actually loaded, which is what we want.
- Add `art-source/homelander/emf/Homelander_All_Animations.bbmodel`, `bake_jem.py`, `test_bake_jem.py`.
- Add `src/main/resources/assets/minecraft/emf/cem/player.jem`, `player_slim.jem` (generated; slim differs only in arm widths 3 px and arm pivots per vanilla slim).
- Add `client/core/emf/EmfBridge.java`, `EmfPresentationOwnership.java`, `RenderedPoseCache.java`.
- Add `client/hero/homelander/emf/HomelanderPoseState.java`, `HomelanderPoseMath.java`, `HomelanderEmfVariables.java`, `HomelanderEmf.java` (`static void register(HeroClientContext ctx)`: ownership predicate = `SkinResolver.heroIdFor(player)` equals Homelander — resolves local players via `ClientHeroState` and remote players via `CoreAttachments.PUBLIC_HERO`, so observer-side rendering works; registers variables and the vanilla-model condition).
- Modify `client/hero/homelander/HomelanderClientModule.java`: call `HomelanderEmf.register(ctx)`.
- Modify `client/mixin/PlayerModelPoseMixin.java`: in the `setupAnim` TAIL handler, `if (EmfPresentationOwnership.isOwned(player)) return;` as the first statement (the HEAD restore still runs for every entity).
- Modify `client/mixin/PlayerRendererMixin.java`: at `PlayerRenderer.render` TAIL — after `renderToBuffer` and the feature-layers loop, where EMF's `animate()` has already written the parts (§4.6) — `RenderedPoseCache.capture(player.getId(), getModel())` for owned players.
- Modify `client/core/flight/FlightPoseTracker.java`: Homelander must no longer play/stop any legacy flight clip; sounds/trail remain.
- Modify `client/core/vfx/anchor/HumanoidAnchors.java` and `client/hero/homelander/fx/EyeLaserChannel.java` so eye anchors come from the EMF-rendered pose and do not depend on `PlayerAnimator.sample`: for owned entities `eyes()` consumes the absolute head transform (pivot offset + rotation in model space) from `RenderedPoseCache` and composes body-yaw + renderer tilt around it (§4.6); the vanilla `headYaw`/`headPitch` + `HEAD_PIVOT` path remains for non-owned callers.
- Remove Homelander legacy animation calls from `HomelanderFx`, `ClapFx`, `EyeLaserChannel`, `IronFistsFx`, `RoarFx`, `SunChargeFx` and any other Homelander call site found by repo-wide search.
- Delete every file under `src/main/resources/assets/superheroes/player_animations/homelander/`: `clap.animation.json`, `flight_boost.animation.json`, `flight_cruise.animation.json`, `flight_hover.animation.json`, `flight_land.animation.json`, `flight_takeoff.animation.json`, `iron_fists_activate.animation.json`, `iron_fists_strike.animation.json`, `laser_charge.animation.json`, `laser_hold.animation.json`, `laser_release.animation.json`, `milk_drink.animation.json`, `roar.animation.json`, `sun_charge.animation.json`.
- Modify `client/ClientSessionState` reset path: `RenderedPoseCache.clearAll()`, `HomelanderPoseState.clearAll()`.
- Tests: `src/test/java/.../client/hero/homelander/emf/HomelanderPoseMathTest.java`, `src/test/java/.../client/core/emf/RenderedPoseCacheTest.java`, `src/test/java/.../HomelanderJemContractTest.java` (parses the shipped jem: required parts/ids present, every custom id from the bbmodel present, no `NaN`/`nan`, all five clips present, no `emf_lab` expressions, no `cloak` part, every `visible`/`visible_boxes` channel is a boolean-valued expression — no numeric `0`/`1` — and both `player.jem` and `player_slim.jem` exist); extend the ArchUnit rule so `traben..` is only imported from `client.core.emf..` and `client.hero.homelander.emf..`.

**Steps:**
- [ ] Spike: close the remaining §4.7 items (JiJ env-skip on the dedicated classpath, LOD default, boolean literal in `visible` channels); record results in the PR description.
- [ ] Write `test_bake_jem.py` first: composing identity parents returns the child; per-part ancestor chains (legs compose `root × leg`, never through `body`); composing root rx -86° then removing the mean returns the residual; the pivot-compensation offset `meanR⁻¹·p − p` reproduces the authored pose after feet-origin rotation; ZYX Euler round-trip error < 1e-4 rad; 20 Hz resample error < 0.5° versus the 60 Hz source for every channel of every shipped clip, including the bezier-authored TAKEOFF channels; jem expressions use `keyframe(t * 20.0, …)` (first arg is a frame index, not seconds) and emit radians in `rx/ry/rz` channels, degrees in model `rotate` arrays; `visible`/`visible_boxes` channels carry boolean-valued expressions only (`1=0`/`1=1`/`is_first_person_hand` forms, never `0`/`1`); `player_slim.jem` arm cubes match the vanilla slim shape.
- [ ] Implement `bake_jem.py` (inputs: bbmodel path, `--slim`; outputs jem + printed root means). The bake implements its own `linear`/`step`/`catmullrom`/`bezier` evaluators — `BedrockAnimationParser` has no bezier support and TAKEOFF/SHOWCASE are authored with bezier easing — and hard-fails on Molang-valued keyframes. Run `python3 -m unittest art-source/homelander/emf/test_bake_jem.py` and the bake.
- [ ] Hide vanilla overlay parts in the jem (`headwear`, `jacket`, sleeves, pants) via boolean `visible` animation channels (`1=0`) because the baked carriers contain the overlay boxes; do not declare a `cloak` part.
- [ ] `is_first_person_hand` gating in the jem: arm/hand carrier channels during MILK/CLAP and the `milk_bottle`/`milk_cap`/`mouth_open` `visible` channels branch on `is_first_person_hand` so first-person arms and props stay vanilla (EMF animates FP hands by default).
- [ ] Implement bridge, ownership, cache and minimal pose state with the new EMF variables only; ownership predicate via `SkinResolver.heroIdFor`; gate the legacy model mixin for Homelander. There is no `legacy_w` and no legacy bone-variable bridge.
- [ ] `./gradlew test --no-daemon`, then `./gradlew qualityGate --no-daemon`.

**Constraints:** no new authored motion yet; no gameplay changes. Old Homelander player animations are intentionally gone. Laser, iron fists, roar, sun charge and landing may temporarily have no player animation.

**Automated verification:** unit tests above; `grep -rn "traben\." src/client/java | grep -v "/client/core/emf/\|/client/hero/homelander/emf/"` returns nothing; `grep -rn "traben\." src/main/java` returns nothing; `runServer`/gametest smoke confirms the bundled EMF/ETF land in `envDisabledMods` on the dedicated classpath (not loaded, no `traben.*` class loading, no crash) while loading normally on the client (`qualityGate` covers the gametest side).

**User in-game verification:** Homelander stands/walks correctly (same skin, no gaps at elbows/knees, normal and slim skin); non-Homelander players unchanged; first-person arms unchanged; flight/laser/iron fists/roar/sun/landing gameplay and VFX still function even where player animation is temporarily absent; dedicated server boots with the bundled mods env-skipped; F3+T reload does not break. No legacy Homelander animation should play anywhere.

**Rollback boundary:** revert PR 1 completely; nothing else depends on it yet.

### Stage 2: HOVER (PR 2)

**Goal:** Flying Homelander uses authored HOVER as base for all non-boost flight, including slow movement.

**Current state:** after Stage 1 the owner shows the vanilla pose in flight (the legacy Homelander clips are deleted, not merely skipped); non-owners are unchanged.

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

**Confirmed gameplay timing:** `MilkBottleItem.getUseDuration` = 126 ticks (6.3 s) so the real use duration matches the authored sequence and the gameplay effect lands after the sip. MADNESS is applied in `finishUsingItem` as today. Releasing use early cancels (vanilla `releaseUsing`), sends `MILK_CANCEL`, no effect. While the milk weight > 0, the third-person held item is hidden for the owner because the authored bottle replaces it. First-person drinking stays vanilla: the arm/prop channels and the `milk_bottle`/`milk_cap`/`mouth_open` `visible` channels in the jem branch on the built-in `is_first_person_hand` variable, so first-person arms and the vanilla drink animation are untouched. This timing is approved by the user.

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

**Hit feedback consequence:** today every firing tick lands (the `eye_laser` damage type bypasses the hurt cooldown — §1.3), so red flash and hurt sound are effectively continuous right now; after this change feedback becomes pulsed at 2 hits/s. That is the intended consequence of the cadence change, not an invariant.

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
- [ ] README: dependency note that EMF+ETF ship bundled inside the mod jar (jar-in-jar), are required on the client, and are env-skipped on the dedicated server.
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

- Jem expression size: about 70 channels × 5 clips × up to 127 samples. EMF compiles expressions once at model bake (ASM path); still, each `keyframe(...)` call allocates its varargs `float[]` per channel per entity per frame (~35 KB/frame for 4 Homelanders — trivially cheap but measurable GC churn). Stage 1 measures frame time with 4 Homelanders on screen (user) and falls back to 10 Hz sampling if needed (the bake test re-checks error); if even that is heavy, clip sampling can move into `HomelanderPoseState` float variables and the jem reduces to ~70 `lerp` lines.
- Variable suppliers are called per expression evaluation: `HomelanderPoseState.read` caches the last UUID and returns preallocated floats; no boxing beyond EMF's `Supplier<Float>` (use cached `Float` instances only if profiling shows GC pressure; otherwise the `BooleanSupplier`/float factory path via `UniqueVariableFactory` is the fallback).
- `RenderedPoseCache`, burn marks, trail points: fixed-size arrays.
- Networking: no new per-tick packets; clap/milk add one or two events per use; burn marks at most 6/s per caster, batched.
- No render-frame raycasts added; burn marks come from the server hit.

## 11. Multiplayer risks

- Remote velocity comes from interpolated positions (jitter at low speed): smoothing + hysteresis; boost needs sustained forward speed.
- Missed one-shot events on late tracking: accepted, documented.
- Clock drift between clients: one-shot clocks start from event receipt, loops are cosmetic; acceptable.
- Server-delayed clap: hit timing identical for everybody because the server applies it.
- There is no no-EMF Homelander animation fallback — with jar-in-jar EMF cannot be absent on a supported client. Dedicated-server compatibility comes from env-skip + softened `depends` (§4.1), not from any fallback animation path.

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
12. EMF+ETF ship jar-in-jar: present and satisfied on every client via `depends`, env-skipped on the dedicated server (no crash, no `traben.*` loading).

---

## implementation handoff

- **Start from:** commit `2dc02edc` (head of PR #138, branch `origin/devin/1790691307-homelander-vfx-fix`). If PR #138 has been merged into `main` by then, start from `origin/main` after that merge. Never start from, merge, or cherry-pick `origin/devin/1790767119-homelander-emf` (PR #140).
- **Implement first:** Stage 1 (EMF foundation). Begin with its spike (§4.7) and the bake script tests; report spike results in the PR before wiring mixins.
- **Do not touch:** other heroes' presentation, `PlayerAnimator`/`AnimationLibrary`/`BedrockAnimationParser`/`PoseSample`/`PlayerPoseApplier` internals for other heroes, `ClientFlightState`, `FlightPhaseResolver`, `FlightTrailManager`, `src/main/generated/`, gameplay constants outside the already-approved changes in Stages 7, 8, 10, the reverted presentation-phase approach (`5c3be771`). **Stage 1 must delete all legacy Homelander `.animation.json` files and remove their Homelander call sites.**
- **Sources of truth:** this plan; `art-source/homelander/emf/Homelander_All_Animations.bbmodel` (after Stage 1 copies it); `AGENTS.md`; `SESSION.md`; EMF docs `.github/emf_animation.txt`, `.github/emf_part.txt`, `FEATURES.md` and `EMFAnimationApi.java` in `Traben-0/Entity_Model_Features` at `mod_version=3.3.9`.
- **After Stage 1 is verified by the user:** Stage 2 (HOVER). Stages 9, 10, 11, 14 may run in parallel at any time because they do not depend on EMF. Clap timing, milk duration and laser damage cadence/DPS are already approved by the user; no further decision gate is needed for Stages 7, 8 or 10.
