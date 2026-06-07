# План v2.2.3-pre — Doomsday + Homelander boss + VFX overhaul

Все 7 пунктов запроса. Фактическая реализация — **только после твоего ОК** по этому плану.
Версия: `2.2.3-pre`. Ветка: `devin/<ts>-v2.2.3-pre-doomsday`.

---

## Пункт 1. Кастомные damage types для абилок Хоумлендера-босса

### Корневая причина «mod attack»

Все 7 AI-целей босса бьют через `boss.damageSources().mobAttack(boss)` →
death-message и адаптация Думсдея логают это как `mob_attack` → у тебя
получается «mod attack» в death-screen и адаптация **не записывается**
(этот тип в `GENERIC_TYPES` адаптации).

Файлы-источники: <ref_snippet file="/home/ubuntu/repos/grebeshok105/src/main/java/com/example/superheroes/entity/ai/HomelanderHandClapGoal.java" lines="88-100" />, <ref_snippet file="/home/ubuntu/repos/grebeshok105/src/main/java/com/example/superheroes/entity/ai/HomelanderEyeLaserGoal.java" lines="128-132" />, <ref_snippet file="/home/ubuntu/repos/grebeshok105/src/main/java/com/example/superheroes/entity/ai/HomelanderHeatVisionSweepGoal.java" lines="108-122" />, <ref_snippet file="/home/ubuntu/repos/grebeshok105/src/main/java/com/example/superheroes/entity/ai/HomelanderLightningCallGoal.java" lines="77-82" />, <ref_snippet file="/home/ubuntu/repos/grebeshok105/src/main/java/com/example/superheroes/entity/ai/HomelanderRoarGoal.java" lines="80-88" />, <ref_snippet file="/home/ubuntu/repos/grebeshok105/src/main/java/com/example/superheroes/entity/ai/HomelanderSonicSlamGoal.java" lines="104-108" />, <ref_snippet file="/home/ubuntu/repos/grebeshok105/src/main/java/com/example/superheroes/entity/ai/HomelanderShockwaveDiveGoal.java" />, <ref_snippet file="/home/ubuntu/repos/grebeshok105/src/main/java/com/example/superheroes/entity/ai/HomelanderBlockThrowGoal.java" />.

### Что добавлю

В `ModDamageTypes.java` — новые ключи + bootstrap + factory-методы:

| ID | Тип | Источник AI |
|---|---|---|
| `homelander_eye_laser` | NEVER + BURNING | EyeLaserGoal |
| `homelander_heat_vision` | NEVER + BURNING | HeatVisionSweepGoal |
| `homelander_hand_clap` | NEVER | HandClapGoal |
| `homelander_sonic_slam` | NEVER | SonicSlamGoal |
| `homelander_shockwave_dive` | NEVER | ShockwaveDiveGoal |
| `homelander_lightning_call` | NEVER | LightningCallGoal (доп-удар, сама молния остаётся `lightning_bolt`) |
| `homelander_roar_boss` | NEVER | RoarGoal (босс-AoE; не путать с player-абилкой) |
| `homelander_block_throw` | NEVER | BlockThrowGoal |
| `homelander_melee` | NEVER | MeleeAttackGoal (опц., если не хочешь оставлять стандартный mob_attack) |

`bootstrap()` зарегистрирует все 9 в `superheroes:damage_type` registry.

`ModDamageTypeProvider.java` (datagen) → добавить ключи. `bootstrap()` уже единое — provider просто перечисляет ключи.

В каждом goal-е:
```
DamageSource ds = boss.damageSources().mobAttack(boss); // было
↓
DamageSource ds = ModDamageTypes.homelanderEyeLaser(boss.serverLevel(), boss); // станет
```

### Локализация death-message

