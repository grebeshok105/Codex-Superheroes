# HUD Redesign — Детальный План Реализации

> **Skill:** writing-plans  
> **Автор:** Viktor AI  
> **Дата:** 2026-06-11  
> **Ветка:** `viktor/hud-redesign`  
> **Базируется на:** `origin/main` (v3.16.3)

---

## 🎯 Цель

Полная переработка HUD мода Superheroes: замена текущего минималистичного текстового HUD на визуально богатый, стилизованный под каждого героя интерфейс с квадратным панельным меню, анимированными HP/Energy барами, иконками способностей с cooldown-индикаторами, перенесённым hotbar и обновлённым радиальным меню.

## 🏗 Архитектура

Модульная система из независимых HUD-компонентов, каждый рендерится отдельно. Все компоненты используют единую дизайн-систему (`HeroHudTheme`), расширяющую текущий `HeroTheme`. GUI Scale адаптивен (1–4). Рендеринг через vanilla `GuiGraphics` API без внешних зависимостей.

## 🛠 Стек

- Minecraft 1.21, Fabric API 0.102.0+1.21, Java 21
- `GuiGraphics` / `RenderSystem` / `HudRenderCallback`
- `InventoryScreen.renderEntityInInventoryFollowsMouse()` для модели персонажа
- Кастомный шрифт через `Font` resource pack override

---

## 📐 Референс (анализ)

На основе приложенного изображения:

```
┌──────────────────────────────────────────────────────────────────┐
│  [HOTBAR — перенесён сюда, над квадратом, слева]                │
│                                                                  │
│  ┌─────────────┬──────────────────────────────┐  ┌──┐┌──┐┌──┐   │
│  │             │  SUPERMAN                    │  │1 ││2 ││3 │   │
│  │  [Модель    │  FORM: SOLAR ASCENDED        │  └──┘└──┘└──┘   │
│  │   персонажа │  ❤ 1,250 / 1,250  ▓▓▓▓▓▓▓▓ │  ┌──┐┌──┐┌──┐   │
│  │   3D]       │  💧 600 / 600     ▓▓▓▓▓▓▓▓  │  │4 ││5 ││6 │   │
│  │             │                              │  └──┘└──┘└──┘   │
│  │             │  SOLAR CHARGE  ☀ 87%         │                  │
│  │             │  ▓▓▓▓▓▓▓▓▓░░░               │  [Abilities →    │
│  │             │  SUPER FLARE READY           │   на месте       │
│  │             ├──────────────────────────────┤   стандартного    │
│  │             │  PASSIVES                    │   hotbar]         │
│  │             │  ☀ Solar  🛡 Invuln  💚 Heal │                  │
│  └─────────────┴──────────────────────────────┘                  │
└──────────────────────────────────────────────────────────────────┘
```

---

## 📊 Анализ текущей кодовой базы

### Существующие HUD-файлы (все в `src/client/java/.../client/hud/`)

| Файл | Что делает | Действие |
|------|-----------|----------|
| `ResourceBarHud.java` | Панель energy/mana с hero name | **ЗАМЕНИТЬ** на `HeroInfoPanelHud` |
| `AbilitiesTooltipHud.java` | Список способностей + описания | **ЗАМЕНИТЬ** на `AbilityBarHud` |
| `RadialMenuHud.java` | Радиальное меню выбора ability | **ПЕРЕПИСАТЬ** дизайн |
| `HudUtil.java` | Утилиты рендеринга (rounded rect) | **РАСШИРИТЬ** |
| `MeleeChargeHud.java` | Gauge зарядки удара | Оставить как есть |
| Overlay HUDs (7 штук) | Эффекты поверх экрана | Оставить как есть |

### Ключевые данные на клиенте

| Источник | Данные |
|----------|--------|
| `ClientHeroState.data()` | heroId, energy, mana, activeAbilities |
| `ClientHeroState.energyMax()` | Max energy |
| `ClientHeroState.manaMax()` | Max mana (0 = нет маны) |
| `ClientHeroState.theme()` | HeroTheme (цвета) |
| `ClientHeroState.abilities()` | List<ResourceLocation> способностей |
| `ClientAbilityCooldowns` | CD тики per ability |
| `AbilityDescriptions` | Passive count, ability kinds |
| `Minecraft.player.getHealth()` | Текущие HP |
| `Minecraft.player.getMaxHealth()` | Max HP |

### Герои — ресурсы и passives

| Герой | Energy | Mana | Abilities | Passives | Тема ресурса |
|-------|--------|------|-----------|----------|-------------|
| Homelander | 100 | 100 | 6 | 3 | Laser/Power ⚡ |
| Iron Man | 1000 | 0 | 6 | 3 | Reactor/Arc 💠 |
| Regulus | 1000 | 0 | 5 | 4 | Lion Heart 🦁 |
| Sung Jin-Woo | 100 | 100 | 6 | 4 | Shadow/Mana 🌑 |
| Doomsday | 200 | 0 | 6 | 5 | Rage/Evolution 💀 |
| Goku | 200 | 0 | 6 | 3 | Ki ☀ |
| Naruto | 200 | 0 | 5 | 3 | Chakra 🌀 |
| Captain America | 200 | 0 | 4 | 3 | Serum/Shield 🛡 |
| Kratos | 250 | 0 | 5 | 4 | Rage/Spartan 🔥 |
| Loki | 200 | 0 | 5 | 3 | Magic/Trickster ✨ |
| Thanos | 400 | 0 | 7 | 4 | Cosmic Power 💎 |
| Reinhard | 1000 | 0 | 8 | 6 | Divine/Sword ⚔ |
| Raiden Shogun | 1500 | 0 | 6 | 0 | Electro ⚡ |
| Invincible | 240 | 0 | 4 | 4 | Viltrumite 💪 |
| Omni-Man | 320 | 0 | 4 | 4 | Viltrumite 💪 |
| Kazuha | 280 | 0 | 4 | 3 | Anemo 🍃 |
| Scaramouche | 320 | 0 | 5 | 3 | Electro/Wind 🌪 |
| Battle Beast | 260 | 0 | 4 | 3 | Blood/Curse 🩸 |
| Rem | 240 | 0 | 7 | 3 | Ice/Oni ❄ |
| A-Train | 300 | 0 | 4 | 3 | Speed/V ⚡ |

