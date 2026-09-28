# Visual Core + Homelander Pilot Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a hero-agnostic, Veil-first client Visual Core (foundation + reusable patterns + in-game showcase) and move every targeted Homelander presentation path (flight, eye lasers, milk + final explosion, Iron Fists, Clap, Roar, animation, sound) onto it, ending with the user-approved OMP assets integrated and verified in multiplayer.

**Architecture:** Server gameplay stays authoritative in `src/main` and only emits two new typed payloads (`VfxEventS2CPayload` one-shot, `VfxChannelS2CPayload` continuous) through `FxBroadcast`. Everything visual lives in `src/client/.../client/core/{vfx,anim,flight}` (hero-agnostic, Veil calls isolated in `client/core/vfx/veil/`) and hero effects are registered from `HomelanderClientModule` through new `HeroClientContext` seams. Passive resources (sounds, Bedrock player animations, milk model, VFX textures) are addressed only by the ids in the shared contract, so placeholders and OMP finals use one integration path.

**Tech Stack:** Java 21, Minecraft 1.21.1 (Mojang mappings), Fabric Loader 0.19.2, Fabric API 0.116.12+1.21.1, Veil 4.1.2 (Quasar particles, dynamic lights, post pipelines; client-only, optional), JUnit 5, Fabric GameTest, ArchUnit, Blockbench Bedrock animation JSON (parsed in-house, no new dependency).

**Spec:** `docs/design/visual-core-homelander/visual-core-homelander-design-spec-v3.1.md` (companion: `docs/design/visual-core-homelander/homelander-omp-assets-design-spec-v2.md`, executed by `docs/superpowers/plans/2026-09-28-homelander-omp-assets.md`).

## Global Constraints

- Core is hero-agnostic: nothing under `client/core/**`, `core/**` or `mechanic/**` may reference `hero.homelander` / `client.hero.homelander` or branch on a hero id (spec §3.1, §3.5; enforced by existing ArchUnit rules).
- Veil (`foundry.veil.*`) only under `src/client`, only inside `client/core/vfx/veil/**` for this plan, always behind `FabricLoader.getInstance().isModLoaded("veil")` (AGENTS.md §7). `grep -rn 'foundry\.veil' src/main/java` must stay empty.
- Server reaches visuals only through `core/net` payloads + `FxBroadcast`; every payload has `TYPE` + `STREAM_CODEC` and a registered client receiver.
- Contract fixes only integration-critical items: states/moments, expected passive resources, approximate durations, runtime events, ownership, placeholder/final replacement path; never artistic direction (spec §6).
- Placeholders use the same ids/paths as finals, roughly match duration, and are replaceable without touching logic (spec §7). Done = zero Homelander placeholders ship (spec §16). Any file standing in for a not-yet-final resource must be recorded in `homelander_placeholders.txt`; replacement marks the line `# replaced`, never deletes it.
- Runtime sounds OGG Vorbis only; check `art-source/` before creating any asset; MP3 → `ffmpeg -i input.mp3 -c:a libvorbis -qscale:a 5 output.ogg`.
- `src/main/generated/` is datagen output: change the provider, run `./gradlew runDatagen --no-daemon`, never hand-edit.
- Out of scope (spec §9): bottom-bar icons, left 3D flying menu, chat overlap, milk/laser damage balance, Uranium redesign, roster-wide animation/sound, mass migration, external editor, unrelated gameplay. Gameplay numbers (damage, radii, cooldowns, durations) stay unchanged.
- Performance: normal heavy scene "roughly 100 FPS or higher" on the current Sodium setup; stress scene: no catastrophic degradation (spec §12).
- Multiplayer is a hard acceptance gate (spec §11).
- Tuning via data (`assets/superheroes/vfx/**/*.json`), not a second engine (spec §13).
- OMP package integrates only after third-session review + explicit user approval (spec §14).
- Finishing gate for every task: `./gradlew qualityGate --no-daemon`.

## Review Focus

1. Observer starts tracking a Homelander mid-laser or mid-flight (join, respawn, walk into range) → sees the running effect, not nothing or a stuck beam. Pinned: Task 3 `updateWithoutStartOpensChannel`, `channelExpiresWithoutUpdates`.
2. Laser origin in third person / while flying tilted / for remote players → beams leave the rendered eyes, never the camera or feet. Pinned: Task 6 `eyesFollowFlightTilt`, `eyesFollowHeadYawAndPitch`.
3. Flight phase flips every tick (hover↔cruise jitter, BOOST↔CRUISE at the speed threshold, brief ground touch) → pose and trail stay continuous, no snapping. Pinned: Task 8 `phaseFlipDoesNotJumpPose`, `stepIsContinuousForLargeDt`.
4. Many simultaneous heavy effects (stress command, several Homelanders detonating) → budget caps instances, frame does not collapse. Pinned: Task 4 `budgetEvictsOldestBeyondCap`.
5. OMP delivers an animation with Molang expressions, an unknown bone, or a length far from the contract → clean contract-test failure / logged skip, never a client crash. Pinned: Task 7 `molangKeyframeRejectedNotThrown`, `unknownBoneIgnored`; Task 1 contract test.

---

## File Structure

Server (`src/main/java/io/github/grebeshok105/codex/`):
- `core/net/VfxEventS2CPayload.java` — one-shot effect trigger (new).
- `core/net/VfxChannelS2CPayload.java` — continuous effect START/UPDATE/STOP (new).
- `core/net/VfxFx.java` — send helpers over `FxBroadcast` (new).
- `core/net/CoreNetworking.java` — register both payloads (modify).
- `core/vfx/VfxShowcases.java` — server registry of named showcase scenes (new, hero-agnostic).
- `core/particle/SilentParticles.java` — `superheroes:silent` particle type used to mute vanilla explosion particles (new).
- `content/command/SuperheroesCommands.java` — `/superheroes vfx …` subtree (modify).
- `sound/HomelanderSounds.java` — all Homelander `SoundEvent`s (new; moves the five from `sound/ModSounds.java`; shared package because `content.boss` and other heroes borrow these events — ArchUnit forbids `content.**`→`hero.**` and hero→hero references).
- `hero/homelander/ability/EyeLaserPhases.java` — pure charge/fire/release timing (new).
- `hero/homelander/ability/{EyeLasersAbility,HandClapAbility,StunningRoarAbility,IronFistsAbility}.java`, `runtime/{IronFistsController,HomelanderMadnessAftermathController,HomelanderMadnessFlightController}.java`, `HomelanderHero.java`, `item/MilkBottleItem.java` — swap vanilla particles/sounds for `VfxFx` + `HomelanderSounds` (modify).
- `hero/homelander/HomelanderVfxIds.java` — Homelander effect/channel ids (new, leaf constants).
- `hero/homelander/HomelanderModule.java` — register showcase scenes (modify).
- `datagen/ModItemModelProvider.java` — drop flat `milk_bottle` (modify).

