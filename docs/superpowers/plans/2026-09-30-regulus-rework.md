# Regulus rework — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Реворк Regulus по `docs/design/2026-09-30-regulus-rework-design.md`: сердца в мобах, «пустота» lion heart, дробь вместо roar, стазис-купол вместо embrace, контратака только от обидчика, ритуал евангелия с платой, MP-фикс, Veil-VFX и EMF-анимации.

**Architecture:** Всё внутри hero-модуля `hero/regulus/` + клиент-пары `client/hero/regulus/`; три точечных shared-расширения hero-agnostic по контракту ниже (`Hero.energyRegenBonus`, `TargetFilters` owned-mob гейт, `Player#causeFoodExhaustion`-миксин). Сервер — `ctx.ticks()`/`ctx.lifecycle()`/`OwnedSessionMap`/attachments; синк — synced attachment + unicast/trackingAndSelf S2C. **Visual Core gap (verified):** в репо НЕТ `core/net/VfxFx`/`VfxEventS2CPayload`/`VfxChannelS2CPayload`/`ctx.vfx`/`PlayerAnimator`/`client/core/vfx` — grep по всему `src/` (2026-09-30) находит только `FxBroadcast`, `BeamFx`, hero-локальный `ScorpionFx`, `VfxSettingsScreen` и `HudAnimator`. Существующей shared VFX/anim-системы, которую можно расширить, нет — поэтому hero-local `RegulusAnimS2CPayload` + `RegulusAnimationDriver` является канонным швом по прецеденту `hero/scorpion` (payload + client fx). Если shared Visual Core появится позже — миграция отдельной задачей.

**Tech Stack:** Java 21, Fabric 1.21.1 (verified: `Item#onUseTick/releaseUsing/getUseDuration(stack,entity)/getUseAnimation/useOnRelease/finishUsingItem`, `LivingEntity#startUsingItem/releaseUsingItem/stopUsingItem`, `Player#causeFoodExhaustion`, `EntityControlLock.acquire(Entity,ControlLockKind,ServerPlayer)`), `OwnedSessionMap`, `WorldDestructionPolicy.tryBreak/tryCarve`, `EnergyLocks`, GameTest (`src/gametest`), JUnit (`src/test`), Veil (client-only), EMF (hard dep — мод client-only, решение заказчика).

## Global Constraints

- **Тайминги анимаций — канон** (authored events, секунды от начала клипа): heart trigger 0.70с; debris_kick sweep 0.55с / impact 0.70с; mania effect 0.94с; embrace acquire 0.66с / effect 0.90с / release 1.62с; counter impact 0.32с; evangelium begin 0.32с / major 1.66с / complete 3.00с; deactivation end 0.96с. 20 тиков = 1с. **Округление:** `seconds * 20`, округление `ceil` (effect-тик наступает не раньше authored event).
- `src/main` никогда не импортирует `veil.*`; Veil живёт только в `src/client` за `FabricLoader.isModLoaded("veil")`.
- Server ticks — только через `ctx.ticks()` фазы; lifecycle — только `ctx.lifecycle()`; никаких `ServerTickEvents`/`DISCONNECT` в hero-коде.
- Per-player state — `OwnedSessionMap` (с `ClearOn`) или attachments; статические `Map<UUID,…>` запрещены.
- `Ability` никогда не проверяет свой кулдаун — router делает это сам. **Кулдаун ставится только на authored fire-тике** (обрыв каста до fire = бесплатная отмена).
- Каждая новая `*S2CPayload` регистрируется `ctx.payloads().s2c(...)` в `RegulusModule` И `ctx.receive(...)` в `RegulusClientModule` (sanity `everyHeroS2CPayloadHasARegisteredReceiver`).
- `en_us.json` и `ru_ru.json` правятся парой; новые damage types получают death-message ключи (`death.attack.regulus_*`).
- `TargetFilters` — единственный фильтр целей для AoE/атак (после Task 1 он же отсекает owned/союзных мобов — см. Task 1 Step 1b). Никаких своих Regulus-исключений в shared core.
- Весь разрушительный контакт с миром — только `WorldDestructionPolicy` (`tryBreak`/`tryCarve`).
- Client `*State`-холдеры регистрируют `ClientSessionState.register(...)` (sanity `assertClientStatesRegisterReset`); non-`Client*State` синглтоны — в `namedSingletons` список `ProjectSanityTest`.
- Звуки только OGG Vorbis; до создания ассета — проверить `art-source/` (роар-кандидат: `art-source/sounds/homelander/cUsersstravvberyDownloadsdragon-studio-epic-dragon-roar-364481.mp3` → ffmpeg → `sounds/regulus/debris_roar.ogg`).
- `lion_roar`/`counter_strike`/`mania_of_greed`/`greeds_embrace`/`lion_heart` — persisted ids; не переименовывать.
- **Каждый таск заканчивается зелёным `qualityGate` в своём порядке коммитов.** Таски, трогающие datagen-входы (damage types, теги, lang, модели), запускают `runDatagen` В ЭТОМ ЖЕ таске и коммитят `src/main/generated/` diff вместе с кодом. Characterization-тесты старого поведения в `RegulusGameTests`/`LifecycleSideEffectsGameTests`, которые новый дизайн намеренно ломает, перечисляются и переписываются/удаляются в том же коммите — строка «планируемая замена characterization-пинов» обязательна в каждом таске.
- **Клиент-часы:** любой прогресс на клиенте считается от `mc.level.getGameTime()` / synced gameTick-дедлайнов, НЕ `System.currentTimeMillis()` и НЕ `mc.player.tickCount` (сбрасывается на респавне).
- **Clip id:** канонический формат `animation.regulus.<clip>` (EMF-authored id); payload несёт его строкой. Не смешивать с голым `debris_kick`.

## Общие интерфейсы (контракты между тасками)