### Кол-во способностей по героям

- 4 abilities: Cap, Invincible, Omni-Man, Kazuha, Battle Beast, A-Train (6 героев)
- 5 abilities: Regulus, Naruto, Kratos, Loki, Scaramouche (5 героев)
- 6 abilities: Homelander, Iron Man, SJW, Doomsday, Goku, Raiden (6 героев)
- 7 abilities: Thanos, Rem (2 героя)
- 8 abilities: Reinhard (1 герой)

**Диапазон: 4–8.** Хотбинды: Z X C V B 3 4 5 (8 слотов максимум).

---

## 📁 Файловая структура изменений

### Новые файлы

```
src/client/java/com/example/superheroes/client/hud/
├── HeroInfoPanelHud.java        # [NEW] Квадратная панель: модель + HP + energy + name + passives
├── AbilityBarHud.java            # [NEW] Полоса способностей (замена hotbar-позиции)  
├── HotbarOverrideHud.java        # [NEW] Перенесённый hotbar (над квадратом, lock/unlock)
├── HeroHudTheme.java             # [NEW] Расширенная тема (энерг. иконки, цвета способностей)
├── HudAnimator.java              # [NEW] Утилита: smooth bars, pulse, fade анимации
├── HudScaler.java                # [NEW] Адаптация под GUI Scale 1-4
├── HudIcons.java                 # [NEW] Генерация SVG-стиля иконок (энергия, пассивки)

src/main/java/com/example/superheroes/hero/
├── HeroHudConfig.java            # [NEW] Конфиг HUD per hero (иконка энергии, название ресурса)

src/client/java/com/example/superheroes/client/
├── HotbarLockState.java          # [NEW] Lock/unlock состояние hotbar
```

### Модифицируемые файлы

```
src/client/java/com/example/superheroes/client/hud/
├── RadialMenuHud.java            # [MODIFY] Полный редизайн рендера
├── HudUtil.java                  # [MODIFY] Новые утилиты (gradient circle, arc, glow)

src/client/java/com/example/superheroes/client/
├── ModKeys.java                  # [MODIFY] +4 ability slots (B,3,4,5) + hotbar lock key
├── SuperheroesClient.java        # [MODIFY] Регистрация новых HUD + hotbar intercept

src/main/java/com/example/superheroes/hero/
├── Hero.java                     # [MODIFY] +getHudConfig() default method
├── HeroTheme.java                # [MODIFY] Расширить запись доп. полями для HUD
├── [Each Hero].java              # [MODIFY] Override getHudConfig() per hero

src/main/resources/assets/superheroes/lang/
├── en_us.json                    # [MODIFY] Новые ключи
├── ru_ru.json                    # [MODIFY] Новые ключи
```

### Удаляемые файлы

```
src/client/java/com/example/superheroes/client/hud/
├── ResourceBarHud.java           # [DELETE] Заменён на HeroInfoPanelHud
├── AbilitiesTooltipHud.java      # [DELETE] Заменён на AbilityBarHud
```

---

## 🔧 ЗАДАЧИ

---

### Task 1: Инфраструктура — HudScaler, HudAnimator, расширение HudUtil

**Цель:** Создать базовые утилиты, на которых будут строиться все HUD-компоненты.

**Files:**
- Create: `src/client/java/com/example/superheroes/client/hud/HudScaler.java`
- Create: `src/client/java/com/example/superheroes/client/hud/HudAnimator.java`
- Modify: `src/client/java/com/example/superheroes/client/hud/HudUtil.java`

#### HudScaler.java

Адаптирует все размеры под текущий GUI Scale. Все HUD-компоненты вызывают `HudScaler.scale(baseValue)` вместо хардкодных пикселей.

```java
public final class HudScaler {
    // Базовый масштаб при GUI Scale 2 (самый частый у целевой аудитории)
    private static final float BASE_GUI_SCALE = 2.0f;
    
    public static float factor() {
        Minecraft mc = Minecraft.getInstance();
        float currentScale = (float) mc.getWindow().getGuiScale();
        return currentScale / BASE_GUI_SCALE;
    }
    
    public static int scale(int base) {
        return Math.round(base * factor());
    }
    
    public static float scale(float base) {
        return base * factor();
    }
}
```

#### HudAnimator.java

Утилита для плавных анимаций: smooth HP/energy bars, pulse эффекты, fade-in/out.

```java
public final class HudAnimator {
    // Lerp для плавного уменьшения HP/Energy баров
    public static float smoothBar(float current, float target, float speed) {
        if (Math.abs(current - target) < 0.01f) return target;
        return current + (target - current) * speed;
    }
    
    // Pulse 0..1 для glowing эффектов (cooldown ready, ult ready)
    public static float pulse(float frequency) {
        return 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / (1000.0 / frequency));
    }
    
    // Circular cooldown progress (0.0 = on CD, 1.0 = ready)
    public static float cooldownArc(int remaining, int total) {
        if (total <= 0) return 1.0f;
        return 1.0f - Math.max(0f, Math.min(1f, remaining / (float) total));
    }
}
```

#### HudUtil.java — расширение

Добавить методы для нового HUD:

```java
// Градиентная дуга (для cooldown circle вокруг ability иконки)
public static void drawArc(GuiGraphics g, int cx, int cy, int radius, int thickness, 
                           float startAngle, float endAngle, int color);

// Glow-эффект вокруг элемента
public static void drawGlow(GuiGraphics g, int x, int y, int w, int h, int color, int spread);

// Вертикальный gradient bar (для HP/Energy)
public static void verticalGradientBar(GuiGraphics g, int x, int y, int w, int h, 
                                       float pct, int topColor, int botColor, int bgColor);

// Горизонтальный smooth bar с закруглениями
public static void smoothBar(GuiGraphics g, int x, int y, int w, int h, 
                             float pct, int fillDark, int fillBright, int glow, int bg);
```

**Коммит:** `feat(hud): add HudScaler, HudAnimator utilities and extend HudUtil`

---

### Task 2: HeroHudConfig и расширение HeroTheme

**Цель:** Добавить per-hero конфигурацию HUD (иконка энергии, название ресурса, цвета ability bar).

