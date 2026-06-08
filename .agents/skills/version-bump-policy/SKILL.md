---
name: version-bump-policy
description: Use when choosing the next Superheroes mod version, preparing a release version, editing mod_version, release tags, PR notes, or release plans, or interpreting wording such as hotfix, small update, крупное обновление, or глобальный релиз.
---

# Version Bump Policy

Use this skill to choose the next mod version from the user's release intent. Read the current version from `gradle.properties` unless the user provides a different explicit base version.

## Algorithm

1. Identify the release class from the user's wording and the scope of changes:
   - **Patch bump**: hotfix, bugfix-only release, compatibility fix, balance correction, small update, minor small update, or narrow polish.
   - **Minor bump**: крупное обновление, new hero, several new abilities, meaningful gameplay expansion, new systems, or a release that should feel like the next regular feature version.
   - **Major bump**: глобальный релиз, major release, breaking public API or save/data expectations, large milestone, or explicit request for next major.
2. Normalize the current version:
   - Treat `X.Y` as `X.Y.0` for calculations.
   - Keep output concise as requested by the user; when updating `mod_version`, use the repository's existing format.
3. Calculate the next version:
   - Patch: `X.Y.Z -> X.Y.(Z+1)`.
   - Minor: `X.Y.Z -> X.(Y+1).0`.
   - Major: `X.Y.Z -> (X+1).0.0`.
4. If the user gives a base version without a patch number, preserve the style in explanations:
   - Patch example: `3.14 -> 3.14.1`.
   - Minor example: `3.14 -> 3.15`.
   - Major example: `3.14 -> 4.0`.
5. If the wording is ambiguous, ask one short clarification before changing files. If the user already gave enough context, decide and state the reason.

## Examples

| User intent | Base version | Next version | Reason |
| --- | --- | --- | --- |
| "hotfix crash on startup" | `3.14` | `3.14.1` | Hotfix is a patch bump. |
| "minor small update with balance tweaks" | `3.14` | `3.14.1` | User policy maps small/minor-small updates to patch. |
| "small update: fix lang and one cooldown" | `3.15.1` | `3.15.2` | Narrow polish stays patch. |
| "крупное обновление с новым героем" | `3.14` | `3.15` | Large feature update is minor. |
| "add several abilities and systems for release" | `3.15.1` | `3.16.0` | Meaningful expansion is minor. |
| "глобальный релиз" | `3.14` | `4.0` | Global release moves to next major `.0`. |
| "major release from 3.15.1" | `3.15.1` | `4.0.0` | Major bump resets minor and patch. |

## Common Mistakes

- Do not treat the phrase "minor small update" as a minor version bump; in this repository policy it means patch.
- Do not bump `3.14` to `3.15` for a hotfix or small update; use `3.14.1`.
- Do not bump `3.15.1` to `3.15.2` for a крупное обновление; use `3.16.0`.
- Do not use `4.1` or `4.0.1` for a global release from any `3.x` version; use the next major `.0`.
- Do not edit `gradle.properties` or create tags unless the user asked to perform the version change or release action.
- Do not invent a version source; check `gradle.properties` for the current mod version when working in the repo.
