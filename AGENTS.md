# AGENTS.md — Codex Superheroes

Fabric mod (Minecraft 1.21.1, Java 21, mod id `superheroes`, package `io.github.grebeshok105.codex`): superhero ability fantasy — transformations, flight physics, signature moves, HUD, VFX — natively embedded in Minecraft. Exact dependency and game versions live in `gradle.properties` and `build.gradle`; current code and passing tests win if any document differs.

## 1. What we are building

A mod whose heroes **feel** like their source material — mechanics, timings, interactions, animations, VFX — while staying readable and genuinely playable inside Minecraft. Recognizability over literal copying: never sacrifice gameplay to mirror a scene. A hero that is faithful but unplayable is a failed hero.

Codex Superheroes is in **revival and architectural modernization**. The current codebase is working history, not automatically the desired architecture. Existing behavior, saves, content, and player-facing contracts matter; old structural choices do not become permanent merely because they already exist.

Every substantial change should improve the whole project: reuse a healthy shared mechanism when it fits, extend it when that keeps the design coherent, and deliberately replace it when it creates coupling, duplication, poor testability, or blocks future work. Never add a disconnected island just to avoid touching legacy code.

**Existing code describes current behavior. It does not automatically define the desired architecture.**

Playable heroes today: `src/main/java/io/github/grebeshok105/codex/bootstrap/HeroModules.java` (`ALL`) is the roster — one line per hero; `core/hero/Heroes` is the runtime registry it fills.

## 2. Heroes, abilities, resources

A hero is a **module pair**, wired by exactly one line in each composition root — `bootstrap/HeroModules.ALL` (server) and `client/bootstrap/HeroClientModules.ALL` (client). Nothing else may name a hero class.

- `hero/<id>/<Id>Module implements HeroModule` — `hero()` returns the hero; `register(HeroModuleContext)` wires the module through narrow registrars: `ctx.abilities()` (`Ability`s into `core/ability/AbilityRegistry`), `ctx.attachments()` (hero-owned attachments), `ctx.payloads()` (typed `CustomPayload` + `StreamCodec`), `ctx.ticks()` (`HeroTickDispatcher` phases), `ctx.lifecycle()` (`PlayerLifecycle`/`HeroLifecycle` hooks), `ctx.content()` (creative-tab entries), `damageTypes()` (module-owned damage-type specs for datagen). Hero entities/sounds/items register through the module's own `<Id>Entities`/`<Id>Sounds`/`<Id>Items` — direct `Registry`/`ModContent` calls, no shared indirection.
- `client/hero/<id>/<Id>ClientModule implements HeroClientModule` — the client twin through `HeroClientContext`: payload receivers (`ctx.receive` — mandatory for every hero `*S2CPayload`), HUD layers, action keys, entity renderers, player layers, skin provider, sound filters, ability decorations, beam styles, client ticks.
- `hero/<id>/<Id>Hero implements Hero` (`core/hero/Hero`) is the hero's whole contract: id, energy/mana caps, hitbox dimensions, `getAbilities()`, `getDefaultBinding()`, and the hooks shared code consults — passives (`passiveAttributes`/`applyPassives`/`removePassives`/`reapplyPassivesAfterRespawn`), `keepsHeroOnDeath`, landing/impact (`onLanded`, `suppressesLanding`, `modifyImpact`, `modifyMeleePush`), ability gates (`canUseAbility`, `onAbilityDenied`, `visibility`, `isAbilitySuppressedBy`, `getEnergyReserveFor`), presentation (`getTheme`, `getHudConfig`, `getPassiveGlyphs`, `getThreatClass`, `getImpactStyle`/`getImpactPower`), `canSuperJump`, `getMeleeBleed`, `isUraniumWeak`, `cancelsFallDamage`, `getSkinTexture`.
- `hero/<id>/` subpackages are by role: `ability/` (one `Ability` class each, owning `public static final ResourceLocation ID = ModId.of(...)`), `runtime/` (controllers and session state), `item/`, `entity/`, `net/` (payloads), `sound/`, `registry/` (damage types). Root files — `<Id>Abilities`, `<Id>Items`, `<Id>Attachments` — only alias leaf constants. **A leaf never imports the module root**: it owns its `ModId.of(...)` literal and the root aliases it. A hero mixin goes to `mixin/hero/<id>/` (server) or `client/hero/<id>/mixin/` only when no shared hook covers it.

