---
name: mod-build-jar
description: Use when building Superheroes mod release jars, preparing build/libs/superheroes-*.jar, or publishing/uploading jar assets to GitHub Releases with gh.
---

# Mod Build Jar

Build the Fabric mod jar from the current `mod_version` and publish it to GitHub Releases with `gh` only after verifying the workspace, artifact, tag, and release state.

## Ground Rules

- Work from `F:\WorkFLow\TestimCodex\grebeshok105-v3.12.2` on Windows unless the user explicitly gives another repo root.
- Do not make a temporary `mod_version` bump unless the user explicitly asks. If choosing the next version is part of the task, use `version-bump-policy` first.
- Release tags are `vX.Y.Z`, where `X.Y.Z` comes from `gradle.properties`.
- Do not force push tags or branches. Do not delete releases, tags, or assets without explicit user approval.
- Commit messages are English conventional-style, for example `chore(release): prepare v3.15.1`.
- Release notes and PR descriptions are in Russian.
- Never stage unrelated changes. If the worktree contains user changes outside the release scope, leave them alone and ask before touching them.

## Preflight

Run these from the repo root:

```powershell
java -version
git status --short --branch
git branch --show-current
git remote -v
gh auth status
gh repo view --json nameWithOwner,url,defaultBranchRef
```

Stop and report the blocker if Java is not JDK 21+, `gh` is not authenticated, the GitHub remote is unclear, or the branch/status does not match the user's release intent.

Read the version and derive the jar/tag:

```powershell
$version = (Select-String -Path gradle.properties -Pattern '^mod_version=').Line.Split('=', 2)[1].Trim()
$tag = "v$version"
$jar = "build\libs\superheroes-$version.jar"
"version=$version tag=$tag jar=$jar"
```

If the user asks for a different release version, confirm whether `gradle.properties` should be changed and committed before building.

## Build

Use the quick jar command when the user asks for a fast/local jar:

```powershell
.\gradlew.bat build --no-daemon -x test
```

Use the full build for release-quality verification or when the user asks for a full/CI-style build:

```powershell
.\gradlew.bat build --no-daemon
```

After a successful build, verify the artifact is the release jar, not the sources jar:

```powershell
$file = Get-Item -LiteralPath $jar
$file | Select-Object FullName, Length, LastWriteTime
if ($file.Length -le 0) { throw "Jar is empty: $jar" }
```

Check that `LastWriteTime` matches the build you just ran. If the jar is missing, stale, tiny, or named for a different version, stop and fix the build/version mismatch before publishing.

## Git State Before Publishing

Re-check the worktree after building:

```powershell
git status --short --branch
git diff --stat
git rev-parse HEAD
```

If release prep changed files and the user wants those changes included:

```powershell
git add <only-release-files>
git commit -m "chore(release): prepare $tag"
git push -u origin HEAD
```

If no commit is needed, publish from the current `HEAD`. If there are unrelated dirty files, do not commit or revert them; either publish from the intended clean commit or ask the user how to proceed.

## Check Tag And Release

Fetch tags before deciding what exists:

```powershell
git fetch --tags --prune
git tag --list $tag
git ls-remote --tags origin $tag
gh release view $tag --json tagName,name,isDraft,isPrerelease,assets
```

Interpretation:

- If `gh release view` succeeds, the release already exists. Upload the jar as an asset only after checking existing asset names.
- If the tag exists but the release does not, create the GitHub release for that existing tag.
- If neither tag nor release exists, `gh release create` may create the tag at the selected target.
- If the local tag and remote tag point at different commits, stop. Do not force push or retarget without explicit approval.

Check existing assets before uploading:

```powershell
gh release view $tag --json assets --jq '.assets[].name'
```

If `superheroes-$version.jar` already exists, ask before overwriting. Use `--clobber` only when the user explicitly approves replacing the asset.

## Create Or Update The GitHub Release

Write release notes in Russian. For longer notes, keep the file in a temp path rather than adding repo documentation:

```powershell
$notesFile = Join-Path $env:TEMP "$tag-release-notes.md"
notepad $notesFile
```

Create a new release when no release exists:

```powershell
gh release create $tag $jar `
  --target HEAD `
  --title $tag `
  --notes-file $notesFile
```

Upload the jar to an existing release when the release already exists and the asset is not present:

```powershell
gh release upload $tag $jar
```

If the user approved replacing an existing asset:

```powershell
gh release upload $tag $jar --clobber
```

After publishing, verify the release:

```powershell
gh release view $tag --json url,tagName,name,assets --jq '{url, tagName, name, assets: [.assets[].name]}'
git status --short --branch
```

Report the release URL, jar path, jar size, build command used, commit/tag target, and any remaining uncommitted files.

## Common Mistakes

- Do not publish `*-sources.jar` instead of `superheroes-$version.jar`.
- Do not infer a new version from the request wording without `version-bump-policy`.
- Do not create a release from a dirty worktree unless the user explicitly accepts the exact target commit and leftover changes.
- Do not use `git push --force`, `git tag -f`, `gh release delete`, or asset replacement without explicit approval.
