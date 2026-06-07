# Hero Design — Sung Jinwoo (Solo Leveling)

**Status:** draft design, not implemented yet
**Slot:** hero #4 (после Homelander / Iron Man / Regulus)
**Niche:** саммонер / shadow army / glass-cannon с танк-слоем из миньонов

---

## Лор / концепт

Sung Jinwoo — главный герой Solo Leveling, «Теневой Монарх». Его уникальная способность — превращать убитых врагов в лояльных **Теневых Солдат** и командовать армией. Сам по себе хрупкий (до определённого уровня), но с армией становится непобедимым.

В MC-реализации упор на то, что **у игрока мало HP и средний урон**, но вокруг — до 10 миньонов, которые дерутся за него. Его ключевая фраза — **"Arise"** (Восстань).

---

## Базовые характеристики

| Атрибут | Значение | Обоснование |
|---------|----------|-------------|
| `MAX_HEALTH` | 20 (10 сердец, базовая) | хрупкий без армии |
| `ARMOR` | +4 | лёгкий плащ, не броня |
| `ARMOR_TOUGHNESS` | +0 | без толстой брони |
| `MOVEMENT_SPEED` | +20% | высокая мобильность |
| `ATTACK_DAMAGE` | +4 (с кулачным ударом 5 итого) | средний melee — сам редко бьёт |
| `ATTACK_SPEED` | +1.5 (у руки) | быстрые выпады |
| `KNOCKBACK_RESISTANCE` | 0.3 | не танк |

**Пассивы (always-on в hero-форме):**
- `NIGHT_VISION` (скрытый, без визуала) — темнота не мешает
- `+25% damage вне прямого солнца` (скрытый бонус — Shadow Monarch слабее под ярким светом)
- Бесконечный `SATURATION` (унаследованно от всех героев через AutoSaturation)

### Ресурсы

- **Energy** (стандартный, как у других героев, регенерация по обычным правилам)
- **Mana** (стандартный)
- **Shadow Charges** (новый ресурс, специфичный для Sung Jinwoo) — 0..100. Тратятся на все теневые способности. Регенерация:
  - +2/сек в покое
  - +5 при killing blow по entity
  - +15 при «поглощении трупа» (см. способность Shadow Extraction)

---

## Способности

### 1. Arise (ПКМ / radial) — извлечение тени из трупа

**Тип:** активная, целевая (наведение на труп моба)
**Cost:** 20 Shadow Charges
**Cooldown:** 2с на одно применение, глобальный rate-limit
**Range:** 8 блоков от игрока до трупа

**Механика:**
- После смерти любого живого entity (не в зоне peaceful-mob) в радиусе 20 блоков от Sung-а, труп остаётся как «faded entity» (полупрозрачный) на 10с
- Игрок наводит crosshair на fading-труп + ПКМ / radial-выбор
- Если у игрока есть 20 Shadow Charges — труп превращается в **Shadow Soldier**:
  - Новая custom entity `ShadowSoldierEntity`
  - HP = 70% оригинала (если был Zombie с 20 HP → у тени 14 HP)
  - Damage = 120% оригинала
  - Attack Speed = +30%
  - Movement Speed = +20%
  - Визуал: полная чёрная силуэт-модель оригинального моба + фиолетовое свечение по контуру
  - Имеет тот же loadout (zombie с мечом → тень с мечом)
- Максимум теней у Sung-а одновременно: **10**. Если 10 уже есть, способность отключена (radial значок затемнён)
- Тень не повреждается от солнечного света, но получает +50% урона от огня

**Subtype — "Arise on dying enemy":**
- Если цель ещё жива, но на < 10% HP — можно наложить `Marked for Death` (visual: фиолетовый маркер над головой). Убийство marked-цели в течение 5с даёт **+50% к качеству тени** (+20% HP, +20% damage) — stimul для агрессии.

### 2. Shadow Exchange (Shift + ПКМ / radial) — смена мест с тенью

**Cost:** 15 Shadow Charges + 20 Energy
**Cooldown:** 10с

