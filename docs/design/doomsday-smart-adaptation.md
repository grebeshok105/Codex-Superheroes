# Plan: Doomsday Smart Adaptation

> Цель: сделать систему адаптации Думсдея «умной» — иммун получается ТОЛЬКО к специфическим/уникальным источникам урона (мод-ability, lava, lightning, drowning и т.д.), но НЕ к общим типам (`player_attack`, `mob_attack`, generic projectile и т.п.). Чтобы один раз получив по морде мечом, Думсдей не становился иммуном к любым ударам в принципе.

## Контекст (что есть сейчас)

После v2.1.0 адаптация работает так:

1. На смерть (`DoomsdayTierController.handleDeath`) — берётся `source.typeHolder().unwrapKey()` → `ResourceKey<DamageType>`, кладётся в `ADAPTED` сет → следующий урон того же типа полностью отменяется через `ServerLivingEntityEvents.ALLOW_DAMAGE`.
2. Иммун работает на УРОВНЕ `DamageType` (не способности и не атакующего).

**Проблема:** многие мод-способности используют ванильные generic типы (`minecraft:player_attack`, `minecraft:mob_attack`) → адаптация к одному удару = иммун ко всем ударам этого класса. Это поломанный баланс.

**Кастомных DamageType в моде сейчас:** только `superheroes:eye_laser`. Всё остальное (Repulsor, Unibeam, Counter Strike Регулуса, Bone Spike, Smash, Doom Grip) бьёт через `damageSources().mobAttack(player)` → попадает под `minecraft:mob_attack`.

## Что надо сделать

### A. Фильтр generic-типов

В `DoomsdayAdaptationController` ввести `Set<ResourceKey<DamageType>> GENERIC_TYPES` — типы, при которых **адаптация не регистрируется** (но смерть всё равно идёт, тир ап тоже идёт):

```java
private static final Set<ResourceKey<DamageType>> GENERIC_TYPES = Set.of(
    DamageTypes.PLAYER_ATTACK,
    DamageTypes.MOB_ATTACK,
    DamageTypes.MOB_ATTACK_NO_AGGRO,
    DamageTypes.ARROW,        // generic projectile, конкретные стрелы из модов имеют свои типы
    DamageTypes.TRIDENT,
    DamageTypes.GENERIC,
    DamageTypes.GENERIC_KILL,
    DamageTypes.MAGIC,        // generic magic (mob_effect от обычных potion-ов)
    DamageTypes.INDIRECT_MAGIC,
    DamageTypes.THROWN,
    DamageTypes.UNATTRIBUTED_FIREBALL,
    DamageTypes.MOB_PROJECTILE,
    DamageTypes.SONIC_BOOM    // спорный, но скорее generic
);
```

В `registerAdaptation`:

```java
if (GENERIC_TYPES.contains(typeKey)) {
    // адаптация НЕ записывается → ALLOW_DAMAGE дальше пропускает урон
    return;
}
adapted.add(typeKey);
ADAPT_COUNT.merge(...);
```

В `ALLOW_DAMAGE`:
- Уже фильтруется автоматически: если тип не в ADAPTED — урон проходит.

### B. Кастомные DamageType для всех уникальных способностей

Сейчас только `superheroes:eye_laser` — это плохо для гранулярности. Добавить кастомные типы для:

| Способность | DamageType key | Файл where |
|---|---|---|
| Repulsor | `superheroes:repulsor` | `RepulsorAbility.java` |
| Unibeam (charge + beam) | `superheroes:unibeam` | `UnibeamController.java` |
| Counter Strike Регулуса | `superheroes:counter_strike` | `RegulusMadnessController.java` |
| Lion Roar Регулуса | `superheroes:lion_roar` | `LionRoarAbility.java` |
| Doomsday Smash | `superheroes:doomsday_smash` | `DoomsdaySmashAbility.java` (`ShockwaveUtil`) |
| Doomsday Battle Roar | `superheroes:doomsday_roar` | `DoomsdayRoarAbility.java` |
| Doomsday Bone Spike | `superheroes:doomsday_bone_spike` | `DoomsdayBoneSpikeAbility.java` |
| Doomsday Charge Tackle | `superheroes:doomsday_charge_tackle` | `ChargeTackleAbility.java` |
| Doomsday Doom Grip | `superheroes:doomsday_doom_grip` | `DoomGripController.java` |
| Sung Jin-Woo Shadow attacks | `superheroes:shadow_attack` | `ShadowSoldierEntity.java` (через `damageSources().source(SHADOW_ATTACK, ...)`) |
| Madness aftermath | `superheroes:madness_aftermath` | `MadnessAftermathMobEffect.java` |