**Files:**
- Create: `src/main/java/com/example/superheroes/hero/HeroHudConfig.java`
- Modify: `src/main/java/com/example/superheroes/hero/Hero.java`
- Modify: `src/main/java/com/example/superheroes/hero/HeroTheme.java`
- Modify: все 20 hero-файлов

#### HeroHudConfig.java

```java
public record HeroHudConfig(
    String energyName,        // "Solar Charge", "Ki", "Chakra", "Reactor", etc.
    String energyNameRu,      // "Солнечный заряд", "Ки", "Чакра", etc.
    EnergyIconType energyIcon,// SUN, LIGHTNING, FLAME, SKULL, etc.
    boolean hasUltimate,      // Есть ли ульта (последняя ability)
    String ultimateName       // Название ульты для индикатора "SUPER FLARE READY"
) {
    public enum EnergyIconType {
        SUN,          // Homelander, Goku (solar/ki energy)
        LIGHTNING,    // Raiden, A-Train, Scaramouche (electro/speed)
        FLAME,        // Kratos (rage/spartan fire)
        SKULL,        // Doomsday (death/evolution)
        SHADOW,       // Sung Jin-Woo (shadow monarch)
        REACTOR,      // Iron Man (arc reactor)
        SHIELD,       // Captain America (serum)
        COSMIC,       // Thanos (infinity/cosmic)
        SWORD,        // Reinhard (divine sword)
        MAGIC,        // Loki (trickster magic)
        LION,         // Regulus (lion heart)
        FIST,         // Invincible, Omni-Man (viltrumite power)
        LEAF,         // Kazuha (anemo/nature)
        SPIRAL,       // Naruto (chakra spiral)
        ICE,          // Rem (ice/oni)
        BEAST,        // Battle Beast (blood curse)
        GENERIC       // Fallback
    }
    
    // Defaults per hero
    public static final HeroHudConfig HOMELANDER = new HeroHudConfig("Laser Power", "Лазерная мощь", EnergyIconType.LIGHTNING, true, "STUNNING ROAR");
    public static final HeroHudConfig IRON_MAN = new HeroHudConfig("Arc Reactor", "Реактор", EnergyIconType.REACTOR, true, "HULKBUSTER");
    public static final HeroHudConfig GOKU = new HeroHudConfig("Ki", "Ки", EnergyIconType.SUN, true, "SPIRIT BOMB");
    public static final HeroHudConfig NARUTO = new HeroHudConfig("Chakra", "Чакра", EnergyIconType.SPIRAL, true, "BIJUUDAMA");
    public static final HeroHudConfig KRATOS = new HeroHudConfig("Spartan Rage", "Ярость Спартанца", EnergyIconType.FLAME, true, "GOD SLAYER");
    public static final HeroHudConfig SUNG_JINWOO = new HeroHudConfig("Shadow Power", "Сила Теней", EnergyIconType.SHADOW, true, "MONARCH'S DOMAIN");
    public static final HeroHudConfig DOOMSDAY = new HeroHudConfig("Rage", "Ярость", EnergyIconType.SKULL, true, "DOOM GRIP");
    public static final HeroHudConfig THANOS = new HeroHudConfig("Cosmic Power", "Космическая сила", EnergyIconType.COSMIC, true, "SNAP");
    public static final HeroHudConfig REINHARD = new HeroHudConfig("Divine Blessing", "Божественное благо", EnergyIconType.SWORD, false, null);
    public static final HeroHudConfig RAIDEN = new HeroHudConfig("Electro", "Электро", EnergyIconType.LIGHTNING, true, "TRANSCENDENCE");
    public static final HeroHudConfig INVINCIBLE = new HeroHudConfig("Viltrumite Power", "Сила Вилтрума", EnergyIconType.FIST, true, "GUARDIAN'S BREAKER");
    public static final HeroHudConfig OMNIMAN = new HeroHudConfig("Viltrumite Power", "Сила Вилтрума", EnergyIconType.FIST, true, "WORLD BREAKER");
    public static final HeroHudConfig CAPTAIN_AMERICA = new HeroHudConfig("Super Serum", "Суперсыворотка", EnergyIconType.SHIELD, true, "COUNTER STANCE");
    public static final HeroHudConfig LOKI = new HeroHudConfig("Magic", "Магия", EnergyIconType.MAGIC, true, "CHAOS BOLT");
    public static final HeroHudConfig REGULUS = new HeroHudConfig("Lion Heart", "Сердце Льва", EnergyIconType.LION, false, null);
    public static final HeroHudConfig KAZUHA = new HeroHudConfig("Anemo", "Анемо", EnergyIconType.LEAF, true, "MAPLE STORM");
    public static final HeroHudConfig SCARAMOUCHE = new HeroHudConfig("Storm Power", "Сила Бури", EnergyIconType.LIGHTNING, true, "SKYFALL BURST");
    public static final HeroHudConfig BATTLE_BEAST = new HeroHudConfig("Blood Curse", "Кровавое проклятие", EnergyIconType.BEAST, true, "BLOODLUST");
    public static final HeroHudConfig REM = new HeroHudConfig("Oni Power", "Сила Они", EnergyIconType.ICE, true, "ONI RAGE");
    public static final HeroHudConfig A_TRAIN = new HeroHudConfig("Compound V", "Compound V", EnergyIconType.LIGHTNING, true, "HYPERSPEED");
    
    public static final HeroHudConfig DEFAULT = new HeroHudConfig("Energy", "Энергия", EnergyIconType.GENERIC, false, null);
}
```

#### Hero.java — добавить default method

```java
default HeroHudConfig getHudConfig() {
    return HeroHudConfig.DEFAULT;
}
```

#### HeroTheme.java — расширить

Добавить поля для нового HUD (цвета HP бара, ability бара, passive иконок):

```java
// Новые поля к существующему record:
int hpBarBright,        // Яркий цвет HP
int hpBarDark,          // Тёмный цвет HP
int hpBarGlow,          // Glow HP
int abilitySlotBg,      // Фон слота способности
int abilitySlotBorder,  // Рамка слота
int abilitySlotActive,  // Активная рамка
int abilityReady,       // Цвет "Ready" подсветки
int passiveIconColor,   // Цвет пассивных иконок
int panelAccent         // Акцентный цвет панели
```