```java
// hero/regulus/runtime/RegulusCastState.java — серверная cast-машина (Task 2).
// Одна активная каст-сессия на игрока; state в OwnedSessionMap<UUID, Cast>
// (ClearOn: LEAVE, DEATH, HERO_CLEAR). Тикает через ctx.ticks().player
// (PLAYERS-фаза, dead players уже отсечены диспетчером).
public final class RegulusCastState {
    public record Spec(ResourceLocation abilityId, int fireTick, int castUntilTick,
                       float activateCost, int cooldownTicks, boolean damageInterrupts,
                       Runnable onFire, Runnable onInterrupt) {}
    public static boolean startCast(ServerPlayer p, Spec spec); // false если уже идёт каст
    public static boolean isCasting(ServerPlayer p, ResourceLocation abilityId);
    public static boolean hasFired(ServerPlayer p, ResourceLocation abilityId);
    public static int castTicksLeft(ServerPlayer p, ResourceLocation abilityId);
    public static void cancel(ServerPlayer p); // явная отмена (despawn/выбор)
    public static void register(HeroModuleContext ctx);
    // Tick: при now >= startTick+fireTick && !fired → fired=true,
    //   charge activateCost через ResourceController.charge (fail → cancel без кд),
    //   spec.onFire.run(), AbilityCooldowns.setCooldownTicks(abilityId, cooldownTicks).
    //   При now >= startTick+castUntilTick → запись удаляется (клип доиграл).
    // Прерывание урона: единый ALLOW_DAMAGE-листенер внутри RegulusCastState
    //   (damageInterrupts → cancel + onInterrupt + Slowness-cleanup).
    // Death/leave/hero-clear: OwnedSessionMap.ClearOn удаляет запись —
    //   эффект не применяется, кулдауна/стоимости нет (fired==false значит ничего
    //   не произошло — «до authored event эффекта не существует»).
}

// hero/regulus/runtime/RegulusHearts.java — набор сердец владельца (Task 3)
public final class RegulusHearts {
    public static int count(ServerPlayer owner);                        // живые сердца
    public static float damageScale(ServerPlayer owner);                // 1.0 + 0.02*count
    public static float energyRegenBonus(ServerPlayer owner);           // min(1.8f, 0.15f*count)
    public static int heartWindowTicks(ServerPlayer owner);             // 60 + 40*count
    public static void register(HeroModuleContext ctx);
    public static void dropAll(UUID ownerId);                           // снимает REGULUS_HEART_OWNER
        // со всех ещё загруженных носителей ДО очистки owner-set (leave/death/hero-clear)
}

// Shared seam (hero-agnostic): Hero получает хук
//   default float energyRegenBonus(ServerPlayer player) { return 0f; }
// и ResourceController.tick добавляет его к regen внутри существующего
// if (!EnergyLocks.isLocked(player)) — бонус уважает лок энергии.

// Shared seam: TargetFilters.harmableBy дополнительно отсекает
//   OwnableEntity.getOwner() == attacker и target.isAlliedTo(attacker) —
//   hero-agnostic, живёт в combat/, применимо ко всем AoE.

// hero/regulus/net/HeartsSyncS2CPayload.java — ТОЛЬКО владельцу (unicast):
//   ServerPlayNetworking.send(owner, ...) — ownerId в payload избыточен
//   (получатель и есть владелец), поле удалено.
public record HeartsSyncS2CPayload(java.util.List<Integer> heartEntityIds,
                                   boolean lionHeartActive, int overheatTicks)
        implements CustomPacketPayload { public static final Type<HeartsSyncS2CPayload> TYPE = ...; }
// Dirty-правило: пакет шлётся при изменении ЛЮБОГО из полей (ids-сет,
//   active-флаг, overheatTicks), не только при смене списка сердец.

// hero/regulus/net/RegulusAnimS2CPayload.java — FxBroadcast.trackingAndSelf
//   (tracking не включает кастера). clipId — "animation.regulus.*".
public record RegulusAnimS2CPayload(java.util.UUID playerId, String clipId)
        implements CustomPacketPayload { public static final Type<RegulusAnimS2CPayload> TYPE = ...; }

// hero/regulus/runtime/RegulusMadnessState — synced attachment (Task 8):
//   AttachmentRegistrar.syncWith НЕ существует → напрямую
//   AttachmentRegistry.create(ModId.of("regulus_madness"))
//     .initializer(() -> EMPTY)
//     .syncWith(STREAM_CODEC, AttachmentSyncPredicate.all());
//   StreamCodec (НЕ DFU — старый CODEC удаляется):
//   StreamCodec.composite(BOOL, ::madness, VAR_LONG, ::ritualUntilTick,
//       VAR_LONG, ::madnessUntilTick, RegulusMadnessState::new)
//   Поля: madness, ritualUntilTick, madnessUntilTick — серверные gameTick-
//   дедлайны. Клиент НИКОГДА не вычисляет прогресс от wall-clock: heartbeat/
//   glitch/FOV/кровь выводятся из (deadline - level.getGameTime()).
//   bonusLife НЕ дублируется в record — authoritative state = отдельный
//   synced attachment RegulusBonusLife.ATTACHMENT (Boolean, targetOnly).
//   Attachment остаётся неперсистентным (текущий код это задокументировал).
```

---

### Task 1: Пакет багов + shared-seams (без дизайн-решений)

**Files:**
- Modify: `src/main/java/io/github/grebeshok105/codex/hero/regulus/runtime/RegulusGreedController.java`
- Modify: `src/main/java/io/github/grebeshok105/codex/hero/regulus/runtime/RegulusMadnessController.java`
- Modify: `src/main/java/io/github/grebeshok105/codex/hero/regulus/runtime/RegulusMadnessState.java`
- Modify: `src/main/java/io/github/grebeshok105/codex/hero/regulus/runtime/RegulusBonusLife.java`
- Modify: `src/main/java/io/github/grebeshok105/codex/hero/regulus/runtime/RegulusTotemController.java`
- Modify: `src/main/java/io/github/grebeshok105/codex/hero/regulus/runtime/GreedCageController.java` (НЕ удалять — файл стирается в Task 6; здесь только отцепить вызовы)
- Modify: `src/main/java/io/github/grebeshok105/codex/hero/regulus/RegulusModule.java`
- Modify: `src/main/java/io/github/grebeshok105/codex/hero/regulus/ability/GreedsEmbraceAbility.java`
- Modify: `src/main/java/io/github/grebeshok105/codex/combat/TargetFilters.java` (shared seam — owned/allied мобы)
- Modify: `src/client/java/io/github/grebeshok105/codex/client/hero/regulus/hud/MadnessHudOverlay.java`
- Test: `src/gametest/java/io/github/grebeshok105/codex/gametest/RegulusGameTests.java`
- Modify: `src/gametest/java/io/github/grebeshok105/codex/gametest/LifecycleSideEffectsGameTests.java` (:95 вызывает `withBonusLife(true)`)

**Interfaces:**
- Consumes: `EntityControlLock`, `OwnedSessionMap.ClearOn`, `RegulusBonusLife.ATTACHMENT`, `OwnableEntity` (vanilla).
- Produces: расширенный `TargetFilters.harmableBy` (owned-mob гейт); `RegulusBonusLife.ATTACHMENT` — единственный authoritative источник бонус-лайфа.