**Регистрация:**
1. Добавить ключи в `ModDamageTypes.java` рядом с `EYE_LASER`.
2. В `bootstrap(BootstrapContext)` зарегистрировать каждый: `context.register(KEY, new DamageType("name", DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.0F))`.
3. Добавить static-helper методы типа `repulsor(ServerLevel, Entity)`, `doomsdaySmash(ServerLevel, Entity)` и т.д.
4. В каждой ability/controller заменить `level.damageSources().mobAttack(player)` → `ModDamageTypes.<custom>(level, player)`.

**Важно про `ShockwaveUtil`:** там сейчас generic mob_attack. Параметризовать его: добавить параметр `Function<ServerLevel, DamageSource>` или принимать готовый `DamageSource`.

### C. Cross-mod ability resolution

Мод-способности из других модов (Botania, Iron's Spells, etc.) скорее всего имеют свои DamageType. То есть для них работает «само из коробки» — их ключ unique → попадёт в ADAPTED → иммун.

Если другой мод использует `mob_attack` → попадёт в GENERIC_TYPES → не запишется. Это by design.

### D. Аугментация ключа атакующим (опционально)

Сейчас `DoomsdayTierController.describeDamageSource` уже добавляет суффикс `@hero_id` если атакующий — трансформированный игрок. Этот суффикс используется только для broadcast-сообщения, **не** для записи в ADAPTED. То есть если разные Хоумлендеры бьют одной Eye Lasers — все они делят один ключ адаптации (`superheroes:eye_laser`), что правильно.

Если хочется сделать «Думсдей умер от лазеров КОНКРЕТНОГО Хоума» — можно завести составной ключ `Object` вместо `ResourceKey`, но это переусложняет. **Не делать**, оставить как есть.

### E. Очистка адаптаций при тир-апе (опционально)

Сейчас адаптации копятся бесконечно (т.к. сет не чистится между смертями). Это может привести к immortality-стену на T7.

Опции:
1. **Не чистить** (текущее поведение) — Думсдей становится absolutely immune к уже виденным источникам.
2. **Чистить кап** — хранить только последние N (например 12) адаптаций, FIFO.
3. **Сбрасывать при detransform** (уже сделано через `removePassives`) — оставить.

**Рекомендую 1 + 3** (текущее), т.к. user явно просил «больше не получал урон» от лавы/лазеров.

### F. Кастомный DamageType с `bypasses_invulnerability` теги

Чтобы одна ability могла пробить адаптацию (например finishing move кого-то против Думсдея), можно добавить тег `c:bypasses_doomsday_adaptation`. В `ALLOW_DAMAGE` проверять:

```java
if (source.is(BYPASSES_DOOMSDAY_ADAPTATION_TAG)) return true;
```

Скорее всего overkill. **Не делать в этом плане**, оставить hook на будущее.

## Файлы (full list)

### Modify
- `src/main/java/com/example/superheroes/damage/ModDamageTypes.java` — добавить ~10 новых ResourceKey + bootstrap + helper методы
- `src/main/java/com/example/superheroes/effect/DoomsdayAdaptationController.java` — добавить GENERIC_TYPES set + проверка в `registerAdaptation`
- `src/main/java/com/example/superheroes/ability/RepulsorAbility.java` — заменить mob_attack на ModDamageTypes.repulsor
- `src/main/java/com/example/superheroes/effect/UnibeamController.java` — то же
- `src/main/java/com/example/superheroes/effect/RegulusMadnessController.java` — для counter_strike
- `src/main/java/com/example/superheroes/ability/LionRoarAbility.java` — lion_roar type
- `src/main/java/com/example/superheroes/ability/DoomsdaySmashAbility.java` — параметризовать ShockwaveUtil или новый source
- `src/main/java/com/example/superheroes/physics/ShockwaveUtil.java` — добавить overload с DamageSource
- `src/main/java/com/example/superheroes/ability/DoomsdayRoarAbility.java`
- `src/main/java/com/example/superheroes/ability/DoomsdayBoneSpikeAbility.java`
- `src/main/java/com/example/superheroes/ability/ChargeTackleAbility.java`
- `src/main/java/com/example/superheroes/effect/DoomGripController.java`
- `src/main/java/com/example/superheroes/entity/ShadowSoldierEntity.java`

### New (datagen jsons)
- `src/main/resources/data/superheroes/damage_type/eye_laser.json` (если не сгенерится автоматом из bootstrap)
- `src/main/resources/data/superheroes/damage_type/repulsor.json`
- ... × 10 для каждого нового типа
- (если используется DataGenProvider — пробросить через там; см. `SuperheroesDataGenerator`)

### Lang
- `lang/en_us.json`, `lang/ru_ru.json` — добавить `death.attack.<damage_type_id>` для красивых сообщений в чате (например `death.attack.doomsday_doom_grip = "%1$s был раздавлен %2$s"`).

## Этапы

1. **Регистрация типов** (~30 мин)
   - Расширить `ModDamageTypes` всеми новыми ключами + bootstrap
   - Сгенерировать data json через datagen (см. `SuperheroesDataGenerator`)
   - Локализовать смерти в lang
2. **Замена generic source → custom source в способностях** (~30 мин)
   - Для каждой ability/controller из таблицы B
   - `ShockwaveUtil` параметризовать
3. **Generic-фильтр в адаптации** (~10 мин)
   - GENERIC_TYPES set + проверка в `registerAdaptation`
4. **Build + smoke** (~10 мин)
5. **PR + release v2.1.1**

**Всего ~90 мин.**

## Тест-план (для Devin / для пользователя)

| # | Шаг | Ожидаемый результат |
|---|---|---|
| 1 | Думсдей T7 умирает от лавы | После респавна — иммун к лаве (стоит в лаве, 0 урона). Но от меча — НЕТ иммуна. |
| 2 | Думсдей T7 умирает от удара мечом | Иммуна к мечу НЕ получает (тип `player_attack` в GENERIC). Тир всё равно растёт. |
| 3 | Думсдей умирает от Eye Lasers Хоума | Иммун к Eye Lasers. От Repulsor Тони — НЕ иммун. От обычного удара Хоума — НЕ иммун. |
| 4 | Думсдей умирает от Counter Strike Регулуса | Иммун к counter_strike. От Lion Roar — НЕТ. |
| 5 | Думсдей умирает от утопления | Иммун к drowning. |
| 6 | Думсдей умирает от Botania Elementium Sword (модовый custom DT) | Иммун к этому конкретному модному типу. |
| 7 | Detransform (Shift+ПКМ Genome) | Сброс всех адаптаций + тир. |

## Релиз

Версия `2.1.1`. Title: `v2.1.1: smart adaptation — granular per-DamageType immunity`. Релиз через skill `release-mod`.

## Что НЕЛЬЗЯ делать в этом плане

- Не менять баланс существующих способностей (только заменить тип урона, цифры те же).
- Не править героев кроме Doomsday-related (Хоум, Тони, Регулус, Сонг — только в части DamageType).
- Не вводить bypasses_doomsday_adaptation тег (отложено до явного запроса).
- Не убирать накопление адаптаций (они должны копиться, см. секция E пункт 1).
- Не плодить новые .md без необходимости.