**⚠ HeroTheme — record.** Добавление полей в record ломает все 20 конструкторов. Решение: создать `HeroHudTheme` как отдельный record, привязанный через `HeroHudConfig` или как companion к `HeroTheme`. Не трогать существующий `HeroTheme` — просто расширить через wrapper.

```java
public record HeroHudTheme(
    HeroTheme base,
    int hpBarBright,
    int hpBarDark,
    int hpBarGlow,
    int abilitySlotBg,
    int abilitySlotBorder,
    int abilitySlotActive,
    int abilityReady,
    int passiveIconColor,
    int panelAccent
) {
    // Factory per hero - все 20 вариантов
    // Дефолт вычисляется из base theme colors
    public static HeroHudTheme fromBase(HeroTheme base) { ... }
}
```

**Коммит:** `feat(hud): add HeroHudConfig and HeroHudTheme per-hero configuration`

---

### Task 3: HudIcons — SVG-стиль иконки рендером

**Цель:** Рисовать тематические иконки для energy и passives чисто кодом (нет файлов PNG/SVG, только `GuiGraphics.fill()` рисунки в стиле pixel-art SVG).

**Files:**
- Create: `src/client/java/com/example/superheroes/client/hud/HudIcons.java`

Каждая иконка — метод, рисующий пиксельную фигуру 12×12 или 16×16:

```java
public final class HudIcons {
    public static void drawEnergyIcon(GuiGraphics g, int x, int y, int size, 
                                      HeroHudConfig.EnergyIconType type, int color) {
        switch (type) {
            case SUN -> drawSun(g, x, y, size, color);
            case LIGHTNING -> drawLightning(g, x, y, size, color);
            case FLAME -> drawFlame(g, x, y, size, color);
            case SKULL -> drawSkull(g, x, y, size, color);
            case SHADOW -> drawShadow(g, x, y, size, color);
            case REACTOR -> drawReactor(g, x, y, size, color);
            case SHIELD -> drawShield(g, x, y, size, color);
            case COSMIC -> drawCosmic(g, x, y, size, color);
            case SWORD -> drawSword(g, x, y, size, color);
            case MAGIC -> drawMagic(g, x, y, size, color);
            case LION -> drawLion(g, x, y, size, color);
            case FIST -> drawFist(g, x, y, size, color);
            case LEAF -> drawLeaf(g, x, y, size, color);
            case SPIRAL -> drawSpiral(g, x, y, size, color);
            case ICE -> drawIce(g, x, y, size, color);
            case BEAST -> drawBeast(g, x, y, size, color);
            default -> drawGeneric(g, x, y, size, color);
        }
    }
    
    // Каждая иконка — набор fill() calls формирующих фигуру
    private static void drawSun(GuiGraphics g, int x, int y, int s, int c) {
        // Центральный круг + 8 лучей
        int cx = x + s/2, cy = y + s/2;
        int r = s/4;
        // Круг (заполнение квадратами по окружности)
        g.fill(cx-r, cy-r, cx+r, cy+r, c);
        // Лучи (крест + диагональ)
        g.fill(cx-1, y, cx+1, y+r, c);           // top
        g.fill(cx-1, cy+r, cx+1, y+s, c);        // bottom
        g.fill(x, cy-1, x+r, cy+1, c);           // left
        g.fill(cx+r, cy-1, x+s, cy+1, c);        // right
        // Диагональные лучи (2px линии)
    }
    
    private static void drawLightning(GuiGraphics g, int x, int y, int s, int c) {
        // Молния — зигзаг
        int u = s / 8; // unit
        g.fill(x+3*u, y,     x+5*u, y+u,   c);
        g.fill(x+2*u, y+u,   x+5*u, y+2*u, c);
        g.fill(x+3*u, y+2*u, x+6*u, y+3*u, c);
        g.fill(x+2*u, y+3*u, x+5*u, y+4*u, c);
        g.fill(x+u,   y+4*u, x+4*u, y+5*u, c);
        g.fill(x+2*u, y+5*u, x+4*u, y+6*u, c);
        g.fill(x+3*u, y+6*u, x+4*u, y+8*u, c);
    }
    
    // ... аналогично для всех 16 типов иконок
    
    // Passive icons — тоже pixel-art рисунки
    public static void drawPassiveIcon(GuiGraphics g, int x, int y, int size, 
                                       int index, ResourceLocation heroId, int color) {
        // По heroId и index определяем тип пассивки и рисуем соответствующую иконку
        // Regen = сердце, Armor = щит, Speed = стрелка, Fire resist = огонь, etc.
    }
}
```

**Коммит:** `feat(hud): add HudIcons for energy and passive icon rendering`

---

### Task 4: HeroInfoPanelHud — Главная квадратная панель

**Цель:** Основной элемент HUD — квадратная панель в нижнем левом углу с моделью персонажа, HP, энергией, маной, именем героя и пассивками.

**Files:**
- Create: `src/client/java/com/example/superheroes/client/hud/HeroInfoPanelHud.java`

#### Лейаут (при GUI Scale 2, базовый размер):

```
Размер панели: 220 x 140 px
Позиция: нижний левый угол, отступ 8px от края

┌──────────────────────────────────────────────┐
│         SUPERMAN                             │  ← Hero name, стилизованный шрифт, цвет из темы
│  ┌──────────┐  ──────────────────────────── │
│  │          │  ❤  1,250 / 1,250  ▓▓▓▓▓▓▓▓ │  ← HP bar (красный градиент)
│  │  [3D     │  ──────────────────────────── │
│  │  Model]  │  ⚡ 600 / 600     ▓▓▓▓▓▓▓▓  │  ← Energy bar (тематический цвет)
│  │          │  ──────────────────────────── │
│  │          │  ☀ SOLAR CHARGE      87%     │  ← Тематический ресурс label + %
│  │          │  ▓▓▓▓▓▓▓▓▓▓░░░░░░░░░░░░░░░  │  ← Прогресс-бар ресурса
│  └──────────┘  ──────────────────────────── │
│              SUPER FLARE READY              │  ← Ульта готова (пульсирующий)
│  ┌─────────────────────────────────────────┐ │
│  │ PASSIVES                                │ │
│  │ ☀ Solar   🛡 Invuln   💚 Heal           │ │  ← До 5 пассивок с иконками
│  └─────────────────────────────────────────┘ │
└──────────────────────────────────────────────┘
```

