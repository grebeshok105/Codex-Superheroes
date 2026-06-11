# Round 4 — Подробный план реализации (v3.24.0)

## Обзор

| # | Задача | Статус |
|---|--------|--------|
| 1 | Система орд (10 волн) | 🔨 в работе |
| 2 | Корректировка HUD (Wild Client стиль) | 🔨 в работе |
| 3 | Улучшение Iron Man (визуал, способности, Iron Legion, смена костюмов) | 🔨 в работе |
| 4 | Возврат клонов Наруто (15 штук, ослепление, скрытие ников) | 🔨 в работе |
| 5 | Scaramouche + Kazuha → админ-билд | ✅ готово |
| 6 | Админ-билд (команда + скрытие предметов) | ✅ готово |
| 7 | Кровотечение для Battle Beast + агрессивных героев | 🔨 в работе |
| 8 | Новые реплики Джарвиса | 🔨 в работе |
| 9 | Джарвис 2.0 (классы угроз, реальные данные) | 🔨 в работе |
| 10 | Звук Mark 85 при полёте + система смены костюмов | 🔨 в работе |

---

## Задача 1: Система орд (Horde Mode)

### 1.1 Активация — предмет «Кристалл Орды» (Horde Crystal)

- Новый предмет: `HordeCrystalItem` → `superheroes:horde_crystal`
- Текстура: кроваво-красный кристалл с паразитическими прожилками (16×16 pixel art, рисую сам)
- Получение: только через `/superheroes admin give` (входит в `ADMIN_ONLY_ITEMS`)
- Поведение: ПКМ → начинается волна 1 (если нет активной орды в радиусе 128 блоков)
- Одноразовый предмет (расходуется при использовании)

### 1.2 Файлы

| Файл | Назначение |
|------|-----------|
| `item/HordeCrystalItem.java` | Предмет-активатор |
| `horde/HordeManager.java` | Сервер-контроллер: волны, таймеры, спавн |
| `horde/HordeWaveDefinition.java` | Описание волны: состав мобов, кол-во, босс |
| `horde/HordeWaves.java` | Реестр 10 волн |
| `horde/HordeBossBar.java` | BossBar для текущей волны |
| `horde/entity/` | 10–15 новых entity-классов на базе паразитов |

### 1.3 Волны

Каждая волна: мобы спавнятся в кольце 20–35 блоков от точки активации. Перерыв между волнами: 15 секунд. BossBar показывает оставшихся мобов.

| Волна | Обычные мобы | Мини-босс / босс | Общее кол-во |
|-------|-------------|-------------------|--------------|
| 1 | 12× Crawler, 6× Lurker | — | 18 |
| 2 | 10× Crawler, 8× Spitter, 4× Swooper | **Broodmother** (мини-босс, 120 HP) | 23 |
| 3 | 8× InfectedZombie, 8× InfectedSkeleton, 6× InfectedSpider | **CorruptedGolem** (мини-босс, 200 HP) | 23 |
| 4 | 10× Stalker, 6× Infector, 8× ParasiticHound | — | 24 |
| 5 | 12× Spitter, 8× Swooper, 6× Lurker | **Hivemind** (босс, 350 HP, призывает +4 Crawler/10с) | 27 |
| 6 | 10× InfectedCreeper, 8× VoidParasite, 6× HollowVillager | **Infector Alpha** (мини-босс, 250 HP, инфицирует мобов) | 25 |
| 7 | 15× Stalker, 8× ParasiticHound, 4× Broodmother | **Leviathan** (босс, 500 HP, AoE slam) | 28 |
| 8 | 10× VoidParasite, 10× Crawler, 8× InfectedSkeleton | **Hive Overlord** (переработанный Hivemind, 600 HP, 2 фазы) | 29 |
| 9 | 12× Swooper, 10× Spitter, 8× Stalker, 6× Infector | **Leviathan Duo** (2× Leviathan, 400 HP каждый) | 38 |
| 10 | 20× смешанных паразитов, 2× Broodmother, 2× CorruptedGolem | **🔥 Infected Homelander** (финальный босс, 800 HP, 3 фазы) | ~30 |

