---
description: Добавить новый предмет в мод
---

Добавь предмет в мод с полным pipeline.

1. Создай класс предмета в `src/main/java/.../item/`
2. Зарегистрируй в `ModItems` (Fabric: `Registry.register(...)` / NeoForge: `ITEMS.register(...)`)
3. Создай текстуру `assets/<modid>/textures/item/<name>.png` (placeholder или подсказку)
4. Создай модель `assets/<modid>/models/item/<name>.json`
5. Добавь перевод `assets/<modid>/lang/en_us.json`
6. Если нужен рецепт — добавь в `data/<modid>/recipes/<name>.json`
7. Проверь билд: запусти `/build`
