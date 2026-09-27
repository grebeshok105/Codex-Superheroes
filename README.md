# Codex Superheroes

Codex Superheroes — Fabric-мод для Minecraft 1.21.1 про супергероев, способности и зрелищные боевые системы. Проект объединяет трансформации, уникальные наборы способностей, полёт, HUD, VFX, кастомные модели и серверную игровую логику.

Сейчас проект проходит полное воскрешение и архитектурную модернизацию: старые системы постепенно приводятся к более чистым границам, тестируемой архитектуре и современной VFX-базе.

## Стек

- Minecraft 1.21.1
- Fabric
- Java 21
- official Mojang mappings
- Fabric API
- GeckoLib
- Veil как опциональная VFX/render-зависимость

Точные версии всегда смотри в `gradle.properties` и `build.gradle`.

## Что уже есть

Актуальный список игровых героев определяется кодом в:

`src/main/java/io/github/grebeshok105/codex/hero/Heroes.java`

Основные системы проекта:

- `Hero`, `HeroData`, `HeroTransformService` — трансформация и состояние героя
- `Ability`, `AbilityRegistry`, `AbilityRouter` — способности и их выполнение
- `ResourceController` — Energy / Mana
- `effect/*Controller` — server-side runtime логика героев и механик
- `client/hud`, `client/render`, `client/fx` — интерфейс, рендер и визуальные эффекты
- `network` — typed Fabric networking
- `physics` — общая физика и движение
- `art-source/` — исходники пользовательских ассетов

Существующие пути описывают текущую реализацию и могут меняться по мере архитектурной модернизации.

## Сборка

Полная проверка:

```bash
./gradlew build --no-daemon
```

Dev-клиент:

```bash
./gradlew runClient --no-daemon
```

Datagen:

```bash
./gradlew runDatagen --no-daemon
```

Готовые jar-файлы появляются в `build/libs/`.

## Разработка

Главные правила разработки и контракт для AI-агентов находятся в `AGENTS.md`.

Ключевой принцип текущего этапа проекта: legacy-код сохраняет ценное поведение, но не считается автоматически правильной архитектурой. Старые решения можно и нужно заменять, когда они создают лишнюю связанность, дублирование, плохую тестируемость или мешают развитию проекта.

Новые изменения должны сопровождаться тестами там, где поведение можно проверить автоматически. Финальный gate перед PR — `./gradlew build --no-daemon`. Runtime-изменения дополнительно проверяются в игре.

## Структура репозитория

- `src/main/java/` — общая и server-side логика
- `src/client/java/` — client-only код
- `src/main/resources/` — runtime-ресурсы
- `src/main/generated/` — datagen output, вручную не редактируется
- `src/test/java/` — JUnit-тесты
- `art-source/` — исходные модели, текстуры, звуки и другие рабочие ассеты
- `.agents/skills/` — актуальные специализированные процедуры для агентов, когда они существуют

## License

См. `LICENSE`.
