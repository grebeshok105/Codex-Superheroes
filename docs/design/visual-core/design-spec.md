# Codex Visual Core + Homelander Pilot — Design Spec (v3)

> Source of truth for `2026-09-27-visual-core-homelander-plan.md`. Copied verbatim from the approved spec v3.

## 1. Purpose
Build a new Veil-based visual core for Codex and prove its first complete version on Homelander.
This stage is **not** a full character-rework program and is **not** a redesign of the whole roster. Homelander is the first real consumer of the new visual system because he gives the core enough varied cases to expose bad assumptions early.
The goal is to leave this stage with:
- a usable Visual Core on Veil;
- Homelander fully running on it;
- a clean multiplayer-capable visual pipeline;
- a repeatable workflow that makes later character migrations much faster;
- no dependency on the old vanilla-effect architecture as a design reference.
Homelander is a pilot and a stress case. He is not a template that every future character must imitate.

## 2. Success Definition
This stage is complete when:
1. the Visual Core is implemented and usable;
2. Homelander is fully integrated with it;
3. Homelander's major visual scenes are rebuilt and coherent in-game;
4. multiplayer works correctly and has been tested by the agent;
5. normal heavy gameplay scenes stay at or above roughly **100 FPS** on the current Sodium-enabled test setup;
6. stress tests with several heavy effects do not expose catastrophic degradation, even though 100 FPS is not a hard gate for those artificial stress cases;
7. temporary Homelander placeholders have been replaced by final assets and removed;
8. the final integrated result has passed independent review and in-game verification;
9. the result is good enough to use as the starting point for later character migrations without requiring a new giant design exercise for every hero.
Regulus is a later external test of the core, not part of the completion gate for this stage.

## 3. Design Principles
### 3.1 Veil first
New high-value visual work should be built around Veil. Vanilla rendering and vanilla particles may remain only where replacing them gives no meaningful visual or architectural benefit. The important visual identity of abilities should not depend on old vanilla particle logic.
### 3.2 Do not copy the old visual architecture
The existing roster is useful as a catalogue of **what visual situations exist**, not as a guide for how those situations should be implemented. Before finalizing the core design, audit the current abilities across the roster and classify the visual needs they represent: beams, impacts, trails, auras, fields, destruction, screen effects, charge phases, world reactions, and similar categories. The audit should inform requirements. It should not preserve old implementation patterns simply because they already exist.
### 3.3 Two-layer core
1. **foundation capabilities** — the reusable building blocks needed to create modern Veil-driven effects;
2. **reusable visual patterns** — higher-level capabilities that can be shared across abilities without forcing all characters into the same visual style.
### 3.4 Stylistically neutral
The core provides capability, consistency of quality, and reusable infrastructure rather than one mandatory art style.
### 3.5 Keep hero-specific behavior outside the shared core
The shared Visual Core must remain hero-agnostic and must not branch on or otherwise depend on a specific hero identity. Hero modules may depend on and extend the Visual Core; the Visual Core must not know which hero is using it. This must remain compatible with the repository's architectural rules and tests.
### 3.6 Core stays evolvable
Homelander establishes the first complete version, not the final immutable version.

## 4. Visual Core Scope
The core owns: Veil-driven runtime VFX; visual effect composition; beams, trails, impacts, auras, fields, charge/release phases; camera presentation; screen-space effects; world interaction visuals (impact marks, debris, smoke, light response); effect timing at runtime; multiplayer-visible effect state and synchronization; in-game debug/showcase paths; integration points for passive assets supplied by OMP; placeholder support; configurable effect parameters. No external editor. Testing and showcase tooling lives inside Minecraft.

## 5. Ownership
**Devin Cloud:** Visual Core, Veil runtime, Homelander runtime VFX, camera, screen effects, runtime timing/sequencing, multiplayer, performance, placeholders, integration of final passive assets, the final coherent in-game result.
**OMP:** passive artistic resources only — models, textures, animations, sounds, other passive resources. OMP does not own Veil runtime code, shader/effect runtime logic, core architecture, multiplayer, gameplay, or runtime timing.

