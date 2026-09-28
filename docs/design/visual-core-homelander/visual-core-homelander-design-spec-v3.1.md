# Codex Visual Core + Homelander Pilot — Design Spec

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

---

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

---

## 3. Design Principles

### 3.1 Veil first

New high-value visual work should be built around Veil.

Vanilla rendering and vanilla particles may remain only where replacing them gives no meaningful visual or architectural benefit. The important visual identity of abilities should not depend on old vanilla particle logic.

### 3.2 Do not copy the old visual architecture

The existing roster is useful as a catalogue of **what visual situations exist**, not as a guide for how those situations should be implemented.

Before finalizing the core design, audit the current abilities across the roster and classify the visual needs they represent: beams, impacts, trails, auras, fields, destruction, screen effects, charge phases, world reactions, and similar categories.

The audit should inform requirements. It should not preserve old implementation patterns simply because they already exist.

### 3.3 Two-layer core

The Visual Core should have two conceptual layers:

1. **foundation capabilities** — the reusable building blocks needed to create modern Veil-driven effects;
2. **reusable visual patterns** — higher-level capabilities that can be shared across abilities without forcing all characters into the same visual style.

The core should be powerful enough to avoid rebuilding common visual machinery per hero, while still allowing unique effects when a character needs them.

### 3.4 Stylistically neutral

The core should not impose one strong visual identity across the entire mod.

Different characters may have radically different visual languages. The core provides capability, consistency of quality, and reusable infrastructure rather than one mandatory art style.

### 3.5 Keep hero-specific behavior outside the shared core

The shared Visual Core must remain hero-agnostic and must not branch on or otherwise depend on a specific hero identity.

If Homelander or another character needs unique behavior, that behavior belongs in the character's own module or integration layer. The core should expose the smallest reusable extension point needed for that module to plug into the shared visual pipeline.

Do not force every unique case into a generic abstraction. A one-off effect may remain hero-specific, but the dependency direction must stay one-way: hero modules may depend on and extend the Visual Core; the Visual Core must not know which hero is using it.

This requirement must remain compatible with the repository's architectural rules and tests that prevent new shared-code dependencies on concrete heroes.

### 3.6 Core stays evolvable

After Homelander, the core is not frozen.

Later characters may extend it or even require changes to existing pieces if that materially improves the system. Homelander establishes the first complete version, not the final immutable version.

---

## 4. Visual Core Scope

The core owns:

- Veil-driven runtime VFX;
- visual effect composition;
- beams, trails, impacts, auras, fields, charge/release phases and similar reusable visual behaviors;
- camera presentation;
- screen-space effects;
- world interaction visuals such as impact marks, debris, smoke, light response and related environmental feedback;
- effect timing at runtime;
- multiplayer-visible effect state and synchronization;
- in-game debug/showcase paths for testing effects;
- integration points for passive assets supplied by OMP;
- placeholder support during parallel development;
- configurable effect parameters where that reduces needless code churn.

The core does **not** need a separate external editor, standalone visual authoring tool, or additional mini-product around it.

Testing and showcase tooling should live inside Minecraft.

---

## 5. What OMP Owns vs What Devin Cloud Owns

### Devin Cloud owns

- the Visual Core itself;
- Veil runtime effect implementation;
- Homelander runtime VFX;
- camera behavior;
- screen effects;
- runtime timing and effect sequencing;
- multiplayer behavior;
- performance behavior;
- placeholders;
- integration of final passive assets;
- the final coherent in-game result.

### OMP owns

OMP has a separate design spec and implementation plan.

OMP is responsible for passive artistic resources only:

- models;
- textures;
- animations;
- sounds;
- other passive visual/audio resources required by the pilot.

OMP does **not** own:

- Veil runtime code;
- shader/effect-system runtime logic;
- core architecture;
- multiplayer logic;
- gameplay logic;
- runtime timing logic for the effect system.

---

## 6. Shared Contract Between the Two Plans

The two workstreams must be independent enough to run in parallel.

They depend on a shared contract, not on each other's implementation progress.

Before both agents start, define only the integration points that actually matter:

- what states or moments exist;
- which passive resources are expected for those states;
- approximate durations where timing matters;
- which runtime events need corresponding passive assets;
- where OMP ownership ends;
- where Devin Cloud ownership begins;
- how placeholders and final resources occupy the same integration path.

Do **not** over-specify artistic details in the contract.

Do not predefine exact animation motion, exact sound design, exact model styling, or frame-by-frame direction unless required for synchronization.

If OMP discovers that the contract produces a clearly bad artistic result, it should propose a contract change and notify Devin Cloud through the repository/PR communication path. This is an exception path, not a normal constant back-and-forth workflow.

---

## 7. Placeholder Strategy

Devin Cloud should not wait for OMP.

Where final passive resources are not ready, Devin Cloud uses placeholders.

A valid placeholder must:

- use the same integration path as the final resource;
- approximately match the expected duration when timing matters;
- let the runtime behavior, multiplayer behavior and VFX composition be tested before OMP finishes;
- be replaceable by the final resource without rewriting the surrounding logic.