Vanilla формат: `death.attack.<type>` и `death.attack.<type>.player`.
Добавлю в `lang/en_us.json` + `lang/ru_ru.json`:
```
"death.attack.homelander_eye_laser": "%1$s was vaporised by Homelander's eye lasers"
"death.attack.homelander_eye_laser.player": "%1$s was vaporised by %2$s's eye lasers"
... аналогично для 7 остальных
```
RU-перевод стилистически — «испепелён лазером Хоумлендера», «расплющен суперударом», «оглушён рёвом и добит» и т.д.

### Совместимость с адаптацией Думсдея

Сейчас `GENERIC_TYPES` в `DoomsdayAdaptationController` блокирует адаптацию к `mob_attack` (правильно — это слишком общий тип). После замены каждый homelander-удар будет **уникальным** damage-type → адаптация будет накапливаться по абилке (по 30 урона каждой).

Также добавлю в `RELATED_GROUPS` группу:
```
Set.of(homelander_eye_laser, homelander_heat_vision, homelander_lightning_call)
```
чтобы один иммун покрывал родственные «электричество/жар» атаки босса.

### Тест-чеклист

- Спавн босса, подставиться под каждую абилку → death-screen показывает уникальное сообщение по абилке.
- Думсдей умирает от laser-а → tier up, адаптация записывает `superheroes:homelander_eye_laser` (а не `mob_attack`).
- Чат-broadcast смерти показывает имя абилки, не «mod_attack».

---

## Пункт 2. Звуки Doomsday

### 2.1 Shockwave (приземление)

Файл: <ref_snippet file="/home/ubuntu/repos/grebeshok105/src/main/java/com/example/superheroes/hero/DoomsdayHero.java" lines="166-185" />.

`RAVAGER_ROAR` всё ещё играет в `NORMAL` и `EPIC` тирах приземления. **Уберу полностью.**
Замена:
- WEAK: `GENERIC_EXPLODE` (низкий питч), без рычания — оставлю как есть.
- NORMAL: `GENERIC_EXPLODE` (basso) + `LIGHTNING_BOLT_THUNDER` (низкий, 0.55 pitch). Убирается ravager.
- EPIC: `GENERIC_EXPLODE` + `WARDEN_SONIC_BOOM` (один раз, не двойной) + `LIGHTNING_BOLT_THUNDER`. Убирается ravager.

Также в `DoomsdaySmashAbility` line 54 — `RAVAGER_ROAR` → `LIGHTNING_BOLT_THUNDER` (slam без рычания).

### 2.2 Боевой рёв (DoomsdayRoarAbility)

Файл: <ref_snippet file="/home/ubuntu/repos/grebeshok105/src/main/java/com/example/superheroes/ability/DoomsdayRoarAbility.java" lines="73-77" />.
Сейчас: `RAVAGER_ROAR` + `WARDEN_SONIC_BOOM`. Wardenовский — нельзя по твоему запрету.

**План: импорт кастомного рёва из art-source.**

Кандидаты-сэмплы из `art-source/rezero-fx-textures.zip`:
- `soulsweapons/sounds/night_prowler_scream.ogg` — низкий зверь-крик, без вардена
- `soulsweapons/sounds/hard_boss_spawn.ogg` — глухой бас-рёв
- `soulsweapons/sounds/decaying_king_idle.ogg` — гортанный гул
- `soulsweapons/sounds/death_screams.ogg` — резкий рык

Подойдёт **night_prowler_scream** (грубый зверский крик) + слой `hard_boss_spawn` (низкий boom). Можно собрать в один OGG через ffmpeg или разнести в `sounds.json` как мульти-сэмпл (vanilla random pick).

Добавлю:
- Файлы: `src/main/resources/assets/superheroes/sounds/doomsday/roar_a.ogg`, `roar_b.ogg`.
- `sounds.json`: `superheroes:doomsday.roar` → 2 варианта (random pick, weight 1).
- `ModSounds.java` (создам если нет): `DOOMSDAY_ROAR` → register.
- `DoomsdayRoarAbility.tryActivate()` — заменю `RAVAGER_ROAR` на `ModSounds.DOOMSDAY_ROAR`, удалю warden-слой (или заменю на `LIGHTNING_BOLT_THUNDER` для bass).
- В `DoomsdayBerserkAbility` line 42 (`RAVAGER_ROAR`) тоже подменю на тот же кастомный.
- В `DoomsdayAdaptationController` line 137 (`RAVAGER_ROAR` при адаптации) — на тот же кастомный (тише, 0.5 vol).

