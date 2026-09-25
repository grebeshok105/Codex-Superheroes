# SESSION.md

## Active work

- Branch: `devin/1790340168-agents-md-rebuild` (PR #36 lands its remaining commits onto `main`, then the branch is done).
- Goal: Codex Superheroes revival / 5.0 foundation work.
- Current phase: repository cleanup, agent workflow restoration, verification baseline, and architectural reacquaintance before new content work.

## Current state

- `AGENTS.md` has been rebuilt around the revival workflow and legacy-modernization rules.
- Repository cleanup removed obsolete Windsurf rules, historical plans/design docs, legacy agent skills, stale addon API docs, and the old release workflow.
- `README.md` and `fabric.mod.json` were refreshed for the revived Codex project.
- Build CI targets Java 21.
- Cross-session continuity is now mandatory through this file.
- PR #34 (AGENTS.md rebuild + revival cleanup) is merged into `main`.
- PR #35 (qualityGate) was stacked on this branch — it merged **into this branch**, not trunk; PR #36 carries those commits onto `main`.
- `qualityGate` has been ported from Jujutsu and adapted to Codex: `./gradlew qualityGate` is now the canonical gate in AGENTS.md §10 and CI (`build` workflow). It runs `build` (compile + JUnit, `failOnNoDiscoveredTests`), `testProjectSanity` (`src/test/java/.../ProjectSanityTest` — 8 source/resource checks), `verifyAssertionsEnabled`, and `auditReleaseJarIsolation`. Jujutsu parts intentionally not ported: GameTest lanes, doc audit, MCP/companion audits, ArchUnit.
- The gate caught real drift: `ability.superheroes.reinhard_wish.desc_v2` existed only in `ru_ru.json` — fixed by moving the v2 text into `desc` in both lang files.
- A repo blueprint now exists for Codex (Java 21 + GCS Maven mirror incl. Loom `artifactUrls` patch + compile maintenance + knowledge commands).
- Stale remote branches were cleaned: only `main`, this branch, `docs/sync-versions-v4`, and `gh-pages` remain (`gh-pages` serves the live grebeshok.eu.cc site).
- Independent architecture/bug review and VFX-foundation research may happen in parallel; their findings should be brought back into durable project context before implementation.
- The independent Opus 5.5 architecture/bug audit is preserved at `docs/audits/2026-09-25-opus-architecture-audit.md` and is the current baseline for the restoration rewrite.
- Architecture audit 2 is preserved as two complementary files: `docs/audits/2026-09-25-hero-modularity-audit.md` (hero locality, `HeroModule`/`HeroProfile`, Scorpion pilot, Reinhard stress-test) and `docs/audits/2026-09-25-hoplite-structural-audit.md` (package cycles, registries, router contract, services, payloads).
- The migration that synthesizes both audits is split into six executable plans in `docs/design/architecture-migration/` (`00-overview.md` is the map; `01`–`06` are the plans). They were revised after an external review and rebased on the bugfix stages 4–12 (#40, #42–#49): plans extend the BF11 seams (`HeroTickDispatcher`, `HeroLifecycle`, `Hero` hooks), BF7 `ClientSessionState` and BF10 `PUBLIC_HERO` instead of adding parallel ones. The monolithic `docs/design/2026-09-25-architecture-migration-plan.md` is an unchanged archive of the pre-review version and is not executed or updated.

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

- Execute the plans in `docs/design/architecture-migration/` stage by stage; the canonical stage graph is `00-overview.md` §2.1 and each plan's «Статус стадий» table is its tracker. First stage: plan 1 `A1` once every bugfix PR (#37–#40, #42–#49) and #41 are on `main`.
- Merging the bugfix stack: it branches after #42; `fabric.mod.json` GameTest lists and `ProjectSanityTest` conflict (union both sides); #43 deletes `RemoteHeroSkins` while #45 still references it in `ClientSessionStateResetTest` and `assertClientStatesRegisterReset` — whichever merges second must drop those references (`00-overview.md` §3.3).
- Owner decision needed before plan 5 stage `E1`: new root package name (proposed `io.github.grebeshok105.codex`, decision R14).
- Rebuild only the project skills that prove useful for the new workflow.
- Design a new release/versioning workflow after the verification baseline is stable.
- Use the VFX research to decide the Codex 5.0 rendering foundation.

## Next session

1. Read this file, current `AGENTS.md`, `docs/audits/2026-09-25-opus-architecture-audit.md`, and — for architecture work — `docs/design/architecture-migration/00-overview.md` plus the one plan whose stage you execute.
2. Use the preserved audit as the baseline for the Opus-led restoration/fix pass.
3. Re-verify findings while implementing; do not assume subagent-only findings are proven until checked.
4. Keep `qualityGate` green and update this handoff after each substantial batch.
5. After PR #36 merges, delete `devin/1790340168-agents-md-rebuild` and `devin/1790343904-qualitygate`; keep `docs/sync-versions-v4` and `gh-pages`.
