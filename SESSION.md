# SESSION.md

## Active work

- Branch: `main`
- Goal: Codex Superheroes revival / 5.0 foundation work.
- Current phase: repository cleanup, agent workflow restoration, verification baseline, and architectural reacquaintance before new content work.

## Current state

- `AGENTS.md` has been rebuilt around the revival workflow and legacy-modernization rules.
- Repository cleanup removed obsolete Windsurf rules, historical plans/design docs, legacy agent skills, stale addon API docs, and the old release workflow.
- `README.md` and `fabric.mod.json` were refreshed for the revived Codex project.
- Build CI targets Java 21.
- Cross-session continuity is now mandatory through this file.
- The Jujutsu-style `qualityGate` has been adapted to Codex, merged, and is now the canonical verification baseline.
- Independent architecture/bug review and VFX-foundation research may happen in parallel; their findings should be brought back into durable project context before implementation.
- The independent Opus 5.5 architecture/bug audit is preserved at `docs/audits/2026-09-25-opus-architecture-audit.md` and is the current baseline for the restoration rewrite.

## Important decisions

- Existing code defines current behavior, not automatically the desired architecture.
- Legacy systems are preserved when they are healthy; they are replaced only when they create concrete architectural, testing, reliability, or extensibility problems.
- The revival is workflow/foundation-first. New content is not required to complete the initial restoration phase.
- `gradle.properties` is the version source of truth.
- Current release automation is intentionally absent until a new verified release flow is designed.

## Verification

No runtime verification was required for the documentation/repository-cleanup work recorded here.

## Open work

- Establish the architecture/debt map from the independent audit.
- Decide and execute package identity cleanup away from `com.example.superheroes` when the verification baseline is ready.
- Rebuild only the project skills that prove useful for the new workflow.
- Design a new release/versioning workflow after the verification baseline is stable.
- Use the VFX research to decide the Codex 5.0 rendering foundation.

## Next session

1. Read this file, current `AGENTS.md`, and `docs/audits/2026-09-25-opus-architecture-audit.md`.
2. Use the preserved audit as the baseline for the Opus-led restoration/fix pass.
3. Re-verify findings while implementing; do not assume subagent-only findings are proven until checked.
4. Keep `qualityGate` green and update this handoff after each substantial batch.
