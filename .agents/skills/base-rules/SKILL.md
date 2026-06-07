---
name: base-rules
description: Use when starting any work in this repo — base behavior rules from the user about what not to do.
triggers: ["model"]
---

# Base Rules

Жёсткие правила поведения в этом репо.

## Не делать без явного запроса

- НЕ создавать `.md` файлы (README, CHANGELOG, docs) без запроса.
- НЕ комментировать код если не просили.
- НЕ делать косметические правки (форматирование, импорты, переименования) — только по делу.
- НЕ описывать структуру проекта в комментариях к коде.
- НЕ использовать deprecated/internal API Minecraft / Fabric / Loom.
- НЕ менять release/workflow-поведение GitHub Actions без отдельного запроса.

## Ассеты

- Перед тем как рисовать/искать текстуру или звук — проверить `art-source/`.
- Звуки в runtime — только OGG Vorbis. MP3 конвертировать:
  `ffmpeg -i input.mp3 -c:a libvorbis -qscale:a 5 output.ogg`.
- Новые runtime-ассеты класть в `src/main/resources/assets/superheroes/...`, оригиналы — в `art-source/`.
- Если ассет из внешнего источника, положить рядом `source.txt` или `LICENSE.md`.

## Версии / релизы

- Эта рабочая папка основана на `v3.12.2`; текущую версию смотреть в `gradle.properties`.
- Для работы с удачной стабильной базой используй `F:\WorkFLow\TestimCodex\grebeshok105-v3.12.2`, а не `grebeshok105`.
- Релизные теги идут в формате `vX.Y.Z`.
- Если bump версии нужен только для локальной сборки релиза, не коммитить временный bump и после релиза откатить `gradle.properties`.
- Коммиты — на английском, conventional-style: `feat(scope): ...`, `fix(scope): ...`.
- PR description — на русском.

## Коммуникация

- Пользователь пишет на русском → отвечать на русском, тех.термины можно оставлять английскими.
- Коротко и по делу, но с конкретными файлами/командами.
- При завершении задачи дать что изменено и как проверено.