> Drag/Wardenовский: проверю что НИГДЕ у Думсдея нет `WARDEN_*` и `ENDER_DRAGON_*`.

### Тест-чеклист

- Приземление любого тира — без ravager-рычания.
- `Z` (Roar ability) — играет кастомный звук, не warden и не dragon.
- Берсерк-актив — кастомный звук.
- Адаптация — кастомный звук.

---

## Пункт 3. Адаптация: damage-types быстрее + immunity к статус-эффектам

### Текущее состояние

<ref_snippet file="/home/ubuntu/repos/grebeshok105/src/main/java/com/example/superheroes/effect/DoomsdayAdaptationController.java" lines="32-73" />:
- Cumulative threshold = 30 hp.
- Lava ~4 hp/tick × 0.5s tick → 30 hp за ~3.7 сек (твой замер «1 секунда» — это первый tick + jolt).
- Drowning 2 hp/sec → 15 сек.
- Wither 1 hp/sec → 30 сек.

### Изменения

#### 3a. Стандартизировать порог адаптации к damage-types

- Снижу `CUMULATIVE_THRESHOLD` до **15 hp** для всех damage-types кроме «безопасных» групп.
- Дополнительно введу **time-based fallback**: если игрок получал урон конкретного типа суммарно ≥ **5 сек** реального времени (tick-counter в `CUMULATIVE`), даже без 15 hp — адаптация триггерится. Так wither (slow ticks) станет 5s, drowning 5s, lava 1-2s.
- Опционально: per-type override map (например, `lava` → 1s, `wither` → 3s, `drown` → 5s, остальные → 5s по умолчанию).

#### 3b. Adaptation к негативным status-effects

Это **новый отдельный механизм** — отдельный controller `DoomsdayEffectAdaptationController`, потому что damage-types и mob-effects разные API.

Список «адаптируемых» эффектов (vanilla):
```
WEAKNESS, MOVEMENT_SLOWDOWN, DIG_SLOWDOWN, POISON, WITHER, BLINDNESS,
DARKNESS, HUNGER, NAUSEA, LEVITATION, GLOWING, BAD_OMEN, UNLUCK,
SLOW_FALLING (debatable, оставлю)
```

Логика:
- Tick-loop: если думсдей **тиром ≥ 2** имеет один из этих эффектов — accumulator[effect] += 1 tick.
- При accumulator ≥ **200 ticks (10 секунд)**: записать в `ADAPTED_EFFECTS<UUID, Set<MobEffect>>`, удалить эффект, отправить broadcast.
- Mixin или event на `LivingEntity#addEffect` (Fabric API не имеет hook → используем `MobEffectInstance#tick` через mixin или просто проверять каждый тик).

Реализация через mixin:
- `LivingEntityEffectMixin` → `@Inject` в `addEffect(MobEffectInstance)` → если `entity instanceof ServerPlayer doomsday && hasAdaptedEffect(doomsday, effect)` → `cir.setReturnValue(false)` (блокировать применение).

После адаптации эффект **никогда не применяется** к Думсдею в этом playthrough (до respawn-cleanup или manual clear).

#### 3c. Сообщения

`hero.superheroes.doomsday.adapted_effect` — «Думсдей адаптировался к: %1$s». Уже есть аналог для damage-types.

### Тест-чеклист

- Прыгнуть в лаву → ~1 сек → immunity (как сейчас).
- Утопиться → ~5 сек → immunity (вместо 15).
- Wither beacon рядом → ~3-5 сек → immunity.
- Splash potion of Weakness × 4 (40 сек длительности, по факту через 10 сек активного ношения) → adapt → потом любой weakness не цепляется.
- Аналогично poison, slowness.