### 1.4 Infected Homelander (финальный босс)

- Использует существующий `InfectedHomelanderBossEntity` как базу (500 HP → расширяю до 800 HP)
- Текстуры: `infected_homelander.png` + `infected_homelander_wounded.png` (уже в моде)
- **Фаза 1 (800–500 HP):** Стандартный AI — удар, лазер, полёт, рывок. Призывает 4 Crawler каждые 30с
- **Фаза 2 (500–200 HP):** Паразитическая ярость — скорость ×1.5, урон ×1.3, новая атака «Acid Rain» (AoE паразитический кислотный дождь в радиусе 8 блоков). Текстура → wounded
- **Фаза 3 (<200 HP):** Берсерк — непрерывный полёт, атаки каждые 0.5с, призывает 6 VoidParasite, земля под ним покрывается ParasiticGrowth блоками

### 1.5 AI системы (из паразитов)

Адаптирую из Project Parasites:
- `SwarmCoordinator` → координация паразитов (не толпятся в одну точку)
- `PackHuntGoal` → стая окружает цель
- `FlankTargetGoal` → заход с флангов
- `AmbushGoal` → Stalker прячется и атакует из засады
- `AdaptiveBehavior` → мобы адаптируются к стилю игрока

### 1.6 Награды

После убийства Infected Homelander:
- Титан в чате: `§6§l[HORDE COMPLETE] §fВы пережили орду!`
- Эффект фейерверка (20 firework rockets в точке активации)
- Опыт: 500 XP orbs

---

## Задача 2: Корректировка HUD (Wild Client стиль)

### 2.1 Что делаем

Опираясь на скриншот wildclient.org:
- **НЕ МЕНЯТЬ:** структуру HUD, расположение элементов, основные цвета
- **Подкорректировать:**
  1. Увеличить радиус скругления углов в `HudUtil.roundedRectFill/Border` (2px → 3px)
  2. Усилить неоновое свечение: увеличить alpha внешнего glow в `neonPanel()`, добавить 3-й слой glow
  3. Добавить тонкую неоновую линию-акцент под заголовками панелей
  4. Сделать border чуть ярче (поднять alpha с текущих значений)
  5. Добавить мягкий outer glow для активных элементов (аналогия красного свечения на Wild Client)

### 2.2 Файлы

| Файл | Изменения |
|------|-----------|
| `client/hud/HudUtil.java` | Радиус скругления 2→3px, усиление glow в `neonPanel()` |
| `client/hud/AbilityBarHud.java` | Добавить glow-подсветку для активных способностей |
| `client/hud/HeroInfoPanelHud.java` | Неоновая линия-акцент, усиленный border glow |
| `client/hud/HotbarOverrideHud.java` | Сглаженные углы (если применимо) |

---

## Задача 3: Улучшение Iron Man

### 3.1 Система смены костюмов (Suit Switch)

**Новая способность:** `IronManSuitSwitchAbility` — биндинг: ENERGY, стоимость: 50 энергии

Доступные костюмы (6 штук):
| # | ID | Текстура | Особенность |
|---|-----|----------|-------------|
| 0 | `default` | `ironman.png` | Базовый — баланс |
| 1 | `mark_85` | `ironman_mark_85.png` | +10% урон repulsor, звук Jarvis Mark 85 при полёте |
| 2 | `mark_1` | `ironman_mark_1.png` | +15% брони, -10% скорости полёта |
| 3 | `stealth` | `ironman_mark_stealth.png` | Невидимость частиц при полёте, -5% урон |
| 4 | `rescue` | `ironman_mark_rescue.png` | +20% реген энергии, -10% урон |
| 5 | `war_machine` | `ironman_mark_war_machine.png` | +20% урон, -15% скорости |

