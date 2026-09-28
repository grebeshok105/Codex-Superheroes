# Codex Homelander OMP Asset Package — Design Spec

## 1. Purpose

Produce the passive artistic resource package needed for the Homelander Visual Core pilot while Devin Cloud builds the runtime Visual Core in parallel.

OMP exists to keep asset-heavy work out of the main agent's context and to shorten total wall-clock time.

OMP should deliver a polished package that can be integrated into the runtime system with minimal friction.

---

## 2. OMP Role

OMP owns passive resources only.

Primary responsibilities:

- models;
- textures;
- animations;
- sounds;
- other passive visual/audio resources required by the Homelander pilot.

OMP should be able to work without waiting for Devin Cloud implementation progress.

OMP does **not** own:

- Visual Core architecture;
- Veil runtime VFX code;
- runtime shader/effect behavior;
- multiplayer logic;
- gameplay logic;
- camera logic;
- screen-effect runtime logic;
- runtime effect timing;
- final integration.

OMP may inspect the repository to understand context and integration points.

---

## 3. Dependency Model

OMP depends on the agreed **shared contract**, not on the completed Devin Cloud branch.

Before OMP starts, the contract should define only what is required for clean integration:

- the Homelander states/moments that need passive assets;
- which resource is expected for each state;
- approximate durations where timing matters;
- the runtime event or presentation moment each asset supports;
- the boundary between passive resource ownership and runtime ownership;
- the placeholder/final replacement point.

Do not over-constrain artistic execution.

OMP should retain freedom over:

- animation motion;
- posing;
- model quality;
- texture treatment;
- sound selection/design;
- artistic detail;
- exact passive-resource implementation.

---

## 4. Contract Change Process

If OMP discovers that the shared contract makes a good result impossible or clearly worse, OMP should not silently diverge.

OMP should:

1. document the issue clearly;
2. propose the smallest contract change that resolves it;
3. notify Devin Cloud through the repository/PR communication path;
4. continue all unrelated work instead of blocking the whole OMP plan.

Constant cross-agent coordination is a failure mode. Contract changes should be exceptional.

---

## 5. Homelander OMP Scope

### 5.1 Flight animation resources

Create or improve the animation resources needed for Homelander flight.

Observable quality requirements:

- ordinary horizontal flight must not visually read as a sequence of sharp flips or arbitrary orientation changes;
- climbing, descending, hovering and horizontal flight need compatible poses/transitions so the runtime can blend them into one continuous movement;
- transitions must avoid instant pose snaps or visibly unrelated body orientations;
- the pose language should support a more cinematic flight presentation without making the character look disconnected from the actual movement direction;
- entering and leaving flight should have usable transition animation rather than relying on an abrupt state switch;
- animation timing should remain close enough to the shared contract that Devin Cloud can build against placeholders without later re-timing the whole scene.

OMP owns the animation quality and continuity. Devin Cloud owns runtime movement, camera behavior, flight VFX and final sequencing.

If a genuinely reusable common flight animation resource is useful, it may be created, but this task does not mass-produce flight animation work for the whole roster.

### 5.2 Eye-laser animation resources

Provide the passive animation resources needed for the eye-laser rework.

The animation package must support:

- a readable charge/start phase;
- sustained firing where needed;
- a readable release/end phase;
- laser use while flying;
- head/upper-body motion that does not make the intended eye origin visually nonsensical during firing;
- transitions that remain usable while the character turns or changes flight state.

The animation should give Devin Cloud stable visual states to anchor the runtime beam to. The final beam itself must originate directly from the eyes and remain coherent in motion, but that anchoring and Veil runtime behavior belong to Devin Cloud.

OMP does not implement the laser beam or Veil runtime effect itself.

### 5.3 Milk model rework

Rework the milk model used by Homelander.

The final model should:

- look intentional rather than placeholder-like;
- fit the visual quality bar of the new pilot;
- integrate cleanly with the existing character presentation;
- be ready for direct use by the runtime branch.

### 5.4 Iron Hands resources

Provide the passive resources needed to make Iron Hands feel properly authored.

This may include:

- animation;
- model/texture assets if needed;
- sound assets.

OMP does not redesign the gameplay mechanic.

### 5.5 Clap resources

Provide the passive animation/audio resources for the Clap visual pass.

