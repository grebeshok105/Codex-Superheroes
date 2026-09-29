# Fourth-Session Review — Visual Core + Homelander pilot

Spec §14 Stream B absolute review of `feat/visual-core-homelander-pilot`
(base `da47948`, 29 pilot commits + 3 review fixes = 32 commits, +10 096/−393,
231 files). Re-checked the design itself, not just the diff. Three real
defects found, all fixed with reproducing tests; gate green.

Gate evidence: `./gradlew qualityGate --no-daemon` — BUILD SUCCESSFUL in 46 s:
`test` (all JUnit, incl. new suites), `testProjectSanity`, `runGametest`
(364/364 passed — the known suite-level flake did not trigger),
`verifyArchitectureBaseline`, `auditReleaseJarIsolation`, `runDatagen` +
`verifyGeneratedSources` all green.

## Per-area verdicts

### 1. Architecture — ✅

- Hero-agnostic core holds: zero `homelander` references under
  `client/core/**` or `core/**` except javadoc prose; `foundry.veil` imports
  exist only in `client/core/vfx/veil/**` plus the pre-existing
  `VeilScorpionFx`; `src/main` never touches Veil (enforced by
  `VeilIsolationTest`/`ArchitectureRulesTest`).
- Server→client strictly through `core.net` payloads
  (`VfxEventS2CPayload`, `VfxChannelS2CPayload` + `BeamFxS2CPayload`,
  `ScreenShakeS2CPayload`), `PayloadRegistrar` + `StreamCodec` typed;
  receivers dispatch via `context.client().execute` (client-thread only).
- `VfxBackends` seam is real: `current()` picks Veil when loaded else
  Fallback; `HomelanderFx` is a single registration hub over
  `HeroClientContext` — a second hero needs no core changes.
- No package cycles introduced by pilot code; client.fx → client.core
  direction was preserved deliberately in the F3 fix.

### 2. Runtime behavior — ⚠️→✅ (one real defect, fixed)

- `VfxRuntime`/`VfxInstanceTable`/`VfxChannelTable`: spawn strictly on the
  client thread inside `execute`, snapshot-iterates tables (CME-safe),
  `ClientSessionState.reset` clears runtime + animator + tracker + auras +
  flash; eviction → `cancel()` → `channels.forget`; silence > 10 ticks →
  release+remove; spawn culls at 160 blocks from camera.
- `PlayerAnimator` lanes are correct (BASE+ACTION merge, weight math
  verified against `PoseSample.weight` restoration, cap 8) — but
  `stop(entityId, layer, fadeTicks)` was lane-wide, and every one of its
  five call sites killed clips owned by other effects (F1, fixed: per-clip
  stop API).
- `FlightPoseTracker`: velocity from position delta (remote-safe), loop
  volume ∝ `horizontalSpeed`, takeoff/land as ACTION one-shots — sound; but
  its BASE-lane stops used to kill `sun_charge` (F1) and the legacy trail
  kept running for presentation heroes (F3, fixed).
- `EyeLaserChannel` release path left the WRAP `laser_hold` clip looping —
  masked before only by the DONE lane-kill (part of F1, fixed explicitly).

### 3. Veil effects — ⚠️→✅ (one real defect, fixed)

- Post pipelines (`vfx_flash`, `vfx_distortion`) resolve and apply cleanly;
  the Task-14 distortion `worldToScreenSpace` fix (`5b498c8`) is still
  correct and locked by `PinwheelShaderContractTest`.
- Defect: `VeilPostEffects.flash` and `FallbackVfxBackend.flash` compared
  each feed against the stored **peak**, so `ScreenFlash`'s 60-tick decayed
  re-feeds could never lower the shown level — a 60-tick fade rendered as
  ~3 s at full brightness + 0.3 s tail (F2, fixed via shared
  `FlashEnvelope`).
- Fallback parity now exact for flash; emitters/lights/distortion degrade
  by design; `distortion`/`light`/`emit` call sites all go through the
  backend interface.

### 4. Multiplayer — ✅