---

## Пункт 4. Tier 7 баффы Думсдея: Regen II + Jump + Strength

### Текущее состояние

<ref_snippet file="/home/ubuntu/repos/grebeshok105/src/main/java/com/example/superheroes/hero/DoomsdayHero.java" lines="91-105" /> — на любом тире Regeneration I и (≥5) Resistance I. Атрибуты лернятся в `buildDoomsdayTierSet` <ref_snippet file="/home/ubuntu/repos/grebeshok105/src/main/java/com/example/superheroes/hero/HeroAttributes.java" lines="163-179" />.

### Что добавлю на тир 7

В `applyPassives()` блок `if (tier >= 7) { ... }`:

| Эффект | Параметры |
|---|---|
| `MobEffects.REGENERATION` | amplifier=1 (Regen II), permanent (-1), `ambient=true, visible=false, showIcon=true` |
| `MobEffects.DAMAGE_BOOST` (Strength) | amplifier=1 (Strength II) — заменяет/дополняет attack_damage attr |
| `MobEffects.DAMAGE_RESISTANCE` | amplifier=2 (Resistance III) на тире 7 (сейчас на ≥5 даёт I) |
| `MobEffects.FIRE_RESISTANCE` | permanent — логично для финальной формы |
| `MobEffects.SATURATION` (нет, уже AutoSaturationController есть) | пропускаем |

Прыгучесть:
- Можно через `MobEffects.JUMP` (Jump Boost II), permanent.
- Или поднять `DOOMSDAY_JUMP` атрибут на 7 тире сильнее (сейчас lerp до +0.6 = ~50% jump). Возьму **MobEffects.JUMP amplifier=1 + увеличу `JUMP_STRENGTH` lerp(0, 0.9, f)**. Двойная страховка.

`removePassives()` — добавить removeEffect для всех 4-х новых.
`removePassives()` уже снимает REGENERATION и DAMAGE_RESISTANCE — расширю.

### Альтернативное архитектурное решение

Вместо хардкода в `applyPassives` — `buildDoomsdayTier7Effects()` метод в `DoomsdayHero` возвращающий `List<MobEffectInstance>`. Чище, легче добавить ещё эффекты позже.

### Тест-чеклист

- Респавн на tier 7 → видны иконки эффектов: Regen II, Strength II, Resistance III, Fire Resistance, Jump II.
- Прыжок выше: видим `~3-4 блока` высоты (vs 1.25 vanilla).
- Урон по нейтральному мобу выше (Strength II = +6 melee).
- Регенерация ~1 hp за 25 ticks (= 1.25 sec).

---

## Пункт 5. Doom Grip — фикс на боссе и игроках

### Корневая причина

<ref_snippet file="/home/ubuntu/repos/grebeshok105/src/main/java/com/example/superheroes/effect/DoomGripController.java" lines="65-91" />.

Текущая логика hold-фазы:
- `target.setPos(holdPos)` — **server-side только**, для игрока не отправляет teleport-пакет.
- `target.setDeltaMovement(Vec3.ZERO)` + `hurtMarked = true`.

Почему **не работает на боссе**:
- `HomelanderBossEntity` — `Monster` с `setNoGravity(true)` и активным `HomelanderFlightGoal`. Каждый tick goal заново вычисляет траекторию и вызывает `setDeltaMovement(...)`, **переопределяя** наш `Vec3.ZERO`.
- AI goals не остановлены — boss продолжает выполнять `EyeLaserGoal`, `SonicSlamGoal` и т.д., которые двигают сущность.
- Дополнительно: `KNOCKBACK_RESISTANCE = 1.0` ничего не ломает напрямую, но `setNoGravity` + flight goal = хост летит.