**Механика:**
- Игрок наводит на любую свою тень (Shadow Soldier) в радиусе 30 блоков
- Мгновенный teleport: Sung перемещается на позицию тени, тень — на позицию Sung-а
- 0.5с иммунитета к урону у обеих сторон после свапа (anti-gank)
- Звук + partico-эффект (чёрный всплеск) в обеих точках

**Зачем:** эскейп-ту́л, репозиционирование, гриф позиции тени-танка в лицо противнику. Counter для магнита Регулуса — если схватил, Sung свапается с тенью вдали.

### 3. Sacrifice (R / radial) — AoE-взрыв от теней

**Cost:** 30 Shadow Charges
**Cooldown:** 60с

**Механика:**
- Все текущие тени (до 10 штук) мгновенно взрываются в точке своего нахождения
- Урон: 8 HP на моба/игрока в радиусе 4 блоков от тени
- Урон масштабируется от качества тени (Shadow Soldier высокого моба даёт больше)
- Не ломает блоки
- Визуал: чёрный implosion-эффект с фиолетовой волной наружу
- После Sacrifice армия Sung-а сбрасывается до 0 — это nuclear-ход, используется как last-resort

### 4. Ruler's Authority (ПКМ удержание / long-press radial) — телекинетический удар

**Cost:** 25 Shadow Charges + 30 Energy
**Cooldown:** 15с

**Механика:**
- Зажатие кнопки 1с — появляется aiming-reticle на крестике прицела (как у Regulus-магнита)
- Любая цель в radius 15 блоков от перекрестья (живое или неживое, кроме глобальных блоков как бедрок):
  - Отрывается от земли и летит в точку перекрестья с силой velocity 2.0
  - При контакте — урон 6 HP + stun 1.5с
- Может быть применена к **блокам**: выбитый блок отправляется как проджектайл в направлении движения крестья; при попадании в entity — bonus damage 4 HP и knockback
- Если цель — союзная тень, эффект отрицательный (Sung не бьёт союзников)

**Зачем:** дистанционный контроль, синергия с Arise (убить летающую цель → поглотить тень).

### 5. Shadow Extraction (радиальное меню → «Extract») — усиление за счёт трупа

**Cost:** 10 Energy
**Cooldown:** 5с per corpse

**Механика:**
- Наведение на fading-труп в радиусе 5 блоков + ПКМ
- Труп исчезает, Sung получает:
  - +15 Shadow Charges
  - +2 HP heal
  - +5 Energy
- Альтернатива Arise — если ты не хочешь тень, но хочешь ресурсы

### 6. **Ultimate** — Monarch's Domain (S+R / ult-radial)

**Cost:** 80 Shadow Charges + 80 Energy + 50 Mana (все три ресурса должны быть)
**Cooldown:** 180с
**Duration:** 10с

**Механика активации:**
- Sung поднимает одну руку вверх, на нём проявляется корона + длинный плащ (temporary model overlay)
- Вокруг Sung-а в радиусе 25 блоков земля покрывается чёрной текстурой (ресурс-пак overlay или shader)
- В течение 10с:
  - Каждые 0.5с в радиусе 20 блоков из земли под каждым враждебным entity (не-союзные тени, не Sung) вылетают **призрачные клинки** — damage 4 HP, кровотечение 5с
  - Shadow Charges регенерируют в 5 раз быстрее
  - Все активные тени получают +50% attack speed и +30% damage
  - Sung получает `RESISTANCE II` и иммунитет к knockback
  - Клинки могут наносить critical hit (10% шанс, +50% урона) — визуально отличаются (красные блики)
- После 10с:
  - Домен спадает
  - Все тени, задействованные в бою, получают heal до 100% HP
  - У Sung-а 5с vulnerability (`WEAKNESS II`, speed -30%) — domain-фатиг

**Балансный гейт:**
- Требует 80/80/50 одновременно — нельзя задействовать без полной зарядки
- `WEAKNESS II` после — серьёзное окно для контратаки Регулуса
- Если Sung убит во время Domain — все тени мгновенно исчезают (анти-гриф)

---

## VFX план

### Постоянно в hero-форме

