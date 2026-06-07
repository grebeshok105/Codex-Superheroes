---
name: datagen
description: Use when running data generation for blockstates, models, recipes, loot tables, damage types, or tags via DataProviders.
---

# Data Generation

Этот мод использует Fabric DataGen API.

## Команда

Из корня `grebeshok105-v3.12.2`:

```bash
./gradlew runDatagen --no-daemon
```

На Windows:

```powershell
.\gradlew.bat runDatagen --no-daemon
```

## Куда попадают результаты

- `src/main/generated/` — auto-generated resources.
- `build.gradle` подключает эту папку в `sourceSets.main.resources.srcDirs`.
- Сгенерированные файлы коммитятся, если они нужны моду.

Не править generated JSON руками; менять DataProvider и снова запускать datagen.

## Когда нужен datagen

- Новый item model / generated model.
- Новый recipe (`data/<modid>/recipe/...` в 1.21).
- Новый loot table (`data/<modid>/loot_table/...` в 1.21).
- Новый damage type или tag.

## Где провайдеры

`src/main/java/com/example/superheroes/datagen/`:

- `SuperheroesDataGenerator`
- `ModDamageTypeProvider`
- `ModDamageTypeTagProvider`
- `ModItemModelProvider`
- `ModRecipeProvider`

## Workflow

1. Изменить DataProvider.
2. Запустить `./gradlew runDatagen --no-daemon`.
3. Проверить diff в `src/main/generated/`.
4. Запустить build, если изменение влияет на runtime.
