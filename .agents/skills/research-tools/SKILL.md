---
name: research-tools
description: Use before writing non-trivial loader-specific or vanilla-Minecraft-touching code in this repo. Tells which Devin tool to use for which research task.
triggers: ["model"]
---

# Research Tools — какой инструмент когда

Не угадывать API по памяти, особенно для 1.21+. Сначала ресёрч, потом код.

## Vanilla Minecraft исходник
- Декомпилированный jar лежит в `~/.gradle/caches/fabric-loom/minecraftMaven/.../minecraft-common-1.21-loom.mappings.1_21.layered+hash.2198-v2/minecraft-common-...jar`
- `unzip -l <jar>` чтобы найти класс
- `unzip -p <jar> path/to/Class.class > /tmp/X.class && javap /tmp/X.class` — посмотреть сигнатуры методов
- `javap -p` для приватных, `javap -c` для байт-кода

Это быстрее чем тянуть `mcdev` и работает офлайн.

## Поиск в репозитории
- `grep` (builtin) для поиска паттернов в коде проекта
- `find_file_by_name` для glob по путям
- `read` для чтения файлов

## Документация Fabric / NeoForge / Java либ
- `web` action=fetch для конкретной страницы (fabricmc.net/wiki, docs.neoforged.net)
- `web` action=search для поиска по теме
- `devin_docs` (поиск по docs.devin.ai)

## Реальные примеры в open-source модах
- `web` action=search с `domain=github.com` — найти как другие моды решили задачу
- `web` action=fetch для raw файлов с github.com (через `https://raw.githubusercontent.com/...`)

## Звуки / ассеты CC0
- `web` action=search с `domain=freesound.org`, `domain=creazilla.com`, `domain=pixabay.com`
- creazilla.com работает напрямую (CDN не блокирует), pixabay блокирует прямые curl-запросы (403)
- Скачивать через `wget` / `curl`, конвертировать `ffmpeg -i in.mp3 -c:a libvorbis -q:a 5 out.ogg`

## Когда писать сразу
- Опечатка / переименование / тривиальная правка → сразу код, не звать ресёрч
- Что-то уже встречалось в этом проекте → `grep` по проекту и пиши

## Параллелизация
- Независимые ресёрч-запросы делать **параллельно** в одном tool-call блоке
- Зависимые (вывод одного нужен для второго) — последовательно