- Payload shapes verified: `VfxEvent` (effect, sourceEntityId, origin,
  target, scale, seed) via `VfxFx.event`/`eventAround`; channel START /
  UPDATE / STOP with `channelId` matching, UPDATE cadence 2 t, heartbeat
  through uranium pauses, silence timeout 10 t.
- Audience correct: `trackingAndSelf` for channels (self sees own lasers),
  `eventAround` for world events.
- Late-tracking pending-aura path present (`IronFistsFx` retry,
  `resolveGraceTicks=20`), channel UPDATE retries cover late entity load.
- Remote-hero resolution: `SkinResolver.heroIdFor` public + PUBLIC_HERO
  sync — `FlightPoseTracker` poses remote flying Homelanders.
- `EyeLasersAbility` sends ACTIVE_TICK heartbeats through `fire=false`
  pauses — the documented design keeps remote beams alive during uranium
  breaks; not a finding.

### 5. Performance — ✅

- No spawns in render paths: effects emit during `tick()`; render only
  draws beams/rings/lights.
- Deque iteration is for-read on lane copies; hot loops allocate bounded
  (Vec3 reuse where hot, per-frame eye anchors computed once).
- Emitter cadences are param-driven (sun embers /4 t, distortion /6 t,
  clap cone 10 steps × 1.5 blocks, roar rings /3 t, laser impacts /3 t).
- `VfxPerfProbe` math (fps avg/min over window) verified correct;
  `VfxDebugHud` reports it gated behind debug flag.
- Task-14 evidence stands: llvmpipe ~14 fps under combat load, recovered
  clean — bounded, no leak signatures (tables evict, session reset clears).

### 6. In-game quality — ✅ (post-fix)

- Read all compositions against §15 acceptance: FlightFx (trail ribbons +
  boost burst + landing), EyeLaserChannel (charge→hold→release, eye anchors
  with flight tilt + head-anim rescale), SunChargeFx (bound aura, ramping
  light/sound/tremble), SunDetonationFx (flash attenuation by camera
  distance + LOS, MADNESS_CRASH scale), IronFistsFx (aura dedupe, hit
  shockwave+strike+shake), ClapFx (contact-timed burst + cone marching),
  RoarFx (mouth-anchored rings + distortion pulses).
- Before the fixes: sun charge mid-flight broke flight animation; clap
  during lasers died silently; the detonation flash pinned at peak for 3 s.
  All three are now correct (F1, F2). Shake values moved to params —
  `roar` shake 0.8 vs legacy 2.0 is tuned weaker; contract doesn't pin it
  and Task-14 footage was accepted — noted, not a violation.

### 7. Placeholder behavior — ✅

- `homelander_placeholders.txt` = `<path> <sha256>` per line with
  `# replaced` markers; guard test forbids deleting lines and tolerates
  replacement markings.
- `finalBuildHasNoPlaceholders` and `contractClipEventTimesMatchManifest`
  correctly `@Disabled` until OMP/Task 15 lands; everything else in the
  contract test is enabled and passing.

### 8. Requirement coverage — see §16 table below.

### 9. Regressions — ✅

- `git diff main...HEAD` over non-pilot files audited file-by-file:
  `ShockwaveUtil.detonate(..., suppressPresentation)` keeps old call sites
  identical; `MilkBottleItem` keeps madness behavior (silent drink +
  MILK_DRINK event); `HomelanderMadnessAftermathController` preserves
  12 f/7 f MOB+fire semantics behind silent-explode overload +
  SUN_CHARGE/SUN_DETONATION events; `HomelanderMadnessFlightController`
  MADNESS_CRASH 0.35; `EyeLasersAbility` shared raycast + channel events;
  `IronFistsController` ON/OFF/HIT with suppressed detonate;
  `HandClapAbility`/`StunningRoarAbility`/`HomelanderHero` event-driven,
  gameplay preserved.
- `PlayerRendererMixin` tilt at TAIL, legacy leg pose kept for
  non-presentation heroes; `hero_presentation.txt` golden unchanged;
  `CrossBeamRenderer` overload only extends; no other hero touched.