**Механика:**
- Способность циклически переключает костюм: 0→1→2→3→4→5→0
- При переключении: звук `IRONMAN_JARVIS_DIAGNOSTIC` + частицы наноботов
- Костюм сохраняется в `HeroData` (новое поле `suitVariant`)
- Клиент-рендер подставляет нужную текстуру по variant ID
- При надевании Mark 85 + активации полёта → `IRONMAN_JARVIS_MARK85_PRESET`

### 3.2 Текстура Mark 85 — удаление камней бесконечности

Скрипт Python (Pillow): загрузить `ironman_mark_85.png`, найти пиксели ярких цветов камней (синий, красный, фиолетовый, зелёный, оранжевый, жёлтый не по палитре Iron Man), заменить на ближайший цвет брони.

### 3.3 Iron Legion (армия дронов)

**Новая способность:** `IronManLegionAbility` — биндинг: ENERGY, стоимость: 200 энергии, кулдаун: 60с

**Новый entity:** `IronLegionDroneEntity extends PathfinderMob` (на базе `ShadowSoldierEntity`)

**Поведение:**
- Спавн: прилетают сверху (спавн на Y+30, fly down)
- Количество: по числу свободных скинов (текущий костюм игрока не используется) → 5 дронов макс
- Каждый дрон носит уникальный скин из свободных вариантов
- **Скрытие ников:** `setCustomNameVisible(false)`, дроны имеют тег игрока (скрывают настоящего среди себя)
- AI: `MeleeAttackGoal` + `NearestAttackableTargetGoal` → атакуют ближайшего враждебного моба
- Летают: используют PathfinderMob + FlyingPathNavigation (как ShadowSoldier)
- **Время жизни: 30 секунд**
- **Уход:** после 30с дроны прекращают бой, взлетают вверх (setDeltaMovement(0, 0.8, 0)) и despawn на Y+40
- Урон: 6.0 (средний), скорость: 0.35, HP: 40

### 3.4 Файлы Iron Man

| Файл | Назначение |
|------|-----------|
| `ability/ironman/IronManSuitSwitchAbility.java` | Переключение костюма |
| `ability/ironman/IronManLegionAbility.java` | Призыв Iron Legion |
| `entity/IronLegionDroneEntity.java` | NPC-дрон (PathfinderMob, fly, attack, retreat) |
| `client/render/IronLegionDroneRenderer.java` | Рендер с player model + текстурой по variant |
| `network/SuitVariantPayload.java` | Синхронизация варианта костюма на клиент |

---

## Задача 4: Улучшение клонов Наруто

### 4.1 Изменения в `KageBunshinEntity`

| Параметр | Было | Стало |
|----------|------|-------|
| Кол-во клонов | 2 | 15 |
| Радиус спавна | 1.5 блока | 10 блоков |
| Время жизни | 120 тиков (6с) | 500 тиков (25с) |
| HP | 1 | 1 (без изменений) |
| Ники | видимые | скрытые (`setCustomNameVisible(false)`) |
| Ориентация | ±30° от игрока | рандом 0°–360° |

### 4.2 Ослепление врагов

При спавне клонов:
- Найти всех враждебных мобов + игроков (кроме хозяина) в радиусе 15 блоков
- Наложить `MobEffects.DARKNESS` (не Blindness, а Darkness — делает экран тёмным) на 5 секунд (100 тиков)
- Это позволяет игроку притвориться клоном, пока враг ничего не видит

### 4.3 Скрытие ника хозяина

- При активации: скрыть ник игрока через `ScoreboardTeam` visibility или `setCustomNameVisible` через пакет
- Через 25с (когда клоны исчезают): вернуть ник

### 4.4 Файлы

