---
name: debug-crash
description: Use when user shares a crash report, stacktrace, latest.log, or asks to debug a runtime crash of the mod.
---

# Debug Crash

## 1. Найти источник

Типовые места:

```bash
run/crash-reports/
run/logs/latest.log
```

Если пользователь дал текст — работать с ним напрямую. Если файл лежит в workspace, читать конкретный файл.

## 2. Найти первопричину

Искать в порядке:

- Последний `Caused by:` в stacktrace.
- `Exception in thread`.
- Строки `at com.example.superheroes.*`.
- Mixin error chain (`MixinTransformerError`, `InjectionError`, `InvalidMixinException`).

## 3. Классифицировать

- **Наш код** — открыть указанную строку, исправить точечно.
- **Чужой мод** — проверить compatibility / conflict и предложить workaround.
- **Vanilla / mappings** — проверить API для Minecraft 1.21 и Mojang mappings.
- **Networking side** — проверить typed payload registration и client/server receiver.
- **Attachment / codec** — проверить `HeroData.CODEC` и persistent attachment migration.

## 4. Типовые краши в этом проекте

- `Receiving network packet on wrong side` → payload/receiver зарегистрирован или вызван не на той стороне.
- `AttachmentSerializer ...` / codec errors → изменили persistent attachment без миграции.
- `NoSuchMethodError` / `NoSuchFieldError` → API/mappings drift или несовместимая зависимость.
- Mixin `defaultRequire` failure → target method changed или injection больше не совпадает.

## 5. Fix-flow

1. Точечный фикс, без рефакторинга.
2. Если затрагивается core или много файлов — сначала согласовать.
3. Проверить `./gradlew build --no-daemon -x test`.