Shared homes — nothing hero-named may live there: `core/` (hero contract + `Heroes` registry, ability router/registry/cooldowns, `resource/`, `transform/` + `HeroData`, `lifecycle/`, `net/`, `attachment/`, `content/`, `module/` contexts), `mechanic/` (hero-agnostic mechanics — flight, impact, motion, targeting, boundweapon, charge, strike, summon, falls, world — plus shared abilities in `mechanic/ability`), `content/` (hero-less `ContentModule` slices: horde, boss, admin, command), `compat/` (other-mod bridges), `bootstrap/` (composition roots). Client mirrors it: `client/core/` (module contracts, HUD framework, input, render, audio, text, shared mixins), `client/bootstrap/`, `client/hero/<id>/`.

Runtime invariants: server ticks register only through `ctx.ticks()` → `HeroTickDispatcher` (phases START → EARLY → GLOBAL → LEVELS → PLAYERS → ABILITY_ACTIVE; dead players are skipped once centrally); lifecycle hooks only through `ctx.lifecycle()` → `PlayerLifecycle` (join/leave/death/respawn/server-stopped — never `ServerPlayConnectionEvents.DISCONNECT`, which can fire off the server thread) and `HeroLifecycle` (transform/clear); per-player session state lives in `OwnedSessionMap` (auto-cleared on leave/death/hero-clear/server stop — never a static `Map<UUID,…>`) or attachments: a synced attachment for long-lived client-visible state, a payload for one-shot events.

Transformation items swap the player's model and hitbox; the `core/transform/HeroData` attachment stores transformation state. Dual resource stays: Energy (auto-regen) + Mana (item refills) owned by `core/resource/ResourceController`; `core/ability/AbilityRouter` validates and routes activation — hero-specific gates live on `Hero` hooks, never in the router. C2S handlers validate the sender via `core/net/C2SGuards` before acting.

Keybinds and slot layouts are defaults, not architecture: a hero design that needs more actions or binds must be able to get them without breaking the seam.

## 3. How user and agent work together

The user supplies the **DESIGN SPEC** — WHAT and WHY: behavior, feel, constraints, edge cases, interactions, acceptance criteria.

The agent owns the technical side — HOW. Before any **non-trivial** implementation, a sufficiently complete implementation plan must exist. If it is missing, the agent researches the repo, reads relevant skills/docs/code, and writes the plan itself. Small, obvious fixes do not require ceremonial planning. A plan never silently changes the user's design decisions.

A complete spec means no re-brainstorming: settled design decisions are not reopened without cause.

When the user asks to invent something large from scratch — system, hero, mechanic, VFX direction, UI — with no spec yet, the **brainstorming gate** applies: research, 2–3 approaches, trade-offs, decision alignment, then a final DESIGN SPEC. The implementation plan is the next stage after that.

## 4. Autonomy

The agent works end to end without micromanagement: understand the goal → research the repo → read relevant project skills and current docs → inspect current code and git history where useful → use available MCP and dev tools → write an implementation plan when the task warrants one → implement → write or update tests → run automated checks → launch the game when runtime is touched → verify in game → fix findings → re-verify → self-review → run independent reviews when risk justifies them → bring the task to DONE.

Anything answerable through code, docs, git, skills, MCP, or research tools is resolved independently. Ordinary technical actions already permitted by this contract need no permission asked. Trivial technical choices never stop execution.

Escalate only real design/product blockers, or forks with fundamentally different behavior or meaning that existing design context cannot resolve. Default to action: never bounce routine questions or status checks to the user, never ask for confirmation the repo can answer.

## 5. Session handoff & continuity

`SESSION.md` is the canonical cross-session handoff. It exists so a new agent can continue the current work without reconstructing decisions from chat history, local state, or guesswork.

At the start of every substantial work session, read `SESSION.md` before changing code. Before ending every substantial task or session — including an unfinished one — update it so the next session can resume immediately.

Keep `SESSION.md` concise and current. It must record:
- active branch and current goal;
- what was completed in this session;
- important technical/design decisions and why;
- verification actually performed and its result;
- known blockers, regressions, or unresolved findings;
- exact next steps for continuation.

Do not turn `SESSION.md` into a permanent changelog or duplicate durable documentation. Replace stale handoff state instead of accumulating history. Durable architectural/product knowledge belongs in the appropriate docs or skills; Git and PRs preserve history.

No important unfinished context may exist only in an agent's memory, chat, terminal output, or local files. The handoff is part of the task, must be committed and pushed with the work, and is required even when the implementation itself is already complete.

## 6. Architecture quality

The implementation must fully work, fit the project logically, create no duplicate systems, take no shortcut that degrades structure, live in the right place, use healthy existing abstractions where reasonable, and stay extensible.

Do not preserve a legacy abstraction merely for consistency. Before extending an old central system, check whether it is already causing hero-specific branching, excessive coupling, duplicated state, difficult testing, client/server leakage, or unrelated responsibilities. If so, prefer a deliberate migration with tests over adding another special case.