Почему **может не работать на других игроках** (или работать странно):
- `target.setPos()` для `ServerPlayer` обновляет server position, но клиент игрока продолжает rubber-band к своей предсказанной позиции. `connection.resetPosition()` помогает, но **не телепортирует камеру/контроль** — нужен `connection.teleport(x, y, z, yaw, pitch, RelativeMovement.ROTATION)`.

### Фиксы

#### 5a. Boss support

В `start()` для боссов:
```java
if (target instanceof HomelanderBossEntity boss) {
    boss.setNoAi(true);          // freeze AI goals
    boss.setNoGravity(true);     // already true, just safety
    boss.setDeltaMovement(Vec3.ZERO);
}
```

В `serverTick()` hold-phase для боссов:
- Сохранить `boss.setNoAi(true)` каждый тик (страховка если что-то сбросило).
- Использовать `boss.moveTo(x, y, z, yaw, pitch)` вместо `setPos` — это сильнее принудительный API для mob.

В `serverTick()` exit (throw-phase):
- `boss.setNoAi(false)` — вернуть AI.
- `boss.setNoGravity(true)` — оставить как было (boss всегда летает).

В `clear()` (на disconnect/respawn): то же — `setNoAi(false)` если был установлен.

Сохранять оригинальный `noAi` state в `GripState` (поле `boolean wasNoAi`), чтобы не сбить bosses которые **уже** noAi (edge case).

#### 5b. Player support

В `serverTick()` hold-phase для `ServerPlayer sp`:
```java
sp.connection.teleport(holdPos.x, holdPos.y, holdPos.z, sp.getYRot(), sp.getXRot());
```
Это **строит новый teleport-пакет каждый tick** и заставляет клиент следовать. Заменяет связку `setPos + resetPosition + ClientboundSetEntityMotionPacket`.

Опц.: `sp.setRespawnPosition(...)` — нет, это для respawn-логики, не нужно.

Опц.: dampить anti-cheat пакет → `connection.aboveGroundTickCount = 0` (не критично).

#### 5c. Throw-phase для боссов

```java
if (target instanceof HomelanderBossEntity boss) {
    boss.setNoAi(state.wasNoAi); // restore
    // не throw-knockback — но настроим velocity в throw direction
    boss.setDeltaMovement(throwVec);
}
```

#### 5d. Защита от двойного захвата

В `start()` — если `ACTIVE.containsKey(doomsday.getUUID())` или target уже захвачен другим думсдеем (для multiplayer-версии), abort.

#### 5e. Cooldown overhead

`COOLDOWN_TICKS = 1800` (90s). Оставлю — это ульт. Если хочешь меньше — скажи.

### Тест-чеклист

- Спавн HomelanderBoss → захват думсдеем → boss замирает на 3 сек, получает 18 hp ×3 урона, отлетает.
- Player2 (multiplayer) → захват → камера фиксируется в позиции перед думсдеем, нельзя двигаться, отлетает в throw.
- Mob (zombie) → как раньше работает.
- Невозможно double-grab за 90s.

---

## Пункт 6. VFX/partial overhaul + custom/legacy toggle

### Inventory текущих частиц у 3-х героев

**Homelander:**
- Боссовые goals — `ParticleTypes.LARGE_SMOKE`, `EXPLOSION`, `CLOUD`, `FLAME`, `CRIT`, `LIGHTNING-related`. Точный список — выпишу в реализации.
- Player-version: `homelander_eye_laser`, `flight_trail` particles в `LaserBeamRenderer`, и т.д.

**Regulus:**
- `MadnessHudOverlay` — клиентский overlay (Greek/kanji-glyphs), не particles.
- `RegulusGreedController` — vanilla particles (`PORTAL`, `WITCH`).
- Lion-roar — `EXPLOSION_EMITTER`, `SWEEP_ATTACK`.
- BloodRain — клиентский HUD, не particles.

