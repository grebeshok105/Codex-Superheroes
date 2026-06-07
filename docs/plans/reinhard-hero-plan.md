# Reinhard van Astrea — план нового героя

План для добавления героя **Reinhard van Astrea** из Re:Zero вместе с его мечом. Цель — сначала зафиксировать дизайн и список файлов, потом реализовывать отдельным PR без смешивания с v3.0 user-fixes.

## 0. Подтверждённые решения пользователя

Пользователь подтвердил:

1. **Трансформация через меч**: отдельного pendant/insignia не нужно.
2. **Меч — отдельный item**: вне формы Reinhard он годится только для превращения в героя; пользоваться им как оружием/ability-key можно только в форме Reinhard.
3. **Баланс — лорно OP**: Reinhard должен быть намеренно сильнее текущих героев, с ощущением Sword Saint / Divine Protections.

## 1. Концепт героя

### ID / название

- Hero ID: `superheroes:reinhard`
- Display EN: `Reinhard van Astrea`
- Display RU: `Рейнхард ван Астрея`
- Основной ресурс: `Energy` как stamina/Divine Protection reserve.
- Mana: `0`.

### Фантазия геймплея

Reinhard — лорно-OP swordmaster с высоким burst-уроном, защитными Divine Protection proc-ами и ультимативным ударом мечом Reid. Он не летает, но очень быстро двигается, игнорирует падение и должен ощущаться как почти непобедимый дуэлянт.

## 2. Ассеты из `art-source/rezero-fx-textures.zip`

В архиве уже есть подходящие Re:Zero ассеты:

- Skin/texture Reinhard:
  - `FX + TEXTURES REZERO/rezeromc/textures/entities/reinhard-van-astrea-re-zero-on-planetminecraft-com.png`
  - дубликат: `FX + TEXTURES REZERO/rezeromc/textures/reinhard-van-astrea-re-zero-on-planetminecraft-com.png`
- Sword / Reid assets:
  - `FX + TEXTURES REZERO/rezeromc/textures/item/dragonsword.png`
  - `FX + TEXTURES REZERO/rezeromc/textures/item/dragonswordreidnew1.png`
  - `FX + TEXTURES REZERO/rezeromc/textures/item/reidstick.png`
- VFX particles:
  - `FX + TEXTURES REZERO/rezeromc/textures/particle/swordexplosion.png`
  - `FX + TEXTURES REZERO/rezeromc/textures/particle/sword_explosion_1.png` … `sword_explosion_6.png`
- UI refs:
  - `FX + TEXTURES REZERO/rezeromc/textures/screens/rezeromcbuttonswordsmanship.png`
  - `FX + TEXTURES REZERO/rezeromc/textures/screens/rezeromcbuttondivineprotection.png`

Runtime copy targets:

- `src/main/resources/assets/superheroes/textures/entity/hero/reinhard.png`
- `src/main/resources/assets/superheroes/textures/item/dragon_sword_reid.png`
- `src/main/resources/assets/superheroes/textures/particle/reinhard_sword_explosion*.png`

## 3. Hero stats/passives

### `ReinhardHero`

Файл: `src/main/java/com/example/superheroes/hero/ReinhardHero.java`

Поля:

- `ID = ModId.of("reinhard")`
- `SKIN = ModId.of("textures/entity/hero/reinhard.png")`
- `HeroTheme.REINHARD` или локальный `THEME` в классе, если не трогаем `HeroTheme`.

Базовые значения:

- Energy max: `500`
- Energy regen: `3.0/tick`
- Mana max: `0`
- Dimensions: vanilla player `0.6 x 1.8`

Атрибуты в `HeroAttributes`:

- Armor: `28`
- Armor toughness: `14`
- Attack damage: `14`
- Attack speed: `2.0`
- Movement speed: `+60% base`
- Max health: `+40`
- Knockback resistance: `0.9`
- Step height: `+1.0`

Пассивные эффекты при трансформации:

- `DAMAGE_RESISTANCE II`
- `MOVEMENT_SPEED II` только если атрибутов не хватает визуально; иначе не дублировать.
- `REGENERATION I`
- Fall damage cancelled.

