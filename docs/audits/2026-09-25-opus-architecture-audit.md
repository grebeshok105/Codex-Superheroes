# Codex Superheroes — architecture & bug audit

> Independent Opus 5.5 audit snapshot, 2026-09-25. Preserved as the baseline for the Codex 5.0 restoration work.

## Статус исправлений

Живой трекер реализации. Каждая находка перед исправлением перепроверяется по текущему `main`; «подтверждено» означает, что механизм воспроизведён кодом, байткодом/исходниками ванили или GameTest'ом, а не только прочитан в отчёте субагента.

| Пункт | Статус | Как закрыт / где |
| :-- | :-- | :-- |
| B1 | ✅ исправлено | Один `PlayerBoundWeaponDropMixin` вместо трёх; `BoundWeapons.interceptDrop` никогда не вызывает `drop` повторно. Рекурсия воспроизведена GameTest'ом на старой логике (`StackOverflowError`). Этап 1. |
| B7 | ✅ исправлено | DataComponent `bound_weapon {owner, issue}` + non-persistent `BOUND_WEAPON_ISSUES`: валидна только текущая выдача владельца, остальные копии исчезают при тике в инвентаре. Этап 1. |
| B2 | ✅ исправлено | `HeroDataStore.update(player, fn)` — единственный писатель `HERO_DATA` (read-modify-write); `ResourceController` перечитывает состояние после каждого callback, деактивация через `AbilityRouter`; `AbilityRouter.deactivate` сначала снимает флаг, потом зовёт `onDeactivate`; возврат стоимости — дельтой, а не снимком. Регрессия Wind Prison воспроизведена GameTest'ом на старой семантике. Этап 2. |
| B3, B4, B8, B17, B23 | ✅ исправлено | `lifecycle/PlayerLifecycle` — единая точка диспетчеризации на событиях серверного потока (`JOIN`/`LEAVE` через `PlayerList.remove`, `AFTER_DEATH`, `AFTER_RESPAWN`, `SERVER_STOPPED`); `lifecycle/EntityControlLock` — рефкаунт-замки на NoAI/NoGravity/noPhysics/invulnerable с NBT-тенью и reconcile на `ENTITY_LOAD`; ability-scoped модификаторы transient (не сохраняются в NBT); `HeroTransformService.clearHeroRuntimeState` — симметричный cleanup для transform/untransform; мёртвые игроки пропускаются в serverTick и windup Snap. Этап 3. |
| B5, B6 | ✅ исправлено | `ABILITY_COOLDOWNS` — persistent attachment с дедлайнами по game time (без copyOnDeath: смерть сбрасывает, релогин нет); transform/untransform больше не трогают кулдауны; `transform()` не лечит (`min(health, maxHealth)`) и не пополняет энергию (переносится как мана при живом герое, полная при первой трансформации). `consumeGauntletAndStones` очищает `InsertedStones` в перчатке + отдельные камни. Этап 4. |
| B9, B11, B20, B21 | ✅ исправлено | Весь учёт урона перенесён на `AFTER_DAMAGE`/`damageTaken` (`ALLOW_DAMAGE` оставлен только слушателям, которые реально возвращают `false` — блок/перенаправление); спасения от смерти — на `ALLOW_DEATH` (Kawarimi в т.ч.); SungJinwoo-divert пропускает `#bypasses_invulnerability`. 33 mod damage types добавлены в `#minecraft:bypasses_cooldown` (сохранённые i-frame'ы мобов: `SHADOW_ATTACK`, `HOMELANDER_MELEE`). `ReinhardTimeSlowController` переписан без `TickRateManager`: заморозка радиусом 80 блоков через `EntityControlLock` + transient-модификаторы скорости игроков, дедлайны по `gameTime`. `currentTimeMillis` вычищен из RegulusMadness/Doomsday*/LandingTracker/ReinhardSpeedJudgment — везде tick-дедлайны; клиенту Regulus по-прежнему шлются remaining-ms. Этап 5. |
| B10 | ✅ исправлено | `world/WorldDestructionPolicy` — единый chokepoint для всех способностей: `tryBreak` (ванильный `destroyBlock` + опциональные дропы), `tryCarve` (тихий вырез без дропов для кратеров/бросков) и `tryPlace` (огонь и прочие постановки) гейтятся `destroySpeed < 0` + тегом `superheroes:ability_immune`; для игроков дополнительно `Level.mayInteract` (защита спавна, граница мира) и `PlayerBlockBreakEvents` BEFORE/CANCELED/AFTER — claim-моды видят все сломы; для мобов и без причины — `RULE_MOBGRIEFING`. Мигрированы все 17 точек (`RegulusMadnessController.carveCrater` — до этого съедал бедрок — `RemDemonismController`, `UnibeamController`, `MadnessAftermathController`, `RushTerrainBreaker`, `ShockwaveUtil`, `BallisticBodyTracker`, `HomelanderBlockThrowGoal`, `MadnessFlightController`, `RaidenMusouIsshinController`, `HeavensStrikeController`, `GuardiansBreakerAbility`, `EyeLasersAbility`). `RushTerrainBreaker`/`BallisticBodyTracker`/`GuardiansBreakerAbility` теперь ломают с дропом — shulker box выскакивает с содержимым, а не исчезает. `BlockBreakPolicy` удалён; `ProjectSanityTest` запрещает сырые мутации мира вне политики. Этап 6. |
| B15 | ✅ исправлено | `ClientSessionState` — реестр сброса: каждый `Client*State` и сессионный синглтон регистрирует свой reset в static-блоке, DISCONNECT гоняет всех (было 10 из ~30); `ProjectSanityTest` валится без регистрации. Кулдауны — по `ClientLevel#getGameTime()` (монотонное время уровня, не `LocalPlayer.tickCount`). Миксины Pandora пропускают `GLFW_RELEASE` — залипших клавиш после ролика нет. `IrisShaderBridge` хранит снапшот до успешного restore, пост-краш-рестарт ретраит 200 тиков. Хит-тест чата (`screenToChatX/Y`) вычитает тот же сдвиг, что и рендер. Этап 7. || B13 | ✅ исправлено | `MirrorDimensionController` больше не ждёт подтверждения клиента: `enforceZone` (yank + `VanityAuthority` debuffs) применяется безусловно, gate `canSend` в `absorbNearby` снят — пакеты жертве остались косметическим хинтом через `sendToVictim`; `handleStatus` теперь только пересылает фидбек кастеру (NO_IRIS/NO_PACK/IRIS_API_FAIL не выпускают жертву). Клиентский deadman `ClientMirrorDimensionState` следит за `trapped` (шифр), а не только `active` (шейдер). Lifecycle: `onPlayerGone`/`resetAll` в MirrorDimensionController и SpatialBindController через `PlayerLifecycle.onLeave`/`onServerStopped`. Этап 8. |
| B13 | ✅ исправлено | `MirrorDimensionController` больше не ждёт подтверждения клиента: `enforceZone` (yank + `VanityAuthority` debuffs) применяется безусловно, gate `canSend` в `absorbNearby` снят — пакеты жертве остались косметическим хинтом через `sendToVictim`; `handleStatus` теперь только пересылает фидбек кастеру (NO_IRIS/NO_PACK/IRIS_API_FAIL не выпускают жертву). Клиентский deadman `ClientMirrorDimensionState` следит за `trapped` (шифр), а не только `active` (шейдер). Lifecycle: `onPlayerGone`/`resetAll` в MirrorDimensionController и SpatialBindController через `PlayerLifecycle.onLeave`/`onServerStopped`. Этап 8. |
| B12, B16 | ✅ исправлено | `lifecycle/PassiveReconciler` — захват объявленных пассивок (ThreadLocal-scope вокруг `applyPassives` + `LivingEntityPassiveEffectsMixin` на `addEffect`/`onEffectRemoved`) и отложенная ре-ассертация на `END_SERVER_TICK` для конкретного игрока (с guard по `heroId`, перезапись на свапе); точки захвата — transform, join/respawn, `DoomsdayTierController.applyProgress`. `RegulusMadnessController.clearMadness` снимает только эффекты с madness-сигнатурой (≤60 тиков, ambient, icon-only, свой amplifier) — зелья/маяки/пассивки больше не сносятся на join/death/respawn. `LivingEntityFallDamageMixin` проверяет `isCounterInvolved(entity)` (владелец/жертва конкретного контра) вместо глобального `isAnyCounterActive`. Этап 9. |
| B14 | ✅ исправлено | Новый synced attachment `PUBLIC_HERO` (persistent + copyOnDeath + `syncWith(STREAM_CODEC, all())`) — узкая проекция только heroId; зеркалируется единственным писателем `HeroDataStore.update` + back-fill `syncPublicHero` на JOIN для старых сейвов. `PlayerDimensionsMixin` читает `PUBLIC_HERO` — удалённые игроки получают реальные размеры хитбокса героя; клиентский `ClientHeroDimsWatcher` вызывает `refreshDimensions()` при изменении synced id (у sync нет колбэка). Удалены `RemoteHeroSkinS2CPayload`/`RemoteHeroSkins`/`broadcastRemoteHeroSkin`/`sendRemoteHeroSkinTo`/`START_TRACKING`-блок; скины чужих героев, ESP, scabbard-layer и nano-suit-up теперь читают `PUBLIC_HERO` с сущности — баг с застрявшим скином после релогина закрыт вместе с ручной разводкой. Этап 10. |
| B18, B19, B22, мелочи, потенциальные | ⏳ | Разбираются по ходу связанных этапов. |
| Долг 2 | ✅ закрыт | Все ~20 прямых писателей переведены на `HeroDataStore`; `ProjectSanityTest.assertHeroDataHasSingleWriter` запрещает прямой `setAttached(HERO_DATA)`/sync вне стора. Полный sync — сразу (порядок с другими пакетами), энергия/мана — один пакет в конце тика (фаза `hero_data_flush`). Этап 2. |
| Долг 5 | ✅ закрыт | Мёртвый `DeactivateAbilityC2SPayload` удалён; лишние `server().execute` в C2S-обработчиках убраны — Fabric и так вызывает их на серверном потоке. Ручной S2C-разводкой для вида героя (`RemoteHeroSkinS2CPayload` + broadcast/track plumbing) заменён synced attachment `PUBLIC_HERO` (этап 10). |
| Долг 1 | ✅ закрыт | `PlayerLifecycle` (этап 3) — единый хаб LEAVE/JOIN/AFTER_DEATH/AFTER_RESPAWN/SERVER_STOPPED; `HeroLifecycle` (этап 11) — события `onClear`/`onTransformed` с централизованной таблицей слушателей в `registerPlayerLifecycle()`, заменившие захардкоженный список `clearHeroRuntimeState`. Этапы 3 и 11. |
| Долг 3 | ⚠️ базовая часть | `HeroTickDispatcher` (этап 11): фазы GLOBAL→LEVELS→PLAYERS→ABILITY_ACTIVE, единый `END_SERVER_TICK`, мёртвые игроки скипаются один раз централизованно (B17 закреплено GameTest'ами), `HeroData` читается один раз на игрока; bootstrap-монолит `SuperheroesMod` разбит на `registerTickHandlers()`/`registerPlayerLifecycle()` — порядок виден в таблице. Миграция ~50 контроллеров, которые всё ещё сами регистрируют `ServerTickEvents`, — следующий этап. |
| Долг 4 | ✅ закрыт | Хуки в `Hero`: `getImpactStyle`/`getImpactPower` (таблицы `CombatImpactEngine` переехали в 20 героев), `getThreatClass` (`HERO_THREATS` удалён, класс переехал `jarvis/`→`hero/`, цикл пакетов разорван), `canUseAbility`/`onAbilityDenied`/`isAbilitySuppressedBy`/`getEnergyReserveFor`/`isUraniumWeak` (`instanceof` ветки `AbilityRouter` и `HomelanderHero.ID` в `FlightController` удалены); `ClientAbilityFilter` переиспользует серверные таблицы `DoomsdayHero.isUnlockedAtTier`/`RemHero.isVisibleIn` вместо своих дублей. `ProjectSanityTest.assertNoHeroTypeDispatch` запрещает `instanceof *Hero` вне героев. Этап 11. |
| Долг 6 | ⏳ этапы 3 и 9 | Transient-модификаторы и замки (этап 3) закрыли утечки баффов; reconciler пассивок (B12) — этап 9 в работе. |
| Долг 7, 8, 9 | ⏳ | Тонкий контракт `Ability`, дубли механик, клиент-монолит — вне этапов 1–11, отдельный план. |

Новые находки, обнаруженные при реализации:

- **N1.** `ServerPlayConnectionEvents.DISCONNECT` может прийти с Netty-потока (Fabric вызывает его и из `Connection.channelInactive`). Текущая очистка в `SuperheroesMod` на DISCONNECT гоняет игровое состояние вне серверного потока. Закрывается в этапе 3 (хук выхода на серверном потоке).
- **N2.** Veil жёстко зависит от клиентских модулей Fabric API и не грузится на выделенном сервере, поэтому `runServer` в dev падает на резолве модов. Код мода Veil уже защищает `isModLoaded`; GameTest-сервер исключает Veil из classpath.
- **N3.** Меч Райден/Рейнхарда после перезахода с активной способностью оставался в инвентаре навсегда (активные способности на входе очищаются без `onDeactivate`). Закрыто этапом 1: такая копия больше не текущая выдача и исчезает.

## Исходный отчёт

Аудит готов. Я нашёл около 30 реальных багов и 9 системных архитектурных проблем, из которых растёт большинство этих багов. Самое опасное: два пути к падению сервера, вечные бессмертие и баффы после перезахода, дюп оружия и способности, которые остаются «активными» после выключения. Код я не менял, рабочее дерево чистое.

**Как проверял.** Ядро (bootstrap, transform, router, ресурсы, attachments, сеть) прочитал сам. Остальные \~55k строк в 565 файлах параллельно разобрали 5 субагентов, каждый только читал свою область. Поведение ванили и Fabric сверял по mapped-jar 1.21.1 и исходникам Fabric API 0.116.12 через `javap`.

Сборка `./gradlew build -x test` и `./gradlew test` проходят, JDK 21 поставил в песочницу через mise. Тестов всего 14, и они покрывают только математику полёта и удара. В игре я ничего не воспроизводил.

Лично по коду или байткоду перепроверены B1–B6, B8–B12, B14, B16, B23 и залипание клавиш из B15. Остальное взято из отчётов субагентов со ссылками file:line.

Пути ниже: `M/` = `src/main/java/com/example/superheroes/`, `C/` = `src/client/java/com/example/superheroes/client/`.

## 1. Реальные баги

### Критичные

**B1. Сервер падает из-за бесконечной рекурсии в no-drop mixin'ах.**

- **Где.** `M/mixin/YamatoNoDropMixin.java:21-26`, и такие же `RoyalIcicle…` и `RemMorningStar…`.
- **Механизм.** Если `add` не удался, mixin вызывает `placeItemBackInInventory`. Без свободного слота ваниль вызывает `Player.drop(stack,false)` (проверено через javap), это снова заходит в mixin, и всё заканчивается `StackOverflowError`.
- **Когда срабатывает.** Sword Draw у Raiden (`RaidenSwordDrawAbility:88-89`) и Reinhard (`:93-94`) при полном инвентаре. Ещё `RemDemonismController.giveMace`, который вызывается каждый тик (`:329`).
- **Решение.** Один mixin по item-тегу, внутри `drop` никогда не вызывать `drop` повторно, плюс защита от повторного входа.
- **Объём.** XS–S, риск низкий.

**B2. `ResourceController` перезаписывает данные старой копией, и способности «воскресают».**

- **Где.** `M/resource/ResourceController.java`: в `:31` читается `HeroData`, в `:68` вызывается `onTickActive`, в `:71-75` обратно пишется старая копия.
- **Scaramouche Wind Prison** (`ScaramoucheWindPrisonAbility:89-93`). Расход 1.2 меньше регена 1.6, поэтому после окончания зоны способность навсегда «активна» на сервере. Она тратит ресурс, 20 раз в секунду шлёт полный NBT-sync, а первое нажатие клавиши её только выключает.
- **Другие случаи.** Raiden Eye (`:80-85`) и `IronFistsController:166` ломаются так же. Тот же паттерн в `AbilityRouter.deactivate` (`:103-115`): он затирает `ensureAbilityActive` у Rem.
- **Решение.** Перечитывать данные после каждого callback, деактивации применять после цикла. Дальше — один writer `HeroDataStore.update(fn)`.
- **Объём.** 2 файла, риск низкий.

**B3. Баффы toggle-способностей переживают перезаход, краш и даже снятие героя.**

- **Причина.** `AttributeModifierSet.apply` использует `addOrReplacePermanentModifier`, такие модификаторы пишутся в NBT. При входе и респавне список активных способностей просто очищается без `onDeactivate` (`HeroTransformService:147-152`, `:162-167`).
- **Пример.** `KratosSpartanRageAbility:45` снимает модификатор только в `onDeactivate:98`, а `KratosHero.removePassives:71-74` его не трогает. В итоге Кратос получает вечные +урон и +HP при любом герое. Аналогично Goku SSJ (`:44`), Naruto Sage, Doomsday Berserk.
- **Решение.** Transient-модификаторы, как в `ironman/IronManSuitStats`, и `cleanup(player, reason)` на join, respawn и disconnect.
- **Объём.** \~6 файлов, риск низкий.

**B4. Флаги, которые пишутся в NBT, используются как временная механика.**

- **Pandora.** `PandoraDeathController:141,212` вызывает `setInvulnerable(true)`. При выходе (`:251-254`) флаг не сбрасывается, а `resetOnHeroTaken` (`:244-248`) после перезахода его уже не снимает. Получается бессмертие навсегда.
- **Doomsday.** `DoomGripController:49` снимает неуязвимость, только если Doomsday онлайн (`:62-66`).
- **Omni-Man.** `OmnimanThinkMarkAbility:224-235` выставляет жертве `NoAI` и `NoGravity`. Снимается это только из тика владельца, который онлайн. Владелец вышел — моб заморожен навсегда. Так же ведут себя Regulus Greed и церемония Reinhard; церемония ещё и «оживляет» мобов, которые были NoAI до неё.
- **Решение.** `EntityControlLock` на non-persistent attachment: время жизни, счётчик ссылок, освобождение на disconnect, смерти, смене мира и остановке сервера.
- **Объём.** S–M.

### Высокие

**B5. Смена героя сбрасывает кулдауны и лечит.** Предмет трансформации не тратится (`TransformationItem:37`), кулдаун трансформации 20 тиков. `transform()` при этом:

- заполняет энергию (`:57`);
- чистит все кулдауны (`:64`);
- сбрасывает тотем Regulus (`:65`);
- лечит до максимума (`:69`).

Смена A→B→A перезаряжает Snap и Monarch's Domain. Перезаход тоже сбрасывает кулдауны, потому что они хранятся только в статическом поле (`AbilityCooldowns:13`). Решение: персистентный attachment с дедлайнами по game time.

**B6. Snap не тратит камни.** Проверка читает камни из перчатки (`ThanosGauntletStateController:89-95`), а `consumeGauntletAndStones` (`ThanosSnapAbility:179-194`) удаляет только отдельно лежащие камни. Snap бесконечен.

**B7. Дюп привязанного оружия.** `giveMace` каждый тик проверяет только инвентарь игрока. Положил булаву в сундук — через тик выдана новая. С мечом Raiden и Royal Icicle то же. Решение: DataComponent-токен `{owner, issueId}`.

**B8. Статическое состояние переживает смену мира в одиночной игре.** `ServerLifecycleEvents` в проекте не используется ни разу.

- **Трансформация блокируется.** `HeroTransformService:27` хранит `server.getTickCount()` в `WeakHashMap<UUID>`, а UUID профиля держит клиент, поэтому запись не удаляется. У нового мира счётчик тиков с нуля, и трансформироваться или снять героя нельзя, пока он не догонит старый (`:118-124`).
- **Утечка целого мира.** `HordeManager:42,72,128-137` держит ссылку на старый `ServerLevel` и никогда её не отпускает. `SungJinwooController` хранит `Map<ServerLevel,…>`.
- **Старые дедлайны.** Аура Monarch's Domain продолжает бить в следующем мире.
- **Решение.** Сброс на `SERVER_STOPPED`, состояние перенести в non-persistent attachments.

**B9. Событие `ALLOW_DAMAGE` используется для подсчёта урона.**

- **Почему это не работает.** Fabric вызывает `ALLOW_DAMAGE` раньше щита, i-frames и брони (проверено по исходникам `fabric-entity-events`). Цепочка обрывается на первом `false`. Порядок 13 слушателей задаётся только порядком строк `init()`.
- **Doomsday** копит сырой урон на каждый вызов: лава и кактус в i-frames дают иммунитет за несколько тиков. Таймер при этом идёт по `currentTimeMillis`, то есть по реальному времени, а не игровому.
- **Kratos** получает ярость за заблокированные и отменённые удары.
- **Greed** копит каждый вызов и потом бьёт суммой.
- **Sung** перенаправляет на тени даже `/kill` и урон от пустоты, так что Sung неубиваем, пока жива хоть одна тень.
- **Решение.** Подсчёт перенести на `AFTER_DAMAGE`, спасения от смерти — в `ALLOW_DEATH`, общая проверка `BYPASSES_INVULNERABILITY`, явные event phases.

**B10. Разрушение мира без каких-либо правил.** В `src/main` нет ни `RULE_MOBGRIEFING`, ни `mayInteract`, ни защиты спавна, ни `PlayerBlockBreakEvents`, поэтому claim-моды ничего не видят.

- `RegulusMadnessController.carveCrater:469-484` ставит `AIR` конусом, вместе с бедроком.
- `BallisticBodyTracker`, `GuardiansBreaker` и `RushTerrainBreaker` ломают блоки без дропа, shulker box пропадает вместе с содержимым.
- Решение: один `WorldDestructionPolicy` плюс тег `ability_immune`.

**B11. Reinhard меняет tick rate всего сервера.** `ReinhardTimeSlowController:132-139` вызывает `setTickRate`: все измерения замедляются на 8.5 с, а админский `/tick rate` затирается.

**B12. Пассивки героев стираются и не возвращаются.**

- Пассивки — это бесконечные эффекты, и накладываются они только при трансформации.
- Их удаляют `LionHeart:43`, `ThanosTimeRewind:44`, death-save Regulus (`:37,47`) и обычное ведро молока.
- `RegulusMadnessController.clearMadness` (`:263-276`) на каждом входе, смерти и респавне снимает Speed, Strength, Jump и Resistance у любого игрока: зелья и маяки пропадают при перезаходе.

**B13. House of Vanity доверяет клиенту жертвы.** Зона включается только после подтверждения от клиента (`MirrorDimensionController:256`). У игрока без Iris весь текст на экране зашифрован до дисконнекта, а модифицированный клиент просто уходит из ловушки.

**B14. Другие игроки видят героя неправильно.** `HERO_DATA` отправляется только владельцу (`ModNetworking:108-110`), а `PlayerDimensionsMixin` читает его для всех игроков.

- Хитбокс Battle Beast (0.86×2.5) у других клиентов ванильный.
- Скин залипает после снятия героя вне зоны видимости: `sendRemoteHeroSkinTo:141-149` ничего не шлёт, если героя нет.
- Решение: в 0.116.12 есть `AttachmentRegistry.Builder#syncWith(...)` (проверено) плюс `refreshDimensions()`.

**B15. Клиентские состояния залипают.**

- При выходе сбрасываются только 9 из 26 `Client*State` (`C/SuperheroesClient.java:275-288`). Если выйти во время time-slow, звуки мира пропадают во всех следующих мирах.
- Кулдауны на клиенте считаются по `LocalPlayer.tickCount`, который обнуляется при респавне; HUD показывает часы.
- Mixin'ы ролика Pandora отменяют и отпускание клавиш, после ролика игрок сам идёт или бьёт.
- Восстановление Iris после краша не срабатывает и удаляет свой снапшот.
- Сдвиг чата ломает клики по ссылкам.

### Средние

- **B16. Глобальный fall-иммунитет.** `LivingEntityFallDamageMixin` проверяет глобальный `isAnyCounterActive()`: любой активный контр Regulus где угодно снимает иммунитет к падению со всех героев.
- **B17. Отложенные действия не проверяют владельца.** Snap срабатывает от трупа. Метка Reinhard после респавна жертвы бьёт `hurt(MAX_VALUE)`. Заряды способностей продолжаются после смерти.
- **B18. Тени Sung хранятся только в статике.** После рестарта сервера армия спавнится заново рядом с сохранёнными тенями. Сироты продолжают атаковать и после снятия героя.
- **B19. PvP и команды игнорируются.** `isPvpAllowed` и `isAlliedTo` нигде не используются, `magic()` без атакующего обходит `pvp=false`. Есть 21 копия предиката выбора целей.
- **B20. i-frames режут урон.** Ни один урон мода не входит в `#bypasses_cooldown`: лазеры по расчёту из кода дают ≈6–12 DPS вместо 56–120.
- **B21. Таймеры по реальному времени.** `currentTimeMillis` используется в TimeSlow, RegulusMadness, DoomsdayAdaptation и LandingTracker. Пауза в одиночной игре и лаг ломают длительности.
- **B22. Телепорты сквозь стены.** Goku IT делает `look*12` без проверки блоков; blink «за спину» ни у кого не проверяет коллизии.
- **B23. Асимметричная очистка.** `clearAdaptations`, `clearOnUntransform` и `Unibeam.clearState` вызываются только в `doUntransform` (`HeroTransformService:99-103`), а при прямой смене героя — нет.
- **Мелочи.**
  - `fabric.mod.json` объявляет `"minecraft": "~1.21"` и `"fabric-api": "*"` при сборке под 1.21.1/0.116.12.
  - У `horde_crystal` нет модели.
  - У 5 сущностей нет lang-имён.

### Потенциальные (вероятные)

- Орда без игроков доигрывает волны и спавнит мобов в невыгруженных чанках на минимальной высоте мира. Мобы не деспавнятся (`BaseHordeEntity:190-198`).
- Ram может дублироваться: `RamCompanionController` держит живую ссылку на сущность.
- `@Pseudo HeroComponentStripMixin` использует `@Shadow` на поле чужого мода. `require=0` это не покрывает, поэтому переименование поля в том моде уронит загрузку.
- Битый клиентский конфиг ломает запуск: ловится только `IOException`, запись неатомарная.

## 2. Архитектурный долг (корневые причины)

1. **Нет жизненного цикла игрока и героя.** Это корень B3, B4, B8, B17 и B23.
   - Очистка разбросана по захардкоженным спискам в `HeroTransformService`, по `removePassives` и по самолечению в тиках.
   - 8 методов очистки не вызываются вообще.
   - Около 105 статических UUID-коллекций.
   - **Решение.** `HeroLifecycleEvents` плюс `Ability.cleanup` плюс non-persistent attachments. Объём M–L.
2. **У `HeroData` нет единственного писателя.** Активный набор способностей меняют и синхронизируют 7 разных мест. Решение: `HeroDataStore.update` и один flush за тик.
3. **Центральный bootstrap и рассылка тиков.**
   - \~70 вызовов `init()` плюс 15 `serverTick` на каждого игрока (`SuperheroesMod:28-130`).
   - 52 регистрации тиков в 50 файлах.
   - Семантика урона и входа зависит от порядка строк.
   - **Решение.** `HeroTickDispatcher`, event phases, модуль на каждого героя.
4. **Ветки под конкретных героев в общем коде.**
   - `AbilityRouter:41-57,66,137`.
   - `CombatImpactEngine` импортирует 20 героев; Scorpion и Pandora уже выпали из его таблиц.
   - `JarvisThreatClass`, `FlightController`.
   - Клиентский `ClientAbilityFilter` дублирует проверки роутера.
   - **Решение.** Хуки в `Hero`: `canUseAbility`, `getCombatProfile` и lifecycle-методы.
5. **Самописный sync-протокол.**
   - 44 payload'а в одном `init`, 26 статических состояний на клиенте.
   - `HeroData` синхронизируется через NBT.
   - `DeactivateAbilityC2S` принимается без каких-либо проверок.
   - **Решение.** Synced attachments и общая проверка C2S-пакетов.
6. **Пассивки на MobEffect и permanent-модификаторах** (B3, B12). Решение: reconciler, который досыпает недостающие пассивки, либо transient-модификаторы.
7. **Слишком тонкий контракт `Ability`.** Около 95 вызовов `setCooldownTicks` и \~380 `sendParticles`. Решение: `AbilitySpec`, runtime-attachment, базовые классы Channel/Charge/Beam, сервисы для целей, урона и мира.
8. **Дубли механик.** Метры, притягивания и заморозки повторяются у разных героев; 3 no-drop mixin'а; 4 сущности-призыва без `OwnableEntity`.
9. **Клиент-монолит.**
   - 24 жёстко прописанных HUD-вызова (`SuperheroesClient:161-199`).
   - Глобальная подмена рендера всех молний (`:64`).
   - Опрос клавиш через GLFW, короткие нажатия теряются.

## 3. Улучшения

- **Сеть.**
  - Ресурсы уходят клиенту каждый тик, пока энергия не полная.
  - Армия Sung рассылается всем игрокам в мире каждый тик.
  - Unibeam делает \~400 `sendParticles`, Kamehameha — до 180 пакетов за тик.
  - Эффекты переприменяются каждый тик.
- **Мелкое.**
  - Лишние `server().execute`: Fabric и так вызывает обработчики на главном потоке.
  - Устаревший `AttachmentRegistry.builder()`.
  - 18 `Component.literal` с кириллицей вместо lang-ключей.
- **Документация.** AGENTS.md и project-profile устарели. В них MC 1.21, 14 героев, 80 способностей и «тестов нет». На деле 1.21.1, 22 героя, 118 из 123 id зарегистрированы, а `src/test` существует.
- **Релизы и ревью.** `release.yml` считает теги `v1.0.N`, игнорируя `mod_version=4.0.0`. `auto-approve-pr.yml` сам апрувит любой зелёный PR, то есть ревью фактически отключено.
- **Тесты.** `fabric-gametest-api` уже есть в зависимостях, им можно покрыть B1, B7 и B10.

## 4. Что опровергнуто

- Утечки `mayfly` (разрешения на полёт) после перезахода нет: ваниль сбрасывает его в `loadGameTypes` после загрузки NBT (проверено javap).
- Resistance V от Lion Heart после перезахода снимается, но только случайно — через баг B12.

## 5. Кандидаты на отдельные PR

| **#** | **PR**                                                                                                           | **Закрывает** | **Объём / риск**   |
| :---- | :--------------------------------------------------------------------------------------------------------------- | :------------ | :----------------- |
| 1     | Единый no-drop mixin по тегу + токен привязанного оружия                                                         | B1, B7        | S / низкий         |
| 2     | Убрать перезапись старой копией в ResourceController и AbilityRouter                                             | B2            | XS / низкий        |
| 3     | Lifecycle: деактивация на disconnect/join, transient-модификаторы, lock вместо флагов, сброс на `SERVER_STOPPED` | B3, B4, B8    | M / низкий–средний |
| 4     | Персистентные кулдауны + расход камней Snap                                                                      | B5, B6        | S / баланс         |
| 5     | Пайплайн урона + отказ от глобального tick rate                                                                  | B9, B11, B20  | M / баланс         |
| 6     | ✅ `WorldDestructionPolicy`                                                                                      | B10           | M / низкий         |
| 7     | Клиентская сессия с TTL, клавиши, Iris, чат                                                                      | B15           | S–M / низкий       |
| 8     | Серверная авторитетность House of Vanity                                                                         | B13           | S / низкий         |
| 9     | Reconciler пассивок, per-player fall-иммунитет                                                                   | B12, B16      | S–M / низкий       |
| 10    | Synced attachment для публичного вида героя                                                                      | B14           | M / средний        |
| 11    | `HeroLifecycleEvents` + `HeroTickDispatcher` + хуки `Hero`                                                       | долг 1–4      | L, после 2–3       |
| 12    | Документация, строгие зависимости, gametest                                                                      | гигиена       | S / нулевой        |

Предлагаю начать с 1 → 2 → 3: они дёшево закрывают краш, god-mode и главные утечки. Большой рефактор (п. 11) лучше делать после них, иначе он перенесёт эти баги в новую архитектуру.