#### Модель персонажа

Используем `InventoryScreen.renderEntityInInventoryFollowsMouse()` для отрисовки 3D модели текущего игрока (со скином героя). Модель следит за курсором мыши, как в инвентаре.

```java
// Minecraft vanilla API для рендера entity в GUI
InventoryScreen.renderEntityInInventoryFollowsMouse(
    graphics, leftX, topY, rightX, bottomY,
    scale, yOffset, mouseX, mouseY, player
);
```

#### HP Bar

- Показывает HP в числах (не сердцах): `1,250 / 1,250`
- Красный градиент бар, плавно уменьшается (`HudAnimator.smoothBar()`)
- При критическом HP (< 20%) — пульсирует красным

#### Energy Bar

- Аналогично HP, но с тематическим цветом из `HeroHudTheme`
- Если герой имеет mana (SJW, Homelander) — показывается второй бар

#### Тематический ресурс

- Название из `HeroHudConfig.energyName` (Solar Charge, Ki, Chakra...)
- SVG-иконка из `HudIcons.drawEnergyIcon()`
- Процент заполнения

#### Ultimate Ready

- Если последняя способность не на КД — текст "SUPER FLARE READY" пульсирует
- Текст из `HeroHudConfig.ultimateName`

#### Passives

- До 5 иконок с названиями, горизонтально
- Количество из `AbilityDescriptions.passiveCount(heroId)`
- SVG-иконки через `HudIcons.drawPassiveIcon()`
- Если 0 пассивок (Raiden) — секция скрыта
- Если > 5 (Reinhard = 6) — скролл или уменьшенный размер

**Smooth анимация HP:**
```java
private static float displayedHp = 0f;
private static float displayedEnergy = 0f;

public static void tick() {
    float targetHp = mc.player.getHealth();
    displayedHp = HudAnimator.smoothBar(displayedHp, targetHp, 0.15f);
    
    float targetEnergy = ClientHeroState.data().energy();
    displayedEnergy = HudAnimator.smoothBar(displayedEnergy, targetEnergy, 0.2f);
}
```

**Коммит:** `feat(hud): add HeroInfoPanelHud with character model, HP, energy, passives`

---

### Task 5: AbilityBarHud — Полоса способностей

**Цель:** Заменить стандартный hotbar-слот на стилизованную полоску способностей с иконками, КД текстом и круговым индикатором готовности.

**Files:**
- Create: `src/client/java/com/example/superheroes/client/hud/AbilityBarHud.java`

#### Лейаут

Центр нижней части экрана (где стандартный hotbar). Количество слотов = количество ability героя.

```
Позиция: нижний центр экрана (x = screenWidth/2 - totalWidth/2, y = screenHeight - 60)

┌────┐  ┌────┐  ┌────┐  ┌────┐  ┌────┐  ┌─────────┐
│ ⚡ │  │ 🔴 │  │ ❄  │  │ 👊 │  │ 💨 │  │ ☀ LOGO │
│    │  │    │  │    │  │    │  │    │  │         │
│5.2 │  │2.8 │  │6.4 │  │7.6 │  │3.1 │  │ READY  │
│ Z  │  │ X  │  │ C  │  │ V  │  │ B  │  │   3    │
└────┘  └────┘  └────┘  └────┘  └────┘  └─────────┘
  │                                         │
  └── Cooldown circle overlay ──────────────┘
```

#### Размер слотов

- Базовый размер: 36x36 px (при GUI Scale 2)
- Если 4 ability — слоты 40x40 (больше места)
- Если 8 ability — слоты 32x32 (компактнее)
- Формула: `slotSize = 36 + (6 - abilityCount) * 2`, clamped 28-44

#### Компоненты каждого слота

1. **Фон**: Квадрат с rounded corners, gradient из темы
2. **Иконка способности**: Pixel-art иконка в центре (рисуется через `HudIcons` или текстура)
3. **Cooldown overlay**: Полупрозрачная тёмная маска + числовой КД снизу
4. **Круговая полоска КД**: `HudUtil.drawArc()` по периметру слота — показывает прогресс от 0% до 100%
5. **Хотбинд**: Маленькая буква снизу (Z, X, C, V, B, 3, 4, 5)
6. **Ultimate слот**: Последний слот визуально увеличен, с уникальным дизайном (логотип героя)

#### Cooldown визуализация

```java
// Числовой КД (как в референсе: "5.2", "2.8")
float cdSeconds = cooldownTicks / 20f;
String cdText = String.format("%.1f", cdSeconds);

// Круговой индикатор
float progress = HudAnimator.cooldownArc(remainingTicks, totalTicks);
HudUtil.drawArc(graphics, cx, cy, slotSize/2 + 2, 2, -90f, -90f + 360f * progress, readyColor);

// Когда ready — пульсирующая подсветка
if (progress >= 1.0f) {
    float pulse = HudAnimator.pulse(1.5f);
    int glowAlpha = (int)(pulse * 128);
    HudUtil.drawGlow(graphics, x, y, slotSize, slotSize, 
                     (glowAlpha << 24) | (theme.abilityReady() & 0xFFFFFF), 4);
}
```

#### Адаптация под количество

```java
int n = abilities.size();
int gap = 4;
int slotSize = Math.max(28, Math.min(44, 36 + (6 - n) * 2));
int totalWidth = n * slotSize + (n - 1) * gap;
int startX = screenWidth / 2 - totalWidth / 2;
int startY = screenHeight - slotSize - 24; // выше нижнего края
```

**Коммит:** `feat(hud): add AbilityBarHud with icons, cooldowns, and circular indicators`

---

### Task 6: HotbarOverrideHud — Перенесённый Hotbar с Lock

**Цель:** Перенести стандартный hotbar из центра внизу в позицию над HeroInfoPanel (слева), добавить lock/unlock механику.

**Files:**
- Create: `src/client/java/com/example/superheroes/client/hud/HotbarOverrideHud.java`
- Create: `src/client/java/com/example/superheroes/client/HotbarLockState.java`
- Modify: `src/client/java/com/example/superheroes/client/ModKeys.java`

#### HotbarLockState.java

