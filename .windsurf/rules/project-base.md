---
trigger: always_on
---

# Base Rules

- Не создавать `.md` без запроса (README, CHANGELOG, docs)
- Не комментировать код если не просили
- Не править файлы косметически — только по делу
- Не использовать deprecated API Minecraft / Fabric / NeoForge
- Структуру проекта не описывать в rules — модель видит её через filesystem MCP

## Ассеты

- Перед созданием новой текстуры/звука — **проверить `art-source/`** на корне репа. Там лежат сырые материалы от пользователя (текстуры, FX, модели, звуки). Подробнее — `.agents/skills/art-source/SKILL.md`.
- Runtime звуки: только OGG Vorbis. MP3 → `ffmpeg -i in.mp3 -c:a libvorbis -qscale:a 5 out.ogg`.
- Сырые файлы в `art-source/`, рантайм-ресурсы в `src/main/resources/assets/superheroes/...`.