Never pick the shortest path when it litters duplication, one-off crutches, or future cost. Refactors must still be scoped: do not turn every feature task into an unrelated rewrite.

## 7. Hard rules

- The mod is client-only (`fabric.mod.json` `environment: "client"`, EMF is a hard dependency) — a dedicated server cannot load it, and there is no `runServer`/`clientnoveil`/`servernoveil` infrastructure. GameTest and datagen still run dedicated-server environments where the mod is env-filtered: the `superheroes-gametest`/`superheroes-datagen` harness mods bootstrap `SuperheroesMod.onInitialize()` and re-declare its mixin configs. Keeping `src/main` free of client-only imports remains good hygiene but is not a gate.
- Veil is client-only: `veil.*` is referenced only from `src/client` (e.g. `client/hero/<id>/fx/`, `client/core/**`), always behind `FabricLoader.isModLoaded("veil")`. `src/main` never imports Veil — Veil declares `environment: "*"` yet hard-depends on client-only Fabric modules, so the gametest/datagen server classpaths still filter it out; `fabric.mod.json` keeps Veil `recommends` even after the full VFX port. Server-side code reaches Veil effects only through `core/net` payloads and `FxBroadcast`.
- Package dependency rules — `core` ← `mechanic` ← `hero.<id>`/`content`/`compat` ← `bootstrap`, hero→hero is string ids/tags only, nothing depends on a composition root — are defined in `docs/design/architecture-migration/00-overview.md` §5.2 and enforced by ArchUnit in `src/test/java/io/github/grebeshok105/codex/architecture/` (`ArchitectureRulesTest`, `ClientArchitectureRulesTest`, `PackageCycleRatchetTest`; the frozen baselines are shrink-only).
- Public Fabric and Minecraft APIs only: `net.fabricmc.fabric.api.*`, never `net.fabricmc.fabric.impl.*`, no deprecated APIs without reason.
- Networking is typed `CustomPayload` + `StreamCodec` via `core/net` — modules register through `ctx.payloads()`/`HeroClientContext.receive`, shared payloads live in `CoreNetworking`; never legacy `PacketByteBuf`-style code.
- Reuse healthy shared mechanisms before building new ones, but never preserve a bad abstraction solely because it already exists.
- Mixins stay narrow and scoped; injected members get `@Unique`; prefer MixinExtras `@WrapOperation` over `@Redirect` when it is the clearer and safer hook.
- Preserve the hero seam: shared code asks the hero/registry or an appropriate shared abstraction, never which concrete hero the player is.
- Runtime sounds are OGG Vorbis only. Before creating any texture, sound, model, or FX, check `art-source/` first.
- `en_us.json` and `ru_ru.json` are updated together.
- `src/main/generated/` is datagen output — regenerate it via `runDatagen`, never hand-edit.

## 8. Skills

Project skills live in `.agents/skills/`. A matching current skill is a work procedure, not a suggestion: read it and follow it.

`AGENTS.md` owns global project rules. Skills own specialized, repeatable workflows. A skill must be Codex-specific enough to save real repeated reasoning, current with the repository, and distinct from existing procedures. Do not create a skill for a micro-action or merely because the same action happened twice.

Legacy skills are not authoritative merely because they exist. If a skill contradicts current code, tests, or this file, fix or replace the skill instead of preserving the contradiction.

## 9. Tools

Prefer the lightest tool that answers. All querying is pre-authorized — on failure, say so once and continue with the repo.

| Tool | For |
|---|---|
| gradle (`build`, `compileJava`, `runDatagen`, `runClient`) | compilation, tests, datagen, dev client |
| javap / decompiled Minecraft sources when available | vanilla internals and method signatures without guessing |
| web search / fetch | current Fabric docs, library docs, open-source mod examples, external API facts |
| git / gh | branches, history, diffs, PRs, tags, releases |
| connected MCP/dev tools | code navigation, Minecraft sources, docs, runtime control, research |

Do not guess unstable Minecraft/Fabric/library APIs from memory when the repo, mapped sources, or current docs can answer.

## 10. Verification

Compilation proves nothing. Before a PR:

