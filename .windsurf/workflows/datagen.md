---
description: Запустить data generation (только Fabric/NeoForge)
---

Сгенерируй ресурсы (блокстейты, модели, рецепты, лутаблицы, тэги).

1. Fabric: `./gradlew runDatagen` или `./gradlew runClient --args="--datagen"`
   NeoForge: `./gradlew runData`
// turbo
2. Сгенерированные файлы попадут в `src/generated/resources/`
3. Перенеси нужное в `src/main/resources/` и проверь вручную
