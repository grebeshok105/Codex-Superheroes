# Архитектурная миграция Codex Superheroes: обзор и карта планов

Этот файл — карта и общий контекст, а не план исполнения. Работа ведётся по шести планам ниже. Каждый план самодостаточен: в нём есть Global Constraints, контекст, стадии с шагами, тестами и критериями приёмки.

Исходный сводный план `docs/design/2026-09-25-architecture-migration-plan.md` сохранён без изменений как архив. По нему не работают и статусы в нём не обновляют. Он соответствует версии до внешнего ревью и до этапов BF4–BF12; актуальны только этот файл и шесть планов. Решения, которые там назывались D1–D14, здесь называются **R1–R14**, чтобы не путать их со стадиями D1, D2a…; решения R15+ появились при доработке.

## 1. Как работать

- Агент читает этот файл и **ровно один** план — тот, стадию которого он исполняет.
- Одна стадия = один PR, если в плане не сказано «PR-units».
- Статус стадии обновляется в таблице «Статус стадий» её плана. Когда план целиком закончен — ещё и в таблице §2 здесь.
- Решение, которое затрагивает несколько планов, записывается в §4 этого файла. Решение внутри одного плана — в разделе «Решения» этого плана.
- Если стадии раздаёт оркестратор нескольким субагентам, он работает по §11 «Оркестрация».

