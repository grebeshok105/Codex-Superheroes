---
name: worker-subagent-dispatch
description: Use when a task has 2+ independent coding, research, or review parts, when the user asks for subagents/workers/parallel agents, or when work can be split into disjoint write scopes.
---

# Worker Subagent Dispatch

Use this skill to split independent work across subagents while the main agent owns integration and verification.

## Trigger Check

Dispatch subagents when at least one is true:

- The task has 2+ independent coding, research, or review parts.
- The user explicitly asks for subagents, workers, parallel agents, or delegation.
- Different files or modules can be changed without overlapping write scopes.
- Research, implementation, and review can happen in parallel without shared mutable state.

Do not dispatch when the task is tiny, sequential, centered on the same files, or has ambiguous write scope.

## Workflow

1. Identify independent units of work and the exact read/write scope for each.
2. Reserve write scopes before dispatch. No two workers may write the same file, directory, generated output, registry section, or migration surface.
3. Dispatch up to 10 subagents concurrently. Prefer fewer clear workers over many vague ones.
4. Track assignments, receive results, resolve integration order, and make final edits if needed.
5. Treat subagent output as a patch proposal, not as completed work.
6. Verify the combined result with the repo-appropriate build, tests, lint, datagen, or targeted checks.
7. Report what each subagent did, what was integrated, what was rejected or adjusted, and what verification ran.

## Subagent Roles

### Explorer

Use explorer subagents for read-only discovery:

- Map existing patterns, APIs, call chains, or similar implementations.
- Find files, assets, generated data, configs, and risky ownership boundaries.
- Return concise findings with file paths, line references, and recommended write scopes.

Explorer rules:

- Read-only only.
- No speculative refactors.
- No final conclusions without file evidence.

### Worker

Use worker subagents for real implementation only when each worker has a disjoint write scope.

Worker prompt must include:

- Goal and acceptance criteria.
- Exact allowed write paths.
- Explicit forbidden paths.
- Existing patterns to follow.
- Expected output: changed files, summary, verification attempted, and risks.

Worker rules:

- One worker owns one non-overlapping write set.
- Workers may read broadly but write narrowly.
- Workers must not run destructive git commands or revert unrelated changes.
- Workers must stop and report if they need to edit outside their assigned write scope.
- Workers should avoid broad formatting and unrelated cleanup.

### Reviewer

Use reviewer subagents after workers finish or when independent review can run on a stable diff.

Reviewer rules:

- Review correctness, integration risk, missing tests, style drift, and scope violations.
- Lead with concrete findings and file references.
- Do not edit files unless explicitly assigned a separate disjoint write scope.
- Distinguish confirmed bugs from questions or preferences.

## Main-Agent Integration Rules

- Keep a visible task map: subagent name, role, write scope, status, and returned files.
- Before applying or accepting worker changes, inspect the diff yourself.
- Resolve conflicts in the main session; do not let workers race over shared files.
- If two tasks unexpectedly need the same file, serialize them and make one agent read-only.
- Re-run verification after integration, even if workers ran partial checks.
- If verification fails, debug from the integrated state and either patch locally or redispatch a narrow worker.

## Common Mistakes

- Dispatching workers with vague goals like "fix the feature" instead of exact write scopes.
- Letting two workers edit the same registry, config, generated file, localization file, or shared helper.
- Treating explorer findings as implementation.
- Accepting worker output without inspecting the diff.
- Skipping integration verification because each subagent reported success.
- Creating more subagents than the dependency graph can support.
- Delegating final architectural judgment to subagents instead of owning it in the main session.
- Forgetting to report rejected, adjusted, or unintegrated worker results.