**Doomsday:**
- Все абилки — `LARGE_SMOKE`, `POOF`, `EXPLOSION`, `WHITE_ASH`, `CRIT`, `DAMAGE_INDICATOR`, `ANGRY_VILLAGER`, `SMALL_FLAME`, `SONIC_BOOM`, `CLOUD`.
- Адаптация-flash — `PORTAL`.
- Всё ванильное.

### Что импортирую из art-source

Доступные кастомные текстуры (из `rezero-fx-textures.zip`):
- `powerborne/textures/particle/white_boom_0..15.png` — 16-кадровая анимация белого взрыва. Идеально для shockwave-приземления (Doomsday) и slam.
- `powerborne/textures/particle/sparks_particle_0..2.png` — искры (Homelander eye laser hit, Doomsday charge tackle).
- `soulsweapons/textures/particle/black_flame.png`, `purple_flame.png`, `dark_star.png` — для Regulus Madness и Doomsday Berserk.
- `soulsweapons/textures/particle/dazzling_particle.png`, `sun_particle.png` — для Homelander.
- `rezeromc/textures/particle/sword_explosion_1..6.png` — 6-кадровая анимация для Doomsday Smash.
- `rezeromc/textures/particle/white_mist_cloud.png` — для roar/ground-mist.

### Custom particle pipeline

Для каждой кастомной частицы:
1. PNG в `src/main/resources/assets/superheroes/textures/particle/<name>.png`.
2. Анимация (если многокадровая) — `<name>.png.mcmeta` с frametime.
3. Регистрация типа в `ModParticles.java` через `Registry.register(BuiltInRegistries.PARTICLE_TYPE, ...)`.
4. Client-side `ParticleFactoryRegistry.getInstance().register(...)` → стандартный `SpriteSet`-based particle (или кастомный класс если нужен specific motion).
5. `superheroes:particles/<name>.json` — `particles/`-resource с `[{ "name": "superheroes:<name>" }]` (multi-frame список).
6. Использовать `level.sendParticles(ModParticles.WHITE_BOOM, x, y, z, count, dx, dy, dz, speed)`.

### Toggle Custom ↔ Legacy — архитектурно

**Ответ на твой вопрос: ДА, можно сделать переключатель.**

Варианты места:

| Вариант | Плюсы | Минусы |
|---|---|---|
| A) **Mod Menu screen** (через Fabric `ModMenu` API) | Стандартное место, ищется по mod-id | Зависит от опц-мода ModMenu (большинство ставят) |
| B) **In-game settings screen** через keybind (`F8` → Superheroes Settings) | Не зависит от внешних модов | Своя UI-разработка |
| C) **Slash-command** (`/superheroes vfx custom`/`legacy`) | 0 UI-кода | Менее удобно |
| D) **Radial menu вкладка** (R → правый клик → Settings) | Контекстно | Усложняет radial-меню |

**Рекомендация: A + C** — ModMenu screen (если установлен) + fallback /команда. Сами Fabric-моды в 99% случаев это и делают.

### Реализация toggle

1. `client/config/SuperheroesClientConfig.java` — singleton, читает/пишет `config/superheroes-client.json`:
   ```json
   { "vfxMode": "custom" }  // или "legacy"
   ```
2. `client/screen/VfxSettingsScreen.java` — простой `Screen` с двумя кнопками + preview-particle-spawn по нажатию.
3. `ModMenuApi` (опц. — отдельный entrypoint в `fabric.mod.json`):
   ```
   "modmenu": [ "com.example.superheroes.client.modmenu.SuperheroesModMenuApi" ]
   ```
   Soft-dep в `fabric.mod.json` через `"suggests": { "modmenu": "*" }`. Без ModMenu меню недоступно — даём slash-команду.