| Файл | Изменения |
|------|-----------|
| `ability/NarutoShadowClonesAbility.java` | 15 клонов, радиус 10, рандом ориентация, Darkness эффект |
| `entity/KageBunshinEntity.java` | lifetime=500, скрытие ника |
| `client/render/KageBunshinRenderer.java` | Скрыть nameplate |
| `network/HideNamePayload.java` | Синхронизация скрытия ника хозяина |

---

## Задача 7: Кровотечение (Bleeding)

### 7.1 Текущее состояние

`BleedingMobEffect` — 1+amplifier урон/сек, блокирует реген. Применяется ТОЛЬКО через Rem (MorningStar + Demonism).

### 7.2 Добавить кровотечение для:

| Герой | Способность | Шанс/условие |
|-------|------------|--------------|
| **Battle Beast** | Все атаки в ближнем бою | 30% шанс при ударе, уровень 1, 5с |
| **Kratos** | Все атаки в ближнем бою | 25% шанс, уровень 1, 4с |
| **Omniman** | Рывковые удары | 40% шанс при скоростной атаке, уровень 2, 6с |
| **Invincible** | Ближний бой | 20% шанс, уровень 1, 4с |
| **Doomsday** | Все атаки (tier 3+) | 50% шанс, уровень 2, 8с |

### 7.3 Реализация

Единый хук в `HeroAttackHandler` или существующем обработчике урона:
```
if (hero == battle_beast && random < 0.3) applyBleeding(target, 1, 100);
```

---

## Задача 8: Новые реплики Джарвиса

### 8.1 Текстовые реплики (чат + HUD)

Добавить в `IronManJarvisController` рандомные фразы при событиях:

**При обнаружении врага (дополнительно к текущему):**
- `"Сэр, обнаружена угроза. Рекомендую повышенную осторожность."`
- `"Фиксирую боевую сигнатуру. Классификация в процессе."`
- `"Сэр, на радаре контакт. Оцениваю уровень опасности."`

**При обнаружении S-tier врага (excited):**
- `"Сэр... Это... Это запредельная угроза. Настоятельно рекомендую отступление!"` + звук `JARVIS_DETECT_EXCITED`
- `"Критическое предупреждение! Сигнатура класса S. Все системы в боевой режим!"`
- `"Сэр, я фиксирую нечто... нечто что выходит за рамки наших протоколов."`

**При смене костюма:**
- `"Загружаю пресет костюма... Калибровка завершена."`
- `"Переконфигурация наносистемы... Готово."`

**При низкой энергии (<15%):**
- `"Сэр, запасы энергии критически низки. Переход в режим экономии."`
- `"Энергия на исходе. Рекомендую прекратить боевые действия."`

**При активации полёта:**
- `"Системы полёта активированы. Стабилизаторы в норме."`

### 8.2 Реализация

Добавить `JarvisQuotes.java` — статический реестр фраз по категориям с рандомным выбором.

---

## Задача 9: Джарвис 2.0

### 9.1 Классы угроз

| Класс | Герои | Цвет | Реакция |
|-------|-------|------|---------|
| **S** | Omniman, Thanos, Doomsday, Battle Beast, Reinhard, Regulus | §4 тёмно-красный | `JARVIS_DETECT_EXCITED` + уникальная фраза |
| **A** | Homelander, Invincible, Sung Jin-Woo, Goku | §c красный | стандартный `JARVIS_DETECT` |
| **B** | Iron Man, Naruto, Captain America | §6 золотой | `JARVIS_DETECT` (тихий) |
| **C** | Kratos, Rem, Raiden Shogun, A-Train | §e жёлтый | только текст, без звука |
| **D** | Kazuha, Scaramouche, Loki | §a зелёный | только текст |

### 9.2 Реальные данные в HUD

