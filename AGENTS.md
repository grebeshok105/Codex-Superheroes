# AGENTS.md — Codex Superheroes

Fabric mod (Minecraft 1.21.1, Java 21, mod id `superheroes`, package `com.example.superheroes`): superhero ability fantasy — transformations, flight physics, signature moves, HUD, VFX — natively embedded in Minecraft. Exact dependency and game versions live in `gradle.properties` and `build.gradle`; current code and passing tests win if any document differs.

## 1. What we are building

A mod whose heroes **feel** like their source material — mechanics, timings, interactions, animations, VFX — while staying readable and genuinely playable inside Minecraft. Recognizability over literal copying: never sacrifice gameplay to mirror a scene. A hero that is faithful but unplayable is a failed hero.

Codex Superheroes is in **revival and architectural modernization**. The current codebase is working history, not automatically the desired architecture. Existing behavior, saves, content, and player-facing contracts matter; old structural choices do not become permanent merely because they already exist.

Every substantial change should improve the whole project: reuse a healthy shared mechanism when it fits, extend it when that keeps the design coherent, and deliberately replace it when it creates coupling, duplication, poor testability, or blocks future work. Never add a disconnected island just to avoid touching legacy code.

**Existing code describes current behavior. It does not automatically define the desired architecture.**

Playable heroes today: `src/main/java/com/example/superheroes/hero/Heroes.java` is the roster.

## 2. Heroes, abilities, resources

- Transformation items swap the player's model and hitbox; the `HeroData` attachment stores transformation state.
- Each hero exposes its abilities via `Hero.getAbilities()`. An ability lives in `ability/<Name>Ability.java`, has an id in `AbilityIds`, is registered in `AbilityRegistry.init()`, and is routed to hero-specific runtime by `AbilityRouter`.
- Dual resource: Energy (auto-regen) and Mana (refilled by items). Each ability binds to a resource; when Energy runs out the system falls back to Mana. `ResourceController` owns this.
- Server-side hero logic ticks in `effect/*Controller` classes registered in `SuperheroesMod.onInitialize()`.
- Keybinds and slot layouts are defaults, not architecture: a hero design that needs more actions or binds must be able to get them without breaking the seam.

These paths describe the current implementation. During refactors, preserve behavior intentionally while moving responsibilities to better boundaries where needed.

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

- Server-authoritative gameplay; rendering, HUD, particles, camera, keybinds, and menus live client-side (`src/client`); nothing client-only in `src/main` — a dedicated server loads it.
- Public Fabric and Minecraft APIs only: `net.fabricmc.fabric.api.*`, never `net.fabricmc.fabric.impl.*`, no deprecated APIs without reason.
- Networking is typed `CustomPayload` + `StreamCodec` via `ModNetworking` — not legacy `PacketByteBuf`-style code.
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

- `./gradlew qualityGate --no-daemon` is the canonical finishing gate — the same one CI runs. It covers the full build with JUnit in `src/test`, the `ProjectSanityTest` source/resource checks (server-safe `src/main`, no Fabric internals, lang sync, OGG-only sounds, wired heroes/controllers/models), the assertions-enabled audit, and the release-jar isolation audit. `./gradlew build -x test` is a quick mid-work check, not a finishing gate.
- New behavioral logic gets tests in `src/test`; a bugfix gets a regression test where possible — no theater tests written just to satisfy a rule.
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
