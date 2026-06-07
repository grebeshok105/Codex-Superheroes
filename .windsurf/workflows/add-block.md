---
description: Добавить новый блок в мод
---

Добавь блок в мод с полным pipeline.

1. Создай класс блока в `src/main/java/.../block/`
2. Зарегистрируй в `ModBlocks`, привяжи `BlockItem` если нужен предмет
3. Создай модели:
   - Блок: `assets/<modid>/models/block/<name>.json`
   - Предмет: `assets/<modid>/models/item/<name>.json` (обычно parent к blockmodel)
4. Создай текстуру(и) в `assets/<modid>/textures/block/`
5. Добавь blockstate `assets/<modid>/blockstates/<name>.json`
6. Добавь перевод в `assets/<modid>/lang/en_us.json`
7. Если нужен loot-table — `data/<modid>/loot_table/blocks/<name>.json`
8. Если нужен рецепт — `data/<modid>/recipes/<name>.json`
9. Проверь билд: запусти `/build`
