# Repository Guidelines

## Project Structure & Module Organization

Это Fabric-мод `superheroes` для Minecraft `1.21` на Java 21. Текущая база рабочей папки `grebeshok105-v3.12.2`: версия `3.15.1`, package `com.example.superheroes`, official Mojang mappings, Fabric Loader `0.19.2`, Fabric API `0.102.0+1.21`, GeckoLib `4.5.8`.

Главный lifecycle идёт через `SuperheroesMod`: attachments/effects/heroes/abilities/items/network/resources/controllers регистрируются там. Client entrypoint `SuperheroesClient` отвечает за HUD, render, FX, keybinds и client networking. `Heroes.java` регистрирует 14 героев; `AbilityRegistry.java` регистрирует способности; `HeroData` хранит состояние трансформации. Data generation живёт в `src/main/java/com/example/superheroes/datagen/`, результат подключён из `src/main/generated/`. Runtime assets лежат в `src/main/resources/assets/superheroes/`, сырьё пользователя сначала ищи в `art-source/`. Публичный addon API описан в `docs/api.md`.

## Build, Test, and Development Commands

- `./gradlew build --no-daemon -x test` - локальная проверка перед PR; jar появляется в `build/libs/`.
- `./gradlew build` - полный CI-style build, используется `.github/workflows/build.yml`.
- `./gradlew runClient --no-daemon` - запуск dev-клиента.
- `./gradlew runDatagen --no-daemon` - генерация ресурсов в `src/main/generated/`.

Отдельного тестового source set или проектного single-test workflow сейчас нет; не придумывай тестовую команду сверх Gradle-конфигурации.

## Coding Style & Naming Conventions

Форматтер/линтер в репозитории не настроены; держи стиль соседнего Java-кода: tabs, `PascalCase` для классов, `UPPER_SNAKE_CASE` для registry constants, package lower-case. Не делать косметические правки, не добавлять комментарии без запроса, не создавать новые `.md` файлы без явного задания. Для Minecraft/Fabric 1.21 использовать typed `CustomPayload` + `StreamCodec`, не старый `PacketByteBuf`-style networking и не `net.fabricmc.fabric.impl.*`.

Новая ability: `AbilityIds`, `ability/<Name>Ability.java`, регистрация в `AbilityRegistry.init()`, затем `Hero.getAbilities()`. Новый controller: `effect/<Name>Controller.java` и обязательный `<Name>Controller.init()` в `SuperheroesMod.onInitialize()`. Новые runtime-звуки только OGG Vorbis; локализацию обновлять одновременно в `en_us.json` и `ru_ru.json`.

## Testing Guidelines

Перед завершением кода запускай `./gradlew build --no-daemon -x test`; для datagen-изменений дополнительно запускай `./gradlew runDatagen --no-daemon` и проверяй diff в `src/main/generated/`. Избегай правок core-файлов без причины: `Hero.java`, `HeroData.java`, `ResourceController.java`, `HeroTheme.java`, HUD-файлы и landing/fall mixins требуют отдельной осторожности.

## Commit & Pull Request Guidelines

История этой worktree содержит только merge-коммит, поэтому опирайся на локальные project rules: коммиты на английском в conventional-style (`feat(scope): ...`, `fix(scope): ...`), PR description на русском. Release tags имеют формат `vX.Y.Z`; release workflow на `baseline` сам собирает jar и публикует GitHub Release, поэтому не меняй GitHub Actions без отдельного запроса.