Client (`src/client/java/io/github/grebeshok105/codex/client/`):
- `core/vfx/{VfxEffect,VfxChannelEffect,VfxEffectFactory,VfxChannelFactory,VfxSpawn,VfxRuntime,VfxRenderContext,VfxInstanceTable,VfxChannelTable}.java` — foundation (new; the two tables are package-private test surfaces).
- `core/vfx/params/{VfxParams,VfxParamsLoader}.java` — data tuning + reload (new).
- `core/vfx/backend/{VfxBackend,LightHandle,VfxBackends,FallbackVfxBackend}.java` — capability interface (new).
- `core/vfx/veil/{VeilVfxBackend,VeilPostEffects}.java` — only Veil touchpoint (new).
- `core/vfx/pattern/{PhaseTimeline,BeamLook,BeamPattern,ImpactPattern,ShockwavePattern,AuraPattern,TrailBuffer,TrailPattern,ScreenFlash,CameraImpulse}.java` — reusable patterns (new).
- `core/vfx/anchor/{EyePair,HumanoidAnchors}.java` — render-consistent anchors (new).
- `core/vfx/debug/{VfxDebugHud,VfxPerfProbe}.java` — in-game inspection (new).
- `core/anim/{Keyframes,BoneTrack,AnimationClip,BedrockAnimationParser,AnimationLibrary,PlayerAnimator,PoseSample,PlayerPoseApplier}.java` — player animation runtime (new).
- `core/flight/{FlightBodyTransform,FlightPoseMath,FlightPoseTracker,FlightPresentation,FlightPresentations}.java` — continuous flight pose + per-hero presentation hook (new).
- `core/module/HeroClientContext.java` (+ its implementation) — seams `vfx`, `vfxChannel`, `flightPresentation` (modify).
- `core/net/CoreClientReceivers.java` — receivers for the two payloads (modify).
- `mixin/PlayerModelPoseMixin.java`, `mixin/PlayerRendererMixin.java` — apply `PoseSample` and `FlightBodyTransform` (modify).
- `hero/homelander/fx/{HomelanderFx,EyeLaserChannel,FlightFx,SunChargeFx,SunDetonationFx,IronFistsFx,ClapFx,RoarFx}.java` — Homelander compositions (new). `HomelanderFx` is the registration hub: `HomelanderClientModule` calls `HomelanderFx.register(ctx)`, which wires every `HomelanderVfxIds` entry to its composition — `LANDING` → `FlightFx.landing`, `LASER` (channel) → `EyeLaserChannel`, `SUN_CHARGE` → `SunChargeFx`, `SUN_DETONATION`/`MADNESS_CRASH` → `SunDetonationFx` (the crash plays the same composition scaled down by the event's `scale` param), `IRON_FISTS_*` → `IronFistsFx`, `CLAP` → `ClapFx`, `ROAR` → `RoarFx`, `MILK_DRINK` → small one-shot hosted inside `HomelanderFx` (`milk_drink` ACTION clip + entity-bound `homelander.milk.drink` sound).
- `hero/homelander/HomelanderClientModule.java` — register effects/presentation; drop `LocalLaserOverlay` only — the `STYLE_LASER` `beamStyle` registration stays (boss `HomelanderEyeLaserGoal` still sends it) (modify).
- `hero/homelander/render/LocalLaserOverlay.java` — delete (superseded by `EyeLaserChannel`).

Resources (`src/main/resources/assets/superheroes/`): `sounds.json`, `sounds/homelander/*.ogg`, `player_animations/homelander/*.animation.json`, `models/item/milk_bottle.json`, `textures/item/milk_bottle.png`, `textures/vfx/homelander/*.png`, `vfx/homelander/*.json`, `vfx/flight/pose.json`, `quasar/emitters/homelander_*.json`, `pinwheel/post/vfx_*.json`, `pinwheel/shaders/program/vfx/*`.

Contract + docs: `docs/design/visual-core-homelander/{roster-visual-audit,shared-contract,migration-workflow,verification}.md`, `src/test/resources/contracts/homelander_pilot.json`, `src/test/resources/contracts/homelander_placeholders.txt`.

Tests: `src/test/java/io/github/grebeshok105/codex/assets/{OggInfo,HomelanderAssetContractTest,HomelanderPlaceholderGuardTest,HomelanderNoVanillaParticlesTest}.java`, `src/test/java/.../core/net/VfxPayloadCodecTest.java`, `src/test/java/.../hero/homelander/EyeLaserPhasesTest.java`, client JUnit under `src/test/java/.../client/core/{vfx,anim,flight}/` (pure-math classes only, no Minecraft bootstrap), `src/gametest/java/.../HomelanderVfxGameTests.java`.

---

### Task 1: Roster audit, shared contract, contract test

**Files:**
- Create: `docs/design/visual-core-homelander/roster-visual-audit.md`
- Create: `docs/design/visual-core-homelander/shared-contract.md`
- Create: `docs/design/visual-core-homelander/migration-workflow.md`
- Create: `src/test/resources/contracts/homelander_pilot.json`
- Create: `src/test/java/io/github/grebeshok105/codex/assets/OggInfo.java`
- Create: `src/test/java/io/github/grebeshok105/codex/assets/HomelanderAssetContractTest.java`

**Interfaces:**
- Produces: contract JSON schema `{ "sounds": [{ "event", "file", "durationMs", "loop" }], "animations": [{ "clip", "file", "lengthMs", "loop", "events": {"<name>": ms} }], "models": [{ "path" }], "textures": [{ "path" }] }` with `durationMs`/`lengthMs` tolerance `±30%` for one-shots, loops only require `≥ 1000` ms and `loop:true`.
- Produces: `OggInfo.durationMs(Path ogg) -> long` (sample rate from Vorbis identification header, last page granule position / rate).
- Produces: clip id rule `animation.superheroes.homelander.<name>` ⇄ `superheroes:homelander/<name>`; bones exactly `head, body, right_arm, left_arm, right_leg, left_leg`.

- [ ] **Step 1: Write the roster audit.** For every module in `bootstrap/HeroModules.ALL`, one table row: hero id, visual needs (beam / trail / impact / aura / field / charge-release / camera / screen / world-interaction / flight / animation), current mechanism (vanilla particles, `BeamStyle`, Veil, HUD). End with the capability list the core must provide (the pattern classes in File Structure) and which heroes consume each.
- [ ] **Step 2: Verify audit coverage.** Run: `for id in $(rg -o 'hero\.([a-z_]+)\.[A-Z]\w+Module' -r '$1' src/main/java/io/github/grebeshok105/codex/bootstrap/HeroModules.java); do rg -q "^\| $id " docs/design/visual-core-homelander/roster-visual-audit.md || echo MISSING $id; done` Expected: no output.
- [ ] **Step 3: Write `shared-contract.md` and `homelander_pilot.json`** with exactly these rows (durations approximate, OMP may request changes per OMP spec §4):

| Moment | Sound event (file under `sounds/homelander/`) | Clip (`player_animations/homelander/`) | Runtime event |
|---|---|---|---|
| Takeoff | `homelander.flight.takeoff` (`flight_takeoff.ogg`, 600 ms) | `flight_takeoff` 400 ms (= `FlightPhaseResolver.TAKEOFF_TICKS` 8) | `FlightPhase.TAKEOFF` |
| Hover | — | `flight_hover` loop | `FlightPhase.HOVER` |
| Cruise | `homelander.flight.loop` (`flight_loop.ogg`, loop) | `flight_cruise` loop | `FlightPhase.CRUISE` |
| Boost | `homelander.flight.boost` (`flight_boost.ogg`, 1200 ms) | `flight_boost` loop | `FlightPhase.BOOST` entry |
| Landing | `homelander.flight.land` (`flight_land.ogg`, 500 ms) | `flight_land` 500 ms | `FlightPhase.LANDING` / `Hero.onLanded` |
| Laser charge | `homelander.laser.charge` (`laser_charge.ogg`, 300 ms) | `laser_charge` 300 ms | channel START |
| Laser hold | `homelander.laser.loop` (`laser_loop.ogg`, loop) | `laser_hold` loop | channel UPDATE |
| Laser release | `homelander.laser.release` (`laser_release.ogg`, 400 ms) | `laser_release` 400 ms | channel STOP |
| Milk drink | `homelander.milk.drink` (`milk_drink.ogg`, 1600 ms) | `milk_drink` 1600 ms (= `MilkBottleItem.DRINK_TICKS` 32) | use start |
| Sun build-up | `homelander.sun.charge` (`sun_charge.ogg`, 10000 ms) | `sun_charge` loop | aftermath start (`AFTERMATH_TICKS` 200) |
| Sun detonation | `homelander.sun.detonate` (`sun_detonate.ogg`, 4000 ms) | — | `detonateSun` |
| Iron Fists on | `homelander.iron_fists.activate` (`iron_fists_activate.ogg`, 1000 ms); existing `homelander.iron_fists.charge` loop | `iron_fists_activate` 1000 ms | activation |
| Iron Fists hit | existing `homelander.iron_fists.impact` | `iron_fists_strike` 400 ms, event `contact` ≤ 120 ms | dash hit |
| Clap | existing `homelander.hand_clap` | `clap` 600 ms, event `contact` ≤ 120 ms | activation tick |
| Roar | existing `homelander.roar`, `homelander.roar.deep` | `roar` 1500 ms | activation tick |

Models: `models/item/milk_bottle.json` (3D, Blockbench Java item model, texture `superheroes:item/milk_bottle`). Textures: `textures/vfx/homelander/{laser_core,laser_glow,sun_flash,sun_ring,ember,shock_ring}.png`. `contact` events ≤ 120 ms because gameplay fires on the activation tick and must not change. `shared-contract.md` also states the ownership boundary (spec §5) and the replacement rule: final files overwrite placeholder files at the same path; no id changes. It also carries a **runtime motion envelope** (synchronization constraint, spec §6 — not artistic direction): the runtime tilts the whole body (CRUISE ≤ 55°, BOOST ≤ 80°) and crossfades clips over 4 ticks, so clips carry limb/torso motion only and `body` root rotation stays ≤ ±15°; `head` motion stays ≤ 10° during `laser_hold` (the head keeps following the player's look on top of the clip); `flight_loop` volume scales with `horizontalSpeed` so it must read from quiet to full.
- [ ] **Step 4: Write `migration-workflow.md`** — the 12 steps of spec §17 verbatim, each with the plan artifact that implements it in this pilot (audit → Task 1, core extension → Tasks 3–7, placeholders → Task 2, …).
- [ ] **Step 5: Write the failing test** `HomelanderAssetContractTest`:
  - `everyContractSoundIsRegisteredAndOgg`: each `event` key exists in `sounds.json` and references `superheroes:homelander/<file stem>`; file exists and starts with bytes `OggS`.
  - `oneShotSoundDurationsWithinTolerance`: `abs(OggInfo.durationMs(f) - durationMs) <= durationMs * 0.30` for `loop:false`; `>= 1000` for `loop:true`.
  - `everyContractClipExistsWithLength`: parse each file, key `animation.superheroes.homelander.<clip>`, `animation_length*1000` within ±30 % (one-shots), `loop == true` for loops, only allowed bone names.
  - `contactEventsAreEarly`: every `events.contact <= 120`.
  - `contractModelsAndTexturesExist`.
- [ ] **Step 6: Run to verify it fails.** Run: `./gradlew test --tests '*HomelanderAssetContractTest' --no-daemon` Expected: FAIL (`sounds.json` lacks `homelander.flight.takeoff`, clip files missing).
- [ ] **Step 7: Implement `OggInfo.durationMs(Path)`** (Vorbis identification packet: sample rate = little-endian int at packet offset 12; duration = last page `granule_position` / rate). Re-run Step 6. Expected: still FAIL only on missing resources (no `OggInfo` errors on the five existing OGGs). Annotate the class `@Disabled("enabled in Task 2 once placeholders exist")` so the gate stays green.
- [ ] **Step 8: Commit** `git add docs/design/visual-core-homelander src/test/resources/contracts src/test/java/io/github/grebeshok105/codex/assets && git commit -m "docs(vfx): add roster audit and Homelander shared asset contract"`.

### Task 2: Homelander sounds module + contract placeholders

**Files:**
- Create: `src/main/java/io/github/grebeshok105/codex/sound/HomelanderSounds.java` — in the shared `sound` package, not `hero/homelander/sound`: `content/boss/homelander/entity/ai/*` plus `hero.invincible`, `hero.doomsday`, `hero.raiden` all reuse `homelander.*` events, and ArchUnit forbids both `content.**` → `hero.**` (`heroModulesAreReferencedOnlyByThemselvesAndTheModuleList`) and hero → hero (`heroModulesDoNotDependOnEachOther`) references
- Modify: `src/main/java/io/github/grebeshok105/codex/sound/ModSounds.java` (remove the five `HOMELANDER_*`), all call sites (`rg -l 'ModSounds.HOMELANDER_' src`)
- Modify: `src/main/resources/assets/superheroes/sounds.json`, `lang/en_us.json`, `lang/ru_ru.json` (subtitles; the long designed sounds — `sun_charge`, `sun_detonate`, `flight_loop`, `laser_loop` — get `"stream": true`, matching the existing `roar`/`roar.deep`/`omniman_react` convention; short one-shots stay unstreamed like `hand_clap`)
- Create: placeholder files for every contract row (sounds, clips, `milk_bottle.json`, vfx textures)
- Create: `src/test/resources/contracts/homelander_placeholders.txt`, `src/test/java/io/github/grebeshok105/codex/assets/HomelanderPlaceholderGuardTest.java`
- Modify: `src/main/java/io/github/grebeshok105/codex/datagen/ModItemModelProvider.java:31` (drop `generateFlatItem(item("milk_bottle"), …)`), regenerate `src/main/generated`
- **Integration checkpoint:** Tasks 1–2 land on `main` as their own PR before the pilot branch continues — this merge is OMP's start signal (contract docs + contract test + `HomelanderSounds` + placeholders + manifest). The pilot branch carries Tasks 3+.

**Interfaces:**
- Consumes: Task 1 contract.
- Produces: `HomelanderSounds.{ROAR, ROAR_DEEP, HAND_CLAP, IRON_FISTS_IMPACT, IRON_FISTS_CHARGE, IRON_FISTS_ACTIVATE, FLIGHT_TAKEOFF, FLIGHT_LOOP, FLIGHT_BOOST, FLIGHT_LAND, LASER_CHARGE, LASER_LOOP, LASER_RELEASE, MILK_DRINK, SUN_CHARGE, SUN_DETONATE}` (`SoundEvent`, via `ModContent.sound("homelander.<…>")`) + `HomelanderSounds.init()` called from `HomelanderModule.register` (hero → `sound` package is a legal direction; `content.boss` → `sound` too).
- Produces: manifest line format `<repo-relative path> <sha256>`.

- [ ] **Step 1: Write the failing test** `HomelanderPlaceholderGuardTest.manifestHashesMatchFiles`: every uncommented manifest line's file exists and its SHA-256 equals the recorded hash (a replaced file gets its line commented `# replaced`, never deleted — keeps the list truthful). `finalBuildHasNoPlaceholders` exists but is `@Disabled("enabled by the OMP branch (its Task 11); verified in Task 15")` and asserts: the manifest has zero uncommented lines AND every `# replaced` line's file SHA-256 differs from its recorded placeholder hash (a dropped-but-not-replaced line cannot slip a placeholder into the final build).
- [ ] **Step 2: Run** `./gradlew test --tests '*HomelanderPlaceholderGuardTest' --no-daemon` Expected: FAIL (manifest missing).
- [ ] **Step 3: Move sounds into `HomelanderSounds`**, same ids for the five hero-owned events (`roar`, `roar.deep`, `hand_clap`, `iron_fists.impact`, `iron_fists.charge`), add the new ones; update **all** call sites — `hero.homelander`, `content.boss.homelander` (4 events), and the `hero.invincible` / `hero.doomsday` / `hero.raiden` borrowers. `homelander.omniman_react` **stays in `ModSounds`**: it is shared by `HomelanderReactionRule` **and** `OmnimanReactionRule`, so it is not Homelander-owned — note this in the class javadoc. Also register `ModSounds.SILENT` (`superheroes:silent`, a `sounds.json` entry with an empty `sounds` list → a valid `Holder<SoundEvent>` that plays nothing; used by Task 10 to mute the explosion's own audio).
- [ ] **Step 4: Create placeholders.** Check `art-source/sounds/homelander/` first. Sounds: trimmed/padded from existing Homelander OGGs or `ffmpeg -f lavfi -i anoisesrc=d=<s>:a=0.05 -c:a libvorbis -qscale:a 5 <file>.ogg` to the contract duration. Clips: minimal valid Bedrock JSON with correct `animation_length`, `loop`, 2 keyframes on `body`. Milk model: one-cuboid Java item model using the existing texture with `display` transforms. VFX textures: 16×16 solid-alpha PNGs. Record each path + `sha256sum` in the manifest.
- [ ] **Step 5: Regenerate** `./gradlew runDatagen --no-daemon` Expected: `git diff --stat src/main/generated` shows only `models/item/milk_bottle.json` deleted.
- [ ] **Step 6: Enable Task 1 test; run** `./gradlew test --tests '*HomelanderAssetContractTest' --tests '*HomelanderPlaceholderGuardTest' --no-daemon` Expected: PASS.
- [ ] **Step 7: Gate** `./gradlew qualityGate --no-daemon` Expected: BUILD SUCCESSFUL (goldens untouched).
- [ ] **Step 8: Commit** `feat(homelander): own sound events and add contract placeholders`.

### Task 3: VFX payloads and server send helpers

**Files:**
- Create: `src/main/java/io/github/grebeshok105/codex/core/net/VfxEventS2CPayload.java`, `VfxChannelS2CPayload.java`, `VfxFx.java`
- Modify: `src/main/java/io/github/grebeshok105/codex/core/net/CoreNetworking.java`
- Test: `src/test/java/io/github/grebeshok105/codex/core/net/VfxPayloadCodecTest.java`

**Interfaces:**
- Produces:
  - `record VfxEventS2CPayload(ResourceLocation effect, int sourceEntityId, Vec3 origin, Vec3 target, float scale, int seed)`; `TYPE = new Type<>(ModId.of("vfx_event"))`; `NO_SOURCE = -1`. `target` is the secondary point an effect aims at (beam end, look target); omnidirectional effects pass `origin`.
  - `record VfxChannelS2CPayload(int entityId, ResourceLocation channel, byte state, Vec3 target)`; `TYPE = ModId.of("vfx_channel")`; `START = 0, UPDATE = 1, STOP = 2`.
  - `VfxFx.event(Entity source, ResourceLocation effect, Vec3 origin, Vec3 target, float scale)` → `trackingAndSelf` for `ServerPlayer`, else `tracking`; seed = `source.level().random.nextInt()`.
  - `VfxFx.eventAround(ServerLevel level, ResourceLocation effect, Vec3 origin, Vec3 target, float scale, double radius)` → `FxBroadcast.around`.
  - `VfxFx.channel(ServerPlayer source, ResourceLocation channel, byte state, Vec3 target)` → `trackingAndSelf`.
  - `VfxFx.CHANNEL_UPDATE_INTERVAL_TICKS = 2`; client-side expiry (Task 4) `CHANNEL_TIMEOUT_TICKS = 10`.

- [ ] **Step 1: Write failing tests** `eventRoundTrips`, `channelRoundTrips` (encode→decode through `STREAM_CODEC` with a `RegistryFriendlyByteBuf`/`FriendlyByteBuf(Unpooled.buffer())`, assert record equality), `channelStateOutOfRangeRejected` (decode of `state=7` throws `DecoderException`).
- [ ] **Step 2: Run** `./gradlew test --tests '*VfxPayloadCodecTest' --no-daemon` Expected: FAIL (classes missing).
- [ ] **Step 3: Implement** payloads using `StreamCodecs` helpers for `Vec3`; register in `CoreNetworking` with `PayloadRegistrar`.
- [ ] **Step 4: Run** the test Expected: PASS. Client receivers come in Task 4. (`everyHeroS2CPayloadHasARegisteredReceiver` only scans `hero.**` payloads, so `core/net` payloads never trip it — either commit boundary is green; committing Tasks 3+4 together is still recommended. Optional hardening: extend the receiver rule to `core.net` payloads so a forgotten `CoreClientReceivers` registration fails the gate too.)
- [ ] **Step 5: Commit** `feat(vfx): add typed VFX event and channel payloads`.

### Task 4: Client Visual Core foundation (runtime, params, backend)

**Files:**
- Create: `client/core/vfx/{VfxEffect,VfxChannelEffect,VfxEffectFactory,VfxChannelFactory,VfxSpawn,VfxRuntime,VfxRenderContext}.java`, `client/core/vfx/{VfxInstanceTable,VfxChannelTable}.java` (package-private tables the tests drive), `client/core/vfx/params/{VfxParams,VfxParamsLoader}.java`, `client/core/vfx/backend/{VfxBackend,LightHandle,VfxBackends,FallbackVfxBackend}.java`
- Modify: `client/core/module/HeroClientContext.java` + implementation, `client/core/net/CoreClientReceivers.java`, client bootstrap (register tick/render/reload listener, `ClientSessionState` reset)
- Test: `src/test/java/io/github/grebeshok105/codex/client/core/vfx/{VfxRuntimeBudgetTest,VfxParamsTest,VfxChannelTableTest}.java`

**Interfaces:**
- Consumes: Task 3 payloads.
- Produces:
  - `interface VfxEffect { void tick(); void render(VfxRenderContext ctx); boolean done(); default void cancel() {} }`
  - `interface VfxChannelEffect extends VfxEffect { void retarget(Vec3 target); void release(); }` (`release` starts the release phase; `done()` turns true when it ends).
  - `@FunctionalInterface interface VfxEffectFactory { VfxEffect create(VfxSpawn spawn); }`
  - `@FunctionalInterface interface VfxChannelFactory { VfxChannelEffect open(Entity source, Vec3 target, VfxParams params); }`
  - `record VfxSpawn(ResourceLocation effect, @Nullable Entity source, Vec3 origin, Vec3 target, float scale, int seed, VfxParams params)`
  - `record VfxRenderContext(WorldRenderContext world, PoseStack pose, MultiBufferSource buffers, Camera camera, float partialTick)` — carries `WorldRenderContext` because `CrossBeamRenderer.draw` and `WorldRenderEvents` consumers need it
  - `VfxRuntime`: `registerEffect(ResourceLocation, VfxEffectFactory)`, `registerChannel(ResourceLocation, VfxChannelFactory)`, `spawn(VfxSpawn) -> boolean`, `channel(int entityId, ResourceLocation channel, byte state, Vec3 target)`, `tick()`, `render(WorldRenderContext)`, `activeCount() -> int`, `reset()`; `MAX_ACTIVE_EFFECTS = 256` (oldest evicted via `cancel()`), `CULL_DISTANCE = 160.0` blocks from camera for spawns, `CHANNEL_TIMEOUT_TICKS = 10`.
  - `record VfxParams(Map<String, Float> numbers, Map<String, Integer> colors)`; `float number(String key, float fallback)`; `int color(String key, int fallback)`; `static VfxParams parse(JsonObject)` (colors as `"#RRGGBB"`/`"#AARRGGBB"`); `EMPTY`.
  - `VfxParamsLoader.get(ResourceLocation id) -> VfxParams` from `assets/<ns>/vfx/<path>.json` (id `<ns>:<path>`), `SimpleSynchronousResourceReloadListener` so `F3+T` re-tunes live.
  - `interface VfxBackend { void emit(ResourceLocation emitter, Vec3 pos); LightHandle light(Vec3 pos, int rgb, float radius, float brightness); void flash(float intensity, int rgb); void distortion(Vec3 center, float radius, float strength); }`; `interface LightHandle { void move(Vec3 pos); void set(int rgb, float radius, float brightness); void remove(); }`; `VfxBackends.current()` → Veil backend when Veil loaded else `FallbackVfxBackend` (emit = no-op, light = no-op handle, flash = HUD overlay alpha, distortion = no-op).
  - `HeroClientContext.vfx(ResourceLocation id, VfxEffectFactory f)`, `HeroClientContext.vfxChannel(ResourceLocation id, VfxChannelFactory f)`.

- [ ] **Step 1: Write failing tests.** Runtime logic that needs no Minecraft classes lives in package-private `VfxInstanceTable` / `VfxChannelTable` used by `VfxRuntime`:
  - `budgetEvictsOldestBeyondCap`: add 257 fake effects → size 256, first one's `cancel()` called.
  - `doneEffectsRemovedOnTick`.
  - `updateWithoutStartOpensChannel`: `UPDATE` for unknown `(entity, channel)` opens it.
  - `stopReleasesAndRemovesWhenDone`; `duplicateStartRetargetsInsteadOfDuplicating`.
  - `channelExpiresWithoutUpdates`: no UPDATE for 10 ticks → `release()` called.
  - `VfxParamsTest.parsesNumbersAndColors` (`{"intensity":1.5,"core":"#FFE03020"}` → `number("intensity",0)==1.5f`, `color("core",0)==0xFFE03020`), `missingKeyReturnsFallback`, `malformedValueIgnored`.
- [ ] **Step 2: Run** `./gradlew test --tests '*client.core.vfx*' --no-daemon` Expected: FAIL (classes missing).
- [ ] **Step 3: Implement** the interfaces above; receivers in `CoreClientReceivers` hop to the client thread and call `VfxRuntime.spawn/channel` (unknown effect id → debug log once, ignored); render on `WorldRenderEvents.AFTER_TRANSLUCENT`; reset registered with `ClientSessionState`.
- [ ] **Step 4: Run** the tests Expected: PASS. Gate `./gradlew qualityGate --no-daemon` Expected: BUILD SUCCESSFUL (receiver rule green again).
- [ ] **Step 5: Commit** `feat(vfx): add client Visual Core runtime, params and backend seam`.

### Task 5: Veil backend (particles, lights, post effects)

**Files:**
- Create: `client/core/vfx/veil/VeilVfxBackend.java`, `client/core/vfx/veil/VeilPostEffects.java`
- Create: `assets/superheroes/pinwheel/post/vfx_flash.json`, `vfx_distortion.json`, `assets/superheroes/pinwheel/shaders/program/vfx/{flash,distortion}.{json,fsh}`
- Test: `src/test/java/io/github/grebeshok105/codex/client/core/vfx/VeilIsolationTest.java`

**Interfaces:**
- Consumes: `VfxBackend`, `LightHandle` (Task 4).
- Produces: `VeilVfxBackend implements VfxBackend` — `emit` via `VeilRenderSystem.renderer().getParticleManager().createEmitter(id)` (same call path as `VeilScorpionFx`), `light` via `VeilRenderSystem.renderer().getLightRenderer()` + `PointLightData`, `flash`/`distortion` via `VeilPostEffects` setting uniforms (`uIntensity`, `uColor`, `uCenter`, `uRadius`, `uStrength`) on pipelines `superheroes:vfx_flash` / `superheroes:vfx_distortion`, active only while intensity > 0.

- [ ] **Step 1: Write failing test** `VeilIsolationTest.veilImportsOnlyInVeilPackage`: scan `src/client/java/**/client/core/**/*.java`; any file containing `import foundry.veil` must be under `client/core/vfx/veil/`.
- [ ] **Step 2: Run** `./gradlew test --tests '*VeilIsolationTest' --no-daemon` Expected: FAIL (the test also asserts `client/core/vfx/veil/VeilVfxBackend.java` exists, so it cannot pass vacuously).
- [ ] **Step 3: Confirm Veil 4.1.2 paths** before writing JSON: `unzip -l ~/.gradle/caches/modules-2/files-2.1/foundry.veil/veil-fabric-1.21.1/4.1.2/*/veil-fabric-1.21.1-4.1.2.jar | rg 'pinwheel/(post|shaders/program)' | head` — mirror the directory layout and JSON keys Veil's own post pipelines use; check `PointLightData` setters and `PostPipeline` uniform API with `javap -cp <jar> foundry.veil.api.client.render.light.data.PointLightData foundry.veil.api.client.render.post.PostPipeline`.
- [ ] **Step 4: Implement** backend + pipelines; every Veil call wrapped like `ClientScorpionFx` (`try { … } catch (Throwable ignored)` at the backend boundary, logging once).
- [ ] **Step 5: Run** test Expected: PASS; `grep -rn 'foundry\.veil' src/main/java` Expected: empty; `./gradlew qualityGate --no-daemon` Expected: BUILD SUCCESSFUL (includes `clientnoveil` compile).
- [ ] **Step 6: Commit** `feat(vfx): add Veil-backed particle, light and post backend`.

### Task 6: Reusable pattern layer and render anchors

**Files:**
- Create: `client/core/vfx/pattern/{PhaseTimeline,BeamLook,BeamPattern,ImpactPattern,ShockwavePattern,AuraPattern,TrailBuffer,TrailPattern,ScreenFlash,CameraImpulse}.java`, `client/core/vfx/anchor/{EyePair,HumanoidAnchors}.java`
- Test: `src/test/java/io/github/grebeshok105/codex/client/core/vfx/pattern/{PhaseTimelineTest,TrailBufferTest,ScreenFlashTest}.java`, `.../anchor/HumanoidAnchorsTest.java`

**Interfaces:**
- Consumes: Task 4 runtime/backend; existing `client/core/render/CrossBeamRenderer`, `client/fx/ScreenShakeManager`; `FlightBodyTransform` (record defined here in `client/core/flight/`, filled by Task 8).
- Produces:
  - `enum PhaseTimeline.Phase { CHARGE, HOLD, RELEASE, DONE }`; `record PhaseTimeline(int chargeTicks, int releaseTicks)`; `Phase phaseAt(int age, int releasedAtAge)` (`releasedAtAge = -1` while held); `float intensity(int age, int releasedAtAge, float partial)` — 0→1 ease-out over charge, 1 in hold, 1→0 over release.
  - `record BeamLook(float coreWidth, float glowWidth, int coreArgb, int glowArgb, float noise)`; `BeamPattern.draw(VfxRenderContext ctx, Vec3 from, Vec3 to, BeamLook look, float intensity)` — delegates to a new `CrossBeamRenderer.draw(WorldRenderContext, Vec3, Vec3, float intensity, BeamLook look)` overload (uses `ctx.world()`) that takes colors/widths from `BeamLook`; the existing `draw(..., float widthMul)` keeps its hardcoded look so current callers are untouched; `noise` is applied by `BeamPattern` itself.
  - `ImpactPattern.spawn(VfxBackend b, Vec3 pos, Vec3 normal, ResourceLocation emitter, VfxParams p)`; `ShockwavePattern` (`VfxEffect`, expanding ring mesh: params `radius`, `durationTicks`, `color`); `AuraPattern` (`VfxEffect` following an `Entity`, periodic emitter + optional light — this is also the `fields` capability from spec §4); `TrailBuffer(int capacity)` ring of `Vec3` with `push`, `size`, `get(i)`; `TrailPattern` ribbon from a `TrailBuffer`.
  - `ScreenFlash.attenuation(double distance, double radius, boolean lineOfSight) -> float` (1 at 0, linear to 0 at `radius`, ×0.35 without LOS); `ScreenFlash.trigger(float intensity, int argb, int fadeTicks)`.
  - `CameraImpulse.shake(float intensity, int ticks)` delegating to `ScreenShakeManager`.
  - `record EyePair(Vec3 left, Vec3 right)`; `HumanoidAnchors.eyesFrom(Vec3 feet, float bodyYawDeg, float headYawDeg, float headPitchDeg, FlightBodyTransform tilt, float scale) -> EyePair` (pure); `HumanoidAnchors.eyes(AbstractClientPlayer p, float partial, FlightBodyTransform tilt, Vector3f headAnimDeg) -> EyePair` (interpolated `yBodyRot`/`yHeadRot`/`xRot`; callers pass `FlightPoseTracker.transform(...)` from Task 8 and the head rotation of `PlayerAnimator.sample(...)` from Task 7 — until those exist, `FlightBodyTransform.IDENTITY` and a zero vector). Eye point: head pivot `1.5` blocks above feet (unscaled), eyes `0.25` forward, `±0.0625` lateral, `0.0625` up from head pivot along head axes; first-person local player uses camera position `+ 0.35` forward, `±0.11` lateral (both from current `LocalLaserOverlay`), `-0.08` down (new constant — keeps the beams out of the camera near plane).
  - `record FlightBodyTransform(float pitchDeg, float rollDeg)`; `IDENTITY`.

- [ ] **Step 1: Write failing tests:**
  - `PhaseTimelineTest`: `(6,8)` → `phaseAt(0,-1)==CHARGE`, `phaseAt(6,-1)==HOLD`, `phaseAt(20,15)==RELEASE`, `phaseAt(23,15)==DONE`; `intensity(3,-1,0)` in `(0,1)`; `releaseBeforeChargeEndsStartsReleaseFromCurrentIntensity` (release at age 2 → intensity at age 2 equals value at age 2 of charge, then decreases monotonically).
  - `TrailBufferTest.overwritesOldest`, `getZeroIsNewest`.
  - `ScreenFlashTest`: `attenuation(0,64,true)==1`, `attenuation(64,64,true)==0`, `attenuation(32,64,false)≈0.175`.
  - `HumanoidAnchorsTest.eyesFollowHeadYawAndPitch` (yaw 90° → eyes offset toward −X; pitch +90° → eyes move down), `eyesFollowFlightTilt` (tilt pitch 80° moves eye midpoint ≥ 1.0 block horizontally forward of feet vs identity), `eyesSymmetric` (midpoint lies on head forward axis).
- [ ] **Step 2: Run** `./gradlew test --tests '*client.core.vfx.pattern*' --tests '*HumanoidAnchorsTest' --no-daemon` Expected: FAIL.
- [ ] **Step 3: Implement** (math in pure static methods; `BeamPattern` delegates to the new `CrossBeamRenderer` overload).
- [ ] **Step 4: Run** tests Expected: PASS; gate Expected: BUILD SUCCESSFUL.
- [ ] **Step 5: Commit** `feat(vfx): add reusable beam, impact, shockwave, aura, trail, flash patterns and humanoid anchors`.

### Task 7: Player animation runtime

**Files:**
- Create: `client/core/anim/{Keyframes,BoneTrack,AnimationClip,BedrockAnimationParser,AnimationLibrary,PlayerAnimator,PoseSample,PlayerPoseApplier}.java`
- Modify: `client/mixin/PlayerModelPoseMixin.java` (apply sample at `setupAnim` TAIL; then re-copy `hat/jacket/leftSleeve/rightSleeve/leftPants/rightPants` from their base parts)
- Test: `src/test/java/io/github/grebeshok105/codex/client/core/anim/{BedrockAnimationParserTest,KeyframesTest,PlayerAnimatorTest}.java`, `src/test/resources/anim/sample.animation.json`

**Interfaces:**
- Produces:
  - `record AnimationClip(ResourceLocation id, float lengthSeconds, boolean loop, Map<String, BoneTrack> bones, Map<String, Float> eventTimes)`; `record BoneTrack(Keyframes rotation, Keyframes position)`; `Keyframes.sample(float seconds) -> Vector3f` (linear and `catmullrom`, clamp outside range). `eventTimes` (seconds) is populated from the clip's `sound_effects` / `particle_effects` / `events` timeline keys — this is how contract `events: {"contact": ms}` reaches the runtime (Task 12's clap flash and Task 11's strike read `eventTimes.get("contact")`; missing → the clip's `contact` contract check fails first).
  - `BedrockAnimationParser.parse(JsonObject root, Consumer<String> warn) -> List<AnimationClip>`; name `animation.<ns>.<hero>.<clip>` → id `<ns>:<hero>/<clip>`; `loop` true / `"hold_on_last_frame"` (treated as non-loop hold); keyframe value arrays or `{pre, post, lerp_mode}`; any string (Molang) value → warn + skip that clip; unknown bone → warn + ignore bone.
  - Extend `HomelanderAssetContractTest` (from Task 1): new check `contractClipsSurviveTheRuntimeParser` — every contract clip file parses through `BedrockAnimationParser.parse` with zero warnings, so a Molang-valued or unknown-bone clip fails the contract instead of silently skipping at runtime.
  - `AnimationLibrary.get(ResourceLocation) -> Optional<AnimationClip>` loaded from `assets/*/player_animations/**/*.animation.json` via reload listener.
  - `enum PlayerAnimator.Layer { BASE, ACTION }`; `PlayerAnimator.play(int entityId, ResourceLocation clip, Layer layer, int fadeTicks)`, `stop(int entityId, Layer layer, int fadeTicks)`, `sample(int entityId, float partialTick) -> PoseSample`, `tick()`, `reset()` (registered with `ClientSessionState`). ACTION overrides BASE per bone by its fade weight.
  - `record PoseSample(Map<String, Vector3f> rotationDeg, Map<String, Vector3f> offsetPx, float weight)`; `EMPTY`.
  - `PlayerPoseApplier.apply(HumanoidModel<?> model, PoseSample s)` — Bedrock→Java: `xRot += -rad(x)`, `yRot += -rad(y)`, `zRot += rad(z)`; offsets `x += -px.x`, `y += -px.y`, `z += px.z` (GeckoLib convention), weighted.

- [ ] **Step 1: Write failing tests:**
  - `parsesLengthLoopAndBones` (fixture: `animation.superheroes.homelander.clap`, length 0.6, bones `right_arm`, `left_arm`) → id `superheroes:homelander/clap`, `lengthSeconds==0.6f`.
  - `molangKeyframeRejectedNotThrown`: `"rotation": {"0.0": ["math.sin(q.anim_time)",0,0]}` → clip absent, one warning.
  - `unknownBoneIgnored`: bone `tail` → clip present without `tail`, one warning.
  - `KeyframesTest.linearMidpoint` (0→[0,0,0], 1→[90,0,0]; `sample(0.5).x==45`), `clampsAfterEnd`, `catmullromPassesThroughKeys`.
  - `PlayerAnimatorTest.actionFadesInOverBase` (weight at fade midpoint ≈0.5), `oneShotEndsAndReleasesToBase`, `loopWraps`.
- [ ] **Step 2: Run** `./gradlew test --tests '*client.core.anim*' --no-daemon` Expected: FAIL.
- [ ] **Step 3: Implement** parser/sampler/animator (pure, driven by an injected tick clock); library + mixin wiring.
- [ ] **Step 4: Run** tests Expected: PASS; gate Expected: BUILD SUCCESSFUL.
- [ ] **Step 5: Commit** `feat(anim): add Bedrock player animation runtime with layered blending`.

### Task 8: Continuous flight presentation + Homelander flight

**Files:**
- Create: `client/core/flight/{FlightPoseMath,FlightPoseTracker,FlightPresentation,FlightPresentations}.java`, `assets/superheroes/vfx/flight/pose.json`
- Modify: `client/mixin/PlayerRendererMixin.java` (inject `setupRotations` TAIL: rotate `pitchDeg` about X, `rollDeg` about Z around body center), `client/mixin/PlayerModelPoseMixin.java` (keep existing static leg pose only when no `FlightPresentation` for the player's hero), `client/core/module/HeroClientContext.java` (`flightPresentation`)
- Create: `client/hero/homelander/fx/FlightFx.java`, `assets/superheroes/vfx/homelander/flight.json`, `quasar/emitters/homelander_flight_*.json`
- Modify: `hero/homelander/HomelanderHero.java:160-195` (landing impact → `VfxFx.event(player, HomelanderVfxIds.LANDING, …, radius)`; remove **all** `sendParticles` **and** the tiered `playSound` calls — `GENERIC_EXPLODE`, `LIGHTNING_BOLT_THUNDER`, `WARDEN_SONIC_BOOM`, `WITHER_SPAWN`; the landing tier still drives the event's `scale`, and `homelander.flight.land` is now the single landing sound), `client/hero/homelander/HomelanderClientModule.java`. The landing's `ShockwaveUtil.detonate` call gains a `suppressPresentation` flag (new overload in `mechanic/shockwave/ShockwaveUtil`: skips the vanilla `EXPLOSION`/`LARGE_SMOKE`/`POOF` particles and `GENERIC_EXPLODE`/`RAVAGER_STEP` sounds, keeps damage/block-break/`ScreenShakeS2CPayload` — the shared shake payload is gameplay feel, not a vanilla visual); Homelander's call sites pass it, other heroes unchanged.
- Create: `hero/homelander/HomelanderVfxIds.java`
- Test: `src/test/java/io/github/grebeshok105/codex/client/core/flight/FlightPoseMathTest.java`, `src/test/java/io/github/grebeshok105/codex/assets/HomelanderNoVanillaParticlesTest.java`

**Interfaces:**
- Consumes: `ClientFlightState.State(FlightMode mode, FlightPhase phase, float horizontalSpeed)` (existing), `PlayerAnimator` (Task 7), `TrailPattern`, `AuraPattern`, `ImpactPattern` (Task 6).
- Produces:
  - `FlightPoseMath.target(FlightPhase phase, Vec3 velocity, float yawRateDegPerTick, VfxParams p) -> FlightBodyTransform` (defaults: HOVER pitch 0–10° by speed, CRUISE up to 55°, BOOST 80°, LANDING/IDLE/TAKEOFF 0; pitch sign follows vertical velocity; roll = clamp(-yawRate × 2.5, ±25°)).
  - `FlightPoseMath.step(FlightBodyTransform current, FlightBodyTransform target, float dtTicks, float halfLifeTicks) -> FlightBodyTransform` — exponential approach `current + (target-current)·(1 − 2^(−dt/halfLife))`, default half-life 3 ticks.
  - `FlightPoseTracker.tick()` (per tracked player: velocity from interpolated position delta, works for remote players), `FlightPoseTracker.transform(int entityId, float partial) -> FlightBodyTransform`.
  - `record FlightPresentation(ResourceLocation takeoffClip, ResourceLocation hoverClip, ResourceLocation cruiseClip, ResourceLocation boostClip, ResourceLocation landClip, @Nullable ResourceLocation trailEffect, @Nullable ResourceLocation boostEffect, SoundEvent loopSound, SoundEvent takeoffSound, SoundEvent boostSound, SoundEvent landSound)`; `FlightPresentations.of(ResourceLocation heroId) -> Optional<FlightPresentation>`; `HeroClientContext.flightPresentation(FlightPresentation)`. Remote players' hero ids resolve through `CoreAttachments.PUBLIC_HERO` (synced to tracking clients, same seam `SkinResolver.heroIdFor` uses).
  - `HomelanderVfxIds` constants (`ModId.of("homelander/<name>")`): `LANDING, LASER, SUN_CHARGE, SUN_DETONATION, MADNESS_CRASH, IRON_FISTS_ON, IRON_FISTS_OFF, IRON_FISTS_HIT, CLAP, ROAR, MILK_DRINK` (`LASER` is the channel id).
  - Phase → clip on `PlayerAnimator.Layer.BASE` with 4-tick crossfades; takeoff/land clips on ACTION; loop sound as an entity-bound `AbstractTickableSoundInstance` whose volume follows `horizontalSpeed`.

- [ ] **Step 1: Write failing tests:**
  - `phaseFlipDoesNotJumpPose`: from converged BOOST (80°) flip target to HOVER for one tick → pitch change ≤ 80·(1−2^(−1/3)) + 0.01.
  - `stepIsContinuousForLargeDt`: `dt=40` → result between current and target, never overshoots.
  - `boostPitchWithinLimit`, `rollClampedTo25`.
  - `HomelanderNoVanillaParticlesTest.homelanderServerCodeSendsNoVanillaParticles`: for every file listed in `CLEANED` (starts with `HomelanderHero.java`), assert no `sendParticles(` and no `ParticleTypes.`. Later tasks append their files. Scope note: this lint only covers direct particle calls — `level.explode` visual particles are handled by Task 10's `SilentParticles` swap, and entity-based visuals by its GameTest (`no LightningBolt spawned`); it is a lint, not proof of zero vanilla visuals.
- [ ] **Step 2: Run** `./gradlew test --tests '*FlightPoseMathTest' --tests '*HomelanderNoVanillaParticlesTest' --no-daemon` Expected: FAIL.
- [ ] **Step 3: Implement** core flight classes, mixin changes, `FlightFx` (trail from each hand/feet anchor while CRUISE/BOOST, boost burst + shock ring on BOOST entry, landing impact effect), Homelander registration.
- [ ] **Step 4: Run** tests Expected: PASS; gate Expected: BUILD SUCCESSFUL (goldens: `hero_presentation.txt` unchanged — `onLanded` behavior other than visuals untouched).
- [ ] **Step 5: Commit** `feat(flight): continuous flight pose and Homelander flight presentation`.

### Task 9: Eye lasers on a VFX channel

**Files:**
- Create: `hero/homelander/ability/EyeLaserPhases.java`, `client/hero/homelander/fx/EyeLaserChannel.java`, `assets/superheroes/vfx/homelander/laser.json`, `quasar/emitters/homelander_laser_impact.json`
- Modify: `hero/homelander/ability/EyeLasersAbility.java` (phases, channel send; remove `BeamFx.laser` and particle spark), `client/hero/homelander/HomelanderClientModule.java` (remove `LocalLaserOverlay.register()`; register channel). **Keep the `STYLE_LASER` `BeamStyle` registration** — `content/boss/homelander` `HomelanderEyeLaserGoal:155` still sends `BeamFx.laser` payloads tagged `STYLE_LASER`; deleting the registration makes the boss's eye lasers invisible (comment the surviving sender in code). If `ModParticles.LASER_SPARK` has no remaining callers after this, delete it in the same task. The madness branch's `level.explode` every 2 ticks intentionally keeps vanilla explosion presentation this pilot — it is gameplay, not staged VFX (a `HomelanderNoVanillaParticlesTest` blind spot; documented, not swept).
- Delete: `client/hero/homelander/render/LocalLaserOverlay.java`
- Test: `src/test/java/io/github/grebeshok105/codex/hero/homelander/EyeLaserPhasesTest.java`, `src/gametest/java/io/github/grebeshok105/codex/gametest/HomelanderVfxGameTests.java`, append `EyeLasersAbility.java` to `HomelanderNoVanillaParticlesTest.CLEANED`

**Interfaces:**
- Consumes: `VfxFx.channel`, `VfxChannelS2CPayload` (Task 3), `PhaseTimeline`, `BeamPattern`, `ImpactPattern`, `HumanoidAnchors.eyes` (Task 6), `PlayerAnimator` (Task 7), `HomelanderSounds.LASER_*`.
- Produces:
  - `EyeLaserPhases.CHARGE_TICKS = 6`, `RELEASE_TICKS = 8` — visual-only phases driving beam intensity via `PhaseTimeline`; `static boolean shouldSendUpdate(int activeTicks)` (every `VfxFx.CHANNEL_UPDATE_INTERVAL_TICKS`).
  - Server: on activate → `START` with current end; while active → `UPDATE` with server raycast end every 2 ticks; on deactivate → `STOP`. UPDATEs keep flowing through the 10-tick `fire=false` pauses of the uranium-threat pulse so the channel never idles into `CHANNEL_TIMEOUT_TICKS = 10`; the beam stays visually lit during pauses (deliberate visual change: today `BeamFx.laser` decays during pauses). The whole `fireBeam` path is byte-identical to today — `target.hurt`, madness explosions, ignite and the fire ring still run on exactly the same `fire` ticks, from tick 0; the charge phase gates nothing on the server.
  - Client `EyeLaserChannel implements VfxChannelEffect`: two beams from `HumanoidAnchors.eyes(source, partial)` to end; local player end = local raycast each frame (responsive), remote = server end lerped over 2 ticks; impact emitter + light at end, one per 3 ticks; `PhaseTimeline(6, 8)` drives intensity; ACTION clips `laser_charge` → `laser_hold` → `laser_release`; sounds charge/loop (entity-bound)/release.

- [ ] **Step 1: Write failing tests:** `EyeLaserPhasesTest.updatesEveryTwoTicksIncludingPulsePauses` (`shouldSendUpdate` true on every 2nd tick across the full active range, including `fire=false` pause ticks), `chargeTicksIsSix`. GameTest `eyeLaserBroadcastsStartUpdateStop`: transform player, activate `EYE_LASERS`, run 10 ticks, deactivate; capture payloads through the test sender hook used by existing FX gametests (if none exists, add a package-private `VfxFx.captureForTests(Consumer<CustomPacketPayload>)`), assert first `START`, ≥ 3 `UPDATE`, last `STOP`. Assert unchanged damage behavior with the existing laser damage assertions (damage lands from the first active tick).
- [ ] **Step 2: Run** `./gradlew test --tests '*EyeLaserPhasesTest' --no-daemon` and `./gradlew runGametest --no-daemon` Expected: FAIL.
- [ ] **Step 3: Implement** server phases/sends and client channel; delete overlay.
- [ ] **Step 4: Run** both Expected: PASS; `rg -n 'LocalLaserOverlay|LASER_SPARK' src` Expected: empty; `rg -n 'STYLE_LASER' src` Expected: only the boss-path registration and `HomelanderEyeLaserGoal` hits; gate Expected: BUILD SUCCESSFUL.
- [ ] **Step 5: Commit** `feat(homelander): eye lasers on VFX channel with eye-anchored beams and phases`.

### Task 10: Milk model, drink, sun build-up and final explosion

**Files:**
- Create: `client/hero/homelander/fx/{SunChargeFx,SunDetonationFx}.java`, `assets/superheroes/vfx/homelander/{sun_charge,sun_detonation}.json`, `quasar/emitters/homelander_sun_*.json`, `core/particle/SilentParticles.java` (+ client no-op provider registration in the core client bootstrap), `assets/superheroes/particles/silent.json` (empty `textures` list — keeps the registry quiet; nothing renders, which is the point)
- Modify: `hero/homelander/item/MilkBottleItem.java` (`use()` start, server side behind the existing `!level.isClientSide()` guard → `VfxFx.event(MILK_DRINK)` — the client plays the `milk_drink` clip + `homelander.milk.drink` sound; `getDrinkingSound`/`getEatingSound` return `ModSounds.SILENT` so vanilla sipping doesn't stutter over the designed 1600 ms one-shot; `finishUsingItem` `WITHER_SPAWN` removed — the `sun_charge` aftermath audio owns that moment), `hero/homelander/runtime/HomelanderMadnessAftermathController.java` (aftermath start → `VfxFx.eventAround(SUN_CHARGE, radius 96)` + `SUN_CHARGE` sound; remove `END_ROD`/`SMALL_FLAME`/visual lightning/`BEACON_ACTIVATE`; `detonateSun` → `VfxFx.eventAround(SUN_DETONATION, radius 160)` — this event is the **single owner** of the `homelander.sun.detonate` sting, played once client-side by `SunDetonationFx` with distance attenuation. Keep both explosions via the 1.21.1 `Level.explode(... ParticleOptions small, ParticleOptions large, Holder<SoundEvent>)` overload — `SilentParticles.SILENT` for both particle args and `BuiltInRegistries.SOUND_EVENT.wrapAsHolder(ModSounds.SILENT)` (registry-backed `Holder<SoundEvent>`) on **both** calls so neither explosion plays vanilla boom or a second copy of the sting; keep fire placement; remove visual-only lightning and vanilla explode/thunder sounds), `hero/homelander/runtime/HomelanderMadnessFlightController.java:74` (→ `VfxFx.event(MADNESS_CRASH)`; remove its `sendParticles` + `GENERIC_EXPLODE` playSound — MADNESS_CRASH owns both)
- Test: append the three files to `HomelanderNoVanillaParticlesTest.CLEANED`; GameTest `sunDetonationKeepsGameplayAndSendsVfx` in `HomelanderVfxGameTests`

**Interfaces:**
- Consumes: `VfxFx.eventAround`, `ShockwavePattern`, `ScreenFlash`, `CameraImpulse`, `AuraPattern`, `VfxBackend.light/distortion/flash`, `PlayerAnimator` (`milk_drink` ACTION, `sun_charge` BASE during aftermath).
- Produces: `SilentParticles.SILENT` (`SimpleParticleType`, id `superheroes:silent`). `SunChargeFx`: 200-tick aura (growing light, ember emitter, heat distortion around the player, rising `sun_charge` sound, subtle camera tremble ramp for players within 32). `SunDetonationFx`: t0 white flash `ScreenFlash.trigger(attenuation(dist,160,LOS), 0xFFFFF4E0, 60)` (the blinding moment), core light radius 48 fading 60 ticks, 2 shock rings, distortion pulse, ember/debris emitters, `CameraImpulse.shake(1.6·att, 30)`.

- [ ] **Step 1: Write failing tests:** GameTest `sunDetonationKeepsGameplayAndSendsVfx`: force aftermath end on a test player with blocks around → blocks destroyed in radius (as before), one `VfxEventS2CPayload` with `effect == superheroes:homelander/sun_detonation`, no `LightningBolt` entity spawned. Extended `HomelanderNoVanillaParticlesTest` fails on the new files.
- [ ] **Step 2: Run** `./gradlew test --tests '*HomelanderNoVanillaParticlesTest' --no-daemon`, `./gradlew runGametest --no-daemon` Expected: FAIL.
- [ ] **Step 3: Implement** server changes + client compositions + milk model hookup (model file is the contract path from Task 2; no Java change needed for the model itself).
- [ ] **Step 4: Run** both Expected: PASS; gate Expected: BUILD SUCCESSFUL.
- [ ] **Step 5: Commit** `feat(homelander): milk drink, sun build-up and final detonation on Visual Core`.

### Task 11: Iron Fists presentation

**Files:**
- Create: `client/hero/homelander/fx/IronFistsFx.java`, `assets/superheroes/vfx/homelander/iron_fists.json`, `quasar/emitters/homelander_iron_fists_*.json`
- Modify: `hero/homelander/ability/IronFistsAbility.java`, `hero/homelander/runtime/IronFistsController.java` (aura interval + shockwave → `VfxFx.event(IRON_FISTS_ON / IRON_FISTS_HIT)`; sounds from `HomelanderSounds`; the hit branch's `ShockwaveUtil.detonate` gets the same `suppressPresentation` flag as the landing call — vanilla burst off, damage + `ScreenShakeS2CPayload` kept) — every server-side `VfxFx` send stays behind the file's existing `!level.isClientSide()` convention
- Test: append both files to `HomelanderNoVanillaParticlesTest.CLEANED`

**Interfaces:**
- Consumes: `AuraPattern` (hand anchors: right/left arm pivot + rotated `-10px` down), `ShockwavePattern` (radius = `IronFistsController.SHOCKWAVE_RADIUS` 4.5 sent as `scale`), `ImpactPattern`, `CameraImpulse`, clips `iron_fists_activate`, `iron_fists_strike`.
- Produces: `IronFistsFx.activate(VfxSpawn)` (hand aura lasting `IronFistsAbility.DURATION_TICKS` 200), `IronFistsFx.deactivate(VfxSpawn)` (early end on `IRON_FISTS_OFF`, sent from `IronFistsController.markDeactivated`), `IronFistsFx.hit(VfxSpawn)`.

- [ ] **Step 1: Write failing test:** extended `HomelanderNoVanillaParticlesTest`.
- [ ] **Step 2: Run** Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** Expected: PASS; gate Expected: BUILD SUCCESSFUL.
- [ ] **Step 5: Commit** `feat(homelander): Iron Fists aura, strike and shockwave on Visual Core`.

### Task 12: Clap and Roar presentation

**Files:**
- Create: `client/hero/homelander/fx/{ClapFx,RoarFx}.java`, `assets/superheroes/vfx/homelander/{clap,roar}.json`, `quasar/emitters/homelander_{clap,roar}_*.json`
- Modify: `hero/homelander/ability/HandClapAbility.java`, `hero/homelander/ability/StunningRoarAbility.java`
- Test: append both files to `HomelanderNoVanillaParticlesTest.CLEANED`

**Interfaces:**
- Consumes: `ShockwavePattern`, `ImpactPattern`, `VfxBackend.distortion`, `CameraImpulse`, clips `clap` (ACTION, contact ≤ 120 ms) and `roar`.
- Produces: `ClapFx` — clap flash between hands at `contact`, forward-cone shock ring + dust from blocks hit, distortion pulse, shake by distance; `RoarFx` — mouth-anchored forward sound-wave cone (repeating rings over 1500 ms), distortion, dust lift, low rumble shake. Both spawned from one `VfxFx.event` each; gameplay (knockback/stun/radius) unchanged.

- [ ] **Step 1: Write failing test:** extended `HomelanderNoVanillaParticlesTest`.
- [ ] **Step 2: Run** Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** Expected: PASS; `rg -n 'sendParticles|ParticleTypes\.' src/main/java/io/github/grebeshok105/codex/hero/homelander` Expected: empty; gate Expected: BUILD SUCCESSFUL.
- [ ] **Step 5: Commit** `feat(homelander): Clap and Roar presentation on Visual Core`.

### Task 13: In-game debug, showcase and performance probe

**Files:**
- Create: `core/vfx/VfxShowcases.java`, `client/core/vfx/debug/{VfxDebugHud,VfxPerfProbe}.java`
- Modify: `content/command/SuperheroesCommands.java` (subtree `vfx`), `hero/homelander/HomelanderModule.java` (register scenes)
- Test: `src/gametest/java/io/github/grebeshok105/codex/gametest/VfxShowcaseGameTests.java`, `src/test/java/io/github/grebeshok105/codex/client/core/vfx/debug/VfxPerfProbeTest.java`

**Interfaces:**
- Produces:
  - `VfxShowcases.register(ResourceLocation id, VfxShowcase scene)`; `@FunctionalInterface interface VfxShowcase { void run(ServerPlayer at, int count); }`; `ids() -> Set<ResourceLocation>`.
  - Commands (permission 2): `/superheroes vfx play <effect> [scale]` (broadcast `VfxFx.eventAround` at the caller's look target, radius 160 — every nearby client sees it), `/superheroes vfx scene <id> [count]`, `/superheroes vfx stress <count>` (count ≤ 64: repeats `homelander/combat` — the heaviest registered scene — in a ring; falls back to the lexicographically-first registered id if it is absent), `/superheroes vfx hud on|off` (sends `VfxEventS2CPayload` with effect `superheroes:debug/hud` to the caller).
  - Homelander scenes: `homelander/flight_path` (applies flight to caller), `homelander/lasers`, `homelander/lasers_flying`, `homelander/sun_detonation` (visual-only: event without explosion), `homelander/combat` (iron fists hit + clap + roar at dummy positions).
  - `VfxDebugHud`: active effects, open channels, Veil on/off, avg FPS, 1 % low, frame ms (from `VfxPerfProbe`).
  - `VfxPerfProbe.record(long frameNanos)`, `averageFps() -> double`, `onePercentLowFps() -> double` over a 600-frame window; `/superheroes vfx hud` shows them.

- [ ] **Step 1: Write failing tests:** `VfxPerfProbeTest.averageAndOnePercentLow` (600 frames at 10 ms + 6 at 50 ms → avg ≈ 97, 1 %-low ≈ 20); GameTest `showcaseSceneBroadcastsToNearbyObservers` (two test players 20 blocks apart, `/superheroes vfx scene homelander/sun_detonation` → both receive the event), `stressCountIsCapped` (`stress 1000` → error message, nothing sent).
- [ ] **Step 2: Run** unit + gametests Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** Expected: PASS; gate Expected: BUILD SUCCESSFUL.
- [ ] **Step 5: Commit** `feat(vfx): in-game showcase commands, debug HUD and performance probe`.

### Task 14: Multiplayer and performance verification (placeholder assets)

**Files:**
- Create: `docs/design/visual-core-homelander/verification.md`

**Interfaces:**
- Consumes: Task 13 commands; `testing-runclient-hud` skill (dev client quirks); two dev clients + `runServer` (or one client LAN host + second client).

- [ ] **Step 1: Multiplayer checklist (spec §11)** — actor and observer each record in `verification.md`: remote flight pose/trail/sounds; lasers seen by observer from the actor's eyes, including observer walking into range mid-laser (Review Focus 1); lasers while flying; sun detonation flash/shake on both with correct distance attenuation; relog mid-effect leaves no stuck beam/light; no client-only state leak (actor sees nothing the observer lacks except first-person offsets). Expected: every row "pass" with a screenshot path.
- [ ] **Step 2: Performance (spec §12)** — normal heavy scene: `/superheroes vfx scene homelander/combat 4` + lasers + flight on the Sodium setup, read `VfxDebugHud` avg FPS over 600 frames. Expected: ≈ 100 FPS or higher. Stress: `/superheroes vfx stress 64`. Expected: no freeze, 1 %-low recovers within 2 s after the burst. Record GPU/CPU, render distance, numbers.
- [ ] **Step 3: Veil-absent run** `./gradlew runClientNoVeil --no-daemon`: trigger every scene. Expected: no crash; fallback flash only.
- [ ] **Step 4: Fix** any failure in the owning task's files, adding the reproducing unit test first; re-run the row.
- [ ] **Step 5: Commit** `docs(vfx): record Homelander multiplayer and performance verification`.

### Task 15: Independent review, OMP integration, final acceptance

**Files:**
- Modify: files under the contract paths (replaced by the approved OMP package), `src/test/resources/contracts/homelander_placeholders.txt` (emptied), `docs/design/visual-core-homelander/verification.md`. (`finalBuildHasNoPlaceholders` is already enabled by the OMP PR — Task 11 of the OMP plan — do not re-enable it here; just require it to pass.)

**Interfaces:**
- Consumes: OMP package from `docs/superpowers/plans/2026-09-28-homelander-omp-assets.md` — only after its Task 10 (third-session review) and Task 11 (user approval recorded in `docs/design/visual-core-homelander/omp-review.md`).

- [ ] **Step 1: Gate check.** Run: `rg -n '^Approved by user:' docs/design/visual-core-homelander/omp-review.md` Expected: one line with date. Missing → stop; integration is blocked (spec §14).
- [ ] **Step 2: Fourth-session review (spec §14)** — a fresh reviewer (superpowers:requesting-code-review) re-checks the original design, not merely the diff, across all nine spec-§14 areas: architecture; runtime behavior; Veil effects; multiplayer; performance; in-game quality; placeholder behavior; requirement coverage; regressions. Every finding fixed with a test first where testable.
- [ ] **Step 3: Integrate OMP assets** — merge the OMP Task 11 PR into the pilot branch (the OMP branch already replaces files at the contract paths; the pilot branch is the merge target the OMP session is told to PR into), then mark each remaining line `# replaced` in the manifest. Run `./gradlew test --tests '*HomelanderAssetContractTest' --tests '*HomelanderPlaceholderGuardTest' --no-daemon` Expected: PASS with `finalBuildHasNoPlaceholders` enabled.
- [ ] **Step 4: Scene tuning** — adjust `vfx/homelander/*.json` (and, per OMP spec §12, OMP resources if isolation quality does not survive the scene), recording changes in `verification.md`.
- [ ] **Step 5: Re-run Task 14 Steps 1–3 with final assets.** Expected: all rows pass.
- [ ] **Step 6: Acceptance scenes (spec §15)** in-game, each recorded: flight; eye lasers; eye lasers while flying; flight VFX; milk final explosion. Expected: user sign-off line per scene in `verification.md`.
- [ ] **Step 7: Definition-of-done check (spec §16)** — tick each bullet in `verification.md` with the evidence path. `./gradlew qualityGate --no-daemon` Expected: BUILD SUCCESSFUL.
- [ ] **Step 8: Commit** `feat(homelander): integrate approved OMP assets and complete pilot acceptance`.
