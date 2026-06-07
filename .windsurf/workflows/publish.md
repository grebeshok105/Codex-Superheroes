---
description: Опубликовать мод на CurseForge и/или Modrinth
---

Подготовь релиз и загрузи.

1. Проверь версию в `gradle.properties` — `mod_version` корректен
2. Проверь `CHANGELOG.md` (или `changelog.md`) на актуальность
3. Собери: запусти `/build`
4. Проверь `build/libs/` — файл без `-sources` и `-dev` — это release jar
5. Если настроен publish plugin (Minotaur / CurseGradle):
   `./gradlew publish` или `./gradlew publishMods`
// turbo
6. Если ручная загрузка — предоставь шаги для Modrinth / CurseForge web UI
