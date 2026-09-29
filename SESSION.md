# SESSION.md

## Active work

- Homelander UX fix round — two open PRs, both CI green, awaiting user in-game check:
  - PR #137 `devin/1790691597-homelander-sounds`: all 7 flight/laser placeholder oggs → real sourced recordings (Mixkit + Pixabay), same filenames, `sounds.json` untouched, `homelander_pilot.json` retimed + `contract-changes.md` logged.
  - PR #138 `devin/1790691307-homelander-vfx-fix`: belly-up descend flip removed, clip limbs locked against vanilla swing, eye anchors roll-then-pitch (match renderer), golden trail → supersonic vapor-cone + streaks + ⊥ shockwave rings, presentation phase derived from real client velocity (server phase stalls in HOVER for packet-driven players).
- State of the program: architecture migration (`docs/design/architecture-migration/`) landed through O-final; Visual Core + Homelander pilot delivered through Task 15 (verification table: `docs/design/visual-core-homelander/verification.md`).
- Remaining OMP gap: sounds/textures/milk-model placeholders — no OMP implementation branch exists; see `contract-changes.md`.
- Delivery rules: `qualityGate` before every PR; per-plan «Статус стадий» tables are the trackers; user verifies all in-game behavior themselves — never claim visual verification for them.

## Important decisions

- Bound weapons are identified by type (`BoundWeaponItem`) and validated by token; untokened copies are treated as stale on purpose.
- Bound weapons never become item entities: returned to the owner when valid and there is room, otherwise deleted (the ability can reissue).
- `HeroData` sync: writes are immediate; resource-only syncs are coalesced per tick into one `ResourceUpdateS2CPayload`.
- `AbilityRouter.deactivate` order is flag-off → `onDeactivate`.
- `EntityControlLock` owns the four persisted entity flags; controllers never touch the flags. Locks are re-established on join, not restored.
- Lifecycle cleanup must not rely on `ServerPlayConnectionEvents.DISCONNECT` (may fire on the Netty thread) — use main-thread hooks before the player is saved.
- `PUBLIC_HERO` mirrors the hero id only — widening it would re-create the `HERO_DATA` broadcast privacy surface.
- Ability-scoped attribute buffs are transient (never serialize); hero base passives stay permanent.
- Flight pose: pitch is head-first 0..80° in every phase (no descend negation); eye anchors must apply roll-then-pitch to match the renderer's `mulPose(XP)·mulPose(ZP)` order; clip limbs are zeroed (`resetDrivenLimbs`) before `apply` so clips render as absolute poses while the head keeps view tracking.

## Completed this session (Homelander UX fix — presentation phase)

- Root cause of «в игре ничего не работает»: `FlightPhaseResolver.resolve` видит только серверную `deltaMovement` ≈ 0 для packet-driven локального игрока → synced phase застревал в HOVER → CRUISE-клипы, тилт, трейл, буст-звук и громкость лупа были мертвы в реальной игре. `FlightPoseTracker` теперь вычисляет `presentationPhase` из реальной клиентской скорости (pos-delta per tick), с teleport-guard (>10 b/t → HOVER); `TrailFx` гейтится на `FlightPoseTracker.phase(entityId)`; loop-volume берёт `max(synced, real)` hSpeed.
- Verified: `qualityGate` green; runClient — `effects:` 0→9 при полёте с зажатым W (трейл спавнится), лазеры идут из глаз. F5 застрял в first-person (dev-client quirk) — визуальный тилт проверяет юзер.

## Known issues / follow-ups

- `auto-approve-pr.yml` auto-approves green PRs (audit §3) — repository-owner decision, unchanged.
- `homelanderbossgametests.bosstargetsplayerandshowsbar` is flaky in CI (racy chunk/nearest-hostile scan, fails ~1/N runs, passes on re-run) — candidate for hardening.
- `FlightPhaseResolver` reads server-side `deltaMovement`, which is ~0 for packet-driven players — presentation now re-derives the phase client-side (`FlightPoseTracker.presentationPhase` mirrors the resolver rules + teleport guard); TrailFx gates on `FlightPoseTracker.phase(entityId)`. The synced phase still drives server logic; revisit if that logic ever needs real speeds on a dedicated server.
- On the 0.08 b/t boundary the phase flaps CRUISE↔HOVER and the trail blinks (parked minor).
- Owner decision needed before plan 5 stage `E1`: new root package name (proposed `io.github.grebeshok105.codex`, decision R14).

## Next session

