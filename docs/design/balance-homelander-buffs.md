# Balance — Homelander Buff Plan

**Status:** идеи, не реализованы
**Context:** после v1.0.23 Homelander получил больше HP, брони 50, knockback resistance, Resistance I. Но в PvP vs Регулус остаётся слабее — лазер недостаточно опасен, флайт сбивается контратакой, нет counter-play против Regulus-magnet и Regulus-reading.

Цель этого дока — накопить buff-идеи, чтобы в будущем сделать выверенный patch одним заходом, не по одному изменению.

---

## Диагноз: почему Homelander слабоват в текущем балансе

| Проблема | Причина | Отклик |
|----------|---------|--------|
| Лазер слабо пугает | Урон ~6-8, слишком мало на 1 hit при 100+ HP Регулуса в безумии | Увеличить base damage + добавить burst-mode |
| Полёт — мишень | Регулус с контратакой сбивает на старте слэма | Дать counter: anti-stun ability или невозможность контратаки пока Homelander в Supersonic |
| Нет способа прервать Regulus reading | Ракетой/лазером в момент чтения — Регулус продолжает, эффекта не даст | Shout AoE stun (ломает reading) |
| Magnet Регулуса тащит | Knockback res 1.0 помогает, но магнит работает через teleport/motion override (обходит attribute) | Нужно либо а) специфичный immunity flag для Homelander, либо б) способность-разрыв «Break Free» |
| Мана не нужна (только energy) | Homelander не использует ману в большинстве абилок, ресурс висит без дела | Перевести часть пассивов на ману или добавить manа-ability |

---

## Предлагаемые изменения

### 1. Лазер — damage pass

**Текущее:** один сфокусированный лазер из глаз, урон ~6 HP, дистанция ограничена.

**Предложение:**
- Base damage 6 → **14 HP**
- Keep single-target focus
- Добавить pierce — лазер проходит через первую цель и наносит **50% урона второй** в линии (максимум 3 цели на прямой, по убывающей 100% / 50% / 25%)
- Урон +25% если цель в воздухе (signature against flying enemies)

**Balance check:** 14 HP на попадание, при cast-time 0.3с и CD 2с = ~7 HP/sec DPS. Регулус в безумии имеет 60+ HP, т.е. 8-9 секунд чистого попадания нужно. Разумно.

**Пока не трогать:** расход энергии. Если слишком сильно — увеличить cost с текущего на +50%.

### 2. Heat Vision Sweep — новая способность

**Slot:** 2-я активная, radial menu
**Cost:** 30 Energy + 10 Mana
**Cooldown:** 15 сек

**Механика:**
- Homelander активирует — 0.5с зарядки (звук нарастающего whine + red-glow глаза)
- Затем конусный лазер, угол 60°, длина 12 блоков
- Урон 6 HP по всем в конусе, игнорирует 30% брони
- Поджигает цели на 3 сек
- Во время конусного выстрела Homelander не может двигаться 0.8с (risk-reward — статичен)

**Зачем:** area-clear против Sung-а с его тенями, и одновременно — **ломает Regulus reading** (нечем игнорировать area-DPS во время evangelion-chant).

**Визуал:** два красных луча из глаз расширяются в веер, жёлто-оранжевое свечение на земле/entity.

### 3. Supersonic Shout — anti-magnet / interrupt

**Slot:** 3-я активная, radial menu
**Cost:** 40 Energy + 40 Mana (дорогое, «панический» эскейп)
**Cooldown:** 40 сек