- [ ] **Step 1: Фикс коллизии FREEZES — РАННИЙ отказ, до любых side effects** — в `RegulusGreedController.releaseAndFreeze` проверка `FREEZES.containsKey(victim.getUUID())` идёт ПЕРВОЙ строкой, до `MAGNETS.remove`, эффектов кастера, локов, таймера `CASTER_FREEZE_UNTIL`: занятая цель → actionbar `superheroes.regulus.already_greed` (en/ru) + `return`. Никаких стероидов/локов/состояния второму кастеру.
- [ ] **Step 1b: `TargetFilters` owned-гейт (shared, hero-agnostic)** — `harmableBy` дополнительно `return false` при `target instanceof OwnableEntity o && attacker.equals(o.getOwner())` и при `target.isAlliedTo(attacker)`; комментарий «pvp=false/team» расширяется «+ owned/allied mobs». Все Regulus-AoE (`embrace`, `mania`, `debris`, `counter`) наследуют правило бесплатно.
- [ ] **Step 2: `GreedCageController` выключается** — убрать `ctx.ticks().global(GreedCageController::tick)` из `RegulusModule.register` и `GreedCageController.create(...)` из `GreedsEmbraceAbility` (файл стирается в Task 6).
- [ ] **Step 3: Friendly-fire fix в `GreedsEmbraceAbility`** — цели только `TargetFilters.hostileTo(player)` (после Step 1b покрывает и петов). `fellOutOfWorld`-урон от лендинга удаляется вместе с клеткой в Task 6.
- [ ] **Step 4: Убрать "卐" и спорные глифы** из `MadnessHudOverlay.FLOATING_SYMBOLS` (оставить Ω/Σ/руны/кандзи).
- [ ] **Step 5: `RegulusBonusLife` чистится** — в `clearMadness`: `player.setAttached(RegulusBonusLife.ATTACHMENT, Boolean.FALSE)`.
- [ ] **Step 6: Мёртвое поле `bonusLifeAvailable`** из `RegulusMadnessState`: компонент рекорда, `withBonusLife`, читатели. `LifecycleSideEffectsGameTests:95` → `regulus.setAttached(RegulusBonusLife.ATTACHMENT, Boolean.TRUE)`.
- [ ] **Step 7: `DODGE_COOLDOWN`-карта — все ссылки**: поле, `put`, `clear()` в `resetAll`, `remove()` в `clearMadness`.
- [ ] **Step 8: Тотем-ревайв не стрипает пассивы** — `RegulusTotemController` :45 и :55 (`removeAllEffects` в обоих revive-ветках) → фильтр по `MobEffectCategory.HARMFUL` (по образцу Lion Heart Step 4 таска 4): пассивы/баффы/внешние позитивы остаются.
- [ ] **Step 9: Тесты** — регрессия: два кастера не ломают фриз одной цели, `clearMadness` пишет `RegulusBonusLife=false`, totem-ревайв сохраняет hero-пассивы, owned-mob фильтр (`tamedWolfNotTargeted`). Удаляемые/переписываемые characterization-пины этого коммита: embrace-friendly-fire пин (`greedsEmbraceHitAllNearby`), `bonusLifeAvailable`-пины.
- [ ] **Step 10:** `./gradlew qualityGate --no-daemon` зелёный → commit `fix(regulus): bug sweep — freeze collision, owned-mob targets, glyph cleanup, totem strip fix`.

---

### Task 2: Cast-машина `RegulusCastState` (инфраструктура для тасков 4–7)

**Files:**
- Create: `src/main/java/io/github/grebeshok105/codex/hero/regulus/runtime/RegulusCastState.java`
- Modify: `src/main/java/io/github/grebeshok105/codex/hero/regulus/RegulusModule.java` (tick-регистрация)
- Test: `src/gametest/java/io/github/grebeshok105/codex/gametest/RegulusGameTests.java`

**Interfaces:**
- Consumes: `OwnedSessionMap`, `AbilityCooldowns.setCooldownTicks`, `ResourceController.charge`, `ServerLivingEntityEvents.ALLOW_DAMAGE`.
- Produces: `RegulusCastState` (контракт выше) — tick-driver для всех one-shot кастов: `ctx.ticks().player(RegulusCastState::tickPlayer)` срабатывает для любого живого игрока, эффект вызывается ровно на fireTick независимо от `AbilityRouter.onTickActive`.

- [ ] **Step 1: Тесты (красные)** — `castDoesNotChargeBeforeAuthoredEvent`, `cancelledCastHasNoCooldown`, `castFiresExactlyAtFireTick`, `doubleCastRejected`, `castDiesWithPlayer`.
- [ ] **Step 2: Машина** — `OwnedSessionMap<UUID, Cast>` (`ClearOn` LEAVE/DEATH/HERO_CLEAR); `startCast` отклоняет повторный каст; `tickPlayer`: `now>=fireTick && !fired` → `charge(activateCost)` (fail → cancel + `onInterrupt`, БЕЗ кулдауна) → `onFire` → `setCooldownTicks`; `now>=castUntilTick` → remove.
- [ ] **Step 3: Прерывание уроном** — один `ALLOW_DAMAGE`-листенер внутри `RegulusCastState`: `spec.damageInterrupts && amount>0` → `cancel` + `onInterrupt` (кастеру — снять Slowness-каст, etc.); сам урон проходит (`return true`) — прерывание, а не блок.
- [ ] **Step 4: Тесты зелёные + commit** `feat(regulus): authored cast state machine (fire tick, free cancel)`.

---

### Task 3: Сердца («маленький король»)

**Files:**
- Create: `src/main/java/io/github/grebeshok105/codex/hero/regulus/runtime/RegulusHearts.java`
- Create: `src/main/java/io/github/grebeshok105/codex/hero/regulus/runtime/RegulusHeartMark.java` (transient `AttachmentType<UUID>`: `AttachmentRegistrar.FABRIC.transientType("regulus_heart_owner")`)
- Create: `src/main/java/io/github/grebeshok105/codex/hero/regulus/net/HeartsSyncS2CPayload.java`
- Create: `src/client/java/io/github/grebeshok105/codex/client/hero/regulus/state/ClientHeartsState.java`
- Modify: `src/main/java/io/github/grebeshok105/codex/hero/regulus/RegulusModule.java` (payload + контроллеры)
- Modify: `src/main/java/io/github/grebeshok105/codex/hero/regulus/RegulusAttachments.java` (алиасы)
- Modify: `src/main/java/io/github/grebeshok105/codex/hero/regulus/registry/RegulusDamageTypes.java` (+ `regulus_heart_backlash` spec: `BYPASSES_ARMOR, BYPASSES_RESISTANCE, BYPASSES_ENCHANTMENTS, BYPASSES_COOLDOWN, NO_KNOCKBACK` + `DamageSources.of(level, key)` БЕЗ attacker — self-cost, не атака)
- Modify: `src/main/java/io/github/grebeshok105/codex/hero/regulus/RegulusHero.java` (`energyRegenBonus` override → `RegulusHearts`)
- Modify: `src/main/java/io/github/grebeshok105/codex/core/hero/Hero.java` (+ default `energyRegenBonus(ServerPlayer)=0f`)
- Modify: `src/main/java/io/github/grebeshok105/codex/core/resource/ResourceController.java` (`tick` прибавляет `hero.energyRegenBonus(player)` внутри существующего `!EnergyLocks.isLocked`-гейта)
- Modify: `src/main/resources/assets/superheroes/lang/{en_us,ru_ru}.json` (`heart_lost` actionbar + `death.attack.regulus_heart_backlash`)
- Modify: `src/client/java/io/github/grebeshok105/codex/client/hero/regulus/RegulusClientModule.java` (receive)
- Create: `src/main/java/io/github/grebeshok105/codex/datagen/ModEntityTypeTagProvider.java` (первый entity-tag провайдер проекта; `heartless` = `wither, warden, ender_dragon, armor_stand`)
- Test: `src/gametest/java/io/github/grebeshok105/codex/gametest/RegulusGameTests.java`

**Interfaces:**
- Produces: `RegulusHearts` (контракт выше), `HeartsSyncS2CPayload`, `ClientHeartsState`, `superheroes:heartless` tag.
- Планируемая замена characterization-пинов: none (новая механика).