Источники: аудит 1 — `docs/audits/2026-09-25-opus-architecture-audit.md` (баги B1–B23, долг 1–9); аудит 2 — два документа, написанных параллельно и дополняющих друг друга: `docs/audits/2026-09-25-hero-modularity-audit.md` (локальность героя, `HeroModule`, `HeroProfile`, пилот Scorpion, stress-test Reinhard, целевая структура `core/mechanic/hero/content/compat`) и `docs/audits/2026-09-25-hoplite-structural-audit.md` (граф пакетов и циклы, S1–S17, реестры вместо списков, контракт роутера, сервисы вместо идиом, mixin policy, payload'ы, M1–M11). Оба документа добавлены в репозиторий этим PR с ветвей `hoplite/phaistos-c8f77e4f` и `hoplite/sestos-ea593710`.

## 2. Планы

| План | Файл | Стадии | Что даёт |
| :-- | :-- | :-- | :-- |
| П1 | [`01-guardrails-and-cleanup.md`](01-guardrails-and-cleanup.md) | A1, A2, N1, N2, N3 | ArchUnit-гейт (замороженный baseline, ratchet циклов, composition roots), помощник `TestHeroes`, полнота героя, удаление мёртвого кода, следов Doctor Strange и фиктивного `api/` |
| П2 | [`02-hero-data-and-ability-contract.md`](02-hero-data-and-ability-contract.md) | B1, B2, B3, C1, C2 | Оставшиеся общие таблицы героев → хуки героя в стиле BF11; пассивки и предмет трансформации у героя; роутер без проверок чужих эффектов |
| П3 | [`03-server-modules-ticks-lifecycle.md`](03-server-modules-ticks-lifecycle.md) | D1, D2a-1, D2a-2, D2b, D2c | Модульный seam поверх `HeroTickDispatcher`/`HeroLifecycle` из BF11: START- и EARLY-фазы, registrar'ы, `OwnedSessionMap`, вертикальный срез на Scorpion, `HeroModule` ×22, вся проводка героев из `SuperheroesMod` в модули |
| П4 | [`04-client-modules.md`](04-client-modules.md) | CL2, CL3a-1, CL3a-2, CL3b, CL4, C4 | Реестр HUD, `HeroClientModule` (сначала срез на Scorpion), клавиши, скины на `PUBLIC_HERO`, доступность способностей с сервера. CL1 закрыт BF7 |
| П5 | [`05-layers-services-pilots.md`](05-layers-services-pilots.md) | E1, E2, M1, F, G1–G3, H | Переименование корня, слои `core`/`mechanic`, сервисы механик, пилот Scorpion, stress-test Reinhard, review gate |
| П6 | [`06-hero-waves-and-finish.md`](06-hero-waves-and-finish.md) | I1–I6, IC, O (L2 — в составе I6b) | Перенос остальных 20 героев и контента, слияние лучевых payload'ов, документация и `add-hero` |

### Статус планов

| План | Статус |
| :-- | :-- |
| П1–П6 | ⏳ не начат |

### 2.1 Канонический граф стадий

Это единственный источник зависимостей. Графы в планах, поля «Зависит от» в паспортах и дорожки §11.2 выводятся из этой таблицы; при расхождении права эта таблица. «База» — `main`, в который влиты все PR bugfix-pass (#37–#40, #42–#49) и PR #41 с планами.

| Стадия | План | Жёсткие зависимости | Почему |
| :-- | :-- | :-- | :-- |
| A1 | П1 | База | барьер: гейт и `TestHeroes` нужны всем |
| A2 | П1 | A1 | |
| N1 | П1 | A1 | |
| N2 | П1 | A1 | |
| N3 | П1 | A1 | |
| B1 | П2 | A2 | golden-тест пишется в `HeroCompletenessGameTests`-стиле, lang-полнота из A2 |
| B2 | П2 | B1 | оба правят каждый класс героя |
| B3 | П2 | N2 | оба правят поле костюма Пандоры в `ModItems` |
| C1 | П2 | A1 | |
| C2 | П2 | A1 | |
| D1 | П3 | A1, N1 | оба правят таблицу `registerTickHandlers` |
| D2a-1 | П3 | D1 | `HeroModuleContext` отдаёт registrar'ы D1 |
| D2a-2 | П3 | D2a-1 | |
| D2b | П3 | D2a-2, C2 | правила C2 переезжают в модули |
| D2c | П3 | D2b | |
| CL2 | П4 | A1 | |
| CL3a-1 | П4 | D2a-1, CL2 | `HeroClientContext.hud` опирается на `HudLayers` |
| CL3a-2 | П4 | CL3a-1, D2a-2 | |
| CL3b | П4 | CL3a-2 | |
| CL4 | П4 | CL3b | |
| C4 | П4 | CL3a-2 | |
| E1 | П5 | все стадии П1–П4; решение R14; нет открытых PR, трогающих `src/` | барьер переименования |
| E2 | П5 | E1 | |
| M1 | П5 | E2 | |
| F | П5 | M1 | |
| G1 | П5 | F | |
| G2 | П5 | G1 | |
| G3 | П5 | G2 | |
| H | П5 | G3 | |
| I1, I2, I3, IC | П6 | H | |
| I4 | П6 | I2, I3 | |
| I5 | П6 | I4 | |
| I6 (I6a → I6b, L2 внутри I6b) | П6 | I3, I5 | |
| O | П6 | I1, I2, I3, I4, I5, I6, IC | |

- Параллельно после A1: A2 → B1 → B2; N2 → B3; N1 → D1 → D2a-1 → D2a-2 → D2b → D2c; N3; C1; C2; CL2. Клиентская дорожка CL3a-1 … CL4, C4 идёт параллельно серверной D2b → D2c.
- Оценка объёма: ~42 PR.

## 3. Исходное состояние

Проверено 2026-09-25 на локальном слиянии всех веток bugfix-pass (вершины #49, #44, #45, #47, #48; скретч-коммит `d255c4a`, в репозиторий не публиковался). Ни один из PR #37–#40, #42–#49 на момент проверки не влит в `main`.

### 3.1 Что сделал bugfix-pass (seams, на которых строятся планы)

| Этап BF | PR | Закрыто | Seam, который планы переиспользуют |
| :-- | :-- | :-- | :-- |
| BF1 | #37 | B1, B7, GameTest lane | `item/bound/` (`BoundWeapons`, `BoundWeaponItem`, `BoundWeaponToken`), один `PlayerBoundWeaponDropMixin`; `src/gametest` + `runGametest` в `qualityGate`; `G/TestPlayers` |
| BF2 | #38 | B2, долг 2, часть долга 5 | `transform/HeroDataStore` (`get`, `update(player, fn)`, `syncFull`, фаза `hero_data_flush`), `ResourcePayment`, `ResourceController.charge/refund` |
| BF3 | #39 | B3, B4, B8, B17, B23 | `lifecycle/PlayerLifecycle` (`onJoin/onLeave/onDeath/onRespawn/onServerStopped`), `lifecycle/EntityControlLock`, transient `AttributeModifierSet.Builder.abilityScoped()`, `TRANSFORM_TICK` |
| BF4 | #40 | B5, B6 | persistent attachment `ABILITY_COOLDOWNS` (дедлайны по gameTime, без `copyOnDeath`); кулдауны **не** очищаются при смене героя и выходе, только на смерти (`HeroSwapGameTests`); трансформация не лечит и не пополняет энергию |
| BF5 | #42 | B9, B11, B20, B21 | учёт урона на `AFTER_DAMAGE`, `ALLOW_DAMAGE` только для отмены, спасения на `ALLOW_DEATH`; тег `minecraft:bypasses_cooldown`; time slow Reinhard без `TickRateManager`; gameTime-дедлайны |
| BF6 | #47 | B10 | `world/WorldDestructionPolicy`, тег `superheroes:ability_immune`, sanity `assertWorldMutationsGoThroughPolicy` |
| BF7 | #45 | B15 | `client/ClientSessionState` — хаб сброса (`register(Runnable)` в static-блоке каждого `Client*State`, `resetAll()` на disconnect), sanity `assertClientStatesRegisterReset`; монотонные клиентские кулдауны; фикс отпускания клавиш в миксинах Pandora; JUnit получает client-классы |
| BF8 | #44 | B13 | серверная авторитетность House of Vanity (`MirrorDimensionController`) |
| BF9 | #48 | B12, B16 | `lifecycle/PassiveReconciler` (`applyAndCapture(player, hero)`), fall-иммунитет только у участников контра |
| BF10 | #43 | B14 | synced attachment `PUBLIC_HERO` (id героя, виден всем), `ClientHeroDimsWatcher`; удалены `RemoteHeroSkins` и `RemoteHeroSkinS2CPayload` |
| BF11 | #46 | долг 1, 3 (база), 4 | `lifecycle/HeroTickDispatcher` (один `END_SERVER_TICK`, фазы `GLOBAL → LEVELS → PLAYERS → ABILITY_ACTIVE`, статический API `onGlobalTick/onLevelTick/onPlayerTick/onActiveAbilityTick`); `lifecycle/HeroLifecycle` (`onClear/onTransformed`, `clearHeroRuntimeState` = `fireClear`); хуки `Hero`: `getImpactStyle/getImpactPower`, `getThreatClass`, `canUseAbility/onAbilityDenied`, `isAbilitySuppressedBy`, `getEnergyReserveFor`, `isUraniumWeak`; `JarvisThreatClass` в `hero/`; sanity `assertNoHeroTypeDispatch` |
| BF12 | #49 | гигиена | `AttachmentRegistry.create(id, builder)` вместо deprecated builder; sanity `assertNoCyrillicLiterals`, `assertEntityLangNames`; зависимости `fabric.mod.json` закреплены |

### 3.2 Что это меняет в миграции (решение R20)

BF11 сделал этап «хуки героя, lifecycle-события, tick dispatcher» по-своему. Планы строятся **поверх** этих seams и не создают рядом вторых:

| Seam bugfix-pass | Как его использует миграция |
| :-- | :-- |
| `HeroTickDispatcher` (BF11) | П3 D1 добавляет фазу `START` (для 3 контроллеров на `START_SERVER_TICK`), фазу `EARLY` (бывшие собственные `END_SERVER_TICK`-листенеры, которые сейчас выполняются до фаз диспетчера) и module-facing `TickRegistrar`. Второй dispatcher не создаётся |
| `HeroLifecycle` (BF11) + `PlayerLifecycle` (BF3) | Два хаба и есть lifecycle ядра. `LifecycleRegistrar` модулей делегирует в них; третьего хаба нет |
| Хуки `Hero` (BF11) | Стиль для оставшихся данных героя (П2 B1). `HeroProfile` не вводится (R15) |
| `ClientSessionState` (BF7) | Единственный хаб сброса клиентского состояния. Бывшая стадия CL1 закрыта |
| `PUBLIC_HERO` (BF10) | Вход `SkinResolver` (П4 CL4) и образец synced attachment для C4 |
| `PassiveReconciler` (BF9) | Основа B2 |
| `WorldDestructionPolicy` (BF6) | Переезжает в `mechanic/world` в E2 |
| `ABILITY_COOLDOWNS` (BF4) | Ни одна стадия миграции не очищает кулдауны при смене героя или выходе |

### 3.3 Слияние стека bugfix-pass (для владельца)

- Стек ветвится после #42: цепочка #43 → #46 → #49 и отдельно #44, #45, #47, #48 на #42. Общей вершины нет.
- При слиянии всех веток конфликтуют: `src/gametest/resources/fabric.mod.json` (списки GameTest — объединить), `ProjectSanityTest` (новые проверки обеих сторон — объединить), `SESSION.md` и трекер аудита.
- **Семантический конфликт:** #43 удаляет `client/RemoteHeroSkins`, а #45 ссылается на него в `ClientSessionStateResetTest` и в списке `assertClientStatesRegisterReset`. Тот PR, который вливается вторым, должен убрать эти ссылки, иначе `compileTestJava` падает.
- После разрешения сводная база компилируется целиком (main, client, test). GameTest-прогон сводной базы не выполнялся.

### 3.4 Замеры на сводной базе (baseline для критериев §8)

| Метрика | Значение | Как мерить |
| :-- | :-- | :-- |
| `.init()` в `SuperheroesMod.onInitialize` | 77 | `grep -c '\.init()' M/SuperheroesMod.java` |
| `PlayerLifecycle.on*` / `HeroLifecycle.on*` в `SuperheroesMod` | 63 (+30 `resetAll()` в `onServerStopped`) / 11 | `grep -c` |
| регистраций в таблице `registerTickHandlers` (BF11) | 18 | `M/SuperheroesMod.java` |
| файлов с собственной регистрацией `START/END_SERVER_TICK` | 51; из них 3 на `START`: `KratosRageController`, `ThanosGauntletStateController`, `MadnessFlightController` | `grep -rl` |
| `ALLOW_DAMAGE` / `AFTER_DAMAGE` слушателей | 7 / 7 | `grep -rn` |
| статических `Map/Set<UUID…>` main / client | 106 / 10 | `grep -rEc 'static (final )?(Map\|Set\|ConcurrentMap\|WeakHashMap\|HashMap\|ConcurrentHashMap)<UUID'` |
| `Ability`-классов с `AbilityCooldowns.isOnCooldown` | 85 | `grep -rl` |
| payload'ов / клиентских receiver'ов | 42 / 35 | `M/network`, `C/network/ClientNetworking.java` |
| `Client*State` | 27, все регистрируют сброс (BF7) | `C/` |
| mixins main / client | 12 / 19 | mixin-конфиги |
| общих таблиц героев | 7: константы `HeroTheme` (11 + `DEFAULT`), `HeroHudConfig` (21 + `DEFAULT`), `HeroAttributes` (487 строк), `AbilityDescriptions.HERO_PASSIVE_COUNT`, `PassiveIcons.MAP`, `SuperJumpController.ALLOWED_HEROES`, switch в `HeroBleedingController`. Таблицы `CombatImpactEngine` и `JarvisThreatClass` удалены BF11 | стадия B1 (П2) |
| baseline ArchUnit (прототип A1, §10) | 32 двунаправленные пары пакетов; заморожено ≈134 зависимости shared → конкретный герой, ≈33 client → конкретный герой, 53 обращения к полям тиков, 74 вызова lifecycle-хуков вне `lifecycle`, 3 зависимости от composition roots, 0 main → client | П1 A1 |

## 4. Межплановые решения

| # | Вопрос / расхождение | Что говорит код сейчас | Решение |
| :-- | :-- | :-- | :-- |
| R2 | Opus: хуки в `Hero`; модульный аудит: `HeroModule`; структурный: хуки `Hero` + per-hero пакет | BF11 уже ввёл хуки `Hero` | `Hero` — данные и правила, которые ядро спрашивает во время игры (`getAbilities`, хуки BF11, хуки П2). `HeroModule` — только bootstrap-проводка через узкие registrar'ы. Ни один из них не становится god-object |
| R6 | Структурный M5 предлагает сначала рвать кольцо пакетов; модульный — переименовать корень до переносов | Bugfix-pass правит десятки файлов | Разрыв колец делается владением, а не переименованием: state-record'ы уезжают к героям в их миграциях, `core`/`mechanic` рождаются новыми пакетами со строгими правилами. Переименование корня E1 — один механический PR на барьере |
| R7 | Модульный: всё геройское в `hero/<id>/`; Veil — `compat/veil` | `VeilScorpionFx` используется только Scorpion; `WildShaders`/`WildRenderer` — общая инфраструктура | Код опциональной библиотеки, нужный одному герою, живёт в `client/hero/<id>/fx/veil/` за guard'ом `isModLoaded`; общая Veil-инфраструктура — `client/core/fx/veil/`. `compat/` — только интеграции с чужими модами |
| R10 | Opus: runtime-attachment вместо ~105 статических коллекций | Часть статики — индексы по жертве, которые надо итерировать каждый тик | Per-player session state → non-persistent attachment на игроке; активные эффекты, которые итерируются тиком, → `OwnedSessionMap` (автоочистка на leave/death/смену героя/`SERVER_STOPPED`). `OwnedSessionMap` только удаляет записи — побочные эффекты старого `clear` остаются явными хуками (§6.3) |
| R12 | Структурный M10: объединять «метровые» payload'ы | Формы метров различаются | Лучевые payload'ы (`LaserFired`, `RepulsorBlast`, `ThanosCosmicBeam` — одна семантика «отрезок луча от стрелка») сливаются в `BeamFxS2CPayload(style, …)`. Метры не сливаются: долгоживущее состояние → synced attachment героя; разовые FX → payload'ы модуля |
| R16 | Внешнее ревью: `HeroModules` в `core.module` нарушает правило слоёв ядра и создаёт циклы `core.module ↔ hero.<id>`; `SuperheroesMod → core → SuperheroesMod.LOGGER` даёт цикл с корневым пакетом | Воспроизведено прототипом на сводной базе | **Composition roots:** `SuperheroesMod`, `client.SuperheroesClient`, пакеты `bootstrap..` и `client.bootstrap..`. Им можно зависеть от всего, от них — никому (`nothingDependsOnCompositionRoots`). Ratchet циклов не считает рёбра, выходящие из composition roots. Контракты (`HeroModule`, `HeroClientModule`) — в `core.module` / `client.core.module`, списки модулей — в `bootstrap` / `client.bootstrap`. Новый код логирует через `LoggerFactory.getLogger(ModId.MOD_ID)`. Проверено прототипом (§10) |
| R19 | Внешнее ревью: seam проверяется поздно — до пилота конвертируются все 22 героя | — | **Вертикальный срез до массовой конверсии:** D2a-1 (сервер) и CL3a-1 (клиент) переводят на модуль только Scorpion и доказывают строгие правила и ratchet на реальном коде; D2a-2 и CL3a-2 — остальные 21 герой |
| R20 | Исходный план поглощал BF11; bugfix-pass выполнил BF11 сам (#46) | §3.1 | Seams BF11 — база П2 и П3 (§3.2). Планы расширяют их, а не заменяют |

Решения, принятые внутри одного плана, лежат в его разделе «Решения»: R8, R13 — П1; R3, R4, R9, R15 — П2; R1, R5 — П3; R11, R14, R17, R18 — П5.

**Открытое решение владельца:** R14 — имя нового корневого пакета (П5). Нужно до стадии E1, остальные стадии от него не зависят.

## 5. Целевая архитектура

### 5.1 Раскладка пакетов (конечное состояние, корень `<root>` — см. R14 в П5)

```text
<root>/
  SuperheroesMod                 # composition root, ≤ 40 строк: core bootstrap, bootstrap.HeroModules.bootstrap(), content, compat
  ModId                          # лист без зависимостей
  bootstrap/     HeroModules (явный список, одна строка на героя), SharedMechanics — composition root
  core/
    hero/        Hero (+ хуки BF11 и П2), HeroTheme, HeroHudConfig, PassiveGlyph, BleedProfile, JarvisThreatClass,
                 AttributeModifierSet, LandingImpact, Heroes (чистый реестр)
    module/      HeroModule, HeroModuleContext, CoreModuleContext, AbilitySink — только контракты
    ability/     Ability, AbilityDenial, AbilityBlocker, AbilityRules, AbilityAvailability,
                 AbilityRouter (без веток героев), AbilityRegistry (чистый реестр), AbilityCooldowns
    resource/    ResourceController, ResourcePayment, ResourceKind, EnergyLocks
    transform/   HeroData, HeroDataStore, HeroTransformService, TransformationItem, TransformationLore
    lifecycle/   PlayerLifecycle (BF3), HeroLifecycle (BF11), LifecycleRegistrar, OwnedSessionMap,
                 EntityControlLock (+ state/shadow), PassiveReconciler (BF9), HeroTickDispatcher (BF11), TickRegistrar
    net/         общие payload'ы (активация, привязка, sync HeroData/ресурсов, кулдауны, тряска экрана),
                 PayloadRegistrar, FxBroadcast, C2SGuards, BeamFxS2CPayload (после L2)
    attachment/  только общие attachments (HERO_DATA, PUBLIC_HERO, ABILITY_COOLDOWNS, CONTROL_LOCKS, TRANSFORM_TICK, …)
  mechanic/
    flight/ impact/ world/ (WorldDestructionPolicy, BF6) targeting/ motion/ boundweapon/ charge/ strike/ summon/ shockwave/
    ability/     общие способности (FLIGHT, VILTRUMITE_RECOVERY)
  hero/<id>/
    <Id>Module  <Id>Hero  <Id>Abilities (id-константы)
    ability/  runtime/ (бывшие *Controller)  item/  entity/  net/  mixin/ (только если нельзя заменить общим хуком)
  content/      horde/  boss/homelander/  admin/ (AdminBuildSync, AdminAbilityDebug)  command/
  compat/       falbiks/  iris/ (серверная часть, если есть)
client/
  SuperheroesClient              # composition root, ≤ 40 строк: client core bootstrap, client.bootstrap.HeroClientModules.bootstrap(), compat
  bootstrap/ HeroClientModules (явный список) — composition root
  core/
    module/  HeroClientModule, HeroClientContext, CoreClientContext — только контракты
    session/ ClientSessionState (BF7)
    hud/     HudLayer, HudLayers, MovableHud, HUD-фреймворк, HudLayoutManager (из реестра)
    input/   ModKeys (только общие клавиши), HeroActionKeys
    render/  SkinResolver, SkinProvider, PlayerLayers, BeamRenderer
    fx/      CustomParticleGate, veil/ (общая инфраструктура)
    mixin/   только общие mixins с делегированием в реестры
  hero/<id>/ <Id>ClientModule  state/  hud/  render/  screen/  fx/  mixin/ (исключения)
  compat/    iris/
```

### 5.2 Правила зависимостей (проверяются ArchUnit: П1 A1, П5 E2)

| Из \ В | core | mechanic | hero.X | hero.Y | content | compat | bootstrap | client.* |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| core | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| mechanic | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| hero.X | ✅ | ✅ | ✅ | ❌ (только строковые id) | ❌ | ❌ | ❌ | ❌ |
| content | ✅ | ✅ | ❌ | ❌ | ✅ | ❌ | ❌ | ❌ |
| compat | ✅ | ✅ | ❌ (только через хуки, которые зарегистрировал герой) | ❌ | ❌ | ✅ | ❌ | ❌ |
| bootstrap (composition root) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ |
| client.core | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | client.core |
| client.hero.X | ✅ | ✅ | ✅ (свой) | ❌ | ❌ | ❌ | ❌ | client.core, client.hero.X |
| client.bootstrap (composition root) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ | всё клиентское |

- Ссылаться на классы `hero.<id>` извне модуля могут только `bootstrap.HeroModules` и `client.bootstrap.HeroClientModules`.
- От composition roots (`SuperheroesMod`, `client.SuperheroesClient`, `bootstrap..`, `client.bootstrap..`) не зависит никто; ratchet циклов не считает выходящие из них рёбра (R16).
- `ModId` остаётся листом в корневом пакете: он ни от чего не зависит, поэтому циклов не создаёт.
- Legacy-пакеты (`ability`, `effect`, `network`, `lifecycle`, …) до своего исчезновения ограничены только замороженными правилами П1.

## 6. Сквозные политики

Действуют во всех планах. Конкретные задачи, которые из них вытекают, лежат в планах: accessor молний (П4 CL3b), правило «у S2C payload героя есть receiver» (П5 F.4), `C2SGuards` (П5 G1), слияние лучей (П6 L2).

### 6.1 Mixin policy

1. Общий mixin (`mixin/`, `client/core/mixin/`) не знает героев: он делегирует в реестр/хук ядра (`InputLock`, `FovModifiers`, `ClientSoundFilters`, `SkinResolver`, `HudJitter`, `TextObfuscationLayers`, `WorldDestructionPolicy`, fall-иммунитет BF9). Проверяется правилами `sharedCodeDoesNotDependOnConcreteHeroes`/`sharedClientCodeDoesNotDependOnConcreteHeroes` (mixin-пакеты — shared).
2. Реестр создаётся в той волне, где появляется **первый** потребитель, и только если у механизма есть правдоподобный второй потребитель; иначе mixin героя живёт в `mixin/hero/<id>/` или `client/hero/<id>/mixin/` и записан в mixin-конфиге в блоке, подписанном id героя.
3. `@Pseudo`-миксины к чужим модам — только в `compat/<mod>/mixin/`, `require = 0`, без `@Shadow` на чужих полях.
4. Внедрённые члены — `@Unique`; `@WrapOperation` предпочтительнее `@Redirect`; `@Inject(cancellable)` не отменяет «отпускание» ввода (урок Opus B15).
6. Судьба текущих миксинов: `PlayerBoundWeaponDropMixin` — общий (BF1) ✔; `PlayerDimensionsMixin` — общий, после BF10 читает synced вид; `PlayerFlightPoseMixin`, `LivingEntityFallFlyingMixin`, `LivingEntityHealBlockMixin` — общие (сверить ветки в волнах); `LivingEntityFallDamageMixin` — BF9 → общий; `LivingEntityEffectMixin`, `KryptoniteShardPickupMixin` — I4d; `ThanosBlockBreakingMixin` — I5a; `falbiks.HeroComponentStripMixin` — I5c → compat; client: skin/renderer/pose — CL4; `SoundEngineMixin` — G2; `GameRendererFovMixin`, `GuiVanillaGlitchMixin` — I5b; `GuiHotbarMixin` — I6b; `LocalPlayerFlightMixin` — I6a; `PandoraCinematic*` ×4, `FontVanityCipherMixin` — I5c; `CameraMixin`, `MinecraftHeroMeleeChargeMixin`, `GuiOverlayMessageMixin`, `ChatComponentMixin` (Opus B15 чат — BF7), `GuiEffectsMixin`, `GameRendererBlurMixin` — сверить ветки героев в той волне, где они встретятся; без веток — остаются общими.

### 6.2 Сеть и синхронизация

1. **Владение payload'ом:** payload героя, его `STREAM_CODEC`, серверный sender и клиентский receiver живут в модулях героя (`hero/<id>/net/`, `client/hero/<id>/`); регистрация — `ctx.payloads()` и `HeroClientContext.receive`. В `core/net` — только общие (активация, привязка, `HeroData`/ресурсы, кулдауны, тряска экрана, обломки, BeamFx после L2).
3. **Synced attachments vs payload:** долгоживущее состояние игрока, которое клиент показывает (метры, режимы, тиры), — synced attachment героя (механизм подтверждает BF10; `HERO_DATA` остаётся на собственном sync `HeroDataStore` — у него coalescing ресурсов, который `syncWith` потерял бы). Разовые события (лучи, вспышки, звуки) — payload'ы. Объединять payload'ы можно только при одинаковой семантике (R12).
5. **C2S:** каждый C2S-handler проверяет, что игрок сейчас тот герой и та способность/состояние, для которых пакет имеет смысл (помощник `core/net/C2SGuards.requireHero(ServerPlayer, ResourceLocation heroId)` и `requireActiveAbility(...)`, появляются в G1 для `ReinhardWishConfirmC2SPayload`); GameTest на каждый C2S: пакет от игрока «не того» героя — no-op.

### 6.3 Размещение runtime state

| Состояние | Где живёт | Очистка |
| :-- | :-- | :-- |
| Persistent данные героя (прогресс, тиры, флаги ролика) | persistent attachment, объявленный модулем (`ctx.attachments()`), id не меняется | по дизайну (copyOnDeath как сейчас) |
| Per-player сессионное состояние (заряд, окно, режим) | non-persistent attachment на игроке **или** `OwnedSessionMap<UUID, V>` модуля | автоматически: смерть сущности/выход; `OwnedSessionMap` — по `ClearOn` |
| Активные эффекты на чужих сущностях, которые тикаются (притяжение, метка, захват) | `OwnedSessionMap<UUID жертвы, V>` с владельцем-кастером | leave/death/hero change владельца, `SERVER_STOPPED` |
| Флаги сущностей, сохраняемые в NBT (NoAI, NoGravity, invulnerable) | только `EntityControlLock` (BF3) | lock-refcount |
| Ссылки на `ServerLevel`/`Entity` | запрещены в статике; хранить UUID + `ResourceKey<Level>` | — |
| Клиентское состояние сессии | статика `Client*State` + `ClientSessionState.register` в static-блоке (BF7) или synced attachment | `DISCONNECT` |

Метрика: статических `Map/Set<UUID…>` в `src/main` — с 106 до 0 вне `OwnedSessionMap` (§8).

**`OwnedSessionMap` только удаляет записи.** Если старый `clear`/`resetAll`/`onPlayerGone` делал что-то ещё (забирал выданное оружие, снимал `EntityControlLock`, модификаторы атрибутов, эффекты, удалял призванные сущности, слал пакет клиенту), этот побочный эффект остаётся явным хуком `ctx.lifecycle().onLeave/onDeath/onHeroClear/onServerStopped` модуля. Перед заменой каждого `clear` исполнитель читает его тело и, если там есть побочный эффект, сначала пишет характеризационный GameTest на этот эффект.

### 6.4 Протокол поведенческих изменений

- Архитектурный коммит не меняет наблюдаемое поведение. Если перенос вскрывает баг, фикс — отдельный коммит `behavior(<scope>): …` с GameTest, который падает на старом поведении.
- Каждое `behavior:` перечисляется в разделе PR «Изменения поведения» (для игрока — в «Для игрока», если заметно).
- Изменения баланса/контента (показ пассивок Scorpion, калибровка профилей Scorpion/Pandora, PvP-правила B19, армия Sung B18) — не в архитектурных PR.

## 7. Риски программы и страховка

| Риск | Где | Страховка |
| :-- | :-- | :-- |
| Стек bugfix-pass сливается с конфликтами | до A1 | §3.3: объединение списков GameTest и проверок sanity, удаление ссылок на `RemoteHeroSkins` |
| Seam проверяется поздно: до пилота конвертируются все 22 героя | П3, П4 | вертикальный срез D2a-1 и CL3a-1 на Scorpion до массовой конверсии (R19); прототип правил на сводной базе (§10) |
| Две конкурирующие архитектуры надолго | D2a-2 → I6 | с D2a-2 **все** герои идут через `HeroModule`; различие между мигрированными и нет — только расположение файлов и shared-касания, а не способ проводки |
| Изменение порядка тиков и хуков | D1, D2b | `START` и `END` различаются в контракте диспетчера; бывшие собственные `END`-листенеры идут в фазу `EARLY` до фаз BF11; сверка межгеройских зависимостей в D2b |
| Возврат исправленных багов при переносе | П2, П3 | «Нельзя менять» в паспортах называет решения BF (кулдауны BF4, `AFTER_DAMAGE` BF5 и др.); GameTests bugfix-pass проходят без изменений |
| `OwnedSessionMap` не выполняет побочные эффекты старого `clear` | D1, волны | §6.3: сначала характеризационный тест на побочный эффект, эффект остаётся явным lifecycle-хуком |
| Потеря persisted id при переносе | B2, E1, F, G, I | Global Constraints; golden-тесты id модификаторов и предметов; GameTest загрузки состояния |
| ArchUnit-правило неверно сформулировано и пропускает нарушения | A1 | негативные пробы в A1.5 (проверены прототипом, §10); строгие правила проверяются на непустоту в D2a-1 и E2 |
| Разрастание `HeroModuleContext`/`Hero` в god-object | D2a-1 → I | чек-лист H; лимит ≤ 7 методов контекста; новые хуки только с ≥ 2 потребителями или записанным обоснованием |
| Окружение без дисплея | все runtime-стадии | сначала проверить, запускается ли `runClient` (дисплей или `xvfb-run`); если нет — PR прямо это говорит, непроверенные runtime-пункты передаются владельцу до мержа |

## 8. Конечные измеримые критерии архитектуры

Проверяются в П5 H (промежуточно) и П6 O (финально).

| # | Критерий | Цель | Как проверяется |
| :-- | :-- | :-- | :-- |
| 1 | Shared-таблиц с данными конкретных героев | **0** (было 9, BF11 убрал 2) | ArchUnit `sharedCodeDoesNotDependOnConcreteHeroes` — строгое, store удалён; `HeroProfile` абстрактный у каждого героя |
| 2 | Импорты героев из `core`/`mechanic`/`content`/`compat`/`client.core` | **0**, строгие правила | `coreDependsOnNothingAboveIt`, `mechanicsDoNotDependUpward`, `clientCoreDoesNotKnowHeroModules` |
| 3 | Кто может ссылаться на `hero.<id>` | только `hero.<id>` и `client.hero.<id>` того же героя, `bootstrap.HeroModules`, `client.bootstrap.HeroClientModules` | `heroModulesAreReferencedOnlyByThemselvesAndTheModuleList`, клиентские аналоги |
| 4 | Зависимости герой → герой | **0** классовых; допустимы только строковые id (`ModId.of("homelander")`) и теги | `heroModulesDoNotDependOnEachOther`, `clientHeroModulesDoNotDependOnEachOther` |
| 5 | Файлы, которые меняются при добавлении героя | новые `hero/<id>/**` + `client/hero/<id>/**`; **1 строка** в `HeroModules`, **1 строка** в `HeroClientModules`; `en_us.json` + `ru_ru.json`; ассеты в `<тип>/<id>/`; строка героя в golden-файлах `hero_presentation.txt` и `passive_glyphs.txt`. Ноль других Java-файлов в `src/main`/`src/client` | skill `add-hero`; ревью PR первого нового героя |
| 6 | Файлы при добавлении способности существующему герою | только внутри `hero/<id>/` (и `client/hero/<id>/`, если есть клиент) + lang ×2 + иконка | то же |
| 7 | `SuperheroesMod` | ≤ 40 строк тела: core-bootstrap (attachments, lifecycle, dispatcher, сеть, ресурсы, трансформация), `bootstrap.SharedMechanics.register`, `bootstrap.HeroModules.bootstrap`, content-модули, compat, команды; **0** ссылок на героев и контроллеры | ArchUnit + `wc -l` |
| 8 | `SuperheroesClient` | ≤ 40 строк тела: client core (`ClientSessionState` (BF7), `HudLayers`, `HeroActionKeys`, `SkinResolver`, core-receiver'ы, частицы/рендереры общих сущностей), `client.bootstrap.HeroClientModules.bootstrap`, compat; **0** ссылок на героев | то же |
| 9 | Серверные тик-регистрации вне `HeroTickDispatcher`/`HeroDataStore` | **0** | `onlyTheDispatcherRegistersServerTicks` — строгое |
| 10 | Lifecycle-регистрации вне модулей/ядра | **0** | `lifecycleHooksAreRegisteredThroughRegistrars` — строгое |
| 11 | Статические `Map/Set<UUID…>` в `src/main` вне `OwnedSessionMap` | **0** (было 106) | `grep -rEc 'static (final )?(Map\|Set\|ConcurrentMap\|WeakHashMap\|HashMap\|ConcurrentHashMap)<UUID' src/main/java` |
| 12 | Двунаправленные пары пакетов (без рёбер из composition roots) | **0** (было 32) | `PackageCycleRatchetTest`, baseline пуст |
| 13 | Ветки героев в `AbilityRouter`, `ResourceController`, `HeroTransformService`, `CombatImpactEngine`, `FlightController`, skin-миксинах, `RadialMenuHud`, `HeroInfoPanelHud`, `AbilitiesTooltipHud` | **0** | правило 1 + grep |
| 14 | Hero-specific mixins в общих mixin-пакетах | **0**; оставшиеся — в `mixin/hero/<id>/` с записанным обоснованием | ArchUnit правило 1 для mixin-пакетов; ревью mixin-конфигов |
| 15 | `Client*State` без регистрации сброса | **0** | `assertClientStatesRegisterReset` (BF7) |
| 16 | Дубли правил клиента и сервера (`ClientAbilityFilter`) | **0** | файла нет; C4 |
| 17 | Payload'ы героя без receiver'а в его client-модуле | **0** | `everyHeroS2CPayloadHasARegisteredReceiver` |
| 18 | Что ломает `qualityGate` автоматически | любое из: новая зависимость shared → герой; герой → герой; core/mechanic → выше; `src/main` → client; тик/lifecycle вне seam; `Client*State` без регистрации сброса; S2C без зарегистрированного receiver'а; модуль не создан в `HeroModules.ALL`; зависимость от composition root; новая пара циклов пакетов; незакоммиченное сокращение baseline; значения хуков героя, id модификаторов или lore отличаются от golden, или героя нет в golden; способность героя не зарегистрирована или без lang | все правила планов 1–6 |

## 9. Покрытие обязательных тем и пунктов аудитов

Стадии → планы: A1, A2, N1–N3 — П1; B1–B3, C1, C2 — П2; D1, D2a-1, D2a-2, D2b, D2c — П3; CL2–CL4, C4 — П4 (CL1 закрыт BF7); E1, E2, M1, F, G1–G3, H — П5; I1–I6, IC, L2, O — П6; BF1–BF12 — bugfix-pass.

| Тема (из задачи) | Стадии |
| :-- | :-- |
| 1. Guardrails и dependency rules | A1, A2, E2, §5.2, §8 |
| 2. `HeroProfile` | B1: хуки героя вместо записи `HeroProfile` (R15), B2 атрибуты |
| 3. Контракт `Ability` | BF11 (хуки гейтинга), C1, C2, C4, D2a-1 (`AbilitySink`) |
| 4. Ownership cooldown/resource/gating | план 2, «Контекст», C1, C2 |
| 5. `HeroModule` | D2a-1 (срез), D2a-2, D2b, F, G |
| 6. Lifecycle героя и игрока | BF3 `PlayerLifecycle` + BF11 `HeroLifecycle`; D1 (`LifecycleRegistrar`, `OwnedSessionMap`), D2c (`keepsHeroOnDeath`, остатки в `HeroTransformService`) |
| 7. Tick dispatcher вместо bootstrap | BF11 `HeroTickDispatcher`; D1 (`START`, `EARLY`, `TickRegistrar`), D2b |
| 8. Hero-specific ветки из shared core | BF11, B1, C2, C4, D2c, CL3, CL4, G, I4–I6, §8 п.13 |
| 9. Runtime state из static collections | §6.3, D1, F, волны, §8 п.11 |
| 10. Server authoritative state | §6.2 п.5 (C2S), C4 (доступность считает сервер), BF8 (B13) как зависимость I5c |
| 11. Synced attachments и network ownership | BF10 `PUBLIC_HERO`, §6.2, C4, CL4, L2 |
| 12. Client state lifecycle | BF7 `ClientSessionState` (CL1 закрыт) |
| 13. HUD registry | CL2, CL3 |
| 14. Input/actions registry | CL3 (`HeroActionKeys`), I5c (`InputLock`) |
| 15. Skin/render ownership | CL4, G2 (`PlayerLayers`), L2 (`BeamRenderer`) |
| 16. Mixin policy | §6.1, G2, I4d, I5, I6 |
| 17. Shared mechanics | M1, I2a (`charge`), I3 (`mechanic/ability`), I4a (`summon`), I4c (`strike`), I6 (`flight`, `impact`), BF1 `boundweapon` |
| 18. Damage/targeting/motion/world services | BF5 (damage), BF6 (world → `mechanic/world` в E2), M1 (motion, targeting; FxBroadcast в `core.net`) |
| 19. Разрыв package cycles | A1.4 ratchet, composition roots (R16), E2, волны, §8 п.12 |
| 20. Dead code / Doctor Strange / fake API / compat | N1, N2, N3, I5a/I5c (compat), IC (GeckoLib) |
| 21. Документация и `add-hero` | H (`migrate-hero`), O (`add-hero`, AGENTS.md §2/§7) |

| Пункт аудитов | Где закрывается |
| :-- | :-- |
| Opus долг 1 (lifecycle) | BF3 + BF11 + D1/D2c |
| Opus долг 2 (единый писатель) | BF2 ✔ |
| Opus долг 3 (bootstrap/tick) | BF11 + D1, D2b |
| Opus долг 4 (ветки героев) | BF11 + B1, C2, C4, D2c, CL3, CL4, волны |
| Opus долг 5 (sync-протокол) | BF2 (частично), BF10, §6.2, L2 |
| Opus долг 6 (пассивки) | BF9, B2 |
| Opus долг 7 (тонкий `Ability`) | C1, C2, M1, I2a |
| Opus долг 8 (дубли механик) | BF1 (no-drop), M1, I2a, I4a, L2 |
| Opus долг 9 (клиент-монолит) | BF7 + CL2–CL4 |
| Opus B1–B23 | BF-этапы (§3.1); B18/B19/B22 — стадии 13–15 bugfix-pass или отдельные `behavior:` PR после I4a/M1 |
| Структурный S1 (гейт цементирует god-object) | A2, D2a-1, D2b |
| S2/S3/S15 (тихие дефолты, две конвенции тем, пассивки по индексу) | B1 |
| S4 (двусмысленный `Ability`) | C1, C2 |
| S5 (идиомы вместо сервисов) | M1 + волны |
| S6 (нет единицы владения) | D2a-1, D2a-2, F, G, волны |
| S7 (mixins как фичи) | §6.1 |
| S8 (три копии правды на клиенте) | BF10, CL4 |
| S9 (HUD-геометрия) | CL2 |
| S10 (клавиши героев) | CL3 |
| S11 (одинаковые payload'ы) | L2 |
| S12 (фиктивный API) | N3 |
| S13 (мёртвый код) | N1, IC (GeckoLib) |
| S14 (cross-mod в ядре) | I5a, I5c |
| S16 (bootstrap знает способности) | BF11 (таблица тиков), D2b |
| S17 (22 `*SuitItem`) | B3 |
| Модульный §7.0–7.4 | A1/A2 (0), B1 (1), C2/D (2), F/G/I (3), H/O (4) |

## 10. Проверка планов и известные ограничения

**Внешнее ревью (2026-09-25).** Все подтверждённые замечания исправлены в планах: правила слоёв и списки модулей (R16), циклы пакетов (R16), цикл статической инициализации в B1 (R15, снимок legacy-значений только во время выполнения), возврат сброса кулдаунов в D2c (BF4), различие `START`/`END` в диспетчере (D1), `core → mechanic` в M1 (R17), `hero → content` в G1 (R18), `inputs.dir(...).optional()` в A1, ложные успехи проверок регистрации (A1, F.4), B3 против F, доступность методов в примере Scorpion, несогласованный граф (§2.1), безусловный отказ от runtime-проверки (§11), поздняя проверка seam (R19), побочные эффекты `OwnedSessionMap` (§6.3), мелкие несоответствия.

**Прототип A1 на сводной базе** (скретч, в репозиторий не публиковался; Java 21, Gradle 9.4.1, ArchUnit 1.5.1):
- `inputs.dir('…/archunit_store').optional()` при отсутствующем каталоге валит `:test` до запуска JUnit — подтверждено; `inputs.files(fileTree(…))` работает.
- Все правила A1 компилируются и выполняются. Без store замороженные правила падают с `Creating new violation store is disabled`, со store — зелёные; baseline: 32 пары циклов и счётчики из §3.4.
- Вертикальный срез D2a-1 + CL3a-1 (контракты `core.module`, `bootstrap.HeroModules`, `hero.scorpion.ScorpionModule`, который сам создаёт героя и 4 способности, `client.bootstrap.HeroClientModules`, `client.hero.scorpion.ScorpionClientModule` с receiver'ом): новых пар циклов нет, строгие правила проверяют реальные классы и зелёные, freeze store сам уменьшился на исправленное нарушение `Heroes.SCORPION`.
- Негативные пробы ловятся: `core` → модуль героя; `ScorpionModule.class` вместо создания в `ALL`; `SuperheroesMod.LOGGER` в `core`; ссылка на модуль героя извне; герой → `bootstrap`; новый цикл `ability ↔ core.module`.
- Вне `Heroes.java` и `AbilityRegistry.java` нет ни одной ссылки на их статические поля — модули могут сами создавать экземпляры, а реестры становятся чистыми (D2a).

**Ограничения:**
- Строки legacy-файлов указаны по сводной базе; после вливания BF-стека в `main` каждая стадия начинается со «Сверки».
- Код в Markdown, кроме правил A1 и среза D2a-1/CL3a-1, не компилировался; GameTests планов не запускались.
- `runClient` в песочнице без дисплея может быть недоступен — тогда runtime-пункты стадий проверяет владелец до мержа.

## 11. Оркестрация

Схема: один оркестратор, до двух исполнителей на стадию, ревьюер после сдачи. Исполнители одной модели, задачи и файлы им раздаёт оркестратор.

### 11.1 Правила

1. **Раздача.** Оркестратор читает стадию целиком, делает её «Сверку» и делит задачи по таблице §11.4. Каждый исполнитель получает свои задачи из плана, **список файлов, которые ему можно менять**, и сигнатуры из блоков **Interfaces**. Все остальные файлы для него только для чтения.
2. **Сначала основа.** Если в §11.4 у стадии есть «основа», её делает один исполнитель, и только после её сдачи второй начинает свою часть. Второй пишет код против сигнатур из плана, а не против файлов, которых ещё нет.
3. **Один файл — один владелец.** Пока исполнители работают параллельно, каждый файл принадлежит одному из них. Если не сказано иное, общие файлы стадии (`SuperheroesMod`, `SuperheroesClient`, `bootstrap.HeroModules`, `client.bootstrap.HeroClientModules`, `AbilityIds`, `AbilityRegistry`, `Heroes`, `ModItems`, `ModItemGroups`, `ModSounds`, `ModAttachments`, `ModNetworking`, `ClientNetworking`, `ModKeys`, `Hero.java`, `en_us.json` и `ru_ru.json`, `src/gametest/resources/fabric.mod.json`) принадлежат исполнителю A. Основу (правило 2) тоже делает A.
4. **Только у оркестратора:** `src/test/resources/archunit_store/**`, `src/test/resources/architecture/package-cycles-baseline.txt`, сгенерированные golden-файлы в `src/gametest/resources/golden/`, таблицы «Статус стадий», `SESSION.md`, этот файл.
5. **Сборку запускает только оркестратор.** Исполнители пишут код и команды `Run:`/`Expected:`, но Gradle не запускают (правило «Delegated execution» из `writing-plans`). После сведения оркестратор выполняет все `Run:` стадии, `qualityGate`, пересобирает ArchUnit baseline и коммитит его. Падение он возвращает владельцу упавшего файла.
6. **Запуск между фазами.** Характеризационные тесты и golden-снимки надо снять на **старом** коде, до переноса. Это отдельная фаза: её код пишет исполнитель, запускает оркестратор, и только потом начинается перенос. Такие фазы есть в A1 (создание store и baseline циклов), B1 (снимок legacy-значений в B1.1), B2 и B3 (golden до переноса), C1 (тест до чистки, store после), C2 (C2.1 на старом коде), F, G1 и в каждой волне П6.
7. **Ветки.** У каждого исполнителя свой git worktree и своя ветка от ветки стадии. Оркестратор сливает их в ветку стадии, и на стадию открывается один PR.
8. **Ревью после сдачи.** Свежий ревьюер, не исполнитель этой стадии, получает общий diff стадии, её паспорт (Acceptance, «Нельзя менять») и Global Constraints. Он проверяет, что поведение не изменилось, сохранённые id не тронуты, ни один файл не вышел за границы задачи и не появилось второго способа делать то же самое. Замечания уходят владельцу файла, после правок оркестратор снова гоняет `qualityGate`. В стадии H ревьюер — главный исполнитель.
9. **Слияние.** Мержит владелец репозитория. Зависимая стадия стартует от `main`, в который уже влита её зависимость. Стеки PR не строить: ArchUnit baseline в стеке конфликтует на каждом слое.
10. **Нагрузка.** Одновременно в работе не больше трёх стадий. Узкое место — сведение и `qualityGate` у оркестратора, а не число исполнителей.
11. **Runtime-проверка.** Если стадия меняет ввод, рендер, HUD, сеть, VFX или сущности, оркестратор сначала проверяет, запускается ли в окружении `./gradlew runClient --no-daemon` (дисплей или `xvfb-run`). Если запускается — проходит runtime-чек-лист стадии. Если нет — PR прямо пишет, что в игре не проверено, и перечисляет непроверенные пункты для владельца (AGENTS.md §10). То, что видят **другие** игроки (скины, хитбоксы, FX), проверяется вторым клиентом, а не видом от третьего лица.

### 11.2 Дорожки параллельности

Дорожки выведены из канонического графа §2.1; стадию можно брать, только когда все её жёсткие зависимости влиты в `main`.

| Когда | Что идёт одновременно |
| :-- | :-- |
| Старт | только A1 (барьер) |
| После A1 | дорожка 1: A2 → B1 → B2; дорожка 2: N2 → B3; дорожка 3: N1 → D1 → D2a-1 → D2a-2; дорожка 4: CL2; в свободные дорожки: N3, C1, C2 |
| После D2a-1 и CL2 | CL3a-1; после D2a-2 — CL3a-2 → CL3b → CL4 и C4; параллельно серверная дорожка D2b (после C2) → D2c |
| П5 | строго по одной стадии; E1 — ни одного открытого PR, трогающего `src/`. Исключение: пока идёт G1, исполнитель B заранее готовит клиентские реестры G2 (см. §11.4) |
| П6 | после go в H: I1, I2, I3 и IC параллельно; I4 после I2 и I3; внутри I4 и I5 герои параллельно; I6a → I6b строго по очереди (оба трогают полёт, L2 трогает лучи обоих); O — после всех волн |

### 11.3 Файлы, на которых сталкиваются стадии

Жёсткие пересечения уже учтены как зависимости в §2.1 (N1 → D1, N2 → B3). Остальные:

| Файл | Стадии | Правило |
| :-- | :-- | :-- |
| `Hero.java`, `DoomsdayHero`, `ThanosHero`, `PandoraHero`, `IronManHero` | B1, C2 (позже B2, C4, D2c) | B1 и C2 можно вести параллельно: правки в разных местах. Конфликт вставки (обе стороны добавили методы рядом) оркестратор решает, оставляя обе стороны |
| `SuperheroesMod` | C2, D1, D2a-1 | правят разные блоки (регистрация правил, строка подключения диспетчера, вызов `bootstrap.HeroModules`); при конфликте оставить все правки |
| `src/gametest/resources/fabric.mod.json` | почти каждая стадия с GameTest | каждая стадия дописывает свои строки; при конфликте оставить все |
| `src/test/resources/archunit_store/**`, `package-cycles-baseline.txt` | почти каждая стадия | не мержить руками: взять версию из `main`, прогнать `./gradlew test`, закоммитить результат |

### 11.4 Как делить стадию на двоих

Где сказано «герои A / B», герои делятся по позиции в `Heroes` / `HeroModules.ALL`, нечётные — A, чётные — B:

- **A:** Homelander, Regulus, Doomsday, Naruto, Kratos, Thanos, Raiden, Omni-Man, Scaramouche, Rem, Scorpion.
- **B:** Iron Man, Sung Jinwoo, Goku, Captain America, Loki, Reinhard, Invincible, Kazuha, Battle Beast, A-Train, Pandora.

Общие способности (`FLIGHT`, `VILTRUMITE_RECOVERY`, `ViltrumiteCharge`) и `bootstrap.SharedMechanics` — у A. Если в строке стоит «один», второго исполнителя оркестратор отдаёт другой стадии, у которой выполнены зависимости.

| Стадия | Основа (первым, один) | Исполнитель A | Исполнитель B |
| :-- | :-- | :-- | :-- |
| A1 | — | один: вся стадия (правила, ratchet, `verifyArchitectureBaseline`, `TestHeroes`) | — |
| A2 | — | A2.1 | A2.2 |
| N1, N2, N3 | — | один на стадию | — |
| B1 | B1.1: снимок legacy-значений (`SuperJumpController.isAllowed`, `HeroBleedingController.bleedFor`, JUnit-дамп глифов, GameTest-дамп хуков) и golden-файлы | B1.2: новые хуки в `Hero` и `PassiveGlyph`; значения для героев A; B1.3 серверные потребители (`SuperJumpController`, `HeroBleedingController`, `HeroMeleeImpactController`); B1.4 удаление констант из `HeroTheme`/`HeroHudConfig` и серверных таблиц | B1.2: значения для героев B (константы темы и HUD — только чтение из `HeroTheme`/`HeroHudConfig`); B1.3 клиентские потребители (`AbilityDescriptions`, `PassiveIcons`, `HudIcons`, `HeroInfoPanelHud`); B1.4 удаление `PassiveIcons` и `HERO_PASSIVE_COUNT` |
| B2 | golden-тест id модификаторов | герои A + удаление `HeroAttributes` в конце | герои B |
| B3 | `TransformationLore`, `LoreLine`, конструктор `TransformationItem` и golden-тест lore | предметы героев A + все правки `ModItems` | предметы героев B (только удаление классов после того, как A перевёл их строки в `ModItems`) |
| C1 | `CooldownGateGameTests` | способности героев A и общие способности | способности героев B |
| C2 | — | один: вся стадия (она маленькая) | — |
| D1 | — | D1.1: `OwnedSessionMap`, `LifecycleRegistrar`, JUnit | D1.2: фазы `START`/`EARLY` и `TickRegistrar` в `HeroTickDispatcher`, `HeroTickDispatcherGameTests` |
| D2a-1 | — | один: срез на Scorpion | — |
| D2a-2 | — | модули героев A; правки `Heroes`, `AbilityRegistry`, `bootstrap.HeroModules`, `SuperheroesMod` за обе половины | модули героев B |
| D2b | — | контроллеры и модули героев A, core и `bootstrap.SharedMechanics`; все правки `SuperheroesMod` (удаление строк обеих половин) | контроллеры и модули героев B |
| D2c | — | один | — |
| CL2 | — | один: вся стадия | — |
| CL3a-1 | — | один: API и срез на Scorpion | — |
| CL3a-2 | — | `ClientNetworking` (удаляет hero-receiver'ы обеих половин), `client.bootstrap.HeroClientModules` + client-модули героев A | client-модули героев B с их receiver'ами |
| CL3b | `HeroActionKeys` | удаление из `SuperheroesClient` и `ModKeys` за обе половины; регистрации HUD, рендереров и клавиши Raiden в client-модулях героев A | регистрации HUD, рендереров и клавиш Iron Man (`NANO_WEAPON`, `ESP_TOGGLE`) в client-модулях героев B |
| CL4 | `SkinProvider`, `SkinResolver`, `PlayerLayers`, `HeroClientContext.skin/playerLayer` | переписывание skin-миксинов и удаление регистрации слоёв из `SuperheroesClient` | в этой стадии все client-модули у B: `SkinProvider` Homelander, Sung, Thanos, Iron Man; `playerLayer` Rem, Reinhard, Iron Man |
| C4 | — | один: вся стадия | — |
| E1, E2 | — | один на стадию | — |
| M1 | — | `FxBroadcast` в `core.net`, перенос 5 циклов рассылки, `Motion`, GameTest `Motion` | `TargetFilter`, `Targeting`, GameTest `TargetFilter` |
| F | — | один: F1, затем F2 | — |
| G1 + G2 | — | G1 целиком; после вливания G1 — перенос клиента Reinhard в `client/hero/reinhard/` (G2) | параллельно G1: клиентские реестры G2 (`ClientSoundFilters` + общий `SoundEngineMixin`, `AbilityDecorations` + `RadialMenuHud`) с регистрацией в существующем `ReinhardClientModule` |
| G3 | — | один | — |
| H | — | ревьюер (не автор F и G) + замеры метрик §8 оркестратором | — |
| I1a, I1b, I2a, I3 (строки с двумя героями) | I2a: `ChargeSession`; I3: `mechanic/ability` | первый герой строки + основа | второй герой строки, после сдачи основы |
| остальные строки П6, IC1–IC3 | — | один на строку; разные строки идут параллельно (§11.2) | — |
| O | — | один | — |

В волнах П6 каждый исполнитель удаляет из общих списков только строки своего героя. Если два героя правят соседние строки, конфликт решает оркестратор, оставляя обе правки.