The runtime VFX, world impact and ability behavior remain the responsibility of Devin Cloud.

### 5.6 Roar resources

Provide the passive animation/audio resources for the Roar visual pass.

The runtime VFX, screen/camera treatment and gameplay behavior remain the responsibility of Devin Cloud.

### 5.7 Milk final-explosion audio/passive resources

Provide any passive resources needed by the final milk explosion presentation.

This may include:

- sound assets;
- model/texture assets used by the runtime effect;
- supporting animation assets where required.

OMP does not implement the Veil explosion effect, blinding runtime effect, camera effect or multiplayer logic.

### 5.8 General Homelander sound pass

Improve weak or placeholder sound presentation for the Homelander pilot.

The goal is not a full-mod audio rewrite.

Focus on sounds directly used by the pilot scope.

---

## 6. Out of Scope

OMP should not spend time on:

- bottom UI icons;
- left-side 3D menu fixes;
- chat/UI overlap bugs;
- damage balancing;
- Uranium redesign;
- full-roster animation production;
- full-roster sound replacement;
- Veil core code;
- runtime VFX;
- multiplayer implementation;
- gameplay rewrites;
- unrelated character work.

---

## 7. Quality Bar

OMP output should be judged as production assets, not placeholders.

Each asset should be:

- visually coherent with Homelander;
- technically valid;
- correctly exported;
- clean enough for direct integration;
- free of obvious clipping, broken transforms or bad transitions;
- suitable for repeated in-game use;
- consistent with the shared contract;
- strong enough that the final integrator does not need to replace it merely because it looks unfinished.

The package does not need to match one global Codex art style. The core is intentionally stylistically neutral.

Homelander's assets should simply look coherent with each other and good in the actual game.

---

## 8. Timing Expectations

Approximate timing matters only where it affects runtime integration.

OMP should respect shared-contract duration expectations closely enough that Devin Cloud can build runtime sequencing with placeholders.

If an animation or sound must materially differ in duration to look good, OMP should propose the contract change instead of silently diverging.

---

## 9. Repository Interaction

OMP may inspect and use the repository to:

- understand existing resource organization;
- understand Homelander states and ability presentation;
- verify how assets are consumed;
- validate exports;
- test assets in-game where appropriate.

OMP should not modify Visual Core runtime architecture.

If OMP finds a core limitation, it should report it rather than solving it inside the OMP branch.

---

## 10. OMP Review Stage

After OMP completes its work, a separate third session performs an **absolute review**.

That review is not limited to checking whether files exist.

It must evaluate:

- model quality;
- animation quality;
- sound quality;
- technical validity;
- export correctness;
- in-game appearance;
- clipping/transforms;
- transition quality;
- timing compatibility;
- contract compliance;
- overall artistic coherence;
- any code or resource glue included in the OMP branch;
- whether the package is genuinely ready for integration.

The third session must fix the issues it finds itself.

It should return a corrected package, not merely a review report.

---

## 11. User Approval Gate

After the third session finishes and fixes the OMP result, the user reviews the corrected package.

This is a hard gate.

Final integration should not begin until the user is satisfied with the visual quality of the OMP package.

If the user dislikes an animation, model, sound or other passive asset, it should be corrected before integration.

---

## 12. Integration Expectations

The OMP package should integrate through the same runtime points Devin Cloud used for placeholders.

The integration does not need to be fully automatic.

A small amount of manual hookup is acceptable.

However, final integration should not require redesigning runtime logic because the OMP package ignored the shared contract.

The final integrator may still modify OMP assets if a resource that is good in isolation performs poorly in the complete in-game scene.

Final scene quality has priority over preserving the original OMP output.

---

## 13. Definition of Done

The OMP package is complete when:

- all agreed Homelander passive assets exist;
- required flight animations are complete;
- required eye-laser animations are complete;
- the milk model is reworked;
- required Iron Hands assets are complete;
- required Clap assets are complete;
- required Roar assets are complete;
- required milk-final-explosion passive assets are complete;
- Homelander pilot sound work is complete;
- assets respect the shared contract;
- assets have been tested in-game where applicable;
- the third-session absolute review is complete;
- all findings from that review are fixed;
- the user has visually approved the corrected package.

Only after this gate should the package move to the final integrator.
