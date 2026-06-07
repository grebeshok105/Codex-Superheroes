---
name: add-block
description: Use when adding a new block to the mod. Full pipeline including registration, model, blockstate, texture, loot table.
---

# Add Block

Полный pipeline добавления блока в Fabric 1.21.

1. **Класс блока** в `src/main/java/com/example/superheroes/block/<Name>Block.java`
   - extends `Block` (или `BaseEntityBlock` если есть BE)

2. **Регистрация в `ModBlocks`**
   ```java
   public static final Block FOO = register("foo", new FooBlock(BlockBehaviour.Properties.of()...));
   ```
   Если нужен предмет — также `BlockItem` через `ModItems`.

3. **Модель блока**: `src/main/resources/assets/superheroes/models/block/<name>.json` или datagen

4. **Модель предмета** (если есть BlockItem): `models/item/<name>.json` — обычно `{"parent": "superheroes:block/<name>"}`

5. **Blockstate**: `src/main/resources/assets/superheroes/blockstates/<name>.json` или datagen

6. **Текстура**: `src/main/resources/assets/superheroes/textures/block/<name>.png` (16x16, sides если multi-face)

7. **Локализация**: `src/main/resources/assets/superheroes/lang/en_us.json` + `ru_ru.json`
   - Ключ: `"block.superheroes.<name>": "Foo"`

8. **Loot table** (если drops != self, MC 1.21): `src/main/resources/data/superheroes/loot_table/blocks/<name>.json`

9. **Recipe** (если crafting, MC 1.21): `src/main/resources/data/superheroes/recipe/<name>.json`

10. **Item group**: добавить в `ModItemGroups` если нужно показать в creative

11. **Build & verify**: skill `build-mod`
