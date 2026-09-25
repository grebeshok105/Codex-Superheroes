# AGENTS.md — Codex Superheroes

Fabric mod (Minecraft 1.21.1, Java 21, mod id `superheroes`, package `com.example.superheroes`): superhero ability fantasy — transformations, flight physics, signature moves, HUD, VFX — natively embedded in Minecraft. Exact versions live in `gradle.properties` and `build.gradle`; code wins if any doc differs.

## 1. What we are building

A mod whose heroes **feel** like their source material — mechanics, timings, interactions, animations, VFX — while staying readable and genuinely playable inside Minecraft. Recognizability over literal copying: never sacrifice gameplay to mirror a scene. A hero that is faithful but unplayable is a failed hero.

The roster already proved the architecture holds. The project is in **systemic development**: every feature must strengthen the whole — reuse existing mechanisms, extend shared systems, never grow a disconnected island.

Playable heroes today: `src/main/java/com/example/superheroes/hero/Heroes.java` is the roster.

## 2. Heroes, abilities, resources

- Transformation items swap the player's model and hitbox; the `HeroData` attachment stores transformation state.
- Each hero exposes its abilities via `Hero.getAbilities()`. An ability lives in `ability/<Name>Ability.java`, has an id in `AbilityIds`, is registered in `AbilityRegistry.init()`, and is routed to hero-specific runtime by `AbilityRouter`.
- Dual resource: Energy (auto-regen) and Mana (refilled by items). Each ability binds to a resource; when Energy runs out the system falls back to Mana. `ResourceController` owns this.
- Server-side hero logic ticks in `effect/*Controller` classes registered in `SuperheroesMod.onInitialize()`.
- Keybinds and slot layouts are defaults, not architecture: a hero design that needs more actions or binds must be able to get them without breaking the seam.

## 3. How user and agent work together

The user supplies the **DESIGN SPEC** — WHAT and WHY: behavior, feel, constraints, edge cases, interactions, acceptance criteria.

The agent owns the technical side — HOW. Before executing any ready spec, a full **implementation plan** must exist. No plan → the agent researches the repo, reads the relevant skills/docs/code, and writes the plan itself. A plan never silently changes the user's design decisions.

A complete spec means no re-brainstorming: settled design decisions are not reopened without cause.

When the user asks to invent something large from scratch — system, hero, mechanic, VFX direction, UI — with no spec yet, the **brainstorming gate** applies: research, 2–3 approaches, trade-offs, decision alignment, then a final DESIGN SPEC. The implementation plan is the next stage after that.

## 4. Autonomy

The agent works end to end without micromanagement: understand the goal → research the repo → read the relevant project skills → read current docs and context → use available MCP and dev tools → write the implementation plan if missing → implement → write or update tests → run automated checks → launch the game when runtime is touched → verify in game → fix findings → re-verify → self-review → run independent reviews → bring the task to DONE.

Anything answerable through code, docs, git, skills, MCP, or research tools is resolved independently. Ordinary technical actions already permitted by this contract need no permission asked. Trivial technical choices never stop execution.

Escalate only real design/product blockers, or forks with fundamentally different behavior or meaning that existing design context cannot resolve. Default to action: never bounce routine questions or status checks to the user, never ask for confirmation the repo can answer.

## 5. Architecture quality

The implementation must fully work, fit the existing project logically, create no duplicate systems, take no shortcut that degrades structure, live in the right place, use existing abstractions where reasonable, and stay extensible. Never pick the shortest path when it litters duplication, one-off crutches, or future cost.

## 6. Hard rules

- Server-authoritative gameplay; rendering, HUD, particles, camera, keybinds, and menus live client-side (`src/client`); nothing client-only in `src/main` — a dedicated server loads it.
- Public Fabric and Minecraft APIs only: `net.fabricmc.fabric.api.*`, never `net.fabricmc.fabric.impl.*`, no deprecated APIs without reason.
- Networking is typed `CustomPayload` + `StreamCodec` via `ModNetworking` — not legacy `PacketByteBuf`-style code.
- Reuse shared mechanisms before building new ones; one concern, one system.
- Mixins stay narrow and scoped; injected members get `@Unique`; prefer MixinExtras `@WrapOperation` over `@Redirect`. The non-obvious traps live in the `loader-gotchas` skill, not here.
- Preserve the hero seam: shared code asks the hero/registry, never which hero the player is.
- Runtime sounds are OGG Vorbis only. Before creating any texture, sound, model, or FX, check `art-source/` first (see the `art-source` skill).
- `en_us.json` and `ru_ru.json` are updated together.
- `src/main/generated/` is datagen output — regenerate it via `runDatagen`, never hand-edit.