- [ ] **Step 1: Тесты (красные)** — `heartsMarkOnlyVanillaMobs`, `hostileMobCannotCarryHeart`, `heartsCapAtTwelve`, `heartBacklashOnBearerDeath`, `heartLostQuietlyOnUnload`, `heartRejectsCustomEntity`, `armorStandCannotCarryHeart`, `heartEnergyRegenScales`, `heartRegenRespectsEnergyLock`, `heartDamageScalesMelee`, `heartOwnerCleanupRemovesBearerMarks`, `overheatSyncChangesWithoutHeartSetChange`.
- [ ] **Step 2: Метка и скан** — `RegulusHeartMark.ATTACHMENT` (transient UUID); `collect` раз в 20t (`ctx.ticks().player`): namespace `minecraft` через `EntityType.getKey(type)`, не `heartless`-tag, не `Player`, **не враждебная** (`!(e instanceof Enemy)` — «маленький король» носит сердца в мирных/нейтральных, зомби-типа в кандидаты не попадают), не уже отмечена; кап 12.
- [ ] **Step 3: Смерть носителя** — `ServerLivingEntityEvents.AFTER_DEATH`: если `REGULUS_HEART_OWNER` == owner и owner онлайн → 10% maxHp через `regulus_heart_backlash` + Weakness I 60t + actionbar. Owner недоступен → молча (несуществующему владельцу откат не шлём).
- [ ] **Step 4: Lifecycle носителей** — `ServerEntityEvents.ENTITY_UNLOAD`: тихое удаление из owner-set; `dropAll(ownerId)` на leave/death/hero-clear (через `ctx.lifecycle()`): снимает `REGULUS_HEART_OWNER` со всех ЗАГРУЖЕННЫХ носителей до очистки set — иначе моб навечно «занят».
- [ ] **Step 4b: Эффекты сердец** — `energyRegenBonus(owner)` кредитуется через новый `Hero`-хук (ResourceController, внутри EnergyLocks-гейта); `damageScale(owner)` = `1 + 0.02*count` (мультипликативно): melee — через `RegulusHero`-модификатор `ATTACK_DAMAGE` в `applyPassives`/динамический пересчёт при изменении count (множитель `ADD_MULTIPLIED_TOTAL` на `(damageScale-1)`); ability-paths (`debris`, `counter`) умножаются явно в своих контроллерах.
- [ ] **Step 5: Payload** — `HeartsSyncS2CPayload(ids, lionHeartActive, overheatTicks)` unicast владельцу каждые 20t при ЛЮБОМ изменении полей; `ClientHeartsState` + `ClientSessionState.register` в том же коммите.
- [ ] **Step 6: Подсветка владельцу** — `WorldRenderEvents`-пасс (паттерн `BeamRenderer`/`IronManEspRenderer`; `ctx.playerLayer` не подходит — рисует только модель игрока): золотой контур entity-ids из `ClientHeartsState`.
- [ ] **Step 7: Datagen** — `runDatagen --no-daemon`, ревью diff (`heartless` entity-tag + `regulus_heart_backlash` damage type + теги), commit generated-вывода в этом же коммите.
- [ ] **Step 8: Тесты зелёные + commit** `feat(regulus): little-king hearts — vanilla anchors, cap 12, backlash, regen/damage scale`.

---

### Task 4: Lion's Heart — «пустота», абсолютная защита, перегрев

**Files:**
- Create: `src/main/java/io/github/grebeshok105/codex/hero/regulus/runtime/LionHeartController.java` (`register(ctx)`, `OwnedSessionMap<UUID, Long> windowDeadlines`, `OwnedSessionMap<UUID, Set<UUID>> frozenProjectiles`)
- Create: `src/main/java/io/github/grebeshok105/codex/hero/regulus/net/RegulusAnimS2CPayload.java` (первый потребитель; регистрация `RegulusModule`/`RegulusClientModule`)
- Create: `src/main/java/io/github/grebeshok105/codex/mixin/PlayerExhaustionMixin.java` (`Player#causeFoodExhaustion` — hero-agnostic гейт: hero хук `blocksExhaustionWhile(player)` или прямой hero-check — минимальный scoped вариант)
- Modify: `src/main/java/io/github/grebeshok105/codex/hero/regulus/ability/LionHeartAbility.java` (Resistance V удаляется — заменяется damage-gate)
- Modify: `src/main/java/io/github/grebeshok105/codex/hero/regulus/runtime/RegulusMadnessController.java` (если нужен общий internal-damage список — см. Step 3)
- Modify: `src/main/java/io/github/grebeshok105/codex/client/hero/regulus/hud/CracksOverlayHud.java`
- Modify: `src/main/java/io/github/grebeshok105/codex/client/hero/regulus/state/ClientHeartsState.java` (окно/перегрев)
- Modify: `src/main/java/io/github/grebeshok105/codex/hero/regulus/RegulusModule.java`
- Test: `src/gametest/java/io/github/grebeshok105/codex/gametest/RegulusGameTests.java`

**Interfaces:**
- Consumes: `RegulusCastState` (fire 14t), `RegulusHearts` (count/window), `EntityControlLock.acquire(entity, NO_GRAVITY, owner)`, `FxBroadcast.trackingAndSelf`, `HeartsSyncS2CPayload`.
- Produces: `LionHeartController.isActive/isBlocking(player)`, `frozenProjectiles`-set, per-mechanic release; `RegulusAnimS2CPayload` (драйвер — Task 9).
- Планируемая замена characterization-пинов: пин «активация надевает Resistance V» → «активирует damage-gate»; `removeAllEffects`-пин → negative-only фильтр.

- [ ] **Step 1: Каст** — `tryActivate` → `RegulusCastState.startCast(player, Spec(LION_HEART, fire=14, until=32, cost=0, cooldown=0, damageInterrupts=true, onFire=activateShield))`; anim `animation.regulus.lion_heart_activation`. Toggle регистрируется как активная абилка сразу, но `costPerTick()`=0 пока `!isBlocking` — drain 10/тик начинается с fire-тика (вернуть в `costPerTick` условно по `hasFired` — см. Ability contract: `costPerTick` не константа, читает `RegulusCastState.hasFired`).
- [ ] **Step 2: Абсолютная защита** — `LionHeartController` регистрирует `ALLOW_DAMAGE`-листенер: `entity instanceof ServerPlayer p && isBlocking(p)` → `false` для ВСЕХ источников, кроме типов `RegulusDamageTypes` internal-set (`heart_backlash`, `lion_heart_overheat`, `blood_price`, `counter_strike` — internal true-cost проходит); `Resistance V`/`removeAllEffects` из `LionHeartAbility` удаляются.
- [ ] **Step 3: Прожектайлы — отдельное владение** — `frozenProjectiles` (OwnedSessionMap `ClearOn` LEAVE/DEATH/HERO_CLEAR): per-tick скан `Projectile` в 4б → `acquire(NO_GRAVITY)` + `setDeltaMovement(ZERO)` каждый тик; выход из радиуса → `release(victim, NO_GRAVITY, ownerId)` по `victim`-токену (НЕ `releaseOwnedBy` — он снимет чужие локи mania/counter того же игрока); выключение/смерть/clear → итерация по своему set, `release` каждого.
- [ ] **Step 3b: Голод** — `PlayerExhaustionMixin` на `causeFoodExhaustion`: `isBlocking(player)` → cancel. (Mixin единственный scoped-путь: FoodData-уровень не знает игрока; hero-локальный, тело ≤20 строк.)
- [ ] **Step 3c: Перегрев-визуал** — `CracksOverlayHud` (спека §2): стадия = `overheatTicks/40t`, вспышка на форс-офф; данные — `ClientHeartsState` (поле `overheatTicks` из payload Task 3).
- [ ] **Step 4: Окно и перегрев** — `windowDeadlines` = `now + heartWindowTicks`; дальше `hurt(regulus_lion_heart_overheat, ramp)` (`1.5hp/s` → `0.75f`/10t, +0.25f каждые 40t), при hp≤4 → принудительный `AbilityRouter.deactivate` + `setCooldownTicks(LION_HEART, 600)`.
- [ ] **Step 5: Выключение** — импульс push 1.5/0.6 по `isAlive` целям (как сейчас) + `release` всех своих прожектайлов → падают.
- [ ] **Step 6: Тесты** — `lionHeartBlocksExternalDamageAfterTrigger`, `lionHeartAllowsInternalTrueCostDamage`, `lionHeartStopsExhaustion`, `projectilesFreezeAndDrop`, `projectileLocksReleaseIndividually`, `windowThenOverheat`, `forcedOffAtLowHp`, `castInterruptedBeforeTrigger`.
- [ ] **Step 7: Commit** `feat(regulus): lion heart — cast-gated absolute void, projectile hold, overheat`.

