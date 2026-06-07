---
name: release-mod
description: Use when user asks to release the mod, create a GitHub release, tag a version, or publish a GitHub build. Triggered by "релиз", "release", "тег".
---

# Release Mod

GitHub release с прикреплённым jar. Работай из актуальной Codex-базы и сверяй версию с `gradle.properties`.

## Шаги

1. Убедиться, что ты в worktree:

```bash
git status --short --branch
```

Ожидаемо: `main` или feature branch, созданная от актуального `origin/main`.

2. Если нужна новая версия, обновить `gradle.properties`:

```properties
mod_version=X.Y.Z
```

Не коммитить временный bump, если пользователь отдельно не попросил version commit.

3. Собрать:

```bash
./gradlew build --no-daemon -x test
```

4. Создать GitHub release:

```bash
gh release create vX.Y.Z build/libs/superheroes-X.Y.Z.jar \
  --target <branch-or-commit> \
  --title "vX.Y.Z: <kind>: <summary>" \
  --notes "<markdown notes>"
```

Notes писать на русском: что вошло, важные фиксы, совместимость, если есть breaking changes.

5. Если bump был только локальный:

```bash
git checkout -- gradle.properties
```

## Naming

- Теги: `vX.Y.Z`.
- Если это hotfix/fork от старой версии, заранее согласовать номер с пользователем.
- PR/commit style: `feat(scope): ...` или `fix(scope): ...`.
