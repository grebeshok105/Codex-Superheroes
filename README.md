# Superheroes Mod

Fabric-мод для Minecraft 1.21 про героев с трансформациями, кастомным HUD, способностями, ресурсами, VFX, боссами и публичным API для аддонов.

## Текущее состояние

- Mod ID: `superheroes`
- Java package: `com.example.superheroes`
- Рабочая версия: `3.14.0` в `gradle.properties`
- База worktree: `main` / feature branches в `Codex-Superheroes`
- Minecraft: `1.21`
- Fabric Loader: `0.19.2`
- Fabric API: `0.102.0+1.21`
- Java target: `21`
- Mappings: official Mojang mappings
- Дополнительно: GeckoLib `4.5.8`

Сейчас в коде `v3.14.0` зарегистрированы 14 героев: Homelander, Iron Man, Regulus, Sung Jin-Woo, Doomsday, Goku, Naruto, Captain America, Kratos, Loki, Thanos, Reinhard van Astrea, Raiden Shogun и Invincible.

## Главные системы

- `Hero` / `HeroData` / `HeroTransformService` — трансформация игрока, passives, размеры, skin sync.
- `Ability` / `AbilityRegistry` / `AbilityRouter` — toggle и active способности.
- `ResourceController` — Energy/Mana с привязками способностей и fallback между ресурсами.
- `ModNetworking` — typed `CustomPayload` C2S/S2C пакеты для Minecraft 1.21.
- `effect/*Controller` — server-side tick-логика героев, ультов, состояний и прогрессии.
- `client/hud`, `client/render`, `client/fx` — HUD, beams, overlays, screen shake и визуальные эффекты.
- `api/*` — стабильный addon API, описан в `docs/api.md`.

## Сборка

```bash
./gradlew build --no-daemon -x test
```

Артефакты появляются в `build/libs/`:

- `superheroes-<version>.jar`
- `superheroes-<version>-sources.jar`

Для запуска dev-клиента:

```bash
./gradlew runClient --no-daemon
```

Для data generation:

```bash
./gradlew runDatagen --no-daemon
```

Сгенерированные ресурсы лежат в `src/main/generated/` и подключены в `build.gradle` как resources source set.

## Навигация

- `AGENTS.md` — главная карта проекта и правила для AI-агентов.
- `.agents/skills/` — узкие workflow-гайды: сборка, релиз, datagen, ассеты, debugging.
- `.windsurf/` — дублирующие правила и workflow для Windsurf.
- `docs/api.md` — публичный addon API.
- `docs/design/` и `docs/plans/` — дизайн-доки и исторические планы; перед доверием сверять с текущим кодом.
- `art-source/` — сырые ассеты пользователя. Runtime-ресурсы должны лежать в `src/main/resources/assets/superheroes/...`.

## Правила разработки

- Работать в этой папке для актуальной Codex-базы: `F:\WorkFLow\TestimCodex\grebeshok105-v3.12.2`.
- Перед кодом читать `AGENTS.md` и `.agents/skills/base-rules/SKILL.md`.
- Не делать косметических правок и не трогать core-файлы без причины.
- Новые runtime-звуки — только OGG Vorbis.
- Новые текстуры/звуки сначала искать в `art-source/`.
- Локализацию обновлять сразу в `en_us.json` и `ru_ru.json`.
- Локальная проверка перед PR: `./gradlew build --no-daemon -x test`.

## License

Проект указывает лицензию `CC0-1.0` в `fabric.mod.json` и содержит файл `LICENSE`.
