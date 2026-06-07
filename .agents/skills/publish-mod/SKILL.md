---
name: publish-mod
description: Use when user asks to publish the mod to CurseForge / Modrinth or another external mod platform.
---

# Publish to CurseForge / Modrinth

GitHub releases — см. skill `release-mod`. Этот скилл — про внешние платформы.

## Пререквизиты

- `MODRINTH_TOKEN` для Modrinth, если публикация автоматизирована.
- `CURSEFORGE_API_KEY` для CurseForge, если публикация автоматизирована.
- В текущем `build.gradle` publish plugin для Modrinth/CurseForge не настроен; по умолчанию публикация ручная через web UI.

## Ручная публикация

1. Убедиться, что версия в `gradle.properties` соответствует нужному jar.
2. Собрать:

   ```bash
   ./gradlew build --no-daemon -x test
   ```

3. Взять release jar из `build/libs/` без `-sources`.
4. Modrinth: `https://modrinth.com/mod/<slug>/versions/create`.
5. CurseForge: `https://www.curseforge.com/minecraft/mc-mods/<slug>/upload`.
6. Changelog брать из GitHub release notes.

## Если publish plugin появится

После настройки `me.modmuss50.mod-publish-plugin` или аналогов:

```bash
./gradlew build --no-daemon -x test
./gradlew publishMods --no-daemon
```

При загрузке указывать Minecraft `1.21`, loader `Fabric`, Java `21+`.