4. Все вызовы `level.sendParticles` для героев Homelander/Regulus/Doomsday оборачиваются в helper:
   ```java
   ParticlePresets.shockwave(level, x, y, z, radius);
   // внутри:
   if (ClientConfig.vfxMode == LEGACY) sendParticles(LARGE_SMOKE,...);
   else sendParticles(ModParticles.WHITE_BOOM,...);
   ```
   Поскольку `sendParticles` — server-side (sends to all clients), решение **per-client** требует, чтобы клиент сам решал какой sprite рендерить — для этого:
   - **Подход 1 (проще)**: server отправляет custom particle всегда, клиент в `ParticleFactory` смотрит `ClientConfig.vfxMode` и при LEGACY рисует ванильный sprite. То есть один `ParticleType` — два рендера.
   - **Подход 2 (server-toggle)**: один глобальный mode для сервера. Меняется командой, broadcastится. Менее гибко но проще.

   **Выбираю Подход 1** — клиент-локальный toggle, ничего не ломается на multiplayer.

### Что конкретно меняется

| Hero | Event | Old particle | New custom particle |
|---|---|---|---|
| Doomsday | onLanded WEAK | LARGE_SMOKE | white_boom (короткий) |
| Doomsday | onLanded NORMAL | LARGE_SMOKE+POOF | white_boom + sparks |
| Doomsday | onLanded EPIC | LARGE_SMOKE+POOF+EXPLOSION | white_boom (full anim) + sword_explosion + sparks |
| Doomsday | Smash | EXPLOSION+LARGE_SMOKE+POOF | sword_explosion + white_mist |
| Doomsday | Roar | SONIC_BOOM+CLOUD | white_mist_cloud + dark_star |
| Doomsday | BoneSpike | WHITE_ASH+CRIT+DAMAGE_INDICATOR | sparks + dazzling |
| Doomsday | Berserk | ANGRY_VILLAGER+SMALL_FLAME | purple_flame + dark_star |
| Doomsday | DoomGrip | PORTAL+LARGE_SMOKE | dark_star + white_boom |
| Homelander | Eye laser hit | (current) | sparks + sun_particle |
| Homelander | Boss laser sweep | LARGE_SMOKE | sparks + sword_explosion |
| Homelander | Boss handclap | EXPLOSION+CLOUD | white_boom + white_mist |
| Homelander | Boss shockwave dive | EXPLOSION+POOF | white_boom (full anim) + sparks |
| Homelander | Boss sonic slam | EXPLOSION | sword_explosion + sparks |
| Regulus | Lion roar | EXPLOSION_EMITTER+SWEEP | white_boom + dark_star |
| Regulus | Madness aura | PORTAL+WITCH | purple_flame + black_flame |
| Regulus | Greed magnet | PORTAL | dark_star (orbit) |
| Regulus | Counter strike | (current) | sword_explosion + sparks |

### Тест-чеклист

- В Mod Menu (или /команде): toggle `custom` ↔ `legacy`.
- Custom: новые анимированные частицы.
- Legacy: ванильные как до v2.2.3.
- Сохраняется между перезапусками (config-файл).
- Multiplayer: каждый игрок видит свой режим.

---

## Пункт 7. «Полный фарш» — GUI + sounds + textures для 3 героев

После согласования по п.6 импортирую (отдельным мини-PR в той же 2.2.3 или подP2):

### GUI / Иконки абилок

Из `powerborne/textures/gui/abilities/`:
- `superman/heat_vision.png` → Homelander eye_lasers icon.
- `superman/super_punch.png` → Homelander hand_clap (boss).
- `superman/freeze_breath.png` → backup для freeze-абилок.
- `superman/xray_vision.png` → Homelander x_ray.
- `superman/flight_boost.png`, `solar_upgrade.png`, `super_speed.png` → Homelander prog.
- `void/darkness_projection.png`, `void/field.png` → Regulus madness, greed.
- `void/teleport.png`, `void/telekinesis.png` → Regulus counter, magnet.
- Под Doomsday — нет прямых аналогов; адаптирую `superman/super_punch.png` (smash), `void/darkness_projection.png` (roar), `void/field.png` (berserk).