---

### Task 5: Дробь (`lion_roar` → `debris_kick`)

**Files:**
- Modify: `src/main/java/io/github/grebeshok105/codex/hero/regulus/ability/LionRoarAbility.java`
- Create: `src/main/java/io/github/grebeshok105/codex/hero/regulus/runtime/DebrisKickController.java`
- Create: `src/main/java/io/github/grebeshok105/codex/hero/regulus/sound/RegulusSounds.java` (`debris_roar`, `debris_impact` — hero-local паттерн `IronManSounds`)
- Create: `src/main/resources/assets/superheroes/sounds/regulus/*.ogg` (конвертация из `art-source/sounds/homelander/` mp3 → OGG)
- Modify: `src/main/resources/assets/superheroes/sounds.json`
- Modify: `src/main/java/io/github/grebeshok105/codex/hero/regulus/registry/RegulusDamageTypes.java` (+ `regulus_debris`: `BYPASSES_COOLDOWN`, exhaustion 0.1 + `death.attack.regulus_debris` lang)
- Modify: `src/main/resources/assets/superheroes/lang/{en_us,ru_ru}.json` («Пинок бездны» + death key)
- Test: `src/gametest/java/io/github/grebeshok105/codex/gametest/RegulusGameTests.java`

**Interfaces:**
- Consumes: `RegulusCastState` (fire 14t), `WorldDestructionPolicy.tryBreak`, `RegulusHearts.damageScale`, `RegulusAnimS2CPayload`.
- Produces: `DebrisKickController.fire(player)`, `RegulusSounds` hero-локальная регистрация.
- Планируемая замена characterization-пинов: старый roar-AoE пин (`lionRoar*`) → дробь-веер тесты.

- [ ] **Step 1: Каст** — `Spec(LION_ROAR, fire=14, until=44, activateCost=250, cooldown=400, damageInterrupts=true, onFire=DebrisKickController::fire)`; `costOnActivate()` → `0f` (заряд на fire-тике машиной), HUD-отображение цены — из Spec; Slowness I на каст снимается в `onInterrupt`/при окончании `castUntil`. anim `animation.regulus.debris_kick`.
- [ ] **Step 2: Веер с окклюзией** — 9 лучей конус 35°, дальность 16: по каждому лучу — block-рейкаст `ClipContext.Block.COLLIDER` до первого **неразрушимого** блока (разрушимый через `WorldDestructionPolicy.tryBreak`, ≤3 на луч — луч продолжается, лимит исчерпан или блок неразрушим → луч останавливается на этом блоке); entity-хит — `AABB`-пересечение луча (не грубый step-scan: `entity.getBoundingBox().clip(eye, rayEnd)`/`RayTrace` по конусу) по `TargetFilters.hostileTo`. **Pellet-rule (как ванильный дробовик):** одна цель получает урон от каждого луча, чей AABB она пересекает — т.е. точка-бланк по крупной цели может нанести несколько ×7; отдельного dedupe нет (открытый вопрос к заказчику — см. Open design questions). Урон `7 * damageScale(owner)`, `regulus_debris`, отброс.
- [ ] **Step 3: Damage type + lang + звук** — `regulus_debris` spec, `ability.superheroes.lion_roar` → «Пинок бездны», `death.attack.regulus_debris`; `RegulusSounds.DEBRIS_ROAR/DEBRIS_IMPACT` (ryk+хруст из art-source mp3→ogg), проигрываются на fire-тике через `level.playSound`/`FxBroadcast` (impact — всем вокруг, рык — trackingAndSelf).
- [ ] **Step 4: Тесты** — `debrisFanHitsConeOnly`, `debrisBreaksBlocksButNotBedrock`, `debrisRayBlockedByUnbreakable`, `castInterruptCancelsShot`, `castDoesNotChargeBeforeAuthoredEvent`, `soundEventRegistered` (sounds.json sanity: файл существует, OGG).
- [ ] **Step 5: Datagen** — `runDatagen`, ревью `regulus_debris` diff, commit generated.
- [ ] **Step 6: Commit** `feat(regulus): debris_kick — cast-gated block-breaking shotgun fan`.

---

### Task 6: Стазис-купол (`greeds_embrace` → `GreedStasisController`)

**Files:**
- Modify: `src/main/java/io/github/grebeshok105/codex/hero/regulus/ability/GreedsEmbraceAbility.java`
- Create: `src/main/java/io/github/grebeshok105/codex/hero/regulus/runtime/GreedStasisController.java`
- Delete: `src/main/java/io/github/grebeshok105/codex/hero/regulus/runtime/GreedCageController.java`
- Modify: `src/main/java/io/github/grebeshok105/codex/hero/regulus/RegulusModule.java`
- Modify: `src/main/java/io/github/grebeshok105/codex/hero/regulus/runtime/RegulusGreedController.java` (ветка stasis внутри ЕДИНСТВЕННОГО `ALLOW_DAMAGE`-хука — arbitration path)
- Test: `src/gametest/java/io/github/grebeshok105/codex/gametest/RegulusGameTests.java`

**Interfaces:**
- Consumes: `RegulusCastState` (acquire-якорь 13t, effect 18t), `EntityControlLock`, `TargetFilters` (после Task 1 Step 1b покрывает петов), единый ALLOW_DAMAGE в `RegulusGreedController`.
- Produces: `GreedStasisController.tryOpen/inStasis`, очередь урона кап 35%.
- Планируемая замена characterization-пинов: cage/молнии пины, `fellOutOfWorld`-лендинг пин — удаляются.

