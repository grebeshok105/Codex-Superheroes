# Roster Visual Audit

Companion to the Visual Core + Homelander pilot plan (`docs/superpowers/plans/2026-09-28-visual-core-homelander-pilot.md`). One row per module in `bootstrap/HeroModules.ALL` — what each hero needs from a visual system, and what mechanism serves that need today.

"Needs" uses the plan's capability vocabulary: `beam` / `trail` / `impact` / `aura` / `field` / `charge-release` / `camera` / `screen` / `world-interaction` / `flight` / `animation`. "Mechanism" names what the code does today: vanilla `sendParticles`/`playSound`, `BeamFx`/`BeamStyle` payloads, Veil, HUD layers/screens, entity renderers, `ScreenShakeS2CPayload`.

| Hero | Visual needs | Current mechanism |
|---|---|---|
| homelander | beam (eye lasers), flight, trail (flight), impact (landing, clap, strikes), aura (Iron Fists hands, sun build-up), charge-release (laser charge, sun build-up → detonation), camera (shake), screen (first-person laser overlay), world-interaction (explosions, lightning, fire ring), animation (14 OMP clips) | vanilla particles + `playSound` + `BeamStyle` `STYLE_LASER` + `LocalLaserOverlay` HUD overlay + `ShockwaveUtil`/`ScreenShakeS2CPayload` |
| ironman | beam (repulsor, unibeam), flight (flight ability, supersonic), trail (flight), impact, charge-release (unibeam), screen/HUD (Jarvis overlay, reactor, ESP scan), world-interaction (missiles) | vanilla particles + `BeamStyle` `STYLE_REPULSOR` + HUD (`JarvisOverlayHud`, `ReactorOverlayHud`, `IronManEspRenderer`) + entity renderers/layers (drones, nanoform suit-up) |
| regulus | aura/field (madness domain, greed's embrace), impact (counter strike), screen (HUD glitch source), flight (madness controller) | vanilla particles + HUD + `hudGlitchSource` |
| sungjinwoo | field (Monarch's Domain), aura (shadow soldiers, extraction), world-interaction (arise, exchange) | vanilla particles + entity renderer (`ShadowSoldierRenderer`) |
| doomsday | impact (smash, tackle, bone spikes), charge-release (charge tackle), aura (berserk), flight-adjacent (tier controller) | vanilla particles |
| goku | beam (kamehameha), aura (Super Saiyan, ki charge), charge-release (ki charge → kamehameha/spirit bomb), screen (solar flare flash), world-interaction (spirit bomb) | vanilla particles |
| naruto | impact (rasengan family), aura (sage mode), world-interaction (rasenshuriken, bijuudama), clones | vanilla particles + entity renderer (`KageBunshinRenderer`) |
| captainamerica | impact (shield slam, dash), world-interaction (shield throw projectile) | vanilla particles + entity renderer (`ShieldProjectileRenderer`) |
| kratos | aura (spartan rage), impact (blade storm, god slayer), world-interaction (leviathan throw, chain whirl) | vanilla particles + HUD |
| loki | aura (glamour, mind charm), world-interaction (tesseract blink, clones) | vanilla particles |
| thanos | beam (cosmic beam), impact (cosmic slam), field (reality tear, space portal), world-interaction (snap, time rewind) | vanilla particles + `BeamStyle` `STYLE_COSMIC_BEAM` |
| reinhard | aura (divine aura), impact (counter riposte, slashes), screen (wish screen), sword waves | vanilla particles + HUD + `ReinhardWishScreen` + `soundFilter` + scabbard layer |
| raiden | impact (plunging strike, musou arts), camera (heavens strike shake), world-interaction (lightning strikes) | vanilla particles + `ScreenShakeS2CPayload` + action key |
| invincible | impact (Guardians breaker), camera (shake) | vanilla particles + `ScreenShakeS2CPayload` |
| omniman | flight (viltrumite rush), impact (world breaker) | vanilla particles (via shared flight mechanic) |
| kazuha | aura/trail (wind swirls, maple storm), impact (midare ranzan), world-interaction (whirlwind) | vanilla particles |
| scaramouche | field (wind prison), flight-adjacent (windstep, skyfall burst), impact | vanilla particles |
| battlebeast | impact (axe cleave, predator leap), aura (bloodlust), world-interaction (war roar) | vanilla particles |
| rem | field (healing magic, ice), aura (oni rage), impact (mace crater, oni kick), camera (shake), world-interaction (ice spikes) | vanilla particles + entity renderers (`RamRenderer`, oni horn layer) + HUD |
| atrain | trail (hyperspeed, mach dash speed lines), impact (sonic boom), charge-release (adrenaline rush) | vanilla particles |
| scorpion | field/aura (hellfire), beam-ish cone (hell breath), world-interaction (spear, fire teleport) | Veil (`ClientScorpionFx`, `client/hero/scorpion/fx/veil/`) + vanilla particles + `ScorpionFx` payloads |
| pandora | field (mirror dimension domain), screen (Iris shader warps, font cipher), world-interaction (spatial bind, space crush), HUD | Iris shader bridge + HUD + vanilla particles |

## Capability list the Visual Core must provide

From the plan's File Structure (`client/core/vfx/pattern/**`, `anchor/**`, `flight/**`, `anim/**`) — each capability and the heroes the audit shows consuming it:

| Core capability | Pattern/runtime class | Consumers (audit) |
|---|---|---|
| Beam (one-shot + continuous) | `BeamPattern`, `BeamLook`, `VfxChannelEffect` + channel table | homelander (laser channel), ironman (repulsor/unibeam), thanos (cosmic beam), goku (kamehameha) |
| Trail | `TrailBuffer`, `TrailPattern` | homelander (flight), atrain (speed lines), ironman (flight), omniman (rush), kazuha/scaramouche (wind) |
| Impact | `ImpactPattern` | homelander, ironman, doomsday, captainamerica, kratos, invincible, omniman, battlebeast, rem, raiden, naruto |
| Shockwave ring | `ShockwavePattern` | homelander (clap, landing, iron fists hit), doomsday, captainamerica, reinhard, rem |
| Aura / field | `AuraPattern` (entity-following emitter + light; spec §4 fields) | homelander, goku, sungjinwoo, kratos, regulus, rem, scorpion, reinhard, battlebeast, loki, scaramouche, pandora |
| Charge → hold → release timing | `PhaseTimeline` | homelander (laser, sun), goku (ki charge), ironman (unibeam), doomsday (charge tackle), atrain |
| Screen flash / overlay | `ScreenFlash` (+ HUD) | homelander (sun flash, first-person), goku (solar flare), ironman, regulus, reinhard, pandora (Iris domain) |
| Camera impulse | `CameraImpulse` (over `ScreenShakeManager`) | homelander, raiden, invincible, rem, ironman |
| World-interaction (block dust, explosion stand-ins, lightning) | composed from `ImpactPattern`/`ShockwavePattern`/`AuraPattern` + Quasar emitters | homelander, ironman, thanos, rem, raiden, naruto, kratos, captainamerica, loki, scorpion, pandora, kazuha, battlebeast, goku |
| Eye/hand/head render anchors | `EyePair`, `HumanoidAnchors` | homelander (eyes, hands), ironman (palms/arc reactor), every humanoid hero eventually |
| Continuous flight pose + per-hero presentation | `client/core/flight/**` (`FlightPoseTracker`, `FlightPresentation`) | homelander, ironman, omniman, scaramouche (windstep), regulus (madness), doomsday (tiers) |
| Player animation runtime | `client/core/anim/**` (`PlayerAnimator`, `AnimationClip`) | homelander (pilot); roster-wide reuse is post-pilot (spec §17), not in scope |
| Runtime payload transport | `VfxEventS2CPayload`, `VfxChannelS2CPayload`, `VfxFx` (server) + `CoreClientReceivers` | every hero — replaces direct `sendParticles`/`playSound` presentation calls |

Today every hero except scorpion renders exclusively through vanilla particles and `playSound`, with `BeamStyle` payloads for the three beam users. That is the gap the pilot closes: one event/channel transport, one pattern layer, and per-hero compositions registered through `HeroClientContext` seams instead of scattered `sendParticles` call sites.
