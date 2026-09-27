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

Актуальный список игровых героев — один модуль на героя в:

`src/main/java/io/github/grebeshok105/codex/bootstrap/HeroModules.java`

Каждый герой — пара модулей: `hero/<id>/<Id>Module` (server) и `client/hero/<id>/<Id>ClientModule` (клиент). Контракт героя — `core/hero/Hero`, способность — `core/ability/Ability`.

Основные системы проекта:

- `core/` — контракты и shared-слои: `hero/` (`Hero`, `Heroes`), `ability/` (`Ability`, `AbilityRegistry`, `AbilityRouter`), `resource/` (Energy/Mana, `ResourceController`), `transform/` (`HeroData`, `HeroTransformService`), `lifecycle/` (`HeroTickDispatcher`, `PlayerLifecycle`, `HeroLifecycle`, `OwnedSessionMap`), `net/` (typed Fabric networking — `PayloadRegistrar`, `C2SGuards`, `FxBroadcast`), `attachment/`, `content/`, `module/` (контракты модулей)
- `mechanic/` — герой-агностические механики: flight, impact, motion, targeting, boundweapon, charge, strike, summon, falls, world, общие способности в `mechanic/ability`
- `hero/<id>/` — server-модуль героя: `<Id>Hero`, `ability/`, `runtime/`, `item/`, `entity/`, `net/`, `sound/`, `registry/`
- `content/` — контент без героя (`ContentModule`: horde, boss, admin, command)
- `compat/` — мосты к другим модам
- `bootstrap/` — composition roots (`HeroModules`, `ContentModules`, `SharedAbilities`, `SharedMechanics`)
- `client/core/` — общая клиентская инфраструктура (HUD-фреймворк, input, render, audio, text, mixin) и `client/hero/<id>/` — клиентский модуль героя
- `art-source/` — исходники пользовательских ассетов

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

Главные правила разработки и контракт для AI-агентов находятся в `AGENTS.md`. Как добавить нового героя — `.agents/skills/add-hero/`.

Архитектура модульная: `core` ← `mechanic` ← `hero`/`content`/`compat` ← `bootstrap`. Направление зависимостей проверяет ArchUnit (`src/test/java/io/github/grebeshok105/codex/architecture/`) — правила в `docs/design/architecture-migration/00-overview.md` §5.2.

**Граница Veil:** Veil — client-only зависимость. `veil.*` референсится только из `src/client` (`client/hero/<id>/fx/`, `client/core/**`), всегда за `FabricLoader.isModLoaded("veil")`. Shared/server код никогда не импортит Veil: выделенный сервер его не несёт (Veil требует client-only модули Fabric), поэтому в `fabric.mod.json` он остаётся `recommends` даже после полного порта VFX на Veil, а source set `servernoveil` + run `runServer` и classpath'ы gametest/datagen фильтруют jar из classpath. Серверный код достаёт Veil-эффекты только через payload'ы `core/net`/`FxBroadcast`.

Новые изменения должны сопровождаться тестами там, где поведение можно проверить автоматически. Финальный gate перед PR — `./gradlew qualityGate --no-daemon`. Runtime-изменения дополнительно проверяются в игре.

## Структура репозитория

- `src/main/java/` — общая и server-side логика
- `src/client/java/` — client-only код
- `src/main/resources/` — runtime-ресурсы
- `src/main/generated/` — datagen output, вручную не редактируется
- `src/test/java/` — JUnit-тесты (включая ArchUnit-правила архитектуры)
- `src/gametest/` — headless server GameTests (`runGametest`) и golden-файлы
- `art-source/` — исходные модели, текстуры, звуки и другие рабочие ассеты
- `.agents/skills/` — актуальные специализированные процедуры для агентов, когда они существуют

## License

См. `LICENSE`.
