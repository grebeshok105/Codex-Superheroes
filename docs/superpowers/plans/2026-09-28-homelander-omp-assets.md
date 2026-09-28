# Homelander OMP Asset Package Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Produce the production-quality passive Homelander resources (player animations, milk model, VFX textures, sounds) that the shared contract names, pass an absolute third-session review, and obtain the user's explicit visual approval so the Visual Core stream can integrate them unchanged.

**Architecture:** OMP works only on files at the contract paths (`assets/superheroes/{sounds/homelander,player_animations/homelander,models/item,textures/item,textures/vfx/homelander}`) plus authoring sources under `art-source/homelander/omp/`; each final file overwrites its placeholder in place, so no id, registration or Java changes. Quality is pinned by an asset-quality JUnit suite that fails on placeholders and passes only on production assets, alongside the contract test from the Visual Core plan.

**Tech Stack:** Blockbench (Bedrock animation export, Java item model export), ffmpeg + libvorbis (OGG Vorbis), JUnit 5 (asset validation, no Minecraft bootstrap), `./gradlew runClient` for in-game checks, Minecraft 1.21.1 / Fabric.

**Spec:** `docs/design/visual-core-homelander/homelander-omp-assets-design-spec-v2.md` (parent: `docs/design/visual-core-homelander/visual-core-homelander-design-spec-v3.1.md`; runtime side: `docs/superpowers/plans/2026-09-28-visual-core-homelander-pilot.md`).

## Global Constraints

- OMP owns passive resources only; it does not touch Visual Core architecture, Veil runtime, shaders, multiplayer, gameplay, camera/screen logic, runtime timing or final integration (spec §2). No edits under `src/main/java`, `src/client/java`, `assets/superheroes/quasar/`, `assets/superheroes/pinwheel/`, `assets/superheroes/vfx/` — `textures/vfx/` is OMP's own territory, don't confuse the two.
- Work against the shared contract (`docs/design/visual-core-homelander/shared-contract.md`, `src/test/resources/contracts/homelander_pilot.json`), never against the unfinished runtime branch (spec §3). Prerequisite: Visual Core plan Tasks 1–2 merged on `main` (contract + placeholders at final paths). This serializes the two streams deliberately: spec A §6 wants the contract agreed before both sides build against it, and the contract is produced inside Visual Core Task 1, so OMP cannot start earlier without it — the real dependency is the contract + the manifest/placeholder mechanism Task 2 creates (OMP edits `homelander_placeholders.txt` from its first asset task on), not runtime progress.
- Artistic freedom over motion, posing, model styling, texture treatment and sound design (spec §3); contract ids, file paths, bone names, loop flags and approximate durations (±30 %) are fixed.
- Contract change (spec §4, §8): document the problem, propose the smallest change, open a PR/comment on the repository path, continue unrelated work; never diverge silently. Retimes that land outside ±30 % of a recorded `durationMs` are a contract change too: update `homelander_pilot.json` in the same commit via a `contract-changes.md` entry (existing rows record the current measured durations — `iron_fists_impact` 1440, `hand_clap` 4570, `roar` 3450, `roar_deep` 6480).
- Production quality, not placeholders: coherent, technically valid, correctly exported, clean transforms, no obvious clipping, repeat-safe, contract-compliant, directly integrable (spec §7). Every file shipped as final must be mono — `positionalSoundsAreMono` runs over all of `FINAL`, which must cover every shipped contract file, so an existing stereo file (`hand_clap`, `roar`, `roar_deep`, `iron_fists_impact`, `iron_fists_charge` are all stereo today) cannot slip through: re-encode with `ffmpeg -ac 1` when no fuller rework is authored.
- Runtime sounds OGG Vorbis only; check `art-source/` first; MP3 → `ffmpeg -i input.mp3 -c:a libvorbis -qscale:a 5 output.ogg`.
- Out of scope (spec §6): bottom UI, left flying menu, chat/UI overlap, damage balance, Uranium, full roster, Veil core/runtime VFX, multiplayer, gameplay rewrites, other heroes.
- Third-session review then explicit user approval are hard gates before integration (spec §10–§11).
- Gate per task: `./gradlew test --tests '*HomelanderAssetContractTest' --tests '*HomelanderAssetQualityTest' --tests '*HomelanderPlaceholderGuardTest' --no-daemon`; before hand-off `./gradlew qualityGate --no-daemon`.