```java
public final class HotbarLockState {
    private static boolean locked = false;
    
    public static boolean isLocked() { return locked; }
    public static void toggle() { locked = !locked; }
    
    // Визуальный индикатор состояния
    public static boolean showIndicator() {
        // Показываем замочек 2 секунды после переключения
        return System.currentTimeMillis() - lastToggleTime < 2000;
    }
}
```

#### Hotbar позиция

```
Позиция: над HeroInfoPanel, слева
    ┌──┬──┬──┬──┬──┬──┬──┬──┬──┐  🔒
    │1 │2 │3 │4 │5 │6 │7 │8 │9 │ [Lock indicator]
    └──┴──┴──┴──┴──┴──┴──┴──┴──┘
    ┌──────────────────────────────┐
    │ [HeroInfoPanel below]       │
    ...
```

#### Lock механика

- Новый keybind в `ModKeys` (например `GLFW_KEY_SEMICOLON` или `GLFW_KEY_L`)
- Когда locked:
  - Клавиши 1-9 НЕ переключают hotbar slot
  - Скролл колёсиком работает нормально
  - Визуальный замочек на hotbar
- Когда unlocked: стандартное поведение

#### Реализация intercept

```java
// Mixin на LocalPlayer или через InputEvent:
// Если locked — подавить нажатие числовых клавиш для hotbar
@Mixin(Minecraft.class)
public class HotbarLockMixin {
    @Inject(method = "handleKeybinds", at = @At("HEAD"))
    private void interceptHotbarKeys(CallbackInfo ci) {
        if (HotbarLockState.isLocked() && ClientHeroState.data().hasHero()) {
            // Suppress 1-9 keys for hotbar
        }
    }
}
```

Альтернатива без Mixin: перехватить через `ClientTickEvents` и `consumeClick()` на маппинги 1-9.

#### Vanilla hotbar hiding

Когда герой активен — скрыть стандартный hotbar рендер через Mixin на `Gui.renderHotbar` или `HudRenderCallback` рисует наш поверх.

**Коммит:** `feat(hud): add relocated hotbar with lock/unlock mechanism`

---

### Task 7: Редизайн RadialMenuHud

**Цель:** Полностью переработать визуал радиального меню, сохранив логику выбора. Основа — текущий код, но с новым дизайном.

**Files:**
- Modify: `src/client/java/com/example/superheroes/client/hud/RadialMenuHud.java`

#### Текущая логика (сохраняем):
- Open по зажатию `ModKeys.RADIAL` (R)
- Выбор через движение мыши (yaw/pitch offset)
- Отпускание = активация выбранной ability
- Dead zone 5f
- Фильтрация по tier/unlocked

#### Новый дизайн (меняем):

**Вместо прямоугольных слотов — секторный радиальный дизайн:**

```
          ┌─────┐
       ╱     3     ╲
     ╱   ┌───┐  ┌───┐  ╲
   ╱  2  │   │  │   │  4  ╲
  │ ┌───┐│ ● │──│   │┌───┐ │
  │ │   ││Hub│  │   ││   │ │
   ╲  1  │   │  │   │  5  ╱
     ╲   └───┘  └───┘  ╱
       ╲     6     ╱
          └─────┘
```

- **Hub (центр)**: Иконка героя + имя, полупрозрачный фон
- **Секторы**: Каждый сектор = одна ability, с gradient фоном
- **Выбранный сектор**: Яркая подсветка + glow + увеличенный
- **Cooldown**: Затемнённый сектор + CD-таймер текстом
- **Визуальный курсор**: Линия от центра + яркий dot на конце
- **Фон**: Полупрозрачный тёмный круг с radial gradient

#### Ключевые изменения рендера:

```java
// Секторный рисунок вместо прямоугольных slots
private static void drawSector(GuiGraphics g, int cx, int cy, int innerR, int outerR,
                                float startAngle, float endAngle, boolean selected,
                                boolean onCooldown, HeroTheme theme) {
    // Заполнение сектора через множество мелких fill()
    // Selected: ярче + glow
    // Cooldown: затемнённый
}

// Иконка ability в секторе (по центру сектора)
private static void drawAbilityInSector(GuiGraphics g, int cx, int cy, int r,
                                         float midAngle, ResourceLocation abilityId,
                                         HeroTheme theme) {
    int ix = cx + (int)(Math.cos(Math.toRadians(midAngle)) * r * 0.7);
    int iy = cy + (int)(Math.sin(Math.toRadians(midAngle)) * r * 0.7);
    // Draw ability name + key at (ix, iy)
}
```

#### Стиль

- Тёмная полупрозрачная основа с blur эффектом (без реального blur — имитация через gradient layers)
- Accentные цвета из HeroTheme
- Тонкие белые разделительные линии между секторами
- Hub — стеклянный эффект (светлый gradient сверху)
- Анимация появления: scale from 0 → 1 за 100ms

**Коммит:** `feat(hud): redesign radial menu with sector-based layout`

---

### Task 8: ModKeys — Расширение хотбиндов

**Цель:** Добавить 4 новых слота способностей (B, 3, 4, 5) и клавишу lock/unlock hotbar.

**Files:**
- Modify: `src/client/java/com/example/superheroes/client/ModKeys.java`

#### Текущие слоты: Z, X, C, V (4 шт)
#### Новые слоты: Z, X, C, V, B, 3, 4, 5 (8 шт)

```java
private static final int[] DEFAULT_SLOT_KEYS = {
    GLFW.GLFW_KEY_Z,      // Slot 1
    GLFW.GLFW_KEY_X,      // Slot 2
    GLFW.GLFW_KEY_C,      // Slot 3
    GLFW.GLFW_KEY_V,      // Slot 4
    GLFW.GLFW_KEY_B,      // Slot 5 (NEW)
    GLFW.GLFW_KEY_3,      // Slot 6 (NEW)
    GLFW.GLFW_KEY_4,      // Slot 7 (NEW)
    GLFW.GLFW_KEY_5,      // Slot 8 (NEW)
};

// Hotbar lock key
public static KeyMapping HOTBAR_LOCK;

public static void init() {
    // ... existing code ...
    
    HOTBAR_LOCK = KeyBindingHelper.registerKeyBinding(new KeyMapping(
        "key.superheroes.hotbar_lock",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_L,       // L по умолчанию
        CATEGORY));
        
    // Expand ABILITY_SLOTS to 8
    ABILITY_SLOTS = new KeyMapping[DEFAULT_SLOT_KEYS.length];
    for (int i = 0; i < DEFAULT_SLOT_KEYS.length; i++) {
        ABILITY_SLOTS[i] = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.superheroes.ability_" + (i + 1),
            InputConstants.Type.KEYSYM,
            DEFAULT_SLOT_KEYS[i],
            CATEGORY));
    }
}
```