- **Плащ**: длинный полупрозрачный чёрный плащ (отдельная модель, rendering layer), развевается от ветра игрока
- **Аура**: фиолетовый glow по контуру skin (shader или particle ring внизу)
- **Глаза**: светятся фиолетовым (eye-texture overlay)
- **Следы**: при беге/спринте — фиолетовые partico-следы на земле (0.3с fade)

### При активации способностей

**Arise:**
- Труп рисуется с растущим чёрным paint-overlay (0.8с анимация)
- Взрыв фиолетового дыма в момент подъёма
- Звук: низкий gothic whoosh + metallic rattle

**Shadow Exchange:**
- В обеих точках — small implosion с фиолетовой искрой
- Звук: glass-shatter + teleport

**Sacrifice:**
- Каждая тень «взрывается» собственной чёрной имплозией
- Центральная shockwave-кольцо в каждой точке (particle)
- Звук: серия басового thud + final boom

**Ruler's Authority:**
- Aiming — появление фиолетового круга-прицела, трясётся
- Запуск — чёрный telekinetic streak от цели к прицелу
- Звук: magical woosh при target-ping, heavy thud на impact

**Shadow Extraction:**
- Fading-труп превращается в чёрный дым, который втягивается в Sung-а
- Звук: soul-suck hiss

**Monarch's Domain (ult):**
- Sky окрашивается тёмно-фиолетовым (shader override)
- Земля покрывается moving black-blood текстурой с pulsing-гл
- Призрачные клинки — 3D-модели, быстро растут из земли, стоят 0.3с, пропадают
- Корона над Sung-ом — glow-эффект + небольшая модель (item_display)
- Звук: orchestral строй + пылесоса-вибрация фоном
- Финал домена — «imploding-wave» обратно в Sung-а

---

## Shadow Soldier AI

**Behavior goals (priority):**
1. Protect Sung — если враг в 10 блоках от Sung-а и атакует его, тень моментально переключается на этого врага
2. Attack Sung's last target — тень идёт за целью, которую Sung ударил последней
3. Follow Sung — если нет цели, следует за Sung-ом в радиусе 8 блоков (TameableMobFollowOwnerGoal-аналог)
4. Aggro on sight — атакует любого не-союзного (не-Sung, не-другая-тень, не-игроки-союзники) в радиусе 12 блоков

**Interaction:**
- Тень не атакует мобов, которых Sung пометил как «не-трогать» (маркер через long-press Shift на моба)
- Урон от Sung-а по тени = 0 (friendly-fire off)
- Тень умирает от урона как обычно, дропает `Shadow Fragment` item (консумируемый, +5 Shadow Charges)

**Rendering:**
- Reskin модели оригинального моба — полностью чёрный с фиолетовым outline
- Красные глаза (2 dot particles в позиции глаз)
- Полупрозрачность 80% opacity
- Без теней от самого моба (noShadow flag)

---

## Ассеты — что нужно

### Текстуры
- `sung_jinwoo_suit.png` — основная текстура скина героя (Steve-геометрия, 64x64). Тёмный плащ + капюшон, фиолетовые акценты.
- `shadow_soldier.png` — 64x64 чёрный силуэт с фиолетовой обводкой (применится как overlay ко всем reskin-ам)
- `monarch_crown.png` — 16x16 модель короны (item display)
- `marked_for_death.png` — 8x8 фиолетовый маркер-иконка

### Модели (JSON / Blockbench)
- `sung_cape.geo.json` — дополнительная геометрия плаща, рендерится как layer
- `monarch_crown.geo.json` — крона (3D)
- `shadow_blade.geo.json` — призрачный клинок для ult (растёт из земли)

### Звуки (OGG)
- `arise.ogg` — gothic whoosh + rattle, 1.5с
- `shadow_exchange.ogg` — glass shatter + teleport, 0.8с
- `sacrifice.ogg` — series of thuds + final boom, 2с
- `rulers_authority_cast.ogg` — magical aiming hum, looped
- `rulers_authority_hit.ogg` — heavy thud, 0.6с
- `shadow_extraction.ogg` — soul suck hiss, 0.7с
- `domain_activate.ogg` — orchestral swell + vacuum, 3с
- `domain_loop.ogg` — ambient dark hum, looped 10с
- `domain_end.ogg` — imploding wave, 1.5с
- `shadow_idle_loop.ogg` — тихий хрип тени, ambient
- `shadow_death.ogg` — soul dissipation, 0.5с