- `./gradlew qualityGate --no-daemon` is the canonical finishing gate — the same one CI runs. It covers the full build with JUnit in `src/test`, the `ProjectSanityTest` source/resource checks (server-safe `src/main`, no Fabric internals, lang sync, OGG-only sounds, wired heroes/controllers/models), the assertions-enabled audit, the release-jar isolation audit, the headless server GameTests in `src/gametest` (`runGametest`), and the ArchUnit architecture rules (`src/test/.../architecture`) with a frozen, shrink-only baseline in `src/test/resources/archunit_store` and `src/test/resources/architecture` (`verifyArchitectureBaseline` fails until a shrunk baseline is committed). `./gradlew build -x test` is a quick mid-work check, not a finishing gate.
- New behavioral logic gets tests: pure logic in `src/test` (JUnit), server runtime behavior (players, inventories, abilities, lifecycle) in `src/gametest` via Fabric GameTest with real joined players. A bugfix gets a regression test where possible — no theater tests written just to satisfy a rule.
- Refactors preserve relevant behavior with tests before or alongside structural change where practical.
- Datagen-touching changes run `./gradlew runDatagen --no-daemon` and the diff in `src/main/generated/` is reviewed.
- Runtime-affecting changes (gameplay, input, rendering, entities, networking, VFX, HUD) need in-game verification via `./gradlew runClient --no-daemon` where the environment allows. If the environment cannot launch the game, say so explicitly in the PR instead of claiming verification.
- Large or risky changes get independent review through available subagents/reviewers. Delegated workers do not duplicate full verification suites unless the workflow explicitly requires it; the main agent owns final build, tests, and runtime verification.

## 11. Definition of Done

DONE only when every relevant item holds: DESIGN SPEC fully implemented; implementation plan executed when one was required; no known open items; tests written or updated; `./gradlew qualityGate` green; datagen diff reviewed where applicable; in-game verification done where applicable or its absence stated; acceptance criteria checked; independent reviews done where risk warrants them; findings fixed or explicitly rejected with reasons; docs updated where the change outdated them; `SESSION.md` updated with an accurate handoff; final self-review done; git state clean, committed, and pushed.

## 12. Git workflow and GitHub

Git is mandatory and GitHub is the only home of the work. Each self-contained task runs on its own branch; changes split into small logical commits (English, conventional-style: `feat(scope): ...`, `fix(scope): ...`); each finished task ships as its own PR. **Nothing task-related may live only on the local machine**: branches, fixes, and docs are pushed to GitHub at task end — no unpushed state is carried across sessions.

PRs follow the same player-first scheme as Jujutsu:

- **Title: explicit, readable, in Russian**, naming the task («фикс полёта Хоумлендера», not «fix», «wip», «upd»).
- **Body opens with «Для игрока»**: what was done and how it works, written for the player — Russian, clear and inviting, emojis welcome, 0% technical part, content-side only.
- **Below: the full technical part for other agents** — what, why, key decisions and migrations/refactors, what was verified and how (tests, build, in-game verification), known limits and follow-up work.
- The PR must contain enough technical context for another agent to understand the change without reconstructing it from the original chat.

## 13. Versioning & releases

The version source of truth is `gradle.properties` (`mod_version`). Release tags use `vX.Y.Z`. Version changes follow semantic intent: bugfix-only work normally increments patch, meaningful feature releases increment minor, and intentionally breaking or milestone releases increment major.

Every build that produces a jar for the user MUST bump `mod_version` first — never ship two jars with the same version:
- fix (bugfix-only) → patch +0.0.1 (e.g. `4.1.1` → `4.1.2`)
- normal update (features, balance, content) → minor +0.1, patch resets (e.g. `4.1.1` → `4.2.0`)

The existing release infrastructure is under audit during the revival. Do not infer current release behavior from legacy branches, tags, comments, or workflows. Do not publish a release or change release automation unless the task explicitly requires it. When release work is requested, inspect the current workflow, tags, built artifact naming, and target branch first; then use or create a current project release procedure based on verified reality.

## 14. Documentation

Docs stay current with the code: a change that outdated a durable document updates it in the same task. Before creating a new markdown file, find whether the information belongs in an existing durable document. Avoid documentation sprawl and temporary facts in long-lived files.

During the revival, documentation is being pruned aggressively. Historical plans and obsolete workflow docs are evidence, not authority. Git history is the archive; do not keep dead documents in the active tree merely for historical preservation.

Durable context should converge on:
- `SESSION.md` — active branch, current goal, latest handoff and exact continuation point.
- `README.md` — public project overview and setup.
- `AGENTS.md` — global agent contract and project-wide rules.
- `docs/api.md` — public addon API only while it is actively maintained.
- `docs/design/` — active or intentionally preserved design specifications, not completed task logs.
- `.agents/skills/` — current specialized procedures.
- `art-source/` — raw/source assets and their provenance.

Authority when sources disagree: current code and passing tests → this file → `SESSION.md` for current work state → current project skills → current durable docs. Historical plans never override current implementation or explicit design decisions.
