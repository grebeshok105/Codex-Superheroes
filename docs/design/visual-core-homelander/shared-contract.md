# Homelander Shared Asset Contract

The single integration contract between the Visual Core pilot (`docs/superpowers/plans/2026-09-28-visual-core-homelander-pilot.md`) and the OMP passive-asset stream (`docs/superpowers/plans/2026-09-28-homelander-omp-assets.md`). Machine-readable form: `src/test/resources/contracts/homelander_pilot.json`, enforced by `HomelanderAssetContractTest`.

Per spec §6 the contract pins only integration-critical points: states/moments, expected passive resources, approximate durations, runtime events, ownership, and the placeholder → final replacement path. Artistic direction is OMP's.

## Addressing rules

- Sound event id `homelander.<name>` ↔ file `assets/superheroes/sounds/homelander/<file>` ↔ `sounds.json` sound name `superheroes:homelander/<file stem>` (the `<name>` suffix with `.` → `_` equals the file stem). Runtime sounds are OGG Vorbis only.
- Animation clip `player_animations/homelander/<name>.animation.json` carries key `animation.superheroes.homelander.<name>` ↔ runtime id `superheroes:homelander/<name>`. Only the six player bones may be animated: `head`, `body`, `right_arm`, `left_arm`, `right_leg`, `left_leg`. `loop` is `true` (infinite) or `"hold_on_last_frame"` (one-shot that holds its final pose); absent = one-shot.
- Models and textures are addressed by path under `assets/superheroes/`.
- Tolerances: one-shot sound `durationMs` ±30 %; loop sounds only require ≥ 1000 ms and `loop: true`; one-shot clip `animation_length` ±30 % of `lengthMs`; clip `events.contact` ≤ 120 ms; declared event times match the clip's timeline keys within 0.05 s once `contractClipEventTimesMatchManifest` is enabled (Task 15).

## Moments

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
| Iron Fists on | `homelander.iron_fists.activate` (`iron_fists_activate.ogg`, 1000 ms); existing `homelander.iron_fists.charge` (`iron_fists_charge.ogg`, loop, ≥ 1000 ms) | `iron_fists_activate` 1000 ms | activation |
| Iron Fists hit | existing `homelander.iron_fists.impact` (`iron_fists_impact.ogg`, 1440 ms) | `iron_fists_strike` 400 ms, event `contact` ≤ 120 ms | dash hit |
| Clap | existing `homelander.hand_clap` (`hand_clap.ogg`, 4570 ms — OMP may retime; the long tail doesn't fit a 600 ms clip) | `clap` 600 ms, event `contact` ≤ 120 ms | activation tick |
| Roar | existing `homelander.roar` (`roar.ogg`, 3450 ms), `homelander.roar.deep` (`roar_deep.ogg`, 6480 ms) | `roar` 1500 ms | activation tick |

`durationMs` for the five existing rows records the *current* measured files so `oneShotSoundDurationsWithinTolerance` passes on them; when OMP retimes a file outside ±30 % of the recorded value, it updates `durationMs` in `homelander_pilot.json` via a `contract-changes.md` entry in the same commit (never diverge silently). For loop rows, `durationMs` is the required minimum length (1000 ms).

Other passive resources: model `models/item/milk_bottle.json` (3D, Blockbench Java item model, texture `superheroes:item/milk_bottle`); textures `textures/vfx/homelander/{laser_core,laser_glow,sun_flash,sun_ring,ember,shock_ring}.png`.

`contact` events ≤ 120 ms because gameplay fires on the activation tick and must not change.

## Ownership boundary (spec §5)

Devin Cloud owns: the Visual Core itself, Veil runtime effect implementation, Homelander runtime VFX, camera behavior, screen effects, runtime timing and effect sequencing, multiplayer behavior, performance behavior, placeholders, integration of final passive assets, and the final coherent in-game result.

OMP owns: passive artistic resources only — models, textures, animations, sounds, and other passive visual/audio resources required by the pilot. OMP does not own Veil runtime code, shader/effect-system runtime logic, core architecture, multiplayer logic, gameplay logic, or runtime timing logic for the effect system.

## Placeholder → final replacement rule

Final files overwrite placeholder files at the same path; no id changes. Any file standing in for a not-yet-final resource is recorded in `src/test/resources/contracts/homelander_placeholders.txt` (`<repo-relative path> <sha256>`); replacement marks the line `# replaced`, never deletes it. Done = zero Homelander placeholders ship (spec §16).

## Runtime motion envelope (synchronization constraint, spec §6 — not artistic direction)

The runtime tilts the whole body (CRUISE ≤ 55°, BOOST ≤ 80°) and crossfades clips over 4 ticks, so clips carry limb/torso motion only and `body` root rotation stays ≤ ±15°; `head` motion stays ≤ 10° during `laser_hold` (the head keeps following the player's look on top of the clip); `flight_loop` volume scales with `horizontalSpeed` so it must read from quiet to full.