### Partico-спрайты
- `shadow_particle.png` — фиолетовые частички ауры
- `monarch_glyph.png` — глиф-печать домена на земле
- `shadow_trail.png` — следы при беге

---

## Hero Suit item

**Name:** Shadow Monarch's Cloak (`superheroes:shadow_monarchs_cloak`)
**Rarity:** epic
**Model:** плащ (armor slot — chestplate слот)

**Crafting:**
```
W O W
O S O
W W W
```
Где W = Wither Rose, O = Obsidian, S = Nether Star.
Дорогой рецепт — один из самых затратных среди героев.

---

## Баланс — позиционирование против текущих героев

| Противник | Advantage | Disadvantage | Counter-play |
|-----------|-----------|--------------|--------------|
| **Регулус (безумие)** | 10 теней долбят пока Регулус читает евангелие. Рулерс-отрывает блоки под ногами. | Контратака бьёт и по теням (30 HP нуки их). Магнит не берёт Sung-а если он в Exchange. | Sung держится в 20+ блоках, отправляет теней, сам не подходит. |
| **Хоумлендер** | Много теней вынудят Hлендера тратить лазер. Ruler's Authority может оторвать его от воздуха. | Лазер-свип Хоумлендера (если добавим — см. buff-doc) выжигает теней за секунду. | Sung прячется, перегруппировывается в Exchange. |
| **Iron Man** | Ракеты Iron Man попадают по тени вместо Sung-а. | Летающий IM вне Ruler's-range. | Sung прокачивает Shadow Charges через минорные мобы, готовит Ult. |

**Win condition Sung-а:** накопить Shadow Charges до ult, не умереть до него, активировать Monarch's Domain в момент, когда оппонент в центре.

**Lose condition:** ранняя агрессия против кого-то с AoE (Регулус-слэм, возможный Homelander-sweep). Если Sung-а запрессовали до того как он набрал армию — он почти беззащитен.

---

## Technical references (для имплементации)

- **Custom entity:** `ShadowSoldierEntity extends PathfinderMob implements OwnableEntity` — реализует follow-owner, aggro-priority. Похоже на tamed Wolf, но без breeding.
- **Custom damage type:** `superheroes:shadow_blade` для клинков в Monarch's Domain — игнорирует ARMOR (но не RESISTANCE).
- **Ticker:** `SungJinwooController` по аналогии с `RegulusMadnessController` — отслеживает количество теней, Shadow Charges, таймер Monarch's Domain.
- **Network packets:**
  - `S2C: ShadowCountUpdate(count)` — обновление HUD
  - `C2S: AriseRequest(corpse_uuid)` — запрос поднятия трупа
  - `C2S: ShadowExchangeRequest(shadow_uuid)` — запрос свапа
- **HUD overlay:** отдельный HUD слева сверху — 10 иконок-силуэтов теней + счётчик Shadow Charges (круговая полоса) + кулдауны способностей.

---

## Open questions

1. **Тени в нежилых мирах** (Nether, End) — работают? Лимит тот же 10?
2. **Мобовые тени на PvP-серверах с плагинами** — могут сломаться если плагины фильтруют entity-владельцев.
3. **Killing blow на другого игрока** — можно ли делать тень из убитого игрока? (Не-lore-канон, но balance-wise это даст 11-ю тень и может быть imba.) Моё мнение: нет, не стоит. Из игроков не поднимать.
4. **Max tier Shadow Soldier** — нужна ли в будущем «evolution» (тень убивает X мобов → апгрейдится)? Это добавляет сложности и не критично для MVP.
5. **Интеграция с ManiaOfGreed** — если Sung убит Регулусом в жадности, нужна ли special animation / dialogue? Необязательно.
