# SESSION.md

## Active work

- Branch: `devin/1790340168-agents-md-rebuild`
- Goal: Codex Superheroes revival / 5.0 foundation work.
- Current phase: repository cleanup, agent workflow restoration, verification baseline, and architectural reacquaintance before new content work.

## Current state

- `AGENTS.md` has been rebuilt around the revival workflow and legacy-modernization rules.
- Repository cleanup removed obsolete Windsurf rules, historical plans/design docs, legacy agent skills, stale addon API docs, and the old release workflow.
- `README.md` and `fabric.mod.json` were refreshed for the revived Codex project.
- Build CI targets Java 21.
- Cross-session continuity is now mandatory through this file.
- `qualityGate` has been ported from Jujutsu and adapted to Codex: `./gradlew qualityGate` is now the canonical gate in AGENTS.md §10 and CI (`build` workflow). It runs `build` (compile + JUnit, `failOnNoDiscoveredTests`), `testProjectSanity` (`src/test/java/.../ProjectSanityTest` — 8 source/resource checks), `verifyAssertionsEnabled`, and `auditReleaseJarIsolation`. Jujutsu parts intentionally not ported: GameTest lanes, doc audit, MCP/companion audits, ArchUnit.
- The gate caught real drift: `ability.superheroes.reinhard_wish.desc_v2` existed only in `ru_ru.json` — fixed by moving the v2 text into `desc` in both lang files.
- Independent architecture/bug review and VFX-foundation research may happen in parallel; their findings should be brought back into durable project context before implementation.

## Important decisions

- Existing code defines current behavior, not automatically the desired architecture.
- Legacy systems are preserved when they are healthy; they are replaced only when they create concrete architectural, testing, reliability, or extensibility problems.
- The revival is workflow/foundation-first. New content is not required to complete the initial restoration phase.
- `gradle.properties` is the version source of truth.
- Current release automation is intentionally absent until a new verified release flow is designed.
- `ProjectSanityTest` stays an honest source/JSON grep, not bytecode analysis; new checks register JavaExec tasks with `group = 'verification'` and are pulled into `check` automatically.
- Model files with a `minecraft:` parent (e.g. `template_spawn_egg`) legitimately carry no `superheroes:` textures — the item-model check treats them as covered by vanilla.

## Verification

`./gradlew qualityGate --no-daemon` ran green locally on this branch: JUnit suite, all 8 `ProjectSanityTest` checks, `verifyAssertionsEnabled` (1 verification JavaExec with `-ea`), `auditReleaseJarIsolation` (`superheroes-4.0.0.jar`, 1344 entries, no test/dev leakage). No runtime verification was required for the documentation/repository-cleanup work recorded here.

## Open work

- Establish the architecture/debt map from the independent audit.
- Decide and execute package identity cleanup away from `com.example.superheroes` — now unblocked by the green verification baseline.
- Rebuild only the project skills that prove useful for the new workflow.
- Design a new release/versioning workflow after the verification baseline is stable.
- Use the VFX research to decide the Codex 5.0 rendering foundation.

## Next session

1. Read this file and current `AGENTS.md`.
2. Check the status/results of the parallel architecture audit and VFX research tasks.
3. Integrate findings without overlapping active branches blindly.
4. Turn verified architectural findings into a prioritized debt map before starting broad refactors.