## Review Focus

1. Positional sounds exported stereo → Minecraft does not attenuate stereo sounds by distance, so the actor's laser/roar is equally loud across the map. Pinned: Task 1 `positionalSoundsAreMono`.
2. Loop clips/sounds with a seam (last keyframe ≠ first, audible click at the loop point) → visible pop every cycle during hover/laser hold. Pinned: Task 1 `loopClipsCloseOnFirstPose`; Task 9 loop-seam listening step.
3. Animation rotates arms/legs into the torso or head at extreme flight tilt (80° boost pitch) → clipping in the most-watched scene. Pinned: Task 1 `jointRotationsWithinHumanLimits`; Task 2 in-game tilt check.
4. Milk model oversized or off-center in hand/GUI/ground/frame → broken inventory icon or hand pose. Pinned: Task 1 `milkModelWithinItemBoundsAndHasAllDisplays`.
5. A replaced asset keeps an uncommented placeholder manifest line (or a placeholder is left unreplaced) → integration ships a placeholder. Pinned: existing `HomelanderPlaceholderGuardTest.manifestHashesMatchFiles` (fails on any changed-but-still-listed file), `finalBuildHasNoPlaceholders` (also asserts every `# replaced` line's file now differs from its placeholder hash) enabled at the end of Task 11.

---

## File Structure

- `art-source/homelander/omp/animations/homelander_player.bbmodel` — Blockbench source for all player clips (player rig with bones `head, body, right_arm, left_arm, right_leg, left_leg`).
- `art-source/homelander/omp/models/milk_bottle.bbmodel` — milk model source.
- `art-source/homelander/omp/audio/<event>/…` — raw/layered audio sources (WAV/FLAC/project files) per sound event; `SOURCES.md` lists origin and license of every raw input.
- `src/main/resources/assets/superheroes/player_animations/homelander/*.animation.json` — exported clips (replace placeholders).
- `src/main/resources/assets/superheroes/models/item/milk_bottle.json`, `textures/item/milk_bottle.png` — milk model.
- `src/main/resources/assets/superheroes/textures/vfx/homelander/*.png` — passive explosion/laser textures.
- `src/main/resources/assets/superheroes/sounds/homelander/*.ogg` — exported sounds (replace placeholders / existing files).
- `src/test/resources/contracts/homelander_placeholders.txt` — mark a line `# replaced` when its file is final (never delete it: the recorded placeholder hash is what `finalBuildHasNoPlaceholders` verifies the final file against).
- `src/test/java/io/github/grebeshok105/codex/assets/HomelanderAssetQualityTest.java` — production-quality checks.
- `docs/design/visual-core-homelander/contract-changes.md` — contract change log.
- `docs/design/visual-core-homelander/omp-review.md` — third-session review + user approval record.

---

### Task 1: Contract intake, raw inventory, asset-quality suite

**Files:**
- Create: `art-source/homelander/omp/SOURCES.md`, `docs/design/visual-core-homelander/contract-changes.md`
- Create: `src/test/java/io/github/grebeshok105/codex/assets/HomelanderAssetQualityTest.java`
- Modify: `src/test/java/io/github/grebeshok105/codex/assets/OggInfo.java` (add `channels(Path)`)

**Interfaces:**
- Consumes: `homelander_pilot.json` schema and `OggInfo.durationMs(Path)` (Visual Core Task 1).
- Produces: `OggInfo.channels(Path) -> int` (Vorbis id header byte 11) added next to `durationMs`; `HomelanderAssetQualityTest` with a `Set<String> FINAL` of contract files already delivered — every quality check runs only over `FINAL`, so each later task adds its files and turns its checks red → green.
- Produces: `contract-changes.md` entry format: `## <date> <id>` / Problem / Smallest change / Status (`proposed|accepted|rejected`) / Link.

- [ ] **Step 1: Inventory raw assets.** List `art-source/sounds/homelander/`, `art-source/**/homelander*` and every existing `sounds/homelander/*.ogg`; write `SOURCES.md` (file, origin, usable for which contract row).
- [ ] **Step 2: Write the failing tests** (all over `FINAL`; seed `FINAL` with one entry, `player_animations/homelander/flight_hover.animation.json`, so the suite is not vacuous):
  - `positionalSoundsAreMono`: `OggInfo.channels(f) == 1`.
  - `soundsAre44100or48000Hz`.
  - `loopClipsCloseOnFirstPose`: for `loop:true` clips, every bone's rotation/position at `0` equals the value at `animation_length` within 0.5°/0.05 px.
  - `clipsAnimateAtLeastThreeBones` (placeholders key only `body`).
  - `jointRotationsWithinHumanLimits`: sampled every 1/20 s — `head` x ∈ [−80°, 60°], arms x ∈ [−200°, 60°], legs x ∈ [−100°, 100°], any z ∈ [−120°, 120°], `body` x/z ∈ [−15°, 15°] (the contract's root-rotation envelope — runtime tilts the whole body).
  - `milkModelWithinItemBoundsAndHasAllDisplays`: all element `from/to` ∈ [−16, 32]; `display` has `thirdperson_righthand, firstperson_righthand, gui, ground, fixed`; ≥ 4 elements.
  - `vfxTexturesArePowerOfTwoWithAlpha`: width/height powers of two, ≤ 256, PNG color type 6.
  - `clipsUseNumericKeyframesOnly`: every keyframe value in every `FINAL` clip is a number array or `{pre, post}` object — any string (Molang) value fails. (The runtime parser warns and skips such clips; a clip that skips at runtime must fail here first.)
  - `finalSetCoversAllDeliveredAssets`: every contract file that exists, is absent from `homelander_placeholders.txt`, **and** was delivered by OMP must appear in `FINAL` — 'delivered' means created at a contract path that had no file at Task-1 baseline, or whose manifest line is marked `# replaced`, or that a task reworked (each rework task adds its files to `FINAL`). Pre-existing files are swept in via their rework tasks — mandatory anyway since all five existing contract sounds are stereo — so at Task 1 `FINAL` starts empty and `positionalSoundsAreMono` is not forced red by untouched files.
- [ ] **Step 3: Run** the gate command. Expected: FAIL (compile error: `OggInfo.channels` missing).
- [ ] **Step 4: Implement `OggInfo.channels`.** Re-run. Expected: FAIL only `clipsAnimateAtLeastThreeBones` on the `flight_hover` placeholder (proves the suite rejects placeholders). Remove the seed from `FINAL` (Task 2 re-adds it); re-run. Expected: PASS.
- [ ] **Step 5: Commit** `test(assets): add Homelander OMP asset quality checks and raw inventory`.

### Task 2: Flight animations

**Files:**
- Modify: `player_animations/homelander/{flight_takeoff,flight_hover,flight_cruise,flight_boost,flight_land}.animation.json`, `art-source/homelander/omp/animations/homelander_player.bbmodel`, `homelander_placeholders.txt`, `HomelanderAssetQualityTest.FINAL`

**Interfaces:**
- Consumes: contract rows Takeoff/Hover/Cruise/Boost/Landing (takeoff 400 ms, land 500 ms, three loops); the runtime tilts the whole body up to 80° in BOOST and 55° in CRUISE and crossfades clips over 4 ticks, so clips carry limb/torso motion only — no root rotation baked into `body` beyond ±15°.

- [ ] **Step 1: Add the five files to `FINAL`.** Run the gate. Expected: FAIL (`clipsAnimateAtLeastThreeBones` on placeholders).
- [ ] **Step 2: Author and export** from Blockbench (Bedrock animation, names `animation.superheroes.homelander.<clip>`); hover = calm controlled float, cruise = streamlined, boost = full "superman" extension, takeoff/land read as deliberate transitions.
- [ ] **Step 3: Run** the gate. Expected: PASS except `HomelanderPlaceholderGuardTest` (files changed, lines present) → mark their five manifest lines `# replaced` → PASS.
- [ ] **Step 4: In-game check (where applicable):** with Visual Core Task 8 available, `./gradlew runClient`, fly through hover → cruise → boost → land in third person (F5) at the 80° boost tilt. Expected: no limb through torso/head, no pop at crossfades. Otherwise check in Blockbench with the body pitched 80°.
- [ ] **Step 5: Commit** `feat(assets): Homelander flight animation clips`.

### Task 3: Eye-laser animations

**Files:**
- Modify: `player_animations/homelander/{laser_charge,laser_hold,laser_release}.animation.json`, bbmodel, manifest, `FINAL`

**Interfaces:**
- Consumes: charge 300 ms, hold loop, release 400 ms. The runtime anchors beams to the rendered head: head rotation in these clips moves the beam origin, so keep `head` motion small (≤ 10°) during `laser_hold`; the head keeps following the player's look on top of the clip.

- [ ] **Step 1: Add files to `FINAL`; run gate.** Expected: FAIL.
- [ ] **Step 2: Author and export** (focused intense stance; charge reads as build-up, release as a controlled end).
- [ ] **Step 3: Run gate; mark manifest lines `# replaced`; run gate.** Expected: PASS.
- [ ] **Step 4: In-game check (where applicable, Visual Core Task 9):** hold eye lasers standing and flying. Expected: beams stay on the eyes, hold loop has no visible seam.
- [ ] **Step 5: Commit** `feat(assets): Homelander eye-laser animation clips`.

### Task 4: Milk model and drink clip

**Files:**
- Modify: `models/item/milk_bottle.json`, `textures/item/milk_bottle.png`, `player_animations/homelander/milk_drink.animation.json`, `art-source/homelander/omp/models/milk_bottle.bbmodel`, manifest, `FINAL`

**Interfaces:**
- Consumes: model path + texture ref `superheroes:item/milk_bottle` (keeps `ProjectSanityTest.assertItemModelsResolveToTextures` green); `milk_drink` 1600 ms (= `MilkBottleItem.DRINK_TICKS` 32), vanilla drink use animation still plays on the item.

- [ ] **Step 1: Add files to `FINAL`; run gate.** Expected: FAIL (`milkModelWithinItemBoundsAndHasAllDisplays` on the one-cuboid placeholder).
- [ ] **Step 2: Model, texture, export** (Java item model with all five displays); author `milk_drink`.
- [ ] **Step 3: Run gate + `./gradlew test --tests '*ProjectSanityTest' --no-daemon`; mark manifest lines `# replaced`; re-run.** Expected: PASS.
- [ ] **Step 4: In-game check:** `/give @s superheroes:milk_bottle`, inspect hotbar, first/third person, dropped, item frame; drink it. Expected: correct scale/orientation in all five, no z-fighting.
- [ ] **Step 5: Commit** `feat(assets): Homelander milk bottle model and drink clip`.

### Task 5: Iron Fists resources

**Files:**
- Modify: `player_animations/homelander/{iron_fists_activate,iron_fists_strike}.animation.json`, `sounds/homelander/iron_fists_activate.ogg`; rework `iron_fists_charge.ogg`, `iron_fists_impact.ogg` when required (below); bbmodel, manifest, `FINAL`

**Interfaces:**
- Consumes: activate 1000 ms, strike 400 ms with `contact` ≤ 120 ms (gameplay hits on the click tick), `iron_fists_charge` stays a loop-safe bed ≥ 1 s. Rework of the two existing files is mandatory when either is stereo (both are — `ffprobe` them) or misses the loudness target below; otherwise optional.

- [ ] **Step 1: Add files to `FINAL`; run gate.** Expected: FAIL.
- [ ] **Step 2: Author clips; design/export sounds** mono, 48 kHz, `-c:a libvorbis -qscale:a 5`.
- [ ] **Step 3: Run gate; mark manifest lines `# replaced`; re-run.** Expected: PASS.
- [ ] **Step 4: Loudness check** `ffmpeg -i <f>.ogg -af ebur128 -f null - 2>&1 | rg 'I:|Peak'` for each file. Expected: integrated −16 ±2 LUFS for one-shots, true peak ≤ −1 dBFS.
- [ ] **Step 5: Commit** `feat(assets): Homelander Iron Fists animations and sounds`.

### Task 6: Clap resources

**Files:**
- Modify: `player_animations/homelander/clap.animation.json`, `sounds/homelander/hand_clap.ogg`, bbmodel, manifest, `FINAL`

**Interfaces:**
- Consumes: `clap` 600 ms, `contact` ≤ 120 ms; `homelander.hand_clap` event (existing 4.57 s file may be shortened; contract row is one-shot, runtime layers its own impact visuals at contact).

- [ ] **Step 1: Add files to `FINAL`; run gate.** Expected: FAIL (existing `hand_clap.ogg` channel/loudness or clip placeholder).
- [ ] **Step 2: Author clip; design sound** (transient at 0 ms aligned to contact, tail for the shockwave).
- [ ] **Step 3: Run gate; mark manifest lines `# replaced`; re-run; loudness check as Task 5 Step 4.** Expected: PASS / within targets.
- [ ] **Step 4: Commit** `feat(assets): Homelander Clap animation and sound`.

### Task 7: Roar resources

**Files:**
- Modify: `player_animations/homelander/roar.animation.json`, `sounds/homelander/{roar,roar_deep}.ogg`, bbmodel, manifest, `FINAL`

**Interfaces:**
- Consumes: `roar` 1500 ms; `homelander.roar` + `homelander.roar.deep` play together at activation (deep layer is the sub-bass bed; existing 3.45 s / 6.48 s files may be retimed to ≈ 1.5–3 s one-shots).

- [ ] **Step 1: Add files to `FINAL`; run gate.** Expected: FAIL.
- [ ] **Step 2: Author clip; design both layers** so they sum without phase cancellation.
- [ ] **Step 3: Run gate; mark manifest lines `# replaced`; re-run; loudness check.** Expected: PASS / within targets.
- [ ] **Step 4: Commit** `feat(assets): Homelander Roar animation and sounds`.

### Task 8: Milk final-explosion passive resources

**Files:**
- Modify: `player_animations/homelander/sun_charge.animation.json`, `sounds/homelander/{sun_charge,sun_detonate}.ogg`, `textures/vfx/homelander/{sun_flash,sun_ring,ember,shock_ring}.png`, manifest, `FINAL`

**Interfaces:**
- Consumes: `sun_charge` loop pose held for the 200-tick aftermath; `sun_charge.ogg` ≈ 10 000 ms rising build-up ending into the detonation; `sun_detonate.ogg` ≈ 4000 ms (runtime plays it once at the blast; it replaces vanilla explode + thunder). Textures are sprites sampled by Quasar/pattern renderers: premultiplied-free RGBA, soft edges, no baked color grading beyond warm white/orange (runtime tints via params).

- [ ] **Step 1: Add files to `FINAL`; run gate.** Expected: FAIL.
- [ ] **Step 2: Author clip, design sounds** (charge ends at peak tension exactly at file end; detonation has a hard transient then long rumble), paint textures.
- [ ] **Step 3: Run gate; mark manifest lines `# replaced`; re-run; loudness check** (detonation may reach −10 LUFS integrated; peak ≤ −1 dBFS). Expected: PASS.
- [ ] **Step 4: Commit** `feat(assets): Homelander sun build-up and detonation resources`.

### Task 9: Focused Homelander sound pass

**Files:**
- Modify: `sounds/homelander/{flight_takeoff,flight_loop,flight_boost,flight_land,laser_charge,laser_loop,laser_release,milk_drink}.ogg`, `textures/vfx/homelander/{laser_core,laser_glow}.png`, manifest, `FINAL`

**Interfaces:**
- Consumes: contract durations (takeoff 600, boost 1200, land 500, laser charge 300, release 400, drink 1600 ms; `flight_loop`, `laser_loop` loops ≥ 1 s). Runtime scales `flight_loop` volume by horizontal speed, so it must sound right from quiet to full.

- [ ] **Step 1: Add files to `FINAL`; run gate.** Expected: FAIL.
- [ ] **Step 2: Design/export** mono OGG; loops cut at zero crossings with matching start/end.
- [ ] **Step 3: Run gate; mark manifest lines `# replaced`; re-run.** Expected: PASS.
- [ ] **Step 4: Package-wide consistency:** loudness check on all 16 contract sounds; loop-seam listen: `ffplay -loop 10 -nodisp sounds/homelander/<loop>.ogg` for each loop. Expected: targets met, no click at wrap.
- [ ] **Step 5: Verify no placeholder remains:** `rg -v '^\s*(#|$)' src/test/resources/contracts/homelander_placeholders.txt | wc -l` Expected: `0` (blank/comment lines tolerated; the manifest only ever lists Task-2-generated placeholders — pre-existing real assets never get a line).
- [ ] **Step 6: Commit** `feat(assets): Homelander flight, laser and drink sounds`.

### Task 10: Third-session absolute review and fixes

**Files:**
- Create: `docs/design/visual-core-homelander/omp-review.md`
- Modify: any package file a finding touches

**Interfaces:**
- Consumes: the OMP branch at the end of Task 9; reviewer is a fresh session that did not author the assets (spec §10).

- [ ] **Step 1: Review** every model, clip, sound and texture against spec §7 and §10: technical validity, export correctness, in-game appearance, clipping/transforms, transitions, timing, contract compliance, package coherence — plus the branch's code/resource glue (the asset-quality suite, `OggInfo.channels`, the placeholder-guard edit) and genuine integration readiness. Record each finding in `omp-review.md` (`id / asset / problem / fix / status`).
- [ ] **Step 2: Fix every finding** (reviewer fixes directly, spec §10), re-running the gate after each.
- [ ] **Step 3: Run** `./gradlew qualityGate --no-daemon`. Expected: BUILD SUCCESSFUL; all findings `fixed`.
- [ ] **Step 4: Commit** `fix(assets): address third-session OMP review findings`.

### Task 11: User approval gate and hand-off

**Files:**
- Modify: `docs/design/visual-core-homelander/omp-review.md`, `src/test/java/io/github/grebeshok105/codex/assets/HomelanderPlaceholderGuardTest.java` (enable `finalBuildHasNoPlaceholders` on the OMP branch)

**Interfaces:**
- Produces: line `Approved by user: <YYYY-MM-DD> <commit sha>` in `omp-review.md` — consumed by Visual Core Task 15 Step 1; nothing integrates without it (spec §11).

- [ ] **Step 1: Present** the corrected package to the user: screenshots/clips of every animation (third person), the milk model in all displays, an audio listening list, and the review log.
- [ ] **Step 2: Apply user feedback** as Task 10 findings until the user approves; record the approval line with the approved commit.
- [ ] **Step 3: Enable `finalBuildHasNoPlaceholders`; run** `./gradlew qualityGate --no-daemon`. Expected: BUILD SUCCESSFUL.
- [ ] **Step 4: Commit and open PR** `feat(assets): user-approved Homelander OMP asset package` into the pilot's feature branch (the branch Plan A Tasks 3–15 commit to — `feat/visual-core-homelander-pilot` by default; the Visual Core integrator names it if different); the integrator may still adjust resources in-scene (spec §12).