1. Read this file, `AGENTS.md`, and the plan's «Статус стадий» tables.
2. Keep `qualityGate` green; add GameTests for server behavior; update this file per session.
3. Dev-client input: held keys need `xdotool keydown --window <wid>` + `windowfocus --sync` — plain `keydown` without `--window` silently never reaches GLFW key state (zza stays 0). Cheats die with each client restart — re-enable via Open to LAN.

## Completed this session (Visual Core pilot — fourth-session review, spec §14 Stream B)

- Абсолютное ревью ветки `feat/visual-core-homelander-pilot` (32 коммита над main, 231 файл): все 9 областей §14.4 проверены, отчёт — `docs/design/visual-core-homelander/fourth-session-review.md`.
- Три реальных дефекта найдены и исправлены (`fix(vfx)` коммиты с воспроизводящими тестами):
  - `b717b06` — `PlayerAnimator.stop` гасил ВСЮ полосу слоя: SunChargeFx убивал `flight_*` BASE-луп, трекер полёта убивал `sun_charge`, EyeLaserChannel на DONE/cancel сносил чужие ACTION-клипы; `release()` никогда не гасил WRAP `laser_hold` (маскировалось лейн-киллом). Теперь `stop(entityId, layer, fadeTicks, clipIds...)` — по-клипово.
  - `c0c2b1a` — легаси `FlightTrailManager` (ванильные END_ROD/CLOUD) работал и для Homelander → двойной след поверх FlightFx-лент. `FlightPoseTracker` маркирует presentation-owned в `ClientFlightState` (без package-цикла fx⇄core), трейл-менеджер их пропускает.
  - `308515e` — `VeilPostEffects`/`FallbackVfxBackend` пинили вспышку на пике: 60-тиковый фейд ScreenFlash рисовался ~3с на максимуме + хвост. Новый `FlashEnvelope` (feed принимается при `intensity >= shown`) делится обоими бэкендами.
- Тесты: `PlayerAnimatorTest` +2 (per-clip stop, fade-sibling), `FlashEnvelopeTest` (6 кейсов), `ClientFlightStateTest` (mark/unmark/clearAll). `qualityGate --no-daemon` BUILD SUCCESSFUL — test, 364/364 gametests, arch-baseline, jar-isolation, datagen-verify.
- Паркованное не трогал (BeamPattern noise-seed, `VfxSpawn.seed` unread, roar shake 0.8 vs 2.0 — настройка в params, не контракт).
- Вердикт: **Pilot ready for final integration**; §16 gaps — только ожидаемые OMP-блокировки.

## Completed this session (Visual Core pilot — Task 15, final acceptance)

- OMP gate: FAILED — реализационной OMP-ветки нет вообще; `omp-review.md` отсутствует на всех ref'ах, в манифесте все 18 placeholder-строк живые. Gap зафиксирован в `docs/design/visual-core-homelander/verification.md`, Steps 3 (merge+contract tests) и 5-final-assets не выполнимы.
- Step 4 tuning: `roar.json` `ringSpacing` 1.5→1.1 (кольца выплёвывались ~15.7b при радиусе урона 12b → теперь ~12.1b). Остальные parked-minors — без дефектов в свежих записях, не тронуты.
- Step 5: `da47948..HEAD` — три фикса параллельного ревью (b717b06 per-clip stop, c0c2b1a legacy trail off, 308515e FlashEnvelope). Перепроверено в игре на 1b58334: ленты трейла спавнятся и рендерятся (`effects: 1` в CRUISE), белая ванильная колонна исчезла, флэш детонации плавно затухает, лазеры гасятся на релизе. Механика подтверждена временным логом (revert): телепорты дают hSpeed=0 → фаза HOVER → трейла нет; нужен реальный ввод. На границе 0.08 b/t фаза флапает CRUISE↔HOVER → трейл мигает (minor, запарковано).
- Step 6: все 5 сцен §15 пересняты/подтверждены на `1b58334`, таблица в verification.md — 5×PASS (bounded: placeholder-ассеты, llvmpipe). §16 DoD gap: только OMP-строки + ~100 FPS на Sodium-железе.
- Риг: tmux `vfx-server` + runClient Player758; `pin-energy` убит, полёт/лазеры выключены.

## Completed this session (OMP intake fixes — Homelander clips)

