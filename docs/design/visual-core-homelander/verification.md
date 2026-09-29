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
