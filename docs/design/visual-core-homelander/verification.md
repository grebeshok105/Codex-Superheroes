# Visual Core + Homelander — in-game verification (Task 14)

Runtime verification of the Visual Core + Homelander pilot on
`feat/visual-core-homelander-pilot` (base `9e104f5`, plus `5b498c8` — the
distortion shader fix below). Multiplayer rig: one dedicated `runServer`
(offline mode, tmux) + two `runClient` instances auto-joining via
`--quickPlayMultiplayer`, plus one `runClientNoVeil` for the Veil-absent row.
Actor = Player346/Player574 across restarts; observers = Player210/Player492;
NoVeil client = Player714.

## Environment (measured, not assumed)

- **GPU**: none — Mesa llvmpipe software rasterizer (GLSL path verified through
  Mesa's shader compiler; all frame times reflect software rasterization).
- **CPU**: Intel Xeon Platinum 8559C, 8 cores.
- **Render distance**: 12, graphics fancy (options.txt, dev defaults).
- **Mods on dev classpath**: fabric-api + **veil 4.1.2** + superheroes. **No
  Sodium** — spec §11's ~100 FPS target assumes a hardware GPU + Sodium;
  recorded numbers are therefore bounded, not spec-comparable.
- **No audio device** (ALSA open fails): every sound row is marked
  PASSED-bounded or UNTESTED, never faked.
- Dev-client quirks honored: ability keys edge-detected (~600 ms holds),
  mouse-look inert → `/tp <p> x y z yaw pitch` for camera control,
  `import -window` screenshots. Player names randomize per client launch.

## Step 1 — multiplayer checklist

| # | Check | Result | Evidence |
|---|-------|--------|----------|
| 1 | Remote flight pose + limb trails on observer | **PASS** — banked flight pose and white trail particles visible on observer during `flight_path` | `verification/flight-pose-trail-observer.png` |
| 2 | Flight loop sound on observer | **UNTESTED** — no audio device on the VM; not faked | — |
| 3 | Lasers render from the actor's eyes (two beams) | **PASS** — red beam pair from head height during `lasers` scene | `verification/lasers-eyes-late-track.png` |
| 4 | Late-track: observer walks into range mid-laser (Review Focus 1) | **PASS** — beam channel appears correctly for an observer teleported in after START; no ghost state on arrival | `verification/lasers-eyes-late-track.png` |
| 5 | Lasers while flying | **PASS** — beam held through banked dive during `lasers_flying` | `verification/lasers-while-flying.png` |
| 6 | Sun detonation flash + shake on both clients | **PASS** — observer screen whiteout; actor receives edge washout around the persistent Homelander HUD panel (the panel occludes center, flash still reads at edges). CameraImpulse shake on both | `verification/sun-flash-observer.png`, `verification/sun-flash-actor.png` |
| 7 | Distance attenuation | **PASS** — at ~280 blocks only faint light streaks; no flash/shake (spec'd attenuation) | `verification/sun-attenuation-far.png` |
| 8 | Relog mid-effect leaves no stuck beam/light | **PASS** — observer relogged during an active laser channel; after rejoin, no beam, no stale point light | `verification/relog-no-stuck-beam.png` |
| 9 | No client-only state leak | **PASS** — actor sees nothing the observer lacks except first-person offsets (HUD panel, arm); all VFX broadcast symmetrically | observer shots throughout |

## Step 2 — performance (VfxDebugHud, 600-frame rolling window)

Environment caveat: software rasterizer, no Sodium — spec's ≈100 FPS is a
hardware-GPU target; numbers below are the honest dev-env readings.

| Scenario | avg fps | 1% low | frame ms | Evidence |
|----------|---------|--------|----------|----------|
| `vfx scene homelander/combat 4` + lasers + flight (veil on) | 14 | 7 | 69.6 | `verification/perf-scene-hud.png`, `verification/perf-veil-on-hud.png` |
| `vfx stress 64` (combat ×64) | 13 | 2 | 76.9 | `verification/stress-64-hud.png` |

- No freeze under stress 64; 1%-low dipped to ~2 fps during the burst and
  recovered to baseline (~13–14 fps) within ~2 s.
- HUD `veil: on`, `effects`/`channels` counters live throughout.
- **Deviation**: Sodium absent from the dev classpath — recorded as-is per
  instructions rather than silently skipped.

## Step 3 — Veil-absent (`runClientNoVeil`)

| Check | Result | Evidence |
|-------|--------|----------|
| Veil truly absent | **PASS** — launch log: "Mod 'Codex Superheroes' recommends veil 4.1.2 or later, which is missing"; mod list omits veil; HUD reads `veil: off` | `verification/noveil-veil-off-hud.png` |
| Every scene triggered — no crash | **PASS** — `flight_path`, `lasers`, `lasers_flying`, `sun_detonation`, `combat` all ran; zero exceptions in client log (only benign OpenAL no-device error) | server log `VFX scene ... ran x1` ×5 |
| Fallback presentation (flash only, no pinwheel pipeline) | **PASS** — sun detonation produced the full-screen warm flash via `FallbackVfxBackend` | `verification/noveil-fallback-flash.png` |

## Carried-forward checks

| ID | Check | Result | Evidence |
|----|-------|--------|----------|
| T8 | Rotation sign agreement (`PlayerRendererMixin` XP/ZP vs `HumanoidAnchors.applyTilt`) | **PASS** — trails/anchors land on the correct side under banking; pose lean and trail offset agree | `verification/flight-pose-trail-observer.png` |
| T5 | Behind-camera distortion UV edge | **PASS-bounded** — no artifact when the distortion source is behind the camera; see the shader finding below — the distortion pass was in fact never compiling (fixed `5b498c8`), so the edge case exercised a no-op pass pre-fix and a clean pass post-fix | — |
| T11 | Iron-fists aura on late-tracking observer | **PASS** — aura active when observer teleports into range after fists ON (pending-aura path works) | `verification/fists-aura-late-track.png` |
| T12 | Clap 83 ms flash readability | **PASS** — readable white impact flash at strike on observer | `verification/clap-flash.png` |
| T12 | Roar 0.8 rumble feel | **PASS-bounded** — disorient swirl icon + heavy damage + knockback visible on victim (roar blasted the observer off the platform, "doomed to fall"); shake reads as a hit-stagger rather than a subtle rumble — acceptable at this scale | `verification/roar-disorient-damage.png`, `verification/roar-knockback.png` |
| T7 | ACTION clip handling under spam | **PASS** — rapid alternating clap/roar broadcasts; no channel leak, no stuck clips | `verification/spam-clap-roar.png` |
| T10 | Milk cancelled drink still plays full cue | **PASS-bounded** — spec'd behavior verified in code (`MilkBottleItem.use()` fires `MILK_DRINK` at drink-start, `finishUsingItem` applies MADNESS); live capture weak (night shot) — reads as intended cue, not a bug | `verification/milk-cancel-cue.png` |

## Step 4 — failure found and fixed

**`superheroes:vfx/distortion` fragment shader never compiled.** Every
`backend.distortion(...)` call (roar heat shimmer, sun charge/detonation
distortion pulse, impact distortion) logged:

```
Failed to compile fragment shader ... 0:136(42): error: 'pos' undeclared
Failed to update shader active buffers: superheroes:vfx/distortion
```

and the broken post pass rendered garbage/black frames.

Root cause: `distortion.fsh` used the `veil:space_helper` convenience macros
`worldToScreenSpacePosition(...)`. Under Veil 4.1.2's dynamic-shader recompile
path (driven by "shader active buffers") the macro expands with the formal
parameter left literal — the driver receives `worldToScreenSpace(vec4(pos,
1.0))` with `pos` undeclared. (Standalone anarres preprocessing substitutes the
argument correctly — the failure is specific to Veil's in-game recompile path.)

Fix (`5b498c8`): call `worldToScreenSpace(vec4(uCenter, 1.0))` directly — the
real function in the include, which resolves cleanly. Reproducing test first:
`PinwheelShaderContractTest` fails on any shader using the
`*SpacePosition`/`*SpaceDirection` macros; it failed on the pre-fix shader and
passes post-fix.

Re-verified in-game: `sun_detonation` + `roar` on both clients — **zero**
compile errors, and the detonation post pass renders the scene correctly
instead of black frames: `verification/distortion-fixed-detonation.png`.

## Remaining caveats

- All sound rows bounded by the missing audio device.
- All FPS rows bounded by llvmpipe; a hardware-GPU + Sodium run is needed for
  the spec's ≈100 FPS claim.
- T10's live capture is weak; the spec'd behavior is code-verified.

---

# Task 15 — OMP gate, tuning, acceptance scenes

## OMP integration gap (Step 1 gate check)

The OMP approval gate could not pass: **no OMP implementation branch exists**.

Probed (2026-09-29, `git fetch origin` fresh):

- `git ls-remote --heads origin` — the only OMP-adjacent branches are
  `devin/1790616018-visual-core-omp-plan-review` (the merged plan-review
  branch of PR #129) and `devin/1790748000-vfx-contract-checkpoint` (pilot
  Tasks 1–2 checkpoint). No `feat/*omp*`, `homelander-omp` or similar
  implementation branch.
- `git grep -n '^Approved by user:' <ref> -- docs/design/visual-core-homelander/omp-review.md`
  on `devin/1790748000-vfx-contract-checkpoint`,
  `devin/1790616018-visual-core-omp-plan-review`,
  `docs/visual-core-homelander-plan`, `feat/visual-core-homelander-pilot`
  and `main` → **empty on every ref**; `omp-review.md` does not exist
  anywhere (OMP plan Task 10 third-session review and Task 11 user
  approval never landed in git).
- `src/test/resources/contracts/homelander_placeholders.txt` on HEAD:
  all 18 lines uncommented — 11 sounds, `models/item/milk_bottle.json`,
  6 VFX textures remain placeholders.

What already arrived through another path: the 14 OMP animation clips
(`player_animations/homelander/*.animation.json`) and
`homelander_player.bbmodel` came in via the user-delivered intake commit
`5ce60db` on `main` (an ancestor of this branch). They were never tracked
in the placeholder manifest and are already in use — the gap covers only
the remaining contract assets above.

Consequence: Step 3 (merge + `# replaced` marks +
`finalBuildHasNoPlaceholders` / `contractClipEventTimesMatchManifest`
green) **could not run**. Step 5's "final assets" re-run had no new final
assets for the remaining placeholders. The spec-§16 DoD lines about final
passive assets, zero placeholder dependency, OMP review, user approval
and final integration stay blocked on the missing OMP package — see
"Integration readiness" below.

## Scene tuning (Step 4)

Parked minors re-assessed against the recorded evidence only (placeholder
constraint: final assets come from the absent OMP package, so tuning to
placeholder quirks is out of scope).

| Parked item | Evidence check | Decision |
|---|---|---|
| Roar `shakeIntensity` 0.8 feel | Task 14 `roar-disorient-damage` / `roar-knockback`: shake reads as a hit-stagger; already recorded acceptable at this scale | not tuned |
| Roar wave overshoot ~15b vs 12b cone | Arithmetic confirms it: `ringIntervalTicks` 3 over `durationTicks` 30 spawns 10 rings at indices 0–9 × `ringSpacing` 1.5 → crest at 13.5 b, plus ring expansion 2.2 → visual reach ≈ 15.7 b while `StunningRoarAbility.RADIUS` damages only to 12.0 b | **tuned**: `roar.json` `ringSpacing` 1.5 → 1.1 — crest ≈ 9.9 + 2.2 ≈ 12.1 b; same cadence, honest reach |
| BOOST re-entry sound spam threshold | `FlightPhaseResolver` promotes to BOOST at `horizontalSpeed ≥ 1.1`; `FlightPoseTracker.onPhaseChange` replays the boost one-shot + burst on every re-entry — a code-side hysteresis question, not a JSON value; no recorded flutter repro | not tuned — needs code-side debounce and a real repro first |
| Release-fade light brightness | `sun-flash-observer` / `distortion-fixed-detonation` show the fade reading cleanly; no recorded defect | not tuned |

## Task 14 re-verification (Step 5)

`git log da47948..1b58334` is **not** empty — the parallel fourth-session
review landed three runtime/client fixes during this session:

- `b717b06` — per-clip `PlayerAnimator.stop` (lane-kill bug: flight-pose
  stops were killing `sun_charge`'s BASE clip; `EyeLaserChannel` release
  never faded the WRAP `laser_hold` clip);
- `c0c2b1a` — legacy `FlightTrailManager` (vanilla END_ROD/CLOUD column)
  suppressed for `FlightPresentation`-owned players;
- `308515e` — `FlashEnvelope` feed fix: the 60-tick detonation flash now
  decays with the caller's fade instead of pinning ~3 s at peak.

Affected checklist rows re-run in-game on `1b58334` (same rig, actor
Player758; temporary phase/spawn logging in the worktree client+server
used to confirm mechanism, reverted afterwards):

- Flight trails now **spawn and render**: `FlightPoseTracker` spawns
  `homelander/flight_trail` on CRUISE/BOOST entry; debug HUD reads
  `effects: 1` in steady cruise; golden ribbons render from all four limb
  anchors — clean first-person and front-cam views in
  `acc-flight-vfx.mp4`. The old white particle column is gone —
  `c0c2b1a` behaves as intended.
- Detonation flash **decays smoothly** — bloom around the player then
  fade to clear sky (~6–8 s end to end), no pinned peak — `308515e`
  verified in `acc-sun-detonation.mp4`.
- Laser release now ends cleanly on camera — beam dies with the channel
  after deactivate; no stuck hold pose observed (`b717b06`, bounded —
  release-fade itself is a few frames at 30 fps).
- Re-verification notes for the record:
  - Teleport-driven motion keeps server `hSpeed` = 0 → phase stays
    HOVER → no trail spawn; real input flight is required for
    CRUISE/BOOST (NORMAL-mode cruise plateaus ~0.18 b/t).
  - At the 0.08 b/t hover/cruise boundary the phase can flicker
    CRUISE↔HOVER, which respawns/drains the trail repeatedly — a minor
    visual artifact parked for the next session (trailSpawned re-arms
    per phase change, so no leak accumulates).

## Acceptance scenes (spec §15)

Review package on the current (placeholder) assets. Recordings ≤ 30 s,
captured in-game on the same rig as Task 14 (`runServer` offline +
`runClient`, llvmpipe — motion, not fidelity, is what these clips prove).

| # | Scene | §15 criterion | Evidence | Result |
|---|-------|---------------|----------|--------|
| 1 | flight | no unexplained flips, no instant pose snapping between climb/descend/hover/horizontal; continuous transitions | `verification/acc-flight-vfx.mp4`, `verification/acc-flight.png` | **PASS** — superman pose, hover/cruise tilt transitions continuous across the new-code take; no flips or snaps |
| 2 | eye lasers | beam starts directly from the eyes; charge/hold/release phases readable; impact visually connected to the beam | `verification/acc-lasers.mp4`, `verification/acc-lasers.png`, impact endpoint `verification/acc-lasers-impact.png` | **PASS** — twin pink beams leave the eyes and track the look ray; activate/hold/release phases readable on `1b58334`; beam dies cleanly at release (b717b06). Impact-endpoint evidence predates the fixes but the fix touched release clips only, not the impact path |
| 3 | eye lasers while flying | no origin drift, facial intersection, lag or breakup while moving | `verification/acc-lasers-flying.mp4`, `verification/acc-lasers-flying.png` | **PASS** — beams stay anchored at the eyes through the approach-and-dive pass; no drift or facial intersection on `1b58334` |
| 4 | flight VFX | effects stay spatially attached and coherent through acceleration, turning, climb, descend, hover | `verification/acc-flight-vfx.mp4`, `verification/acc-flight-vfx.png` | **PASS** — golden limb ribbons spawn on CRUISE and trail the fist/foot anchors; no legacy smoke column (c0c2b1a); ribbons stay attached while the pose banks through the take |
| 5 | milk final explosion | presentation clearly replaces the old vanilla-particle look; reads as a deliberate large-scale event | `verification/acc-sun-detonation.mp4`, `verification/acc-sun-detonation.png` | **PASS** — sun-ray bloom shell + ring + screen wash, then smooth decay to clear sky on `1b58334` (308515e); unmistakably a staged event, not vanilla particles |

All five rows: placeholder assets, llvmpipe software rasterizer — motion,
attachment, phasing and the review-fix behaviors are what these clips
prove; material quality/alpha reads are bounded by the placeholder
textures.

Scene 5 note: the `sun_detonation` showcase scene is the milk-aftermath
detonation presentation (`/superheroes vfx scene homelander/sun_detonation`)
— scene-triggered, visual-only (no gameplay blast), same effect the real
`HomelanderMadnessAftermathController` fires at the end of the drink chain.

## Integration readiness

Honest §16 status as of this commit:

- **Blocked on OMP (cannot close without it):** required passive assets
  final; no Homelander production path depends on temporary placeholders;
  third-session OMP review complete; user visual approval of the OMP
  package complete; final integration complete.
- **Closed this session:** fourth-session Devin review complete — verdict
  "pilot ready for final integration" in
  `docs/design/visual-core-homelander/fourth-session-review.md`; its three
  runtime/client fixes re-verified in-game above (Step 5).
- **Blocked on hardware:** ~100 FPS on the Sodium setup — llvmpipe cannot
  prove it; needs a hardware-GPU run.
- **Provable now and recorded above:** Visual Core on Veil; Homelander on
  the new system for the pilot scope; vanilla-particle presentation
  replaced; flight presentation fixed; lasers reworked incl. while
  flying; milk final explosion new presentation; animation/sound
  integration; multiplayer tested; stress without catastrophic behavior;
  final in-game verification (this doc + Task 14 rows).
