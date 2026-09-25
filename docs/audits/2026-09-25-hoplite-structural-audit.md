# Codex Superheroes — структурный аудит (maintainability / extensibility)

> Независимый аудит Hoplite (Claude), 2026-09-25. Дополняет `2026-09-25-opus-architecture-audit.md` и **не повторяет** его: известные пункты (bootstrap на ~70 `init()`, ветки героев в `AbilityRouter`/`CombatImpactEngine`/`JarvisThreatClass`/`FlightController`/`ClientAbilityFilter`, 44 payload'а, 26 клиентских стейтов, 24 HUD-вызова, отсутствие lifecycle, тонкий `Ability`, `HeroData` без единого писателя) упоминаются только там, где найдена новая причина.

Цель — не баги, а то, что будет мешать нескольким автономным агентам параллельно развивать проект: скрытые связи, границы пакетов, циклы, god-objects, дубли, one-off системы, неясное ownership, тихие дефолты, ручная регистрация.

Пути: `M/` = `src/main/java/com/example/superheroes/`, `C/` = `src/client/java/com/example/superheroes/client/`, `A/` = `src/main/resources/assets/superheroes/`.

## 0. Резюме

- Кодовая база: `M/` 429 файлов / 42 329 строк, `C/` 131 файл / 12 398 строк. 38 пакетов, 36 двунаправленных пар зависимостей, 24 пакета состоят хотя бы в одном 2-цикле. Ядро `ability ↔ hero ↔ effect ↔ transform ↔ network ↔ resource ↔ attachment` — полное кольцо: ни один из этих пакетов нельзя понять, собрать или протестировать отдельно.
- Единица владения «герой» отсутствует: даже минимальный Kazuha (4 способности, без контроллеров) размазан по 6 своим и 10 shared-файлам; Reinhard — 35 своих файлов в 9 директориях плюс 17 shared-файлов в 14 директориях.
- Расширение идёт через ручные списки в 2–4 shared-файлах на каждый путь. Часть списков защищена `ProjectSanityTest` (и тем самым **цементирует** текущий god-object), часть не защищена ничем и тихо дефолтится — Scorpion и Pandora уже выпали из четырёх таблиц.
- Единственный здоровый образец — пакет `flight/` (чистая математика, 4 теста) и хук `Hero.onLanded` (единственный per-hero хук, который shared-код вызывает через seam).
- Предлагаемый порядок миграций: чистка мёртвого кода → реестры вместо списков (клиентские стейты, HUD, частицы) → контракт роутера → `HeroProfile` → разрыв кольца пакетов → сервисы вместо идиом → per-hero модули → mixin-policy / клавиши / payload'ы.

## 1. Метод и ограничения

- Статический анализ исходников: граф зависимостей пакетов по импортам и полностью квалифицированным ссылкам (скрипт), grep-инвентаризация идиом и статического состояния, `git show --stat` реальных коммитов добавления героев как измерение стоимости пути.
- В игре ничего не запускалось; байткод/ArchUnit не применялись. Количество «shared-файлов на героя» измерено по упоминанию имени героя, а не по семантике.
- Код не менялся.

## 2. Карта архитектуры

### 2.1 Слои по факту

| Слой | Пакеты / файлы | Кто реально владеет состоянием |
|---|---|---|
| Bootstrap | `M/SuperheroesMod.java` (72 вызова `init()` в `:28-99`, 15 per-player `serverTick` в `:108-120`, ещё два — по проверке конкретных способностей в `:123-128`); `C/SuperheroesClient.java` (393 строки: рендереры, 32 фабрики частиц, порядок HUD `:161-199`, клавиши, тики `:201-207`, сброс на disconnect `:275-288`) | никто — порядок строк |
| Идентичность героя | `M/hero/Hero.java` (14 методов), `Heroes.java` (статические синглтоны `:12-33`, регистрация `:39-60`), `HeroAttributes.java` (483 строки — модификаторы всех героев в одном классе), `HeroTheme.java`, `HeroHudConfig.java` | общие таблицы, а не герой |
| Способности | `M/ability/` — 122 файла: Ability-классы + `AbilityRouter`, `AbilityCooldowns`, `AbilityIds` (123 id), `AbilityRegistry` (285 строк), плюс 3 контроллера; `M/ability/ironman/` — единственный per-hero подпакет | статика внутри классов способностей |
| Серверная логика | `M/effect/` — 78 файлов: ~60 контроллеров, MobEffect'ы, 4 state-record'а, `ScorpionFx`, `VanityAuthority` | 131 статическая коллекция в `M/` (`static final Map/Set/List<…>`) |
| Состояние | `M/attachment/ModAttachments.java` (9 attachment'ов), `M/transform/HeroData.java` | attachment + дублирующая статика |
| Сеть | `M/network/` — 46 файлов; `C/network/ClientNetworking.java` (231 строка) | ручные пары register/receive |
| Клиент | `C/Client*State` ×26, `C/hud/` ×37, `C/render/` ×19, `C/mixin/` ×19 | статические поля |
| Острова | `M/horde/` (собственный реестр из 25 сущностей, `HordeManager` 583 строки, hard-dependency GeckoLib), `M/jarvis/`, `M/physics/`, `M/flight/`, `M/api/` | — |

### 2.2 Граф зависимостей пакетов

Полные 2-циклы (пример ребра в каждую сторону):

| Цикл | Доказательство |
|---|---|
| `ability ↔ hero` | `AbilityRouter.java:5-6` → `Hero`, `Heroes`; все `*Hero.java` → `AbilityIds` |
| `ability ↔ transform` | `AbilityRouter.java:11` → `HeroData`; `HeroTransformService.java:3-4` → `Ability`, `AbilityRegistry` |
| `ability ↔ effect` | `AbilityRouter.java:4,53` → `ModEffects`, `MirrorDimensionController`; `effect/DoomsdayTierController` → `AbilityCooldowns`, `ChargeTackleAbility` |
| `ability ↔ network` | `AbilityRouter.java:7` → `ModNetworking`; `ModNetworking.java:3,86,94` → `AbilityRouter`, `ReinhardWishAbility`, `OmnimanThinkMarkAbility` |
| `ability ↔ resource`, `ability ↔ flight`, `ability ↔ entity`, `ability ↔ api`, `ability ↔ debug`, `ability ↔ ability.ironman` | `RepulsorChargeController.java:3` → `api.HeroApi`; `ReinhardSpeedJudgmentAbility` → `debug.AdminAbilityDebug`; `SmartMissileEntity` → `SmartMissileAbility` |
| `attachment ↔ effect` | `ModAttachments.java:20-46` → `RegulusMadnessState`, `DoomsdayProgress`, `ReinhardState`, `RaidenState`, лежащие в `effect/` (`DoomsdayProgress.java:11`, `RaidenState.java:16`, `ReinhardState.java:10`, `RegulusMadnessState.java:6`) |
| `attachment ↔ transform`, `effect ↔ transform`, `effect ↔ network`, `effect ↔ resource`, `effect ↔ entity`, `effect ↔ item`, `network ↔ transform`, `network ↔ resource`, `resource ↔ transform` | `HeroTransformService.java:63-66,99-104` → 7 контроллеров; `RamEntity` → `RamCompanionController`, `RemDemonismController`; `EvangelionItem` → `RegulusMadnessController` |
| `hero ↔ effect`, `hero ↔ physics`, `hero ↔ resource`, `hero ↔ ability.ironman` | `BattleBeastHero` → `BattleBeastCurseController`; `DoomsdayHero` → `physics.ShockwaveUtil`; `CombatImpactEngine.java:11` → `KazuhaHero` (и ещё 19 героев); `IronManHero` → `IronManSuitStats` |
| `horde ↔ horde.entity ↔ horde.entity.projectile`, `entity ↔ entity.ai`, `entity ↔ item` | `HordeManager` ↔ `BaseHordeEntity`; `HomelanderBossEntity` ↔ `HomelanderEyeLaserGoal`; `ModItems` → `ModEntities`, `VoughtSignalItem` → `HomelanderBossEntity` |
| Клиент: `client ↔ client.hud/screen/network/fx/render` | `ClientHeroState.java:6` → `hud.ScreenFlashHud`; `hud.AbilitiesTooltipHud` → `ClientAbilityCooldowns` |

Неочевидные рёбра, которые агент не ожидает увидеть:

- `attachment → effect` — пакет хранилища состояния зависит от пакета контроллеров (см. выше).
- `client.render.lightning → M/mixin` — клиентский рендер импортирует серверный mixin-пакет (`LightningBoltAccessor`).
- `physics → hero` и `hero → physics` одновременно: `physics` не является листом, хотя выглядит как утилитарный слой.
- `ability → api` — «внешнее» API используется внутренним кодом (`RepulsorChargeController.java:3,28`).
- `mixin.falbiks → effect` и `client.iris → network` — интеграции с чужими модами живут внутри общих пакетов.

### 2.3 Здоровый образец

`M/flight/` (9 файлов: enum'ы `FlightMode`/`FlightPhase`, `FlightMotionMath`, `FlightPhaseResolver`, `FlightProfiles`, `FlightTuning`) зависит только от `AbilityIds` и `HeroData`, покрыт 4 из 6 тестовых классов проекта. Это единственный пакет, который агент может менять, прочитав только его.

## 3. Структурные smells

### S1. Verification gate цементирует god-object

`src/test/java/com/example/superheroes/ProjectSanityTest.java:198-213` (`assertControllersAreWired`) требует, чтобы каждый `*Controller` с `public static void init()` был **буквально** вызван строкой `Name.init()` из `SuperheroesMod.onInitialize()`. Любая миграция к диспетчеру или per-hero модулям начинается с падения гейта. Проверка привязана к суффиксу имени: `HeroLandingTracker`, `IronManReactorTracker`, `HeroEquipmentLock`, `BallisticBodyTracker` (все с `init()`) под неё не попадают. `assertEveryHeroRegistered` (`:175-190`) так же завязан на текст `Heroes.java`.

### S2. Таблицы с «тихим дефолтом»

Новый герой, не добавленный в таблицу, не ломает компиляцию и не ловится тестом:

| Таблица | Дефолт | Кто уже выпал |
|---|---|---|
| `M/jarvis/JarvisThreatClass.java:41-64`, `:68-69` | класс `C` | Scorpion, Pandora |
| `M/physics/CombatImpactEngine.java:289-301` (`styleFor`), `:303-325` (`heroPower`) | `DEFAULT` / `1.0` | Scorpion, Pandora |
| `C/hud/AbilityDescriptions.java:27-49` (`HERO_PASSIVE_COUNT`) | 0 пассивок → не отображаются | Scorpion, Pandora |
| `C/hud/PassiveIcons.java:15-33` | нет глифов | Scorpion, Pandora |
| `M/hero/HeroHudConfig.java:29-51` | `DEFAULT` («generic») | Pandora (единственный герой без `getHudConfig`) |
| `M/effect/SuperJumpController.java:33-38` | герой не умеет супер-прыжок | allow-list из 6 героев |

Git подтверждает механизм: коммит Omni-Man `bd2adc0` изменил 8 shared-Java-файлов + 2 lang и пропустил Jarvis/Impact-таблицы; коммит Scorpion `ab26340` — 9 shared-Java + 2 lang, те же таблицы пропущены.

### S3. Одно понятие — две конвенции

`HeroTheme` для 11 героев лежит в общей таблице (`M/hero/HeroTheme.java:23-254`, `DEFAULT = HOMELANDER` в `:254`), для 10 других — inline `new HeroTheme(` внутри собственного класса (`RegulusHero.java:25`, а также ATrain, BattleBeast, Doomsday, Invincible, Kazuha, Omniman, Rem, Scaramouche, SungJinwoo). `PandoraHero.java:111` возвращает `RegulusHero.THEME` — зависимость герой → герой. В `HeroAttributes.java:430` остался `STRANGE_HP` для Pandora (след переименования Doctor Strange).

### S4. Контракт `Ability` двусмыслен

`AbilityRouter.java:69` проверяет кулдаун, но 86 файлов способностей повторяют `AbilityCooldowns.isOnCooldown` внутри `canActivate` (образец: `KazuhaChihayaburuAbility.java:49-51`, установка в `:90`). Стоимость списывается **до** `tryActivate` и вручную возвращается при `false` (`AbilityRouter.java:80-95`, `restoreActivationCost` `:155-160`) — ручной двухфазный коммит на каждую активацию. Гейтинг размазан между роутером (`:20-32` эффекты, `:41-57` герои, `:66` Iron Fists, `:73-76` EnergyLocks) и способностью (`canActivate`). Кто владеет правилом «можно ли активировать» — не определено.

### S5. Отсутствующие сервисы видны по идиомам

Сверх `sendParticles`/`setCooldownTicks` из Opus-аудита, в `src/main`: 124× `hurtMarked = true`, 78× ручной `ClientboundSetEntityMotionPacket`, 42× `invulnerableTime = 0` (обход i-frames), 100× inline-предикат в `getEntitiesOfClass(LivingEntity.class, …)`, 52× `displayClientMessage`. Каждая идиома — отсутствующий сервис (`Motion`, `Targeting`, `Damage`, `Feedback`), и каждая копия — место, где правило (PvP, союзники, спектаторы, i-frames) может расходиться.

### S6. Нет единицы владения «герой»

Собственные файлы героя vs. shared-файлы, которые его упоминают:

| Герой | Свои файлы / директорий | Shared-файлы с упоминанием / директорий |
|---|---|---|
| Iron Man | 43 / 11 | 18 / 13 |
| Reinhard | 35 / 9 | 17 / 14 |
| Pandora | 26 / 7 | 17 / 13 |
| Regulus | 22 / 7 | 29 / 17 |
| Thanos | 21 / 8 | 27 / 14 |
| Doomsday | 19 / 8 | 21 / 16 |
| Rem | 18 / 9 | 14 / 13 |
| Kazuha (минимальный) | 6 / 3 | 10: `Heroes`, `HeroHudConfig`, `AbilityIds`, `AbilityRegistry`, `ModItems`, `ModItemGroups`, `CombatImpactEngine`, `JarvisThreatClass`, `C/hud/AbilityDescriptions`, `C/hud/PassiveIcons` |

`M/ability/ironman/` — единственный прецедент модуля, но там лежат и контроллеры (`IronManNanoFormController`, `IronManSuitSyncController`), а `RepulsorChargeController` — в `M/ability/`. Граница `ability/` ↔ `effect/` случайна: в `ability/` 6 не-Ability классов, в `effect/` — state-record'ы и MobEffect'ы.

### S7. Mixin'ы как per-hero фичи

Main (`superheroes.mixins.json`, 13 mixin'ов): 5 геройских — `ThanosBlockBreakingMixin.java:39`, `KryptoniteShardPickupMixin.java:27` (Doomsday), `LivingEntityEffectMixin.java:21-22` (Doomsday), `LivingEntityFallDamageMixin.java:26-27` (Regulus), 3 no-drop mixin'а под 3 предмета. Client (19 mixin'ов): 4 mixin'а ролика Pandora (`PandoraCinematic*`), `FontVanityCipherMixin` (Pandora), `SoundEngineMixin.java:16` (Reinhard), `GameRendererFovMixin.java:15` (Regulus), `GuiHotbarMixin.java:48` (Iron Man), `AbstractClientPlayerSkinMixin.java:41-54` (Homelander/Sung/Thanos/IronMan), `PlayerRendererMixin.java:47-56` (Sung/IronMan/Thanos). Глобальный ванильный хук без владельца — каждая новая геройская фича стремится стать ещё одним mixin'ом.

### S8. Три копии правды о герое на клиенте

`C/ClientHeroState.java:13-14` (статик), запись в attachment на клиенте `C/network/ClientNetworking.java:49-50` — потому что серверный `M/mixin/PlayerDimensionsMixin.java:17-32` читает attachment на обеих сторонах, — и `C/RemoteHeroSkins` для чужих игроков. 87 обращений к `ClientHeroState.*` по клиенту.

### S9. HUD-геометрия дублируется

`C/hud/HudLayoutManager.java:22-30` — фиксированный `ALL` из 7 id; `C/screen/HudEditScreen.java:26-32` — параллельный список элементов; `:198-243` — `switch` с копией layout-математики каждого HUD (комментарий `:233`: «mirrors AbilitiesTooltipHud»). Только 5 из 37 HUD-классов перемещаемы (4 читают `HudLayoutManager.offset` сами, `MeleeChargeHud` — через обёртку в `SuperheroesClient.java:187-194`); остальные — нет. Порядок отрисовки = порядок строк в `SuperheroesClient.java:161-199`. Общие HUD с геройскими ветками: `SpartanRageHud.java:34-35` (Kratos + Rem в одном классе).

### S10. Клавиши героев в клиентском entrypoint

`C/ModKeys.java:16-18` — `RAIDEN_SWORD_DRAW`, `NANO_WEAPON`, `ESP_TOGGLE`; обработка с `IronManHero.ID.equals` в `SuperheroesClient.java:248-252, 296-298, 306-308, 315-317`. Слотов способностей фиксированно 8 (`ModKeys.java:19-33`), выбор способности по индексу — `RadialMenuHud.java:134`.

### S11. Одинаковые payload'ы и рендереры ×3

`LaserFiredS2CPayload.java:12`, `RepulsorBlastS2CPayload.java:12`, `ThanosCosmicBeamS2CPayload.java:12` — идентичный `(UUID shooter, Vec3 start, Vec3 end)`; 3 broadcast-метода `ModNetworking.java:151-185` с одинаковым `PlayerLookup.tracking`-циклом (всего 5 копий цикла в `:111-185`); 3 рендерера со списком лучей (`C/render/LaserBeamRenderer.java:13,22`, `RepulsorBeamRenderer.java:21,30`, `CosmicBeamRenderer.java:18,27`). Параллельно существуют ≥4 VFX-пайплайна: vanilla-частицы через `CustomParticleGate` (`C/fx/CustomParticleGate.java:27` — VfxMode), beam-рендереры, Veil (`WildRenderer`/`WildShaders`/`VeilScorpionFx`), Iris-мост (`C/iris/`).

### S12. Публичное API — фикция

`M/api/package-info.java:238` ссылается на `docs/api.md`, которого нет. Единственный потребитель `api/` — внутренний код (`RepulsorChargeController.java:3`). При этом все ~60 контроллеров экспонируют `public static` методы — «всё публично», и `api/` ничего не изолирует.

### S13. Мёртвый и осиротевший код

- 0 внешних ссылок: `M/ability/ViltrumiteThunderClapAbility`, `C/hud/LowResourceVignetteHud`, `C/hud/ResourceBarHud`, `C/render/horde/HordeGeoRenderer`.
- 5 `AbilityIds` никогда не регистрируются: `IRON_MAN_NANO_REPAIR`, `NARUTO_FLYING_RAIJIN`, `NARUTO_KURAMA_CLOAK`, `NARUTO_TAILED_BEAST_BOMB`, `VILTRUMITE_THUNDER_CLAP`.
- `METEOR_SLAM` и `SHOCKWAVE_PULSE` зарегистрированы (`AbilityRegistry.java:20,22`), `MeteorSlamAbility.serverTick` вызывается на каждого игрока каждый тик (`SuperheroesMod.java:110`), но ни один герой их не перечисляет.
- `DoctorStrangeSuitItem` (`ModItems.java:205-207`) жив после замены Strange на Pandora.
- GeckoLib бандлится jar-in-jar (`build.gradle:87-89`) ради `BaseHordeEntity implements GeoEntity` (`M/horde/entity/BaseHordeEntity.java:27`), при том что все 25 horde-сущностей рендерятся ванильными моделями (`SuperheroesClient.java:72-93`), а `HordeGeoRenderer` не используется.

### S14. Cross-mod код в ядре

`M/effect/ThanosCrossModSnapHook.java:10-20` (reflection по `com.falbiks.heroes…`) и `M/mixin/falbiks/HeroComponentStripMixin.java:28-34` (`@Pseudo` + `@Shadow` на чужое поле) — интеграция одного героя с чужим модом внутри общих пакетов. Владелец неясен, тестировать невозможно, в `fabric.mod.json` зависимость не объявлена.

### S15. Пассивки — тройная синхронизация по индексу

Ключи `hero.<id>.passive.N` (`C/hud/AbilityDescriptions.java:55-61`) + таблица количества (`:27-49`) + таблица глифов (`PassiveIcons.java:15-33`) + два lang-файла. Гейт проверяет только паритет en/ru, а не полноту по зарегистрированным героям. `Hero` о своих пассивках ничего не знает.

### S16. Bootstrap знает конкретные способности

`SuperheroesMod.java:123-128` проверяет `NARUTO_SAGE_MODE`/`GOKU_SUPER_SAIYAN_AURA` перед `serverTick`, хотя активные toggle-способности уже тикаются через `ResourceController.onTickActive` — второй параллельный путь тика для двух способностей. `SuperheroesMod.java:153-163` (`AFTER_DEATH`) содержит `DoomsdayHero.ID` — исключение одного героя из общего правила смерти зашито в bootstrap.

### S17. Транзакционные предметы — 22 копии одного класса

22 `*SuitItem` наследуют `TransformationItem` (`M/transform/TransformationItem.java:12-41`) и отличаются только lore-ключами в `appendHoverText` (образец: `M/item/GokuGiItem.java`). Добавление героя = новый Java-класс ради 4 lang-ключей.

## 4. Шесть путей добавления

«Обязательные» — без них не компилируется/не работает; «тихо дефолтится» — компилируется и молча работает неправильно.

| Путь | Обязательные точки в shared-коде | Тихо дефолтится | Файлов, типично |
|---|---|---|---|
| **Герой** | `Heroes.java:12-33,39-60` (гейт), `AbilityIds`, `AbilityRegistry` (поле + `register`), `HeroAttributes` (id + set), `ModItems`, `ModItemGroups.java:74-76`, новый `*SuitItem` (S17), `A/models/item`, `en_us`+`ru_ru`; при контроллере — `SuperheroesMod.init()` (гейт S1); при персистентном состоянии — `ModAttachments` + record в `effect/`; при cleanup — `HeroTransformService.java:63-66, 99-104, 178-185, 187-192` | S2 целиком: `HeroTheme`, `HeroHudConfig`, `JarvisThreatClass`, `CombatImpactEngine`, `AbilityDescriptions`, `PassiveIcons`, `SuperJumpController` | 8–10 shared + 2 lang + 3–5 своих (git: Omni-Man 8+2, Scorpion 9+2); с контроллером, состоянием и HUD — 14–17 |
| **Способность** | `AbilityIds`, `AbilityRegistry` ×2 места, `XHero.getAbilities()`, lang ×2, `A/textures/gui/abilities/x.png`; тик вне toggle → строка в `SuperheroesMod.java:108-128` (сейчас 13 способностей); геройский гейт → `AbilityRouter.java:41-57` **и** `ClientAbilityFilter.java:27-87`; своя клавиша → `ModKeys` + `SuperheroesClient` | иконка (fallback-бейдж), дубль кулдауна в `canActivate` (S4) | 5 + 2 lang; с тиком, гейтом и клиентским стейтом — 10–12 |
| **VFX — частица** | `M/particle/ModParticles` (+1), `A/particles/x.json`, текстура, фабрика в `SuperheroesClient` (+1–3 строки, `CustomParticleGate` для VfxMode). 32 частицы = 32 × 3 ручных синхронизации без проверки | нет проверки json ↔ type ↔ factory | 4 |
| **VFX — луч/шейдер** | payload-record + `ModNetworking.init` + sender + `ClientNetworking.init` + renderer с `register()` + `SuperheroesClient` (S11); Veil/Iris — отдельные пайплайны со своими гардами `isModLoaded` | — | 5–6 |
| **Сущность** | `M/entity/X`, `ModEntities.java:11-87` (register + `FabricDefaultAttributeRegistry`), `C/render/XRenderer`, `SuperheroesClient.java:65-72`, lang ×2 (не проверяется), опц. spawn egg; для орды — второй реестр `HordeEntities` со своей конвенцией рендереров (`GenericHordeRenderer.*`) | lang-имя, `OwnableEntity` (призывы без него) | 4–6 + 2 lang |
| **HUD** | `C/hud/X` (статик + `render`), `SuperheroesClient.java:161-199` (позиция в списке = z-order), tick в `:201-207` или `:233-240`; перемещаемый → `HudLayoutManager.ALL` + `HudEditScreen` ×2 места + lang ×2; сброс на disconnect `:275-288` | сброс состояния (у 17 из 26 `Client*State` нет метода clear/reset/onDisconnect) | 1 свой + 3–5 shared + 2 lang |
| **Networked state** | payload-record, `ModNetworking.init` (+1), sender с ручным `PlayerLookup.tracking` (5 копий в `:111-185`), `ClientNetworking.init` (+receiver), `ClientXState` (статик), потребитель, disconnect-clear; персистентный → `ModAttachments` + record + cleanup в `HeroTransformService` | сброс на disconnect | 2 новых + 3–4 shared (6–7 для персистентного) |

Пример полной цепочки для одного метра (Kratos Rage): `KratosRageS2CPayload.java:9` → `ModNetworking.java:54` → `KratosRageController.java:124` → `ClientNetworking.java:187-188` → `ClientKratosRageState.java:11-12` → `SpartanRageHud.java:44-45` (+ ветка `:34-35`). 6 файлов, 4 из них shared.

Общий знаменатель: **ни один путь не проходит через `Hero`**. Всё — ручные списки в 2–4 shared-файлах, часть из которых защищена гейтом (усложняет миграцию), часть — не защищена ничем (тихие дефолты).

## 5. Кандидаты на миграцию

Порядок учитывает зависимости и Opus-план (PR 1–4 там закрывают краши/god-mode и должны идти первыми).

| # | Миграция | Что закрывает | Объём / риск | Зависимости |
|---|---|---|---|---|
| M1 | **Чистка**: S13 целиком, `STRANGE_HP`, `api/` (восстановить `docs/api.md` и запретить внутреннее использование — либо удалить), cross-mod код (S14) → отдельный опциональный compat-модуль | S12, S13, S14 | S / нулевой | нет — первый PR |
| M2 | **Реестры вместо списков**: интерфейс `ClientState { clear() }` с авто-сбросом на disconnect; `HudElement { id, render, tick, bounds, movable }` — `HudLayoutManager.ALL`, порядок отрисовки и `HudEditScreen` выводятся из реестра; `ModParticles.register(name, gated)` с клиентской фабрикой из итерации реестра + sanity-проверка json ↔ type | S9, часть S8, путь VFX/HUD | S каждый / нулевой | нет |
| M3 | **Контракт роутера**: `Ability.cooldownTicks()`, роутер владеет гейтингом и кулдауном; удалить 86 дублей `isOnCooldown`; резервирование стоимости — один метод; `Hero.canUseAbility(player, id)` заменяет ветки `AbilityRouter.java:41-57` и `ClientAbilityFilter` | S4, Opus-долг 4 (частично) | S–M / низкий (механическая правка) | после Opus PR 2 |
| M4 | **`HeroProfile`** — record, которым владеет `Hero`: combat power/style, threat class, theme, hud config, список пассивок (glyph + lang key), super-jump флаг, attribute set. Заменяет `HeroAttributes` как таблицу констант, `JarvisThreatClass:41-64`, `CombatImpactEngine:289-325`, `AbilityDescriptions:27-49`, `PassiveIcons:15-33`, `SuperJumpController:33-38`, обе конвенции `HeroTheme`. Гейт: полнота профиля у каждого зарегистрированного героя | S2, S3, S15, Opus-долг 4 | M / низкий | нет; независим от M5 |
| M5 | **Разрыв кольца пакетов**: state-record'ы из `effect/` → к владельцу (M7) или в `transform/`; `attachment` зависит только от `transform`; `ShockwaveUtil`-подобные утилиты → сервисы, чтобы `physics` стал листом; тест «нет обратных рёбер» (ArchUnit или свой grep-тест в стиле `ProjectSanityTest`) | §2.2 | M / низкий | после M4 |
| M6 | **Сервисы вместо идиом** (S5): `Motion.apply(entity, vec)` (hurtMarked + пакет), `Targeting.around(...)` с единым предикатом PvP/союзники/спектаторы, `Feedback.actionBar(...)`; единая точка обхода i-frames | S5; побочно Opus B19/B20 | M / средний (баланс) | после Opus PR 5 |
| M7 | **`HeroModule` + per-hero пакет** (`M/hero/<name>/`, зеркало в `C/hero/<name>/`): Hero, способности, контроллеры, state-record, payload'ы, HUD одного героя в одном месте; регистрация через хуки `Hero` (`registerRuntime`, `tick`, lifecycle). Убирает списки в `SuperheroesMod`, `HeroTransformService:63-104`, распределяет `effect/` (78) и `network/` (46). Прецедент — `ability/ironman/`. **Обязательно** переписать `ProjectSanityTest:198-213` на «каждый зарегистрированный модуль подключён» | S1, S6, S16, Opus-долг 1–3 | L / средний | после Opus PR 2–3, M3, M4 |
| M8 | **Mixin policy**: каждый геройский mixin → generic-хук (`Hero.canBreakBlock`, `Hero.onItemPickup`, `InputLock`-сервис вместо 4 Pandora-mixin'ов, `TextObfuscationLayer` вместо `FontVanityCipherMixin`); один no-drop mixin по тегу (Opus PR 1) | S7 | M / низкий | после M7 |
| M9 | **Клавиши и команды через `Hero`**: `Hero.extraActions()` → динамическая регистрация KeyMapping; `Hero.registerDebugCommands()` вместо `SuperheroesCommands.java:60-82`; число слотов — из `getAbilities().size()`, не из константы | S10 | S / низкий | после M7 |
| M10 | **Консолидация payload'ов**: `BeamFxS2CPayload(kind, uuid, start, end)` + один `BeamRenderer` (S11); ревизия «метровых» payload'ов на общий `HeroMeterS2CPayload` (формы различаются — проверить перед объединением); synced attachments (Fabric 0.116 `syncWith`) для персистентных стейтов — путь «networked state» сокращается с 6–7 файлов до 2 | S11, Opus-долг 5 | M / средний | после M2 |
| M11 | **`TransformationItem` без подклассов**: lore из lang по конвенции `item.<id>.lore.*`, 22 класса `*SuitItem` → 0 | S17 | S / нулевой | нет |

Рекомендуемый порядок: M1 → M2 → M11 → M3 → M4 → M5 → M6 → M7 → M8/M9/M10. M2, M4 и M11 дают агентам локальность сразу и не конфликтуют с Opus PR 1–4.

## 6. Новые причины уже известных проблем

- **Ветки героев в общем коде** (Opus-долг 4) — корень не в отсутствии дисциплины, а в отсутствии `HeroProfile`: 7 независимых таблиц в 7 файлах (S2). Правильный путь через seam уже существует и работает — `Hero.onLanded` (`M/effect/HeroLandingTracker.java:140`, реализован 7 героями), просто он единственный.
- **Клиентский монолит** (Opus-долг 9) усиливается тем, что гейт S1 и `HudEditScreen` (S9) делают дублирование *обязательным*, а не случайным: агент не может «сделать правильно» без правки теста и редактора.
- **Самописный sync-протокол** (Opus-долг 5) дорог ещё и потому, что клиент вынужден писать attachment вручную (S8) ради серверного mixin'а, читающего `HERO_DATA` на обеих сторонах.
- **Центральный bootstrap** (Opus-долг 3): помимо 72 `init()`, он содержит логику конкретных способностей и героев (S16), то есть является не только реестром, но и местом бизнес-правил.

## 7. Приложение — инвентарь в цифрах

| Метрика | Значение |
|---|---|
| Java-файлов / строк `M/` | 429 / 42 329 |
| Java-файлов / строк `C/` | 131 / 12 398 |
| Пакетов / двунаправленных пар / пакетов в 2-циклах | 38 / 36 / 24 |
| `init()` в `SuperheroesMod.onInitialize` | 72 |
| per-player `serverTick` в `END_SERVER_TICK` | 15 (+2 по флагу) |
| `AbilityIds` / зарегистрировано / перечислено героями | 123 / 118 / 116 |
| Файлов способностей с дублем кулдауна в `canActivate` | 86 |
| Статических `static final Map/Set/List<…>` в `M/` | 131 |
| `Client*State` / с явным clear/reset/onDisconnect | 26 / 9 |
| HUD-классов / перемещаемых через `HudLayoutManager` | 37 / 5 |
| Mixin'ов main / из них геройских | 13 / 5 |
| Mixin'ов client / из них геройских | 19 / 10 |
| Payload'ов / из них с формой `(UUID, Vec3, Vec3)` | 44 / 3 |
| Частиц: `ModParticles` / json / клиентских фабрик | 32 / 32 / 32 (ручная синхронизация) |
| Классов `*SuitItem` на базе `TransformationItem` | 22 |
| Идиомы в `M/`: `hurtMarked = true` / `ClientboundSetEntityMotionPacket` / `invulnerableTime = 0` / `getEntitiesOfClass(LivingEntity.class` / `displayClientMessage` | 124 / 78 / 42 / 100 / 52 |
| Тестовых классов / из них про `flight/` | 6 / 4 |
