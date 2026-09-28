# Reusable Character-Migration Workflow

Spec §17 defines a short repeatable workflow for moving a character's presentation onto the Visual Core after the Homelander pilot. This file keeps the twelve steps verbatim and records which artifact implements each step in this pilot — the same mapping applies to the next migration.

| # | Spec §17 step | Pilot artifact |
|---|---|---|
| 1 | audit the character | Task 1 — `roster-visual-audit.md` (one row per `HeroModules.ALL` hero) |
| 2 | identify which existing core capabilities already cover the character | Task 1 — the capability list at the end of `roster-visual-audit.md` |
| 3 | extend or modify the core when genuinely needed | Tasks 3–7 — VFX payloads, client runtime, Veil backend, pattern layer, animation runtime |
| 4 | place temporary runtime placeholders | Task 2 — placeholder assets + `homelander_placeholders.txt` + `HomelanderPlaceholderGuardTest` |
| 5 | produce passive assets in parallel | the OMP plan (`2026-09-28-homelander-omp-assets.md`), running against `shared-contract.md` + `homelander_pilot.json` |
| 6 | review both sides independently | OMP third-session review (OMP plan Task 10) + Devin fourth-session review (pilot Task 15 Step 2) |
| 7 | integrate | Task 15 Step 3 — merge the approved OMP package over the contract paths, mark manifest lines `# replaced` |
| 8 | test multiplayer | Task 14 Step 1 — the spec §11 observer checklist in `verification.md` |
| 9 | test performance | Task 14 Step 2 — `VfxDebugHud`/`VfxPerfProbe` numbers against the spec §12 target |
| 10 | visually review the final result | Task 15 Steps 5–6 — acceptance scenes (spec §15) recorded as the review package |
| 11 | remove temporary placeholders | Task 15 Step 3 — `finalBuildHasNoPlaceholders` passes with every manifest line `# replaced` |
| 12 | ship the migrated character | Task 15 Steps 7–8 — spec §16 definition-of-done check and the final commit |

Steps 8–9 deliberately reuse placeholder assets (Task 14 runs before OMP integration) so the runtime is verified before final assets exist; Task 15 Step 5 re-runs them with finals. Nothing here requires a new architecture plan per hero — the next migration repeats this table against its own contract row set.