Minor manual hookup during integration is acceptable. The goal is low-friction integration, not building an elaborate automatic asset-delivery system.

All Homelander-specific temporary placeholders must be removed from the final integrated result.

---

## 8. Homelander Pilot Scope

Homelander is the first complete consumer of the core.

### Must be addressed now

#### Flight

Rework Homelander flight so it reads as one continuous, controlled movement rather than a sequence of abrupt pose changes.

Observable quality requirements:

- during ordinary horizontal flight, Homelander must not perform sharp flips or change orientation without a visible reason;
- transitions between climbing, descending, hovering and horizontal movement must read as one continuous motion rather than instant pose snaps;
- the character's body orientation should follow the movement naturally instead of visibly fighting the movement direction;
- entering and leaving flight should have a readable transition rather than an abrupt state switch;
- the flight presentation should remain visually stable while the camera changes direction or speed;
- flight VFX must remain attached to the intended body/world positions without visible lag, popping or detachment;
- the result should feel deliberately cinematic while still responding immediately enough to player input.

The old implementation is only a reference for the intended gameplay state. Its animation and visual behavior are not a quality baseline.

#### Eye lasers

Fully rebuild the eye-laser presentation and use it as one of the primary showcase cases for the Visual Core.

Observable quality requirements:

- the beam must visually originate directly from the eyes in every relevant state, including while flying;
- the beam must not visibly lag behind the head, detach from the eyes, cross through the face, or shift to an incorrect origin during camera or character movement;
- the start of the shot must be clearly readable even against bright environments and visually busy scenes;
- charge/start, sustained firing and release/end should read as deliberate phases rather than the beam simply appearing and disappearing;
- the laser should preserve visual continuity while the player turns, changes flight direction or moves the camera;
- the effect should communicate substantially more power than the old vanilla-particle implementation through beam presence, light, impact and supporting effects rather than only by increasing particle count;
- impact with the world should feel connected to the beam and should not look like a separate unrelated particle event;
- multiplayer observers must see a coherent beam origin, direction and impact rather than a client-local approximation.

The new laser should be judged by how stable, readable and physically connected it looks in motion, not merely by whether it is technically larger or brighter than the old one.

#### Milk model

- rework the milk model as a passive asset;
- integrate it cleanly into the Homelander presentation.

#### Milk final explosion

Rework the presentation of the final explosion:

- remove old vanilla-particle dependence;
- create a new visual effect;
- improve sound;
- add appropriate camera/screen/world reaction where useful;
- support a blinding visual presentation.

Gameplay targeting rules for blindness are not the main goal of this visual-core stage unless they are required for the effect to function correctly.

#### Iron Hands

Give the ability a proper visual/audio/animation pass.

Do not turn this stage into a full mechanical redesign unless the existing mechanics directly block the visual presentation.

#### Clap

Rework:

- visual presentation;
- vanilla particles;
- sound;
- impact feel.

Damage and broader gameplay behavior are outside the main visual-core scope unless a small change is necessary for coherent presentation.

#### Roar

Rework:

- visual presentation;
- vanilla particles;
- sound;
- impact feel.

Damage and wider gameplay behavior are not the focus of this stage.

#### Homelander animation package

Add the animations required for Homelander's abilities and flight.

Shared flight animation support may be added if it is genuinely useful, but this stage does not mass-produce animations for the rest of the roster.

#### Homelander sound package

Replace weak or placeholder sound presentation for the Homelander pilot.

A reusable sound integration pattern may be improved if necessary, but this stage does not rework the entire roster's audio.

---

## 9. Explicitly Out of Scope for This Stage

Do not pull these into the Visual Core/Homelander pilot unless they become blockers:

- full bottom-bar icon replacement;
- left-side 3D menu presentation while flying;
- chat message overlap/UI bug;
- laser damage balancing while milk is active;
- broad redesign of the Uranium idea;
- full roster animation production;
- full roster sound rework;
- broad balance changes;
- general character redesign work;
- mass migration of the roster;
- a separate visual editor outside Minecraft.

The body/block-destruction bug during milk-powered flight is also not part of the primary visual scope unless it prevents correct flight testing.

---

## 10. In-Game Debug and Showcase Support

The core should support direct testing inside Minecraft.

The exact interface can remain simple, but agents should be able to:

- trigger important effects without performing long gameplay setup;
- test heavy scenes repeatedly;
- reproduce key Homelander visual moments;
- inspect runtime behavior while developing;
- run multiplayer visual checks efficiently.

Do not build a separate external editor or visual-authoring application for this stage.

---

## 11. Multiplayer

Multiplayer is a hard acceptance gate.

The work is not acceptable if multiplayer is broken, obviously desynchronized, or visually unreliable.

The agent must test multiplayer directly.

At minimum, validate that:

- another player sees the important effects correctly;
- major transitions occur at the correct time for observers;
- flight-related visuals do not break for remote players;
- eye lasers behave correctly for the actor and observer;
- large effects such as the final milk explosion are visible and coherent to all relevant players;
- no major client-only assumptions corrupt the presentation.