- Ledger discrepancy noted once more: the request names
  `.superpowers/sdd/.../progress.md`; the actual ledger is `SESSION.md`
  (progress.md doesn't exist in the branch). Not a defect.

## Findings fixed

| Commit | What | Reproducing test |
|---|---|---|
| `b717b06` | `PlayerAnimator.stop` is per-clip (`clipIds...`); pose tracker stops only `tracked.baseClip`, SunChargeFx stops only `sun_charge`, EyeLaserChannel stops only its clips and now explicitly fades `laser_hold` at release — sibling clips (flight loops, sun_charge, clap/roar/milk/laser_release) survive | `PlayerAnimatorTest.stopRemovesOnlyTheNamedClip`, `stopWithFadeLeavesSiblingClipsUnaffected` |
| `c0c2b1a` | Legacy `FlightTrailManager` (vanilla END_ROD/CLOUD) skipped players owned by a `FlightPresentation` — Homelander had double trails (old particles + new ribbons). Marker carried via `ClientFlightState` to keep the `client.fx ⇄ client.core` direction acyclic | `ClientFlightStateTest` mark/unmark/clearAll |
| `308515e` | Flash backends pinned at peak: `ScreenFlash`'s 60-tick detonation fade rendered ~3 s full-brightness. New shared `FlashEnvelope` (accept feed when `intensity ≥ shown`) makes both backends track the caller's fade | `FlashEnvelopeTest` (decay, re-feed tracking, pinning, weaker-feed rejection) |

## Findings parked

- `BeamPattern` noise via `System.currentTimeMillis()` — deterministic per
  endpoint and cheap; threading a channel seed through
  `VfxChannelFactory.open` is an API change for cosmetic jitter
  determinism. Cost if wrong: beams jitter visibly-but-acceptably.
- `VfxSpawn.seed` propagated but unread by any effect — a varint per event
  kept for future emitter randomness.
- `roar` shakeIntensity 0.8 (was server-side 2.0): tuned param in
  `roar.json`, contract doesn't pin it, footage accepted.
- Parser `scale` channel ignored; clap `contactSeconds=0.083` fallback —
  bound to clip `eventTimes` (contract row exists, disabled test tracks it).
- `EyeLaserChannel` `impact` sparks fire during CHARGE ramp — cosmetic,
  beam is already drawn; param-gated.
- Feet-pivot tilt, `milk` event on any player holding the bottle —
  deliberate/consistent per ledger.

## Requirement coverage (spec §16)

- ✅ Visual Core implemented on Veil (Veil backend + post pipelines + seam).
- ✅ Homelander uses the new system for the pilot scope (all 11 ids).
- blocked-on-OMP — required passive assets final (placeholder manifest +
  disabled contract tests track the gap cleanly).
- blocked-on-OMP — no production path on placeholders (guard-enforced).
- ✅ major vanilla-particle presentation replaced — *fully true only after
  F3* (the legacy flight trail was still rendering alongside ribbons).
- ✅ flight presentation fixed (tilt, phases, trails, sounds; F1 prevents
  mid-flight anim loss).
- ✅ lasers reworked, function while flying (tilt-aware eye anchors;
  F1 fixes hold→release).
- ✅ milk final explosion has the new presentation.
- ✅ animation + sound integration complete for the pilot.
- ✅ multiplayer actively tested and passes (Task-14 two-client evidence).
- blocked (environment) — ~100 FPS on Sodium: llvmpipe VM has no Sodium;
  probe math + bounded llvmpipe perf are the available evidence.
- ✅ stress testing reveals no catastrophic behavior (caps, culls,
  cadences verified in code + Task-14 evidence).
- blocked-on-OMP — third-session OMP review.
- blocked-on-OMP — user visual approval of OMP package.
- ✅ fourth-session Devin review — this report.
- blocked-on-OMP/final — final integration and final in-game verification
  (downstream of this verdict + OMP assets).

## Verdict

**Pilot ready for final integration** — after the three review fixes
(`b717b06`, `c0c2b1a`, `308515e`). Before them, flight/sun/laser
compositions had real runtime defects (lane-killed clips, double trail,
pinned flash) that contradict the pilot's job as the migration template.
The remaining DoD gaps are the expected OMP-blocked items, all cleanly
isolated and tracked by disabled tests + the placeholder manifest.