**Механика:**
- Homelander выдаёт звуковой удар AoE вокруг себя, radius 10 блоков
- Все entities (не Homelander):
  - Получают `Nausea II` на 3с
  - Получают `Weakness I` на 5с
  - Отталкиваются от центра (knockback v=2.0)
  - Если в состоянии «channeling» (читают Regulus evangelion, прицеливаются из Ruler's Authority, строят Domain) — channeling прерывается
- Сам Homelander получает `JUMP_BOOST I` на 2с для эскейпа после shout-а

**Визуал:** концентрическая shockwave-волна, деформация воздуха в 2-3 кадрах, блоки типа листьев срывает, цветы ломает. Звук — sonic boom.

**Балансно:** 40с CD + двойной ресурс-кост + только 10 блоков радиус. Не breakable abuse — это именно «панический эскейп» и interrupt tool.

### 4. Anti-Counter — пассивный бонус

**Проблема:** контратака Регулуса работает на любой урон и стрипает полёт. Homelander один раз ударил → его сдёрнули вниз, слэм 27 HP.

**Предложение — пассив «Iron Will»:**
- Когда Homelander в воздухе и получает «airborne interrupt» (принудительное прекращение полёта через AbilityRouter.deactivate): есть **10% шанс сопротивления** (gracefully отклоняет, flight остаётся активным)
- Не работает против вакуума-магнита (его нельзя пропустить полностью)
- Cap: 1 save per 20 сек

**Балансно:** 10% — это не деактивация контратаки, а small **скейтинг**. Регулус всё ещё 90% попадёт и сбьёт. Но иногда Homelander вывозит.

### 5. X-Ray Vision — расширение

**Текущее:** Homelander получает `GLOWING` по всем entities (рентгеновский эффект, видно сквозь стены).

**Расширение:**
- X-Ray активен по default в hero-форме, но только на ближайшие 30 блоков (чтобы не засорять мир по всему chunk-у)
- Shift+активация — **Precision Aim**: следующий лазер в течение 5 сек наводится на внутренние органы цели (помеченные X-Ray маркером), +30% урона
- Counter к Iron Man-у (просматривает под броню)

### 6. HP / Regen / Tier-buff

**Текущее (v1.0.23+):** +20 HP (40 итого), Resistance I, броня 50, knockback res 1.0, regen default.

**Предложение:**
- Regen с 1 → 2 (как у Регулуса в безумии для симметрии)
- +10 дополнительно max health → итого 50 HP (25 сердец)
- `MINING_SPEED +50%` — он не майнер, но должен ломать блоки быстро кулаками
- `ATTACK_DAMAGE +6 → +12` — кулак в ближнем бою должен быть страшным

### 7. Mana-использование — Shield пассив

**Проблема:** мана у Homelander-а не использована.

**Предложение — Energy Shield (пассив, тратит ману):**
- Пока у Homelander-а 20+ mana, первый удар за 30 сек абсорбируется (0 HP damage) — «invulnerability frame»
- На каждый absorbed hit — расход 10 mana
- Визуал: жёлтый shield-ripple на модели при absorb

**Зачем:** даёт Homelander-у hidden depth, заставляет Regulus-а быть осторожным с первым ударом (тратит заряд контратаки впустую если у Homelander-а есть Shield).

### 8. Ultimate idea — Nuclear Threat

**Slot:** ult, radial
**Cost:** 80 Energy + 80 Mana
**Cooldown:** 240с
**Animation time:** 2с зарядка (видимая — Homelander парит, глаза светятся)

**Механика:**
- После зарядки — гигантский лазер из глаз, radius 3 блока, длина 40 блоков
- Урон 50 HP по всем в луче, поджигает землю под лучом
- Блоки типа stone/dirt ломаются (НЕ ломает обсидиан, netherite, bedrock — как оговорено в project rules)
- Homelander после удара — 8с в `SLOWNESS II + WEAKNESS II` (recover window)

**Balance:** огромный урон, но длинная анимация + публичное окно уязвимости после. В 1v1 применить сложно, в 1vN — wipe.

---

## Порядок внедрения (если решишь делать все сразу)

1. **Фаза 1 (must-have):** §1 damage pass + §2 Heat Sweep + §3 Supersonic Shout
   - Решает основные проблемы PvP.
2. **Фаза 2 (quality-of-life):** §4 Iron Will + §6 HP/Regen/Attack buffs
   - Делает Homelander живучим.
3. **Фаза 3 (depth):** §5 Precision X-Ray + §7 Energy Shield
   - Добавляет скилл-потолок.
4. **Фаза 4 (spectacle):** §8 Nuclear Threat
   - Ult для спецэффектов.

---

## Числовой обзор после всех изменений

| Стат | До (v1.0.23) | После (план) |
|------|--------------|--------------|
| Max HP | 40 | 50 |
| Regen | 1 | 2 |
| Armor | 50 | 50 (не меняется) |
| Attack damage | +6 | +12 |
| Knockback res | 1.0 | 1.0 |
| Resistance | I | I |
| Laser base damage | ~6 | 14 |
| Laser pierce | нет | 3 цели |
| Active abilities | 2 | 4 (+ Heat Sweep, + Supersonic Shout, ult) |

---

## Что NOT to do

- **Не делать Homelander лучше Регулуса**. Цель — сделать его сопоставимым (в 45/55 соотношении, не 60/40). Если после всех изменений Homelander начнёт доминировать — откатить §7 Energy Shield или §2 pierce.
- **Не добавлять Homelander-у способ ломать стены**. Nuclear-ult ломает мягкие блоки, но bedrock/obsidian/netherite — табу (project rule).
- **Не давать Homelander-у full-flight anti-grab**. §4 Iron Will это пассив на 10%, не больше — иначе Регулус-контратака теряет смысл.
- **Не добавлять ману в обычный полёт**. Полёт остаётся на energy, мана — только на defensive/specials (§7 Shield, §3 Shout).

---

## Open questions

1. **Регулус после buff-ов Homelander-а** — нужно ли что-то компенсаторно добавить Регулусу, или он и так OP и всё ок?
2. **Heat Sweep против Sung Jinwoo** — теней 10 штук, sweep убьёт 2-3 за cast. Это многовато или в норме? Можно балансировать через cost.
3. **Supersonic Shout против Iron Man-а в полёте** — сбивает flight? Если да, Iron Man тоже должен иметь anti-stun. Если нет — Homelander слабее vs IM.
4. **Shield-absorb vs контратака Regulus-а** — если Shield съедает первый удар контратаки (слэм), это обесценивает всю механику контратаки. Решение: контратака **игнорирует Shield** (whitelisted damage type).

---

## Notes for implementation day

- Все числовые константы — в `HeroAttributes.HOMELANDER` и `HomelanderHero` констант.
- Heat Sweep и Supersonic Shout — новые абилки в `ability/` package.
- Iron Will passive — hook в `AbilityRouter.deactivate` с rate-limit.
- Energy Shield — hook в `LivingEntity#hurt` через mixin (но карф: если mod уже имеет hero-иммунитеты там, надо не дублировать).
- Nuclear Threat — самый сложный: большой проджектайл/raycast, block-breaking с whitelist, частицы.