A visually impressive single-player result with bad multiplayer behavior is considered failed work.

---

## 12. Performance

Use the current Sodium-enabled Minecraft setup as the practical reference environment.

Normal gameplay is currently much higher than the target, so the visual system has meaningful headroom.

### Hard target

A normal heavy scene with one major expensive visual sequence should remain at roughly **100 FPS or higher**.

### Stress testing

Also test several heavy effects together.

The artificial multi-effect stress test is intended to expose catastrophic scaling problems. It does not carry the same strict 100 FPS threshold as the normal heavy-scene test.

Do not weaken the visual system purely to optimize pathological scenes that are unlikely to occur in normal gameplay.

---

## 13. Configuration Philosophy

The core should allow common visual parameters to be adjusted without rewriting the implementation each time.

Examples include:

- intensity;
- scale;
- duration;
- fade behavior;
- noise/distortion strength;
- color-related parameters;
- other tuning values that are expected to change during visual iteration.

Do not turn the core into a fully data-driven second engine inside Minecraft.

Runtime logic may stay in code. The point is to make visual tuning cheap.

---

## 14. Review and Integration Workflow

### Stream A — OMP

1. OMP completes the passive Homelander asset package.
2. A separate third session performs an absolute review of the OMP result.
3. That reviewer must inspect:
   - assets;
   - models;
   - animations;
   - sounds;
   - technical correctness;
   - in-game appearance;
   - overall visual quality;
   - contract compliance.
4. The reviewer fixes everything it finds itself.
5. The user manually reviews the corrected OMP result.
6. User approval is a hard gate before final integration.

### Stream B — Devin Cloud

1. Devin Cloud completes the core and Homelander runtime implementation using placeholders where needed.
2. A separate fourth session performs an absolute review of the entire Devin Cloud result.
3. It re-checks the original design, not merely the diff.
4. It must inspect:
   - architecture;
   - runtime behavior;
   - Veil effects;
   - multiplayer;
   - performance;
   - in-game quality;
   - placeholder behavior;
   - requirement coverage;
   - regressions.
5. It fixes everything it finds itself, including large rewrites when necessary.

The fourth session does not need to return to the user for permission merely because the previous implementation was poor.

### Final integration

The fourth session then becomes the final integrator.

It:

- brings in the user-approved OMP package;
- replaces Homelander placeholders;
- may modify OMP resources if necessary to make the full scene work coherently;
- re-tests the complete integrated result;
- removes Homelander-specific temporary placeholders;
- performs final multiplayer and performance verification.

The quality target is the final in-game experience, not preservation of each sub-agent's original work.

---

## 15. Homelander Acceptance Scenes

The most important acceptance scenes for the core are:

1. **flight** — no unexplained flips, no instant pose snapping between climb/descend/hover/horizontal movement, stable body orientation and continuous transitions;
2. **eye lasers** — beam starts directly from the eyes, start/hold/end phases are readable, impact is visually connected to the beam;
3. **eye lasers while flying** — no origin drift, facial intersection, visible lag or effect breakup while the character and camera are moving;
4. **flight VFX** — effects remain spatially attached, stable and coherent through acceleration, turning, climbing, descending and hovering;
5. **milk final explosion** — the new presentation clearly replaces the old vanilla-particle look and reads as a deliberate large-scale visual event.

These scenes should exercise different parts of the core and should be used as the primary proof that the first version is useful. Passing them means satisfying the observable criteria above in actual in-game motion, not merely completing the implementation.

Iron Hands, Clap, Roar, animation integration and sound integration complete the pilot and help prove overall coherence.

---

## 16. Definition of Done

The stage is done only when all of the following are true:

- Visual Core is implemented on Veil;
- Homelander uses the new system for the agreed pilot scope;
- required passive assets are final;
- no Homelander production path depends on temporary placeholders;
- major vanilla-particle presentation targeted by this spec has been replaced;
- flight presentation is fixed;
- lasers are fully reworked and function while flying;
- milk final explosion has the new presentation;
- Homelander animation and sound integration is complete for the pilot;
- multiplayer has been actively tested and passes;
- normal heavy scenes meet the ~100 FPS target on the current Sodium setup;
- stress testing does not reveal catastrophic performance behavior;
- third-session OMP review is complete;
- user visual approval of the OMP package is complete;
- fourth-session Devin review is complete;
- final integration is complete;
- final in-game verification is complete.

---

## 17. Reusable Workflow After Homelander

After the pilot, retain a short repeatable character-migration workflow:

1. audit the character;
2. identify which existing core capabilities already cover the character;
3. extend or modify the core when genuinely needed;
4. place temporary runtime placeholders;
5. produce passive assets in parallel;
6. review both sides independently;
7. integrate;
8. test multiplayer;
9. test performance;
10. visually review the final result;
11. remove temporary placeholders;
12. ship the migrated character.

Do not require a new giant architecture plan for every hero.

The core is allowed to keep evolving as later characters expose new needs.