## 4. Меч Reid / Dragon Sword

### Предмет

Файл: `src/main/java/com/example/superheroes/item/DragonSwordReidItem.java`

Регистрация:

- `ModItems.DRAGON_SWORD_REID`
- model: `assets/superheroes/models/item/dragon_sword_reid.json`
- texture: `assets/superheroes/textures/item/dragon_sword_reid.png`
- lang:
  - EN: `Dragon Sword Reid`
  - RU: `Драконий меч Рейд`

Поведение:

- `stacksTo(1)`, `fireResistant()`, `rarity(EPIC)`, durability `2500`.
- `use()` вне формы Reinhard трансформирует игрока в `superheroes:reinhard`.
- Вне формы Reinhard меч **не должен работать как оружие**: melee damage минимальный/нулевой, durability не тратится, abilities не активируются.
- В форме Reinhard меч раскрывается как OP-оружие: высокий melee damage, sweep/crit VFX и доступ к sword abilities.
- Shift-use в форме Reinhard может делать untransform по паттерну `TransformationItem`, если пользователь не попросит отдельное управление.

Важно: не ломать `HeroEquipmentLock`; если текущий lock запрещает предметы не-броню, проверить, что меч можно держать в форме героя.

## 5. Трансформация через меч

Отдельный transformation item не нужен. `DragonSwordReidItem` должен совмещать:

- transformation behavior при `use()` вне формы Reinhard;
- untransform behavior при shift-use в форме Reinhard;
- locked combat behavior: не-Reinhard не может пользоваться мечом как оружием;
- full combat behavior: Reinhard получает весь урон/ability synergy меча.

Реализационно лучше не наследоваться напрямую от `TransformationItem`, если нужен кастомный combat lock. Сделать `DragonSwordReidItem extends Item` и внутри `use()` вызвать `HeroTransformService.transform/untransform`.

## 6. Abilities

### 6.1 Sword Saint Dash

ID: `reinhard_sword_saint_dash`

Тип: one-shot dash slash.

Файл: `ability/ReinhardSwordSaintDashAbility.java`

Поведение:

- Cost: `45 energy`.
- Cooldown: `4s`.
- Требует `DRAGON_SWORD_REID` в main/offhand и форму Reinhard.
- Игрок рывком движется вперёд на 12–16 блоков.
- Все LivingEntity в капсуле/линии получают `28–36` damage.
- Сильный knockback по направлению рывка.
- Частицы `SWEEP_ATTACK`, `CRIT`, custom `reinhard_sword_explosion_*` если подключим particle provider.

### 6.2 Divine Protection

ID: `reinhard_divine_protection`

Тип: toggle или passive proc.

Рекомендация: passive controller, чтобы не занимать слот ability.

Файл: `effect/ReinhardDivineProtectionController.java`

Поведение:

- Когда Reinhard получает урон, раз в `8–12s` может сработать защита:
  - уменьшить incoming damage на 80–95% через Fabric damage event/mixin/controller pattern;
  - оттолкнуть атакующего;
  - дать короткий `ABSORPTION`/`REGENERATION`;
  - погасить огонь/негативные vanilla effects, кроме явно исключённых эффектов вроде void/kill.
- Если проще без нового damage hook: tick-controller держит `DAMAGE_RESISTANCE II`, а active ability даёт `ABSORPTION` на 10s.

### 6.3 Reid Draw / Dragon Sword Release

ID: `reinhard_reid_draw`

Тип: ultimate one-shot AoE cone.

Файл: `ability/ReinhardReidDrawAbility.java`

Поведение:

- Требует `DRAGON_SWORD_REID` в main/offhand.
- Cost: `180 energy`.
- Cooldown: `30s`.
- Перед ударом charge `20–30 ticks` с частицами вокруг меча.
- Конус перед игроком: range `18`, angle `70°`.
- Damage: `70–100`, отдельный cap только против major bosses если потребуется.
- Сильный knockback + flash/sound.
- Не разрушает блоки.

### 6.4 Astrea Counter