#### Lang keys

```json
// en_us.json
"key.superheroes.ability_5": "Ability 5",
"key.superheroes.ability_6": "Ability 6",
"key.superheroes.ability_7": "Ability 7",
"key.superheroes.ability_8": "Ability 8",
"key.superheroes.hotbar_lock": "Lock/Unlock Hotbar"

// ru_ru.json
"key.superheroes.ability_5": "Способность 5",
"key.superheroes.ability_6": "Способность 6",
"key.superheroes.ability_7": "Способность 7",
"key.superheroes.ability_8": "Способность 8",
"key.superheroes.hotbar_lock": "Блокировка/Разблокировка хотбара"
```

**Коммит:** `feat(keys): expand ability slots to 8 (Z X C V B 3 4 5) and add hotbar lock key`

---

### Task 9: SuperheroesClient — Интеграция

**Цель:** Зарегистрировать новые HUD-компоненты, убрать старые, подключить hotbar lock.

**Files:**
- Modify: `src/client/java/com/example/superheroes/client/SuperheroesClient.java`

#### Изменения в HudRenderCallback

```java
HudRenderCallback.EVENT.register((graphics, tracker) -> {
    JarvisOverlayHud.render(graphics, tracker);
    
    // REMOVED: ResourceBarHud.render(graphics, tracker);
    // REMOVED: AbilitiesTooltipHud.render(graphics, tracker);
    
    // NEW: Replacement HUD components
    HeroInfoPanelHud.render(graphics, tracker);    // Квадратная панель
    AbilityBarHud.render(graphics, tracker);         // Полоса способностей
    HotbarOverrideHud.render(graphics, tracker);     // Перенесённый hotbar
    
    com.example.superheroes.client.hud.SpartanRageHud.render(graphics, tracker);
    RadialMenuHud.render(graphics, tracker);          // Обновлённый дизайн
    // ... rest of overlays unchanged
});
```

#### Tick registration

```java
ClientTickEvents.END_CLIENT_TICK.register(client -> {
    // ... existing ticks ...
    
    // REMOVED: AbilitiesTooltipHud.tick();
    HeroInfoPanelHud.tick();          // Smooth HP/Energy animation
    AbilityBarHud.tick();             // CD updates
    
    // Hotbar lock toggle
    while (ModKeys.HOTBAR_LOCK.consumeClick()) {
        if (ClientHeroState.data().hasHero()) {
            HotbarLockState.toggle();
        }
    }
    
    // ... existing radial menu tick, etc.
});
```

#### Vanilla hotbar suppression

Когда герой активен, нужно скрыть стандартный hotbar render. Два подхода:

1. **Mixin на `Gui.renderHotbar()`** — cancel при `hasHero()`
2. **Рисовать `HotbarOverrideHud` поверх** — менее чисто, но без mixin

Рекомендую Mixin:

```java
@Mixin(Gui.class)
public class GuiHotbarMixin {
    @Inject(method = "renderHotbarAndDecorations", at = @At("HEAD"), cancellable = true)
    private void onRenderHotbar(GuiGraphics graphics, DeltaTracker tracker, CallbackInfo ci) {
        if (ClientHeroState.data().hasHero()) {
            ci.cancel(); // Скрыть ванильный hotbar, наш рисуется отдельно
        }
    }
}
```

**Коммит:** `feat(hud): integrate new HUD components, remove old ResourceBarHud and AbilitiesTooltipHud`

---

### Task 10: Локализация

**Цель:** Добавить все новые строки в en_us.json и ru_ru.json.

**Files:**
- Modify: `src/main/resources/assets/superheroes/lang/en_us.json`
- Modify: `src/main/resources/assets/superheroes/lang/ru_ru.json`

```json
// en_us.json
{
  "hud.superheroes.hp": "HP",
  "hud.superheroes.energy": "Energy",
  "hud.superheroes.mana": "Mana",
  "hud.superheroes.ultimate_ready": "%s READY",
  "hud.superheroes.hotbar.locked": "Hotbar Locked",
  "hud.superheroes.hotbar.unlocked": "Hotbar Unlocked",
  "hud.superheroes.passives": "PASSIVES",
  "hud.superheroes.cd": "CD",
  
  "hud.superheroes.energy.solar_charge": "Solar Charge",
  "hud.superheroes.energy.ki": "Ki",
  "hud.superheroes.energy.chakra": "Chakra",
  "hud.superheroes.energy.arc_reactor": "Arc Reactor",
  "hud.superheroes.energy.spartan_rage": "Spartan Rage",
  "hud.superheroes.energy.shadow_power": "Shadow Power",
  "hud.superheroes.energy.rage": "Rage",
  "hud.superheroes.energy.cosmic_power": "Cosmic Power",
  "hud.superheroes.energy.divine_blessing": "Divine Blessing",
  "hud.superheroes.energy.electro": "Electro",
  "hud.superheroes.energy.viltrumite_power": "Viltrumite Power",
  "hud.superheroes.energy.super_serum": "Super Serum",
  "hud.superheroes.energy.magic": "Magic",
  "hud.superheroes.energy.lion_heart": "Lion Heart",
  "hud.superheroes.energy.anemo": "Anemo",
  "hud.superheroes.energy.storm_power": "Storm Power",
  "hud.superheroes.energy.blood_curse": "Blood Curse",
  "hud.superheroes.energy.oni_power": "Oni Power",
  "hud.superheroes.energy.compound_v": "Compound V",
  
  "key.superheroes.ability_5": "Ability 5",
  "key.superheroes.ability_6": "Ability 6",
  "key.superheroes.ability_7": "Ability 7",
  "key.superheroes.ability_8": "Ability 8",
  "key.superheroes.hotbar_lock": "Lock/Unlock Hotbar"
}
```