Обновить `JarvisDetectionHud` / `JarvisOverlayHud`:
- **Расстояние:** `Vec3.distanceTo()` между игроком и целью, обновление каждый тик → формат: `"42.3m"`
- **Класс героя:** Показывать название героя + класс: `"OMNIMAN [S]"` или `"HOMELANDER [A]"`
- **Опасность:** Цветовая индикация по классу (см. таблицу выше)
- **Формат HUD-строки:** `"■ OMNIMAN [S] — 42.3m — ЗАПРЕДЕЛЬНАЯ УГРОЗА"`

### 9.3 Файлы

| Файл | Изменения |
|------|-----------|
| `jarvis/JarvisQuotes.java` | Новый файл — реестр фраз |
| `jarvis/JarvisThreatClass.java` | Enum: S/A/B/C/D + маппинг героев |
| `ironman/IronManJarvisController.java` | Использовать threat class, реальное расстояние |
| `client/hud/JarvisDetectionHud.java` | Отображение класса + расстояния |
| `client/hud/JarvisOverlayHud.java` | Цвет border по классу угрозы |

---

## Задача 10: Звук Mark 85 при полёте

- При `IronManFlightAbility.tryActivate()`: если текущий `suitVariant == "mark_85"` → воспроизвести `IRONMAN_JARVIS_MARK85_PRESET`
- Одноразовый триггер (не каждый тик, только при включении полёта)
- Интеграция в `IronManFlightAbility` через проверку `HeroData.suitVariant()`

---

## Порядок реализации

1. **Задача 4** — Наруто клоны (простое, быстрый коммит)
2. **Задача 7** — Кровотечение (простое, быстрый коммит)
3. **Задача 2** — HUD корректировка (визуальное, мало кода)
4. **Задача 8+9** — Джарвис (реплики + threat class + реальные данные)
5. **Задача 3** — Iron Man (suit switch → Legion → текстура Mark 85)
6. **Задача 10** — Звук Mark 85 (зависит от suit switch)
7. **Задача 1** — Орды (самое объёмное, последним)

---

## Изменённые/новые файлы (всего ~25 файлов)

### Новые Java файлы (~15)
- `horde/HordeManager.java`
- `horde/HordeWaveDefinition.java`
- `horde/HordeWaves.java`
- `horde/HordeBossBar.java`
- `horde/entity/` — ~10 entity-классов (Crawler, Lurker, Spitter, Swooper, Stalker, Infector, Broodmother, CorruptedGolem, Hivemind, Leviathan, VoidParasite, InfectedZombie/Skeleton/Spider/Creeper, ParasiticHound, HollowVillager)
- `item/HordeCrystalItem.java`
- `ability/ironman/IronManSuitSwitchAbility.java`
- `ability/ironman/IronManLegionAbility.java`
- `entity/IronLegionDroneEntity.java`
- `client/render/IronLegionDroneRenderer.java`
- `jarvis/JarvisQuotes.java`
- `jarvis/JarvisThreatClass.java`
- `network/SuitVariantPayload.java`

### Изменённые Java файлы (~10)
- `ability/NarutoShadowClonesAbility.java` — 15 клонов, Darkness, радиус
- `entity/KageBunshinEntity.java` — lifetime 500, скрытие ников
- `client/hud/HudUtil.java` — скругление 3px, усиление glow
- `client/hud/AbilityBarHud.java` — glow для активных
- `client/hud/HeroInfoPanelHud.java` — неоновый акцент
- `ironman/IronManJarvisController.java` — threat class, фразы
- `client/hud/JarvisDetectionHud.java` — реальное расстояние, класс
- `client/hud/JarvisOverlayHud.java` — цвет по классу
- `hero/IronManHero.java` — новые abilities
- `sound/ModSounds.java` — 4 новых звука ✅
- `item/ModItemGroups.java` — HordeCrystal в admin items

### Ресурсы
- `sounds.json` — 4 новых звука ✅
- `textures/item/horde_crystal.png` — текстура предмета (рисую)
- `ironman_mark_85.png` — очистка от камней бесконечности
- `en_us.json` + `ru_ru.json` — все новые строки