- [ ] **Step 1: Каст** — `Spec(GREEDS_EMBRACE, fire=18, until=44, activateCost=400, cooldown=700, damageInterrupts=true, onFire=open dome)`; `costOnActivate()` → 0; acquire-фаза (0–13t) фиксирует прицельную точку (`anchor` = position target на момент acquire-fire 13t); anim `animation.regulus.greeds_embrace_cast`.
- [ ] **Step 2: Купол** — r8, 80t; `LivingEntity` внутри через `TargetFilters.hostileTo`: **мобы** → `EntityControlLock` NO_AI + NO_GRAVITY, velocity=0; **игроки** (`ServerPlayer`) → positional lock как в существующем greed-фризе: сохранённая позиция + `connection.teleport(lockPos)` каждый тик + `setDeltaMovement(ZERO)` + `hurtMarked` (NO_AI игрока не останавливает). Входящий урон — очередь в ЕДИНОМ `ALLOW_DAMAGE`-листенере `RegulusGreedController`: `inStasis` проверяется ПЕРВОЙ и short-circuit-ит freeze-ветку (двойная очередь запрещена); уже замороженная сущность в очередь повторно не кладётся.
- [ ] **Step 3: Закрытие — порядок release** — `state → closing` (выход из `inStasis`) → `release` локов → агрегация очереди с капом 35% maxHp → `hurt` → импульс от центра → удаление state. Caster leave/death/hero-clear (`ClearOn`) выполняет тот же release-flow: `OwnedSessionMap` сам по себе только чистит запись — side effects в явном drop-callback перед удалением.
- [ ] **Step 4: Удаление клетки** — `GreedCageController` + `spawnFrozenLightning` стираются.
- [ ] **Step 5: Тесты** — `stasisSkipsAllies` (союзник-игрок И tameable-питомец), `stasisActuallyPinsPlayer` (игрок пытается двигаться — позиция возвращается), `stasisCapsAtThirtyFivePercent`, `stasisReleaseDamageIsNotRequeued`, `stasisReleasesOnCasterGone`, `oneDomePerCaster`.
- [ ] **Step 6: Commit** `feat(regulus): stasis dome replaces lightning cage; player pin, capped release, single damage arbiter`.

---

### Task 7: Mania + Counter баланс

**Files:**
- Modify: `src/main/java/io/github/grebeshok105/codex/hero/regulus/ability/ManiaOfGreedAbility.java`, `runtime/RegulusGreedController.java`
- Modify: `src/main/java/io/github/grebeshok105/codex/hero/regulus/ability/CounterStrikeAbility.java`, `runtime/RegulusMadnessController.java`
- Modify: `src/main/resources/assets/superheroes/lang/{en_us,ru_ru}.json` (`counter_no_target` actionbar)
- Test: `src/gametest/java/io/github/grebeshok105/codex/gametest/RegulusGameTests.java`

**Interfaces:**
- Consumes: `RegulusCastState` (mania effect 0.94с → `ceil(18.8)=19t`), `RegulusAnimS2CPayload`, `AbilityCooldowns`, `EnergyLocks`.
- Produces: freeze 150 энергии, player-freeze 80t/mob 200t, release cap 0.40/0.60; counter timeline по §4.3.
- Планируемая замена characterization-пинов: пины старого counter-урона (30+27+взрыв), nearest-hostile фолбэк, `EnergyLocks` 15с — переписываются.

- [ ] **Step 1: Mania** — `Spec(MANIA_OF_GREED, fire=19, until=42, activateCost=0, cooldown=500, damageInterrupts=true, onFire=startMagnet)`; `tryActivate` только стартует каст (магнит не выбирает цель до fire); drain 8/тик начинается на fire-тике (`costPerTick` читает `hasFired`); `onDeactivate` ставит кулдаун ТОЛЬКО если магнит реально стартовал (`hasFired`/`MagnetState` существовал — обрыв до 19t не даёт кд); фриз-цена 150 энергии (нет → жертва отпускается без лока); кап релиза 40%/60% maxHp; убрать Resist V/Speed III/Str II/+2kb из `releaseAndFreeze`.
- [ ] **Step 1b: Counter-клип** — `counter_attack` (0.90с) стартует на последние ~6t фазы ARRIVE (см. Step 2): клиентская анимация якорится приёмом payload, authored `impact` 0.32с ≈ 6.4t → SLAM-контакт совпадает с битом удара.
- [ ] **Step 2: Counter retune + authored timeline** — `findTarget` берёт ТОЛЬКО `RegulusMadnessController`-lastDamager: `LAST_DAMAGER_TIMEOUT_TICKS` 200→**240t**, `SEARCH_RANGE` 120→**40**; `getLastHurtByMob`-фолбэк (:73) и nearest-hostile scan удаляются; валидного обидчика нет → `tryActivate` false + `RegulusHero.onAbilityDenied` пишет actionbar `counter_no_target`. Timeline: `COUNTER_LIFT_TICKS=20` (лифт), `COUNTER_ARRIVE_TICKS=20` (пауза+клип-старт на 14-м тике ARRIVE — его impact @6t встречает SLAM на 20-м); на `ARRIVE→SLAM` переходе — единственный `hurt` `min(45, 15 + 0.15*maxHp) * damageScale(owner)` (`counter_strike`); старый `hurt(30f)` на SLAM-входе удаляется, `finalSlam`-урон удаляется тоже — суммарный урон один раз. `EnergyLocks.lockTicks` 15*20→**8*20**; `COOLDOWN_TICKS` 600→**800**; кратер `CRATER_RADIUS` 4→**3**, `CRATER_DEPTH` 20→**8**; сцепка: teleport-высота слэма разводится отдельной `SLAM_DROP_DEPTH=20` (старый код читал `CRATER_DEPTH` для `impact.getY() - CRATER_DEPTH + 1`). **Взрыв строго визуальный**: `level.explode(...)` убирается — даже `ExplosionInteraction.NONE` наносит entity damage (нарушит кап и заденет окружающих); вместо него `sendParticles` (EXPLOSION_EMITTER/LARGE_SMOKE уже есть) + `level.playSound` + client-side screen-shake/VFX через существующий визуальный payload/событие; кратер — только `WorldDestructionPolicy.tryCarve`.
- [ ] **Step 3: Тесты** — `counterRequiresRealAttacker`, `counterDamageFormula` (`counterDealsFormulaOnce` — суммарный урон один раз, не 30+27), `counterVisualExplosionDealsNoExtraDamage`, `counterImpactMatchesAnimationEvent` (SLAM-контакт на тике ARRIVE==20), `craterShrunk`, `maniaFreezeDeniedAtZeroEnergy`, `maniaReleaseCapRespected`, `maniaDoesNotTargetBefore19t`.
- [ ] **Step 4: Commit** `balance(regulus): mania/counter retuned — real attacker, one-hit formula, visual-only slam`.

---

### Task 8: Евангелие — ритуал, плата, MP-фикс

**Files:**
- Modify: `src/main/java/io/github/grebeshok105/codex/hero/regulus/item/EvangelionItem.java`
- Modify: `src/main/java/io/github/grebeshok105/codex/hero/regulus/runtime/RegulusMadnessController.java`, `RegulusMadnessState.java`, `RegulusBonusLife.java` (synced)
- Modify: `src/client/java/io/github/grebeshok105/codex/client/hero/regulus/state/ClientMadnessState.java`, `hud/MadnessHudOverlay.java`, `hud/ClientHudGlitch.java`, `hud/EvangelionZoomHud.java`, `hud/BloodRainHud.java`, `RegulusClientModule.java`
- Create: `src/client/java/io/github/grebeshok105/codex/client/hero/regulus/render/EvangelionBookLayer.java` (prop-книга)
- Modify: `src/main/resources/assets/superheroes/models/item/evangelion.json` → Blockbench-экспорт (geo-модель книги + анимация раскрытия); полный item-renderer путь — см. Step 6
- Delete: `net/MadnessSyncS2CPayload.java`, `net/MadnessVisualS2CPayload.java` (state → synced attachment; события → edge-detector по фронту флага)
- Test: `src/gametest/java/io/github/grebeshok105/codex/gametest/RegulusGameTests.java`

