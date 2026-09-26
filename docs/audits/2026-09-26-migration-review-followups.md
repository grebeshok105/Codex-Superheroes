# Спека: follow-up по ревью миграционных PR (#63–#70, #72, #73)

Дата: 2026-09-26. Источник: глубокое ревью смёрженных PR по плану [PR #41](https://github.com/grebeshok105/Codex-Superheroes/pull/41) (`docs/design/architecture-migration/00..06`). Ревью только читало код, на `main` @ `9480e00`.

Критичных дефектов нет. Перенос кода механически полный: обработчики, способности и ресиверы не потеряны и не задвоены. Эта спека закрывает три класса проблем:

1. Скрытые изменения поведения без коммита `behavior:` (нарушение политики из `00-overview.md`).
2. Тесты и ArchUnit-правила слабее, чем требует план.
3. Описания PR, `SESSION.md` и раздел «Решения» не совпадают с кодом.

Пометка **(проверить)** значит, что ревью не подтвердило факт до конца. Исполнитель сначала проверяет, потом делает.

## 0. Порядок выполнения

| Группа | Когда | Задачи |
|---|---|---|
| F-A: блокирует D2b-2 | до начала D2b-2 | F1, F2, F3, F4 |
| F-B: поведение | отдельные PR с `behavior:` | F5, F6, F7 |
| F-C: тесты и защита | можно параллельно | F8–F15 |
| F-D: архитектурный долг | до I-волн (план 06) | F16, F17, F18 |
| F-E: документация | в любом PR группы | F19 |

Каждая задача = отдельный коммит. Задачи, которые меняют поведение игрока, идут коммитом `behavior(<scope>): …` с GameTest и записью в раздел PR «Изменения поведения».

## F-A. Блокирует D2b-2

### F1. Решить позицию core-тиков (из #70, Major)

**Проблема.** `HeroModules.bootstrap` регистрирует тики модулей раньше `registerTickHandlers()`. Поэтому:
- `ResourceController.tick` был 14-м из 45 в PLAYER, стал последним (после `SharedMechanics.registerPost`).
- `PassiveReconciler.serverTick` был 3-м в GLOBAL, стал последним в GLOBAL.

Тики героев после старой позиции #14 теперь видят энергию и активные способности с прошлого тика. Тики #1–13 видят `onTickActive` того же тика.

**Сделать.**
1. Для каждого тика героя, который читает `isActive`, `EnergyLocks` или ресурс, определить, ломает ли его сдвиг (проверить).
2. Ввести `CoreMechanics.register(ctx)` и вызывать его до `SharedMechanics.register`. Либо принять новый порядок.
3. Записать решение в «Решения» `03-server-modules-ticks-lifecycle.md`.

**Приёмка.** Решение записано. Позиция двух core-тиков закреплена тестом порядка фаз (см. F4).

### F2. Порядок `AttackEntityCallback` и заморозка Reinhard (из #70, Major)

**Проблема.** Было: IronFists → InvincibleCombat → OmnimanMomentum → HeroMeleeImpact → ReinhardTimeSlow. Стало: IronFists → TimeSlow → InvincibleCombat → OmnimanMomentum → MeleeImpact. TimeSlow возвращает `FAIL` для замороженного атакующего, и цепочка обрывается. Замороженный Invincible или Omniman больше не запускает свои on-hit эффекты.

**Сделать.** Выбрать один вариант:
- (а) вернуть старое поведение: перенести запрет атаки замороженным в `SharedMechanics.registerPost` после MeleeImpact;
- (б) принять новое поведение отдельным коммитом `behavior(reinhard-time-slow): …` с GameTest «замороженный Invincible не наносит on-hit эффект».

**Приёмка.** GameTest фиксирует выбранное поведение. Описание в «Изменения поведения», если вариант (б).

### F3. Полная таблица перестановок порядка из D2b-1 (из #70, Major)

**Проблема.** PR назвал 2 инверсии (Charge/Rush/Unibeam/Landing/Flight). В коде их больше:
- глобальные тики общих механик (Landing.prune, MeleeImpact, Ballistic, Flight.cleanup, HeavensStrike) теперь идут до всех глобальных тиков героев;
- EquipmentLock, SuperJump, AutoSaturation, PassiveRegen теперь до всех player-тиков героев;
- `AFTER_DAMAGE`: было RemDemonism → GokuKiStack → KratosRage → KratosHandStrikeFx → DoomsdayKryptonite, стало DoomsdayKryptonite → GokuKiStack → Kratos → RemDemonism;
- JOIN: было ThanosGauntlet → IronManSuitSync → AdminBuildSync, стало обратно;
- START: Kratos и Thanos поменялись местами;
- Scorpion сдвинулся в PLAYER и GLOBAL почти в конец.

**Сделать.** Добавить в `03-server-modules-ticks-lifecycle.md` (раздел D2b) таблицу «было → стало». Для каждой пары отметить, есть ли общие данные. Отдельно проверить:
- передают ли `InvincibleCombat`, `OmnimanMomentum`, `DoomGrip` тела в `BallisticBodyTracker` или `HeroMeleeImpact` в своём `serverTick` (проверить);
- вызывает ли `RaidenMusouIsshinController.serverTick` `HeavensStrikeController.start/cancel` (проверить).

**Приёмка.** Таблица есть. Каждая пара с общими данными либо исправлена, либо обоснована.

### F4. Усилить тест фаз тиков (из #63, Major)

**Проблема.** `HeroTickDispatcherGameTests.startRunsBeforeEndPhasesAndEarlyBeforeGlobal` проверяет только относительный порядок в логе. Если START выполнится в конце END-тика, тест всё равно пройдёт. D2b-2 переносит START-листенеры Kratos, MadnessFlight и Thanos и опирается на этот инвариант.

**Сделать.** Писать в лог маркер тика (`server.getTickCount()` или маркер из `START_WORLD_TICK`) и требовать, что START и EARLY относятся к одному тику. Добавить проверку, что core-тики стоят там, где решено в F1.

**Приёмка.** Тест падает, если START перенести в END-фазу.

## F-B. Поведение

### F5. Клавиши N и K у Iron Man (из #72 и #68, Major)

**Проблема.** Раньше `tickNanoWeaponSelect` и `tickEspToggle` выходили до `consumeClick()`, если игрок не Iron Man. Нажатия копились и срабатывали после превращения. Теперь `HeroActionKeys` всегда сливает очередь. Изменение сидит в refactor-коммите `22be14a`. Для Raiden поведение не менялось. План 04 противоречит сам себе: спецификация `HeroActionKeys` подразумевает слив, а CL3b запрещает менять поведение клавиш.

**Сделать.**
1. Решить: слив (новое) или очередь (старое). Рекомендация: слив, старое поведение похоже на баг.
2. Если слив: коммит `behavior(client-keys): …`, запись в «Изменения поведения», тест на чистую функцию «fire или drain».
3. Убрать противоречие в `04-client-modules.md`.

**Приёмка.** Решение в «Решения» плана 04. Тест на логику `HeroActionKeys`.

### F6. Видимость способностей до прихода синка (из #73, Minor)

**Проблема.** `ClientAbilityVisibility.visibleFor` при отсутствии вложения `ABILITY_AVAILABILITY` показывает все способности. Старый `ClientAbilityFilter` по умолчанию скрывал (tier 1, `madness=false`, `open=false`). Вложение пишется в фазе PLAYERS END_SERVER_TICK, и нет гарантии, что оно придёт в одном кадре с `HeroData`.

**Сделать.** Один из вариантов:
- писать вложение сразу при трансформации и входе игрока;
- на клиенте не показывать способности героя, пока вложение не пришло.

**Приёмка.** GameTest: сразу после трансформации в Doomsday способности выше tier 1 недоступны клиенту. Исправлено утверждение «absent = старое поведение».

### F7. Неописанные изменения в C4 (из #73, Minor)

**Проблема.**
- Правило COUNTER_STRIKE стало действовать только для Regulus, раньше было глобальным. Эквивалентно, только если COUNTER_STRIKE есть лишь у Regulus (проверить).
- Панель Thanos обновляется каждый тик, раньше маска приходила раз в 10 тиков.

**Сделать.** Подтвердить эквивалентность или оформить `behavior:`. Записать в «Решения» плана 04.

## F-C. Тесты и защита

### F8. Тесты `OwnedSessionMap.create()` (из #63, Major)

**Сделать.**
- Фейковый `LifecycleRegistrar`, который запоминает хуки. Проверить связь `ClearOn` → onLeave/onDeath/onHeroClear и всегда onServerStopped.
- `requireNonNull` для key и owner в `put`, тест на NPE.
- Javadoc: только серверный поток, порядок хуков = порядок class-init, хуки не должны иметь побочных эффектов.
- GameTest: состояние CapShieldSlam очищается при leave и death.
- Гигиена `heroTickRunsOnlyForThatHero`: leave и снятие трансформации в конце, флаг `armed`, второй игрок с другим героем.

### F9. Строгие ArchUnit-правила (из #64 и #68, Major)

**Сделать.** Поставить `allowEmptyShould(false)` на правила, которые уже непустые:
- серверные: `coreDependsOnNothingAboveIt`, `heroModulesDoNotDependOnContentCompatOrBootstrap`, `heroModulesAreReferencedOnlyByThemselvesAndTheModuleList`, `everyHeroModuleIsConstructedInTheModuleList`;
- клиентские: `clientCoreDoesNotKnowHeroModules`, `clientHeroModulesAreReferencedOnlyByThemselvesAndTheModuleList`, `clientHeroModulesDoNotDependOnEachOther` (модулей уже 22).

**Приёмка.** Переименование пакета `client.hero` или `hero.<id>` роняет тесты.

### F10. Реестры отвергают дубликаты (из #64, #67)

**Сделать.**
- `Heroes.register` и `AbilityRegistry.register` бросают исключение на повторный id.
- Флаг «уже выполнено» в `HeroModules.bootstrap`, `HeroClientModules.bootstrap` и `HeroTickDispatcher.init`.
- `CoreClientContext.receive` проверяет результат `registerGlobalReceiver` и бросает `IllegalStateException("duplicate receiver " + id)`.

### F11. Полнота регистрации героев и способностей (из #67)

**Сделать.**
- ArchUnit: каждый конкретный подкласс `Hero` создаётся ровно одним `HeroModule`.
- GameTest: `AbilityRegistry.all().keySet()` равно объединению `getAbilities()` всех героев. Общие id (FLIGHT, VILTRUMITE_RECOVERY) регистрирует только `SharedAbilities`.
- `modulesCoverEveryHeroInRegistryOrder` сравнивает с явным списком 22 id по порядку.
- `scorpionIsRegisteredThroughItsModule`: искать `m instanceof ScorpionModule`, проверять порядок `getAbilities()`.

### F12. Контроллеры не отключены молча (из #70)

**Сделать.** ArchUnit через `JavaMethod.getCallsOfSelf()`: каждый `static register(HeroModuleContext)` в `*Controller` вызывается из `*Module` или `SharedMechanics`. `controllersHaveNoStaticInit` расширить на любые статические методы с `ServerTickEvents`/callback-регистрацией, не только `init`.

### F13. Регистрация ресиверов (из #69)

**Сделать.**
- ArchUnit: `ClientPlayNetworking.registerGlobalReceiver` вызывается только из `CoreClientContext` и `ClientNetworking`.
- Тест: `HeroClientModules.ALL` совпадает с `HeroModules.ALL` по `heroId()` и порядку.
- Аналог `everyHeroModuleIsConstructedInTheModuleList` для клиента, уникальность `heroId`.

### F14. Golden для пассивок и лора (из #65, #66)

**Сделать.**
- `TransformationLoreGameTests`: писать `DIVIDER@<COLOR>` и флаги стиля (`key@COLOR[BI]`), префикс `▸ `. Golden перегенерировать с коммита `bc50a97`.
- GameTest: для каждого `TransformationItem` `getHeroId()` есть в реестре героев (ловит опечатки в литералах `ModId.of("…")`).
- `passiveAttributeIdsAreStable`: после `removePassives` не осталось модификаторов `superheroes`.
- Golden для динамических наборов: Doomsday tier 1..7, фазы Reinhard 1..5, `REINHARD_DRAW`, `SECOND_COMING`, `RAIDEN_BURST`, `KRATOS_RAGE`, `NANO_*`, `REGULUS_MADNESS`.
- Проверка, что у каждого героя из `Heroes.all()` есть строки в golden пассивок или явный opt-out.

### F15. Тесты доступности способностей (из #73)

**Сделать.** GameTest для:
- Thanos: камни и snap при 6 камнях;
- Pandora: открытые и закрытые сессии;
- Rem с демонизмом: способности появились, ONI_RAGE скрыта;
- «пишется только при изменении»: и без изменения, и с изменением;
- переходы: герой → без героя, смена героя, tier 7 → 1, промежуточный tier 4.

## F-D. Архитектурный долг

### F16. Разнести `AbilityScopedModifiers` по владельцам (из #66, Major)

**Проблема.** B2 удалил `HeroAttributes`, но создал новый общий файл с данными 7 героев (Kratos, Reinhard, Raiden, Doomsday, Regulus, Iron Man, Thanos).

**Сделать.** Перенести каждый набор к владельцу (способность, контроллер или класс героя). `DoomsdayHero.passiveAttributes()` возвращает набор текущего tier или пустой набор с комментарием. `buildDoomsdayTierSet` переезжает в `DoomsdayHero`. Убрать override `applyPassives`, которые повторяют default (Scaramouche, BattleBeast). В javadoc `passiveAttributes()` написать, что метод покрывает только модификаторы атрибутов, не эффекты.

**Приёмка.** Файла `hero/AbilityScopedModifiers.java` нет. Id модификаторов не изменились (golden из F14 зелёный). Пары циклов `hero ↔ …` не выросли.

### F17. Остатки героев в клиентском core (из #69, #72)

**Сделать.**
- Перенести ресивер `SuitVariantS2CPayload` из `ClientNetworking` в `IronManClientModule`.
- Назначить стадию (CL4, проверить по плану 04) для остатков в `SuperheroesClient`: `LaserBeamRenderer`, `CosmicBeamRenderer`, `LocalLaserOverlay`, рендерер `HOMELANDER_BOSS`, `tickThinkMarkDash` (Omni-Man), проверка `ClientMadnessState.isReading()` (Regulus), проверка `RoyalIcicleItem` в `chargeFriendly`, геройские частицы.
- `entityRenderer` внести в интерфейс `HeroClientContext` в плане 04.
- `LightningBoltAccessor`: решить пакет по политике §6.1 (`client/core/mixin/`), проверить.
- Проверить, обновлён ли ArchUnit store после CL3b.

### F18. Прочее

- `TooltipFrame` перенести из `transform/` в нейтральный пакет (`tooltip/` или `ui/`) (из #65).
- `AdminBuildSyncController` вынести из `SharedMechanics` в core-wiring (из #70).
- `AbilityRouter` отклоняет способность, если `hero.visibility(...) != AVAILABLE`, либо `visibility` по умолчанию выводится из `canUseAbility` (из #73). Проверить `canActivate` у способностей Rem и COUNTER_STRIKE.
- Thanos: обходить камни один раз за `compute`, не создавать объекты без изменений (из #73).
- `Visibility.LOCKED`: убрать или реализовать на клиенте. `STREAM_CODEC` через `idMapper`/VarInt (из #73).
- Записать инвариант в javadoc `HeroModuleContext`: `register(ctx)` не трогает реестры и типы пакетов сразу, только в лямбдах (из #70).
- Записать в javadoc `HeroClientContext.actionKey`: клавиши разных героев должны различаться (из #68).

## F-E. Документация

### F19. Привести документы в соответствие с кодом

- `SESSION.md`: удалить маркеры конфликта (`>>>>>>> origin/main`, `||||||| 538416f`). Оставить один раздел B3, исправить `70/70`/`71/71` и `KratosBladeItem` → `BladeOfChaosItem`. Восстановить заметки D2a-1 под их заголовком (проверить). Убрать ложное утверждение про `ScorpionController.init()`.
- Раздел «Решения» в плане 03: у `ScorpionController` нет `init()`, Step 2 для Scorpion неприменим, место bootstrap; изменение порядка `AbilityRegistry.all()`; перестановка Scorpion и Pandora в `Heroes`; разделение `HeroMeleeImpact` на `register`/`registerPost`; разделение D2b на D2b-1/D2b-2.
- План 03: `nothingDependsOnCompositionRoots` это `FreezingArchRule`, а не строгое правило.
- План 02: откатить лишнюю правку статуса B3 из PR B2 (если ещё актуально).
- Правило процесса для следующих PR: статус «✅ merged» ставится после мержа, runtime-проверка указывает sha итогового head, в теле PR есть метрики «до/после» (§3.3) и раздел «Изменения поведения».

## Вне рамок

- Hellport: баг в `SafeTeleport.clamp`, существовал до миграции (найден в runtime-комментарии #64). Нужен отдельный issue.
- Raiden: клавиша F по умолчанию для `raiden_sword_draw` перехватывается `swapOffhand` (runtime-комментарий #72). Существовала до миграции. Нужен отдельный issue.

## Сводка по PR

| PR | Стадия | Соответствие плану | Задачи |
|---|---|---|---|
| #63 | D1 | да, тесты слабее плана | F4, F8, F10 |
| #64 | D2a-1 | код да, документация нет | F9, F10, F11, F19 |
| #65 | B3 | в основном да | F14, F18, F19 |
| #66 | B2 | частично | F14, F16, F19 |
| #67 | D2a-2 | да | F10, F11 |
| #68 | CL3a-1 | частично (тесты) | F5, F9, F10, F13 |
| #69 | CL3a-2 | частично (SuitVariant) | F13, F17 |
| #70 | D2b-1 | частично (порядок не описан) | F1, F2, F3, F12, F18 |
| #72 | CL3b | частично (behavior-политика) | F5, F17 |
| #73 | C4 | в основном да | F6, F7, F15, F18, F19 |