## 7. Skills

Project skills live in `.agents/skills/` and are work procedures, not suggestions: read the matching one and follow it. `.windsurf/` mirrors the same rules and workflows for Windsurf.

Current set: `project-profile`, `base-rules`, `build-mod`, `mod-build-jar`, `release-mod`, `version-bump-policy`, `publish-mod`, `add-item`, `add-block`, `datagen`, `debug-crash`, `loader-gotchas`, `art-source`, `research-tools`, `mod-change-report`, `admin-ability-debug`, `worker-subagent-dispatch`, `minecraft-mod-dev`.

A stable workflow repeated twice or more becomes a project skill on its own, no asking needed. No skill per micro-action: one-off repetitions stay inline.

## 8. Tools

Prefer the lightest tool that answers. All querying is pre-authorized — on failure, say so once and continue with the repo.

| Tool | For |
|---|---|
| gradle (`build`, `compileJava`, `runDatagen`, `runClient`) | compilation, tests, datagen, dev client |
| javap + decompiled MC jar (`research-tools` skill) | vanilla Minecraft internals, method signatures — offline, no API guessing |
| web search / fetch | Fabric docs, open-source mod examples, external API facts |
| git / gh | branches, history, PRs, releases |
| MCP servers when connected (`.windsurf/rules/agents.md`) | mcdev sources, context7 docs, sequential-thinking — that doc says which agent to call when |

## 9. Verification

Compilation proves nothing. Before a PR:

- `./gradlew build --no-daemon` is the finishing gate — the same full build CI runs, JUnit in `src/test` included. `./gradlew build -x test` is a quick mid-work check, not a finishing gate.
- New behavioral logic gets tests in `src/test`; a bugfix gets a regression test where possible — no theater tests written just to satisfy a rule.
- Datagen-touching changes run `./gradlew runDatagen --no-daemon` and the diff in `src/main/generated/` is reviewed.
- Runtime-affecting changes (gameplay, input, rendering, entities, networking, VFX, HUD) need in-game verification via `./gradlew runClient --no-daemon` where the environment allows. If the sandbox cannot launch the game, say so explicitly in the PR instead of claiming verification.
- Large or risky changes get independent review through subagents (`worker-subagent-dispatch` skill). A worker's verification ceiling is compilation; full builds, test suites, and game runs belong to the main agent only.

## 10. Definition of Done

DONE only when every relevant item holds: DESIGN SPEC fully implemented; implementation plan executed; no known open items; tests written or updated; `./gradlew build` green; datagen diff reviewed where applicable; in-game verification done where applicable or its absence stated; acceptance criteria checked; independent reviews done for large changes; findings fixed or explicitly rejected with reasons; docs updated where the change outdated them; final self-review done; git state clean and pushed.

## 11. Git workflow and GitHub

Git is mandatory and GitHub is the only home of the work. Each self-contained task runs on its own branch; changes split into small logical commits (English, conventional-style: `feat(scope): ...`, `fix(scope): ...`); each finished task ships as its own PR. **Nothing task-related may live only on the local machine**: branches, fixes, and docs are pushed to GitHub at task end — no unpushed state is ever carried across sessions.

PRs are written for a Russian-speaking player audience:

- **Title: explicit, readable, in Russian**, naming the task — not «fix», «wip», «upd».
- **Body opens with what was done and how it works, written for the player** — Russian, clear and inviting, content-side only.
- **Below: the full technical part** — what, why, key decisions, what was verified and how (build, tests, in-game verification), known limits.

## 12. Versioning & releases

The version lives in `gradle.properties` (`mod_version`); the `version-bump-policy` skill is authoritative for choosing the next number — hotfix / small fix → patch, крупное обновление → minor, глобальный релиз → major. Tags are `vX.Y.Z`.

Releases ship through the `release-mod` skill (`gh release create` with the built jar; notes in Russian, for players). `.github/workflows/release.yml` auto-publishes on pushes to `baseline` — GitHub Actions are not changed without an explicit request.

## 13. Documentation

Docs stay current with the code: a change that outdated a document updates it in the same task. Before creating a new markdown file, find the existing place for the information. No sprawl, no temp facts in durable files — this file points at context, it does not store it.

Context map: `README.md` (overview), `docs/api.md` (public addon API), `docs/design/` (hero/balance specs), `docs/plans/` plus `plan.md`, `ROUND4-PLAN.md` (implementation plans), `.agents/skills/` (procedures), `.windsurf/` (Windsurf mirror), `art-source/` (raw assets).

Authority when sources disagree: current code and passing tests → this file → project skills → docs and plans.