**Interfaces:**
- Consumes: `RegulusCastState`-семантика (ритуал — item-use канал, не cast-state: машина для абилок, ритуал живёт в vanilla item lifecycle), synced attachment, `player.startUsingItem/releaseUsingItem`.
- Produces: `beginRitual/interruptRitual/completeRitual`, `madnessUntilTick`, `bloodPrice` drain 0.6hp/с, `madnessDurationTicks=900`.
- Планируемая замена characterization-пинов: `startReading`-пины (200t неуязвимый канал → 60t прерываемый), `bonusLifeAvailable`-пины (Task 1), `madness_sync` receiver-пины.

- [ ] **Step 1: Item→vanilla channel** — `use`: `player.startUsingItem(hand)` на обеих сторонах (server через `InteractionResultHolder.consume` + `use` на сервере; клиент сам зовёт `use()` → `startUsingItem` — без этого `releaseUsing` не дойдёт) + `RegulusMadnessController.beginReading` (state `ritualUntilTick=now+60`); `getUseDuration(stack, entity)`=60 (двухаргументная 1.21.1-сигнатура); `getUseAnimation`=`UseAnim.BOW`/`CUSTOM`; `useOnRelease` default `false` (early release не вызывает `finishUsingItem`).
- [ ] **Step 2: Прерывание** — существующий `ALLOW_DAMAGE`-хук в `RegulusMadnessController` (урон ≥4hp во время ритуала) → `player.releaseUsingItem()` (НЕ `stopUsingItem` — тот не вызывает `Item#releaseUsing`, вся abort-логика молча пропустится) + `player.getCooldowns().addCooldown(item, 400)`; movement-cancel: `onUseTick` сравнивает позицию с точкой начала (>0.5б → тот же `releaseUsingItem`); Slowness II на канал. Неуязвимости нет.
- [ ] **Step 3: Завершение** — `finishUsingItem` (срабатывает по `getUseDuration`=60 через `updateUsingItem` → `completeUsingItem`): `completeRitual` — `madness=true`, `madnessUntilTick=now+900`, баффы (как сейчас), `setAttached(RegulusBonusLife.ATTACHMENT, TRUE)` (один раз — единственный write-site гранта), sync через synced attachment. `releaseUsing` — только ранний обрыв (`interruptRitual`), нормальное завершение через `finishUsingItem` независимо.
- [ ] **Step 4: Длительность и цена крови** — `madness` живёт 900t; каждые 20t `hurt(regulus_blood_price, 0.6f)` (0.6 HP/с, НЕ `hurt(12)` раз в 20t); конец → `madness=false` + `madnessUntilTick=0` + anim `evangelium_deactivation` (state end 0.96с); кулдаун повторного чтения — item cooldown (в спеке не задан отдельно от 400t обрыва — оставить 400t на обрыв; длительный lockout после успешного режима = сам madness 900t + снятие героя).
- [ ] **Step 5: MP-фикс** — `RegulusMadnessState` → synced attachment (`AttachmentRegistry.create().initializer(EMPTY).syncWith(STREAM_CODEC, AttachmentSyncPredicate.all())`; StreamCodec composite — см. контракт); `RegulusBonusLife.ATTACHMENT` → synced `targetOnly`; `ClientMadnessState` читает attachment `mc.player` и вычисляет прогресс от `level.getGameTime()` к `ritualUntilTick`/`madnessUntilTick` (никакого `System.currentTimeMillis()`/латченного `madnessStartedAtMs` — поздно подключившийся клиент видит тот же дедлайн); `madness_sync`/`madness_visual` удаляются; `BloodRain`/глитч/FOV дёргаются через `ctx.clientTick` edge-detector (`prev!=cur`); FOV-константы под 60t: `RegulusClientModule` `total=10000L`→**1200** (60t = 3с) и `EvangelionZoomHud.TOTAL_MS`→**1200** + перевод обеих на gameTick-разность вместо ms; `BloodRainHud` в `namedSingletons` (`reset → clear()`).
- [ ] **Step 5b: Эмбиент-частицы безумия** — `ANGRY_VILLAGER` (:227) и `LAVA` (:230) ambient-тик → rune-glyph ванильные частицы (SOUL/ENCHANT-набор); Veil-руна — Task 9.
- [ ] **Step 6: Модель книги** — проверить текущий item-renderer стек проекта; полный путь: `geo/item/evangelion.geo.json` + texture png + `models/item/evangelion.json` (совместимый parent/texture для `assertItemModelsResolveToTextures`) + client renderer (`BuiltinItemRenderer`/соответствующий проектный эквивалент — выбрать по коду других geo-итемов) + `EvangelionBookLayer` рисует prop-книгу вокруг игрока во время ритуала (читая synced `ritualUntilTick`).
- [ ] **Step 7: Тесты** — `evangelionEarlyReleaseAborts`, `evangelionNormalCompletionDoesNotRunAbort`, `ritualInterruptedByDamage`, `madnessExpiresAt45s`, `bloodPriceIsPointSixHpPerSecond`, `bonusLifeOncePerMadness`/`bonusLifeHasSingleAuthoritativeState`, `syncedAttachmentVisible`, `madnessStaysOffBefore60t`, `madnessDeadlineSurvivesLateClientObservation` (attachment дедлайн читается поздно подключившимся).
- [ ] **Step 8: Commit** `feat(regulus): evangelion ritual — vanilla channel, paid madness, synced state`.

---

### Task 9: Кровь-спрайты, Veil-VFX, EMF-драйвер

**Files:**
- Modify: `src/client/java/io/github/grebeshok105/codex/client/hero/regulus/hud/BloodRainHud.java`, `MadnessHudOverlay.java`
- Modify: `build.gradle` + `src/main/resources/fabric.mod.json` — EMF `entity_model_features` в `depends` (hard dep, подтверждено заказчиком); `fabric.mod.json` `environment` → `"client"`; **мёртвая server-инфраструктура удаляется**: source sets `servernoveil`/`clientnoveil` и таска `runServer` в `build.gradle` теряют смысл (их назначение — выживание dedicated server без Veil/EMF); то же касается `servernoveil`-класспас-фильтров в gametest/datagen-конфигурации. ⚠️ **Проект-контракт:** AGENTS.md §7 «dedicated server loads src/main» устаревает этим решением — в Task 9 же обновить правило §7 (мод client-only; server-safe `src/main` остаётся хорошей гигиеной, но не гейтом).
- Create: `src/main/resources/assets/superheroes/textures/gui/regulus/blood_*.png` (4–6 спрайтов)
- Create: `src/client/java/io/github/grebeshok105/codex/client/hero/regulus/fx/veil/RegulusVeilFx.java` (+ emitters за `isModLoaded("veil")`)
- Create: `src/client/java/io/github/grebeshok105/codex/client/hero/regulus/anim/RegulusAnimationDriver.java` + `state/ClientRegulusAnimState.java`
- Modify: `src/client/java/io/github/grebeshok105/codex/client/hero/regulus/RegulusClientModule.java` (receive anim payload + driver tick)
- Test: sanity-обновления + gametest payload

**Interfaces:**
- Consumes: все контроллеры/события выше, `RegulusAnimS2CPayload` (создан Task 4), EMF-рантайм (внешний — precondition), `FxBroadcast.trackingAndSelf`.
- Produces: `RegulusVeilFx.*`, `RegulusAnimationDriver.tick`, `ClientRegulusAnimState`.
- Планируемая замена characterization-пинов: fill-rect blood пины → sprite-слои.

