---
name: mod-change-report
description: Use when preparing a user-facing report of recent Superheroes mod changes, especially when the agent must separate old/base changes from new work, compare current branch/PR against GitHub main, summarize gameplay impact, and produce an in-game verification checklist.
---

# Mod Change Report

Use this skill to explain what changed without mixing together the base branch, the current branch, local uncommitted edits, and the PR diff. The report should help the user understand the real gameplay delta, not just a list of touched files.

## Establish the Comparison Base

Run these checks before writing the report:

```bash
git status --short --branch
git branch --show-current
git log --oneline --decorate --graph --max-count=30
git tag --sort=-version:refname
```

Also read the current mod version from `gradle.properties`.

If there is a GitHub PR, use it as the source of PR truth:

```bash
gh pr view --json url,state,baseRefName,headRefName,commits,files
```

If `gh` is unavailable, say so and fall back to local refs. Do not pretend the local branch is the PR.

Fetch the comparison ref when possible:

```bash
git fetch origin main --tags
git merge-base origin/main HEAD
git diff --stat <merge-base>..HEAD
git diff --name-status <merge-base>..HEAD
```

If the PR base is not `main`, report both facts explicitly:

- `PR base`: the branch from `gh pr view`.
- `GitHub main comparison`: `origin/main`.
- `Actual diff range used`: `<merge-base>..HEAD`.

Never use local `main`, current branch, or the latest tag as interchangeable bases. Tags only help identify release/version context; they are not automatically the diff base.

## Separate Old, New, and PR Changes

Classify findings into these buckets:

- **Already in base**: behavior, files, assets, or registrations present at the merge-base or in `origin/main`. Verify with `git show <base-ref>:<path>` or by checking that a hunk is absent from `git diff <merge-base>..HEAD`.
- **Added by recent commits**: commits and hunks present in `git log <merge-base>..HEAD` and `git diff <merge-base>..HEAD`. If the user asks for "last N commits", use `git diff HEAD~N..HEAD` and name that narrower range.
- **Local uncommitted changes**: items from `git status --short` and `git diff`. Keep them separate from committed PR work.
- **PR relative to GitHub main**: files and commits from `gh pr view` plus local confirmation with `git diff <merge-base>..HEAD`. If GitHub and local refs disagree, state the mismatch instead of blending them.

For changed files, inspect enough context to avoid false attribution:

```bash
git diff <merge-base>..HEAD -- <path>
git log --oneline -- <path>
git show <base-ref>:<path>
```

When summarizing features, say whether each one is new in the current branch, pre-existing in base, or only local/uncommitted.

## Report Format

Write the response in Russian unless the user asked otherwise. Keep it user-facing and grouped by gameplay systems.

Use this structure:

1. **Сравнение**: branch, base ref, merge-base commit, PR URL/base if available, mod version, and whether uncommitted changes were included.
2. **Что изменилось по системам**: group globally, for example heroes/abilities, combat and balance, progression/resources, movement/flight, PvP, rendering/HUD/FX, data/assets/lang, networking/API.
3. **Что уже было в базе**: mention only meaningful items that could be confused with new work.
4. **Краткая техчасть**: important touched modules, registries, data generation/assets, networking, mixins, controllers, or public API changes.
5. **Риски / bug zones**: likely regression zones and why they matter.
6. **Verification evidence**: exact commands or manual checks that were actually run, with outcomes. If something was not run, say `не запускалось`.
7. **Игровой чеклист**: system-level checks, not button-by-button instructions.

Avoid tiny checklist items like "press the ability key", "open inventory", or "click button". The checklist should validate systems and balance at the level the player/mod owner cares about.

## In-Game Checklist Guidance

Write checklist items as broad gameplay validations, such as:

- проверить баланс Kratos rage across normal combat, boss fights, and resource recovery;
- проверить tier progression Doomsday from early transformation through peak scaling;
- проверить PvP Reinhard against burst, crowd-control, and armor-heavy opponents;
- проверить flight regression у всех летающих heroes, including takeoff, landing, sprint flight, fall damage, and server/client sync;
- проверить shared resource systems after switching heroes and relogging;
- проверить HUD/resource display consistency during transformation, death, dimension change, and respawn;
- проверить multiplayer sync for abilities with cooldowns, projectiles, beams, summons, or persistent effects;
- проверить compatibility of new assets/lang/data with generated resources and existing runtime assets.

Prefer "what system can break?" over "what key should I press?".

## Guardrails

- Do not report a feature as new until the diff proves it is new relative to the selected base.
- Do not combine unrelated branches, old release notes, or historical features into the latest-change report.
- Do not hide dirty working tree state; either include it as local-only or exclude it explicitly.
- Do not claim verification that was not run.
- Do not over-focus on file counts. The user needs gameplay meaning first, technical detail second.
