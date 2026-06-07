# План v2.2.1-pre — фиксы Кэпа, темы, языки, тултип

## Скоуп

- НЕ трогать Регулуса (heartbeat остаётся как есть после прошлого PR).
- Один PR на всё.
- После релиза v2.2.1-pre с уже мерджнутыми фиксами Думсдея/Регулуса.

---

## 1. Кэп Америка

### a) Лишний текст `passive.3`
Не Кэп-специфично — у новых героев (goku/naruto/cap) lang-ключи пронумерованы с 0 (`passive.0/1/2`), а `AbilityDescriptions.passiveCount=3` лоадит их с индексом 1. Фикс: переименовать lang-ключи на `passive.1/2/3` (как у старых героев) для **goku/naruto/cap** в обеих локалях `en_us` + `ru_ru`.

### b) Текстура щита криво в руке
Сейчас `vibranium_shield.json` имеет `parent: minecraft:item/handheld` без явного `display`-блока, поэтому щит в offhand клипает в тело. Фикс: добавить `display`-блок с `rotation/translation/scale` для:
- `firstperson_lefthand`, `firstperson_righthand`
- `thirdperson_lefthand`, `thirdperson_righthand`
- `gui`, `ground`, `fixed`

### c) Кидаться реальным щитом
В `ShieldProjectileRenderer` уже `new ItemStack(ModItems.VIBRANIUM_SHIELD)`, но `ItemDisplayContext.GROUND` укладывает 3D-модель плоско. Поменять на `FIXED` + добавить корректный `fixed` в display.

### d) Шоквейв при прыжке
**Решение пользователя:** ТОЛЬКО при способности `cap_shield_slam` (Z), не при обычном приземлении. Сейчас способность не наносит AoE при приземлении — это баг. Проверить `CapShieldSlamAbility.execute(...)`: AoE-урон должен срабатывать при касании земли после прыжка-слэма. Добавить hook через `HeroLandingTracker` или в самой Ability per-tick на `onGround()`. Радиус 5 блоков, knockback, шоквейв-частицы.

### e) HUD синий
Добавить `HeroTheme.CAPTAIN_AMERICA` (синий основной + красный акцент + белая подсветка) и привязать через `Heroes.themeFor(id)`.

### f) Сытость 100%
**Решение пользователя:** не сломает баланс. Реализовать пер-герой override в `AutoSaturationController`: для Кэпа `food=20, saturation=20` → постоянная natural regen. Остальные герои остаются на `food=17, saturation=0` как раньше.

---

## 2. Все герои — нечитаемый раскрытый help

При двойном `H` тултип раскрывается, но описания (`ability.<id>.desc`) обрезаются.

**Фикс:**
- `AbilitiesTooltipHud.PANEL_WIDTH` 260 → 320.
- В `drawAbilityRow` для длинных описаний делать **wrap** в 2 строки через `Font.split(Component, maxWidth)`. `ROW_HEIGHT` адаптируется под кол-во строк.
- Аналогично для passives.

---

## 3. Темы

| Hero | Сейчас | Будет |
|---|---|---|
| Goku | DEFAULT (золотой) | оранжевый — `0xFFE85D04` border / orange-red gradient |
| Naruto | DEFAULT | bright yellow — `0xFFFFD60A` (не дефолтное золото) |
| Cap | DEFAULT | синий + красный акцент — основной `0xFF1E40AF`, highlight `0xFFEF4444` |

---

## Порядок работы

1. Lang fix (`passive.0/1/2` → `1/2/3`) для goku/naruto/cap, обе локали.
2. 3 новые `HeroTheme` константы + привязка героев.
3. `AutoSaturationController` — пер-герой переопределение для Кэпа.
4. `vibranium_shield.json` — `display`-блок с корректными ориентациями.
5. `ShieldProjectileRenderer` — `GROUND` → `FIXED`.
6. `CapShieldSlamAbility` — реально применять AoE при приземлении после слэма.
7. `AbilitiesTooltipHud` — расширить панель, перенос строк.

---

## Подтверждённые ответы пользователя на открытые вопросы

1. Регулус — НЕ ТРОГАТЬ. Heartbeat остаётся как сейчас.
2. Шоквейв Кэпа — ТОЛЬКО при способности `cap_shield_slam`, не при обычном приземлении.
3. Saturation 100% не сломает баланс.
4. Goku — оранжевый прям оранжевый.
5. Один PR на всё.

---

## Также в этой волне (отдельно от PR)

- Сборка и релиз `v2.2.1-pre` с уже мерджнутыми фиксами Думсдея (PR #33).