- [ ] **Step 0: Preconditions (стоп-условия до старта)** — файлы EMF-пака должны существовать в репо/рабочем дереве: `Regulus_All_Animations.bbmodel`, `export/Regulus_EMF_Authoring.json`, HANDOFF. EMF-риг/`.jem` — выход параллельной задачи; если контракт отсутствует — Task 9 останавливается на Step 0 с отчётом, механика работает без анимаций. Проверить реальный EMF API кормления `var.regulus_*` (не предполагать).
- [ ] **Step 1: Спрайты крови** — `blood_edge_*.png`/`blood_drop_*.png`/`blood_hit.png`; `BloodRainHud` на текстурные слои с альфа-рампой; heartbeat-пульс — драйвер из `MadnessHudOverlay`, привязанный к `madnessUntilTick`-прогрессу (не wall-clock).
- [ ] **Step 2: Veil-слой** — `RegulusVeilFx` по каждой строке таблицы §6: купол лайон-харта, **перегрев (трещины+белый шум из CracksOverlayHud-стадии)**, **золотой пульс сердец**, магнит-нить, стазис-купол, обломки дроби (windup sweep 0.55с=11t → выстрел impact 0.70с), контратака-флэш, ритуал-руны (major @1.66с — событие, НЕ перезапуск клипа; заменяет ANGRY_VILLAGER/LAVA из Task 8 Step 5b), лендинг-искажение. Fallback — ванильные партиклы.
- [ ] **Step 3: Anim-драйвер** — `RegulusAnimS2CPayload(playerId, clipId)`; `ClientRegulusAnimState` (clip + `startedAtGameTime` от `level.getGameTime()` — НЕ `mc.player.tickCount`; prune на entity-unload; `ClientSessionState.register`); `RegulusAnimationDriver`: приоритет `cast-clip > evangelium_active_idle > combat_idle > vanilla`, blend-in/out по HANDOFF, конец one-shot по authored длительности; restart = перезапись `var.regulus_<clip>_time` в 0 (нет EMF `forceAnimationReset`); весь драйвер за `isModLoaded("entity_model_features")`; `evangelium_activation` стартует ОДИН раз (major @1.66с = VFX-event, не второй payload); `evangelium_active_idle`/`combat_idle` выбираются локально по synced `madness`.
- [ ] **Step 4: Тесты** — `animPayloadBroadcastToTrackingAndSelf`, `clientStateResetCoversHeartsAndAnimAndMadness`, sanity-ресиверы.
- [ ] **Step 5: Commit** `feat(regulus): blood sprites + veil fx + emf driver`.

---

### Task 10: Финиш — качество, доки, runtime

- [ ] **Step 1:** `./gradlew runDatagen --no-daemon` — полный ревью diff в `src/main/generated` (все damage types, heartless-тег, lang, модели).
- [ ] **Step 2:** `./gradlew qualityGate --no-daemon` — зелёный.
- [ ] **Step 3:** `./gradlew runClient --no-daemon` — in-game чеклист: сердца светятся только владельцу; lion heart блочит урон/не жрёт голод; дробь ломает ≤3 блока, стреляет на impact; купол пинит игрока; counter требует реального обидчика; кровь — спрайты; Veil-эффекты по §6 (скриншоты против таблицы).
- [ ] **Step 4: LAN-прогон ДВУМЯ игроками** — host + non-host + наблюдатель: у не-хоста ритуал/безумие/кровь/HUD идут по synced attachment (MP-регресс спеки §5.3); зрители видят анимации Regulus (trackingAndSelf).
- [ ] **Step 5:** no-Veil sanity (отключить Veil → ванильный fallback не ломается); dedicated-server `runServer`-smoke НЕ требуется — мод client-only по решению заказчика (EMF hard dep).
- [ ] **Step 6:** `SESSION.md` хендофф; независимое ревью (subagent reviewer).
- [ ] **Step 7:** финальный commit + push; PR по §12 AGENTS.md («Для игрока» + тех. разбор).

## Новые тесты (сводный список)

`castDoesNotChargeBeforeAuthoredEvent`, `cancelledCastHasNoCooldown`, `lionHeartBlocksExternalDamageAfterTrigger`, `lionHeartAllowsInternalTrueCostDamage`, `lionHeartStopsExhaustion`, `heartDamageScalesMelee`, `heartOwnerCleanupRemovesBearerMarks`, `armorStandCannotCarryHeart`, `hostileMobCannotCarryHeart`, `heartRegenRespectsEnergyLock`, `overheatSyncChangesWithoutHeartSetChange`, `projectileLocksReleaseIndividually`, `stasisActuallyPinsPlayer`, `stasisReleaseDamageIsNotRequeued`, `stasisReleasesOnCasterGone`, `counterDealsFormulaOnce`, `counterVisualExplosionDealsNoExtraDamage`, `counterImpactMatchesAnimationEvent`, `evangelionEarlyReleaseAborts`, `evangelionNormalCompletionDoesNotRunAbort`, `bloodPriceIsPointSixHpPerSecond`, `madnessDeadlineSurvivesLateClientObservation`, `bonusLifeHasSingleAuthoritativeState`, `maniaDoesNotTargetBefore19t`, `madnessStaysOffBefore60t`, `heartsMarkOnlyVanillaMobs`, `heartsCapAtTwelve`, `heartBacklashOnBearerDeath`, `heartLostQuietlyOnUnload`, `heartRejectsCustomEntity`, `heartEnergyRegenScales`, `projectilesFreezeAndDrop`, `windowThenOverheat`, `forcedOffAtLowHp`, `castInterruptedBeforeTrigger`, `debrisFanHitsConeOnly`, `debrisBreaksBlocksButNotBedrock`, `debrisRayBlockedByUnbreakable`, `castInterruptCancelsShot`, `stasisSkipsAllies`, `stasisCapsAtThirtyFivePercent`, `oneDomePerCaster`, `maniaFreezeDeniedAtZeroEnergy`, `maniaReleaseCapRespected`, `counterRequiresRealAttacker`, `craterShrunk`, `tamedWolfNotTargeted`, `ritualInterruptedByDamage`, `madnessExpiresAt45s`, `syncedAttachmentVisible`.

## Open design questions (что осталось)

Открытых вопросов нет — все развилки закрыты заказчиком:
- Сердца НЕ цепляются на враждебных мобов (`!(e instanceof Enemy)` в скане, Task 3) — backlash на мирных/нейтральных остаётся механикой риска.
- Дробь point-blank без dedupe/cap (pellet-rule как у ванильного дробовика) — подтверждено.
- EMF — hard `depends`, мод client-only (`environment: "client"`); dedicated-server совместимость снята осознанно, `servernoveil`/`clientnoveil`/`runServer` инфраструктура удаляется в Task 9, AGENTS.md §7 правится там же.

## Self-review заметки

- `debris_kick` привязан к `lion_roar` id; имя/lang меняются, id — нет (persisted).
- `GreedStasisController`/`RegulusHearts`/`RegulusCastState` — новые `OwnedSessionMap`/attachments; `GreedCageController` полностью удалён в Task 6.
- MP-баг — synced attachment вместо ручных пакетов; клиент-прогресс только от gameTick-дедлайнов.
- Числа: heart cap 12, window 60+40×hearts (≤27с), mania 4с/40%, stasis 80t/35%, counter 15+15%cap45, evangelium 60t ритуал/900t/0.6hp-с — подлежат in-game тюнингу.