## 6. Shared Contract
Define only: which states/moments exist; which passive resources are expected; approximate durations where timing matters; which runtime events need assets; ownership boundary; how placeholders and final resources share one integration path. Do not over-specify artistic details. OMP may propose contract changes through the repository/PR path as an exception.

## 7. Placeholder Strategy
Placeholders use the same integration path, approximately match durations, allow runtime/multiplayer/VFX testing, and are replaceable without rewriting logic. All Homelander placeholders are removed from the final result.

## 8. Homelander Pilot Scope
- **Flight:** one continuous controlled movement; no sharp flips during horizontal flight; continuous climb/descend/hover/horizontal transitions; body follows movement; readable enter/leave transitions; stable while camera changes; flight VFX attached without lag/popping; cinematic yet responsive.
- **Eye lasers:** originate from the eyes in every state including flight; no lag/detach/face-crossing/origin shift; readable start; deliberate charge/sustain/release phases; continuity while turning; more power via presence, light, impact; impact connected to beam; multiplayer observers see coherent origin, direction and impact.
- **Milk model:** rework as passive asset, integrate cleanly.
- **Milk final explosion:** remove vanilla-particle dependence; new VFX; better sound; camera/screen/world reaction; blinding presentation.
- **Iron Hands:** visual/audio/animation pass, no mechanical redesign.
- **Clap / Roar:** rework visuals, vanilla particles, sound, impact feel.
- **Animation package** and **sound package** for Homelander.

## 9. Out of Scope
Bottom-bar icons; left-side 3D menu while flying; chat overlap bug; laser damage balance while milk active; Uranium redesign; roster-wide animation/sound; balance changes; general character redesign; mass roster migration; external visual editor; body/block destruction bug during milk flight (unless it blocks flight testing).

## 10. In-Game Debug and Showcase
Trigger effects without long setup; repeat heavy scenes; reproduce key Homelander moments; inspect runtime behavior; efficient multiplayer checks.

## 11. Multiplayer (hard gate)
Observers see important effects correctly; transitions on time; flight visuals work for remote players; lasers correct for actor and observer; milk explosion visible and coherent; no client-only assumptions.

## 12. Performance
Sodium setup. Normal heavy scene with one major sequence ≈ **100 FPS or higher**. Multi-effect stress must not reveal catastrophic scaling; not held to 100 FPS.

## 13. Configuration
Intensity, scale, duration, fade, noise/distortion, color and similar tuning values adjustable without code churn. Not a data-driven second engine.

## 14. Review and Integration Workflow
Stream A (OMP) → third-session absolute review (fixes everything) → user approval (hard gate). Stream B (Devin Cloud) → fourth-session absolute review against the original design (fixes everything, may rewrite) → fourth session becomes final integrator: brings in approved OMP package, replaces placeholders, re-tests, removes Homelander placeholders, final multiplayer and performance verification.

## 15. Acceptance Scenes
1. flight; 2. eye lasers; 3. eye lasers while flying; 4. flight VFX; 5. milk final explosion. Iron Hands, Clap, Roar, animation and sound integration complete the pilot.

## 16. Definition of Done
Visual Core on Veil; Homelander on the new system; final passive assets; no placeholder dependence; targeted vanilla presentation replaced; flight fixed; lasers reworked incl. flight; milk explosion new; animation+sound integrated; multiplayer tested and passing; ~100 FPS normal heavy scenes; no catastrophic stress behavior; third-session OMP review; user OMP approval; fourth-session review; final integration; final in-game verification.

## 17. Reusable Workflow After Homelander
audit → map to core → extend core if needed → placeholders → passive assets in parallel → independent reviews → integrate → multiplayer test → performance test → visual review → remove placeholders → ship.