Куда: `assets/superheroes/textures/gui/abilities/<hero>/<ability>.png`.
Где использовать: `AbilitiesTooltipHud`, `RadialMenuHud`, `JarvisOverlayHud` (если применимо).

### Звуки

Из `soulsweapons/sounds/`:
- `night_prowler_scream.ogg` → Doomsday roar (см. п.2.2).
- `hard_boss_spawn.ogg` → Doomsday transform / berserk activate.
- `hard_boss_death_long.ogg` → Doomsday death (broadcast).
- `darkness_rise.ogg` → Regulus madness enter.
- `gatling_gun_startup/barrage/stop.ogg` → Iron Man unibeam (опц., если хочешь — потом).
- `knight_charge_sword.ogg` → Doomsday charge tackle wind-up.
- `knight_sword_smash.ogg` → Doomsday smash impact.
- `blinding_light_explosion.ogg` → Homelander handclap.
- `day_stalker_chaos_storm.ogg` → Regulus counter strike.

Все OGG-Vorbis уже, конвертация не нужна. Просто скопировать в `assets/superheroes/sounds/<hero>/` и зарегистрировать в `sounds.json`.

### Textures (минор)

- `powerborne/textures/particle/sparks_*.png`, `white_boom_*.png` — уже учтено в п.6.
- `rezeromc/textures/particle/*` — учтено в п.6.

### Скоуп

Импорт **только под 3 героев**. Sung Jinwoo / Iron Man / Captain America / Goku / Naruto не трогаю. Под них — отдельный круг по твоему запросу.

### Тест-чеклист

- В радиальном меню (R) у Homelander/Regulus/Doomsday — иконки, не плейсхолдер.
- При активации абилок — кастомный звук вместо ванильного `RAVAGER_ROAR`/`WARDEN_*`.
- В тултипах (H toggle) — иконка слева от названия абилки.

---

## Порядок реализации (зависимости)

1. **п.1** — damage types (база, не зависит ни от чего).
2. **п.2.1** — убрать RAVAGER из Doomsday landing (1 файл, 30 sec).
3. **п.4** — tier 7 баффы (1 файл, ~10 строк).
4. **п.3a** — стандартизация порога адаптации damage (1 файл).
5. **п.5** — Doom Grip фикс (1 файл, ~30 строк + new method).
6. **п.7** (звуки) — импорт OGG → `ModSounds`.
7. **п.2.2** — заменить ravager-рёв на `ModSounds.DOOMSDAY_ROAR` (использует п.7).
8. **п.3b** — adaptation к status effects (mixin + controller, средняя сложность).
9. **п.6** — VFX overhaul + toggle (большой блок, отдельный sub-commit).
10. **п.7** (GUI) — иконки в HUD-ах.

Каждый пункт — отдельный commit, всё в одном PR `v2.2.3-pre`.
В конце автоматически: build → jar → release upload.

---

## Что я хочу подтвердить перед началом кода

1. **Кастомный рёв Doomsday**: согласен на `night_prowler_scream` + `hard_boss_spawn` (микс)? Или другой из списка soulsweapons?
2. **Tier 7 эффекты**: добавить **Resistance III** (на тир 7) — оставить, или сильнее?
3. **VFX toggle UI**: ModMenu screen ОК, или хочешь отдельный keybind (например, `F8 → Superheroes Settings`)?
4. **Doom Grip cooldown**: оставить 90s, или меньше?
5. **Damage-types boss-melee** (`MeleeAttackGoal`) — оставить ванильный `mob_attack` (логично, это просто кулак), или отдельный `homelander_melee`?
6. **Adaptation-effects скоуп**: всё негативное вкл. `BAD_OMEN`/`UNLUCK`, или только базовые (Weakness/Slowness/Poison/Wither/Hunger/Nausea/Blindness)?

Жду «ок по плану» или правки. После твоего ОК — начну ветку и кодинг по порядку выше, без CI, с автосборкой jar+релизом в финале.