ID: `reinhard_astrea_counter`

Тип: короткий defensive parry.

Файл: `ability/ReinhardAstreaCounterAbility.java`

Поведение:

- Toggle/charge на `2s`.
- Если игрок получает melee/projectile damage в окне parry, damage cancel/reduce и ответный slash по атакующему.
- Cooldown: `8s`.

## 7. Damage types / datagen

Новые damage types:

- `reinhard_sword_dash`
- `reinhard_reid_draw`
- `reinhard_counter`

Файлы/правки:

- `damage/ModDamageTypes.java`
- `datagen/ModDamageTypeProvider.java`
- generated JSON в `src/main/generated/data/superheroes/damage_type/`
- lang death messages EN/RU.

## 8. Registration checklist

Код:

- `hero/ReinhardHero.java`
- `hero/Heroes.java` — добавить static field + `register(REINHARD)`.
- `hero/HeroAttributes.java` — добавить modifiers.
- `ability/AbilityIds.java` — добавить IDs.
- `ability/AbilityRegistry.java` — instantiate/register abilities.
- `effect/ReinhardDivineProtectionController.java` — init in `SuperheroesMod`.
- `item/DragonSwordReidItem.java`.
- `item/ModItems.java`.
- `item/ModItemGroups.java`.

Assets/resources:

- `assets/superheroes/textures/entity/hero/reinhard.png`
- `assets/superheroes/textures/item/dragon_sword_reid.png`
- `assets/superheroes/models/item/dragon_sword_reid.json`
- `assets/superheroes/lang/en_us.json`
- `assets/superheroes/lang/ru_ru.json`

Datagen:

- если item models генерируются через `ModItemModelProvider`, добавить туда `dragon_sword_reid` вместо ручного JSON.
- выполнить `./gradlew runDatagen --no-daemon`.

## 9. Баланс v1

Рекомендуемый старт как лорно-OP герой:

- Reinhard без активных abilities уже сильнее большинства героев по melee/defense.
- Reinhard с Reid: OP burst, короткие cooldowns, высокий урон.
- Нет постоянного creative-flight.
- Практически неубиваем в обычном бою, но без полного бессмертия против void/kill commands.
- Divine Protection имеет короткий cooldown и сильно режет обычный урон.
- Ultimate не ломает блоки; по обычным мобам может ваншотить, по major bosses — damage cap по необходимости.

## 10. Риски

- Damage reduction/counter требует аккуратного hook-а в damage pipeline. Если нет готового Fabric event для нужной точки, лучше начинать с explicit active parry state и минимального mixin-а.
- Так как меч сам является предметом трансформации, нужно аккуратно не дублировать его при transform/untransform и не терять item при смерти.
- Сторонние Re:Zero assets из `art-source` нужно сохранить с понятным source note, если ещё нет лицензии/описания.
- Если нужен 3D-меч GeckoLib/Blockbench, это отдельный scope; v1 можно делать vanilla item texture/model.

## 11. Минимальный первый PR реализации

Чтобы быстро получить playable героя:

1. Reinhard hero + attributes + skin.
2. Dragon Sword Reid item as transformation item + locked weapon behavior.
3. Sword texture/model from Re:Zero assets.
4. 2 abilities: `Sword Saint Dash`, `Reid Draw`.
5. Lang EN/RU, models, item group.
6. Datagen + build.

`Divine Protection` и `Astrea Counter` можно делать вторым PR, если первый станет слишком большим.

## 12. Проверка

Команды:

```bash
export JAVA_HOME=/home/ubuntu/jdk-21.0.2 && export PATH=$JAVA_HOME/bin:$PATH
./gradlew runDatagen --no-daemon
./gradlew build --no-daemon -x test
```

Manual smoke-test:

- Sword transforms/untransforms Reinhard.
- Sword cannot be used as a real weapon outside Reinhard form.
- Skin отображается.
- Sword appears in creative tab and has texture/model.
- Dash damages enemies in line and spends energy/cooldown.
- Reid Draw requires sword, charges, hits cone, spends energy/cooldown.
- Death/untransform removes passives cleanly.
