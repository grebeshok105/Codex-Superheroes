---
name: add-item
description: Use when adding a new item to the mod (including consumables, transformation items, mana refills).
---

# Add Item

1. **Класс предмета** в `src/main/java/com/example/superheroes/item/<Name>Item.java`
   - extends `Item` (или `TransformationItem` для смены героя, или специальные базы из `item/`)

2. **Регистрация в `ModItems`**
   ```java
   public static final Item FOO = register("foo", new FooItem(new Item.Properties()...));
   ```

3. **Текстура**: `src/main/resources/assets/superheroes/textures/item/<name>.png` (обычно 16x16/32x32)

4. **Модель**: `src/main/resources/assets/superheroes/models/item/<name>.json` или datagen через `ModItemModelProvider`
   ```json
   {"parent": "minecraft:item/generated", "textures": {"layer0": "superheroes:item/<name>"}}
   ```

5. **Локализация**: `src/main/resources/assets/superheroes/lang/en_us.json` + `ru_ru.json`
   - Ключ: `"item.superheroes.<name>": "Foo"`

6. **Recipe** (опционально, MC 1.21): `src/main/resources/data/superheroes/recipe/<name>.json` или datagen

7. **Item group**: `ModItemGroups` если нужно в creative tab

8. **Build & verify**: skill `build-mod`

## Специфические подвиды

- **Transformation item** (даёт героя при right-click): расширить `TransformationItem` или скопировать паттерн из `IronManSuitItem`/`HomelanderSuitItem`
- **Mana refill** (восполняет ману): смотреть паттерн `CompoundVItem` — обновить `HeroData.withMana(...)`, `setAttached(...)`, затем `ModNetworking.syncResources(...)`