```json
// ru_ru.json — аналогично на русском
{
  "hud.superheroes.hp": "ХП",
  "hud.superheroes.energy": "Энергия",
  "hud.superheroes.mana": "Мана",
  "hud.superheroes.ultimate_ready": "%s ГОТОВ",
  "hud.superheroes.hotbar.locked": "Хотбар заблокирован",
  "hud.superheroes.hotbar.unlocked": "Хотбар разблокирован",
  "hud.superheroes.passives": "ПАССИВНЫЕ",
  
  "hud.superheroes.energy.solar_charge": "Солнечный Заряд",
  "hud.superheroes.energy.ki": "Ки",
  "hud.superheroes.energy.chakra": "Чакра",
  "hud.superheroes.energy.arc_reactor": "Реактор",
  "hud.superheroes.energy.spartan_rage": "Ярость Спартанца",
  "hud.superheroes.energy.shadow_power": "Сила Теней",
  "hud.superheroes.energy.rage": "Ярость",
  "hud.superheroes.energy.cosmic_power": "Космическая Сила",
  "hud.superheroes.energy.divine_blessing": "Божье Благо",
  "hud.superheroes.energy.electro": "Электро",
  "hud.superheroes.energy.viltrumite_power": "Сила Вилтрума",
  "hud.superheroes.energy.super_serum": "Суперсыворотка",
  "hud.superheroes.energy.magic": "Магия",
  "hud.superheroes.energy.lion_heart": "Сердце Льва",
  "hud.superheroes.energy.anemo": "Анемо",
  "hud.superheroes.energy.storm_power": "Сила Бури",
  "hud.superheroes.energy.blood_curse": "Кровавое Проклятие",
  "hud.superheroes.energy.oni_power": "Сила Они",
  "hud.superheroes.energy.compound_v": "Compound V",
  
  "key.superheroes.ability_5": "Способность 5",
  "key.superheroes.ability_6": "Способность 6",
  "key.superheroes.ability_7": "Способность 7",
  "key.superheroes.ability_8": "Способность 8",
  "key.superheroes.hotbar_lock": "Блокировка хотбара"
}
```

**Коммит:** `feat(lang): add HUD redesign localization keys for en_us and ru_ru`

---

### Task 11: Сборка, тест, верификация

**Цель:** Собрать мод, убедиться в компиляции, подготовить jar.

```bash
export JAVA_HOME=/work/tools/jdk-21.0.11+10 && export PATH=$JAVA_HOME/bin:$PATH
cd /work/projects/codex-superheroes
./gradlew build --no-daemon -x test
```

#### Чеклист верификации

- [ ] `./gradlew build` проходит без ошибок
- [ ] Все 20 героев имеют `HeroHudConfig`
- [ ] `HeroInfoPanelHud` рендерится в нижнем левом углу
- [ ] 3D модель персонажа отображается и следит за мышью
- [ ] HP бар показывает числовые значения и плавно анимируется
- [ ] Energy бар с тематической иконкой и процентами
- [ ] Passives показываются (1-5 иконок) или скрываются при 0
- [ ] `AbilityBarHud` показывает 4-8 слотов в зависимости от героя
- [ ] Cooldown числа и круговые индикаторы работают
- [ ] Хотбинды Z X C V B 3 4 5 активируют способности
- [ ] Hotbar перенесён над квадратом
- [ ] Lock/Unlock переключается клавишей L
- [ ] В locked режиме 1-9 не работают, колёсико работает
- [ ] Радиальное меню открывается на R с новым дизайном
- [ ] Все цвета адаптируются под героя (HeroTheme)
- [ ] GUI Scale 1-4 отображается корректно
- [ ] Vanilla hotbar скрыт когда герой активен
- [ ] Lang keys в en_us и ru_ru корректны

**Коммит:** `chore(build): verify HUD redesign compilation`

---

## ⚠ Риски и подводные камни

1. **CRLF**: Файлы в main используют CRLF + tabs. Все новые файлы создавать с CRLF. При модификации существующих — использовать Python с `newline=''`.

2. **HeroTheme — record**: Нельзя добавить поля без ломки 10+ конструкторов → используем отдельный `HeroHudTheme` record.

3. **Vanilla hotbar Mixin**: Метод `renderHotbarAndDecorations` может иметь другое имя в Mojang mappings для 1.21. Проверить exact name через mappings.

4. **3D модель в HUD**: `InventoryScreen.renderEntityInInventoryFollowsMouse()` рассчитан на inventory screen. В HUD контексте нужен правильный scissor/viewport. Тестировать тщательно.

5. **Performance**: 20+ fill() calls per ability icon × 8 abilities × 60fps = ~10K fill calls. `HudUtil.drawArc()` через мелкие fill() может быть тяжёлым. Оптимизировать: кэшировать, ограничить сегменты дуги.

6. **Lock hotbar**: Перехват числовых клавиш может конфликтовать с другими модами или vanilla keybindings. Нужна проверка: работает ли игрок с героем? Если нет героя — стандартное поведение.

7. **AbilityDescriptions.passiveCount**: Reinhard имеет 6 пассивок, а макет рассчитан на 5. Нужна обработка overflow (уменьшить иконки или scroll).

---

## 📐 Порядок выполнения

```
Task 1 (Utilities)
  └─→ Task 2 (Config/Theme)
       └─→ Task 3 (Icons)
            └─→ Task 4 (HeroInfoPanel)    ← MAIN FEATURE
            └─→ Task 5 (AbilityBar)       ← MAIN FEATURE
            └─→ Task 6 (Hotbar Override)
            └─→ Task 7 (Radial Redesign)
       └─→ Task 8 (ModKeys)
  └─→ Task 9 (Integration)
  └─→ Task 10 (Localization)
  └─→ Task 11 (Build & Verify)
```

Задачи 4-7 можно делать параллельно после завершения 1-3.

---

## 📝 Документация для будущего Design Skill

По ходу реализации документировать:
- Размеры элементов и формулы масштабирования
- Цветовую палитру и правила выбора цветов
- Стиль иконок (pixel-art SVG approach)
- Правила анимации (скорости, easing)
- Паттерны рендеринга (`HudUtil` API)
- Как добавить нового героя в HUD (чеклист)

Это ляжет в основу скилла `hud-design-system`.