- Branch `devin/1790664303-omp-intake-fixes` off `main` HEAD (post-#131). Applied the two pending OMP intake fixes from `docs/superpowers/plans/2026-09-28-homelander-omp-assets.md` §Global Constraints.
- Tripwire: `HomelanderAssetContractTest.contractClipEventTimesMatchManifest` un-`@Disabled`ed — verified RED first (missing `contact`), GREEN after fixes. `finalBuildHasNoPlaceholders` stays disabled (11 sounds, milk model, 6 VFX textures still placeholders).
- Fix A: `events` timeline `{"<s>": {"name": "contact"}}` — `clap` at 0.0833 (arm-cross keyframe, matches `ClapFx` fallback) and `iron_fists_strike` at 0.12 (fist full extension); both ≤120 ms. Both `BedrockAnimationParser.eventTimes` and the test helper read `effect`/`name` under `sound_effects`/`particle_effects`/`events` — `"event"` is NOT a recognised field.
- Fix B: `body` rotation flattened per-axis to ≤±15° (scale keeps temporal shape): `flight_boost` 90°, `flight_cruise` 84°, `flight_land` 79°, `flight_takeoff` 18°, `iron_fists_strike` 26° (y-twist + 18° x), `sun_charge` 17°. `roar` = exactly 15° untouched. Runtime supplies real tilt (CRUISE ≤55°, BOOST 80°) — no contract-changes entry needed.
- Docs: `verification.md` OMP-integration-gap section updated; stale `ClapFx` comments corrected (clip now declares `contact`).
- Remaining OMP gap: sounds/textures/milk-model placeholders; no OMP implementation branch exists.

## Completed this session (Homelander model-drift fix + flight clip rework)

- Root cause of "model broken / drifts off": `PlayerPoseApplier` adds clip deltas on top of `ModelPart` state, but vanilla `setupAnim` never calls `resetPose` and only rewrites the axes it animates — pivot offsets (`x/y/z`) and rotations on untouched axes accumulated every frame and stayed corrupted after clips ended. `PlayerPoseApplier.apply` now snapshots every touched part's `PartPose` and returns a `Restoration`; `PlayerModelPoseMixin` restores it at HEAD of the next `setupAnim`. New `PlayerPoseApplierTest` covers apply-from-baseline, per-frame non-accumulation, restore, empty samples.
- Flight clips reworked (`homelander` scope only): `flight_takeoff` — vertical ascent, relaxed pose, one leg trailing, no flip (end pose still lands on hover's first pose); `flight_cruise` — right arm extended forward (~128deg, superman), left tucked; `flight_land` — gentle absorb instead of +/-39deg leg kick; `flight_boost`/`flight_hover` already matched spec, unchanged. `body` root pitch stays <=15deg; loops still close on first pose; `contact` events untouched.
- Verified: `./gradlew test` + `./gradlew qualityGate --no-daemon` BUILD SUCCESSFUL. In-game (runClient, third person): model stays assembled through laser_hold/clap/flight and returns to clean vanilla pose after clips; before-branch screenshots show the twisted head + fragmented parts for contrast.

## Completed this session (fix — eye lasers: two red beams from the eyes)

- Branch `devin/1790684081-fix-eye-lasers` off `main` HEAD (909b003). User report: lasers must be two sharp red beams from the eyes; in flight they spawn "from thin air" and read yellow.
- Root causes found:
  1. **Air-origin in flight**: `EyeLaserChannel.headAnim()` fed raw Bedrock sample rotations to `HumanoidAnchors.eyes`. The renderer (`PlayerPoseApplier.apply`) maps them as `xRot += -x`, `yRot += -y`, `zRot += +z` (model +z = back axis, so +zRot is a *negative* forward-roll). Correct entity-space delta is `(-v.x, -v.y, -v.z)·weight` — the old code used the raw `(+v.x, +v.y, +v.z)`, so `flight_cruise`/`flight_boost` head.x ≈ -65°/-73° put the anchor ~130° off the rendered head → beams detached in flight. Small laser-clip offsets (-2…-8°) masked it standing.
  2. **Yellow/single look**: `laser.json` core `#FFFFF3E8` (warm white, reads yellow on additive) + glow 0.18 wide vs eyes only 0.125 apart (`EYE_LATERAL=0.0625`) → one fat pale beam.
- Changes:
  - `PlayerPoseApplier.renderedRotationDeg(sample, bone)` — returns `(-v.x,-v.y,-v.z)·weight`; `EyeLaserChannel.headAnim` delegates.
  - `HumanoidAnchors.EYE_LATERAL` 0.0625 → 0.125 (2px eye centres on the 8px head face); `eyes()` javadoc pins the entity-space convention.
  - `laser.json`: coreWidth 0.05→0.04, glowWidth 0.18→0.11, core `#FFFF3322`, glow `#8CE61400`, light `#FF2200`, noise 0.35→0.3. Impact gradient `homelander_laser.json` shifted hot-red → red → dark-red.
- Tests: new `PlayerPoseApplierTest` (sign convention, weight rescale, empty cases); `HumanoidAnchorsTest` + eyes-under-tilted-head case. Scoped `./gradlew test` green.
- Verification: in-game repro of the OLD bug captured (single pale beam from air). In-game re-verification of the fix STOPPED per user instruction — user verifies themselves. Beams render via `CrossBeamRenderer` (backend-independent → fallback gets the same two red eye-anchored beams).

## Completed this session (Homelander VFX fix round — flip, limbs, lasers, supersonic trail)

- Branch `devin/1790691307-homelander-vfx-fix` off `main` HEAD (958b2e5). User report: legs dangle in flight, model flips upside-down while descending, lasers leave from "the sky above the head", golden trails look wrong.
- Root causes:
  1. **Belly-up flip + sky lasers (one bug)**: `FlightPoseMath.target` negated pitch when `velocity.y < -descendEps` — diving flipped the rendered body belly-up, and the same negated tilt swung the eye anchors up-back, so beams left from the air above/behind the head. Negation removed: pitch stays positive (head-first) in every phase; `descendEps` dropped from `vfx/flight/pose.json`. `FlightPoseMathTest.boostPitchWithinLimit` now pins `+80` on descent.
  2. **Dangling legs**: `PlayerPoseApplier` adds clip deltas on top of the vanilla pose, and the static straight-leg base only ran for heroes *without* a `FlightPresentation` — Homelander's limbs kept vanilla walk/fall swing under the clips. New `PlayerPoseApplier.resetDrivenLimbs` zeroes the rotations of every sample-driven part except `head` (head keeps view tracking) before `apply`, gated on `flying && hasPresentation` in `PlayerModelPoseMixin` — clips now render as authored absolute poses.
  3. **Anchor order**: `HumanoidAnchors.applyTilt` applied pitch then roll; the renderer's `mulPose(XP)·mulPose(ZP)` applies roll to the vector first. Swapped to roll-then-pitch so anchors match the render exactly under banked flight.
  4. **Yellow trails**: `FlightFx.TrailFx` golden limb ribbons replaced by a supersonic signature — vapor-cone core + wide faint sheath ribbons off the chest, hairline speed streaks off the fists, and `ShockwavePattern` pressure rings popped every `ringIntervalTicks` on a new oriented-ring constructor (plane ⊥ flight direction). Colors retuned cool white/cyan in `vfx/homelander/flight.json`; boost ring recolored to match the barrier-break flash; landing ring unchanged.
- Sounds: delegated — child session `devin-98655835c36e4994892b4d102dba478b` replaced all 7 flight/laser placeholder oggs with real sourced SFX (PR #137, CI green).
- Verified: in-game repro captured the belly-up dive + skyborne beams before the fix. `./gradlew qualityGate --no-daemon` BUILD SUCCESSFUL (24 tasks). Trail alphas bumped after code review vs the old gold trail's 0x90 (core 0xB8, sheath 0x45, streaks 0x9E, rings 0x70/0x90) so the additive `lightning` render stays readable against bright sky. Temp debug prints removed. In-game re-check left to the user per the no-verification rule.

## Completed this session (Homelander flight/laser SFX — real recordings)

- Branch `devin/1790691597-homelander-sounds` off `main` HEAD. Replaced the 7 synthesized placeholder tones (`flight_takeoff`, `flight_loop`, `flight_boost`, `flight_land`, `laser_charge`, `laser_loop`, `laser_release`) with real recordings — same filenames, `sounds.json` untouched, other hero/homelander oggs untouched.
- Sources: user-downloaded Pixabay mp3s in `art-source/sounds/homelander/` (thunderclap → boost, ground-impact → land) plus Mixkit SFX downloaded this session (raw mp3s kept in art-source, full URL+license table in `art-source/sounds/homelander/SOURCES.md`).
- Mastering: layered mixes in numpy float32 @44.1kHz; one-shots ≈ −15 LUFS, loops ≈ −16 LUFS integrated; post-Vorbis decode peaks ≤0.96 (re-encode loop kills codec overshoot clipping); both loops rebuilt seamless — wrap tail ghosted into head via equal-power crossfade (wrap sample step inside the natural step distribution, no click).
- Verification: ffprobe codec/sr/duration, decode-peak, wrap-seam and band-energy checks all programmatic — NOT verified in game (user verifies everything themselves).
