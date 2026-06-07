---
name: build-mod
description: Use when user asks to build the mod, compile, check for errors, run Gradle, or produce a .jar. Run before opening a PR.
---

# Build Mod

## Пререквизит

Проект компилируется с Java target 21. Используй установленный JDK 21+ и убедись, что Gradle видит его:

```bash
java -version
```

В Codex Desktop на Windows обычно достаточно системного `java`. В Linux/VM, если JDK 21 лежит отдельно, выставь `JAVA_HOME` перед Gradle.

## Команды

Запускать из корня `grebeshok105-v3.12.2`:

```bash
./gradlew compileJava --no-daemon
./gradlew build --no-daemon -x test
./gradlew build --no-daemon
```

На Windows можно использовать:

```powershell
.\gradlew.bat build --no-daemon -x test
```

`--no-daemon` оставляем по умолчанию: меньше сюрпризов с памятью и stale Gradle daemon.

## Артефакты

После успешного `build`:

- `build/libs/superheroes-3.15.0.jar` — release jar для этой базы
- `build/libs/superheroes-3.15.0-sources.jar` — sources

Версия берётся из `gradle.properties: mod_version=...`.

## Типовые ошибки

- `Unsupported Java version` → Gradle использует не тот JDK.
- `Could not resolve net.fabricmc.fabric-api:fabric-api` → сеть/кеш зависимостей; после восстановления сети можно запустить с `--refresh-dependencies`.
- `cannot find symbol` на Minecraft API → проверить Mojang mappings / MC 1.21 сигнатуры через `research-tools` или декомпилированный jar.
