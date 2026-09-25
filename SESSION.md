# SESSION.md

## Active work

- Goal: implement `docs/audits/2026-09-25-opus-architecture-audit.md` as the restoration backlog — fix real bugs and their architectural root causes, stage by stage, without blind rewrites or gameplay redesign.
- Delivery: a stack of PRs, one per audit stage. Each stage re-verifies its findings on current code before fixing and adds regression tests.
- The audit's «Статус исправлений» table is the live tracker; update it with every stage.

## Stage roadmap (audit §5)

| Stage | Scope | Branch | State |
| :-- | :-- | :-- | :-- |
| 1 | B1 no-drop recursion crash, B7 bound-weapon dup, GameTest lane | `hoplite/kroton-d9205130` | PR open |
| 2 | B2 stale `HeroData` write-back; single `HeroData` writer (debt 2) | `hoplite/kroton-d9205130--herodata-writer` | PR open (stacked on 1) |
| 3 | Lifecycle: B3, B4, B8, B17, B23, N1 (main-thread leave hook) | `hoplite/kroton-d9205130--lifecycle` | PR open (stacked on 2) |
| 4 | B5 cooldown/heal reset on hero swap, B6 Snap stones | `hoplite/kroton-d9205130--cooldowns-snap` | PR open (stacked on 3) |
| 5 | Damage pipeline B9, B20, B21; B11 global tick rate | `hoplite/kroton-d9205130--damage-pipeline` | PR open (stacked on 4) |
| 6 | B10 `WorldDestructionPolicy` | | todo |
| 7 | B15 client state/session | `hoplite/kroton-d9205130--client-state-reset` | PR open (stacked on 5) |
| 8 | B13 House of Vanity server authority | | todo |
| 9 | B12 passive reconciler, B16 fall immunity | | todo |
| 10 | B14 synced public hero attachment | | todo |
| 11 | Hero hooks / lifecycle events / tick dispatcher (debt 1–4) | | todo |
| 12 | Hygiene: deps, docs, missing model/lang | | todo |

## Completed this session (stage 1)

- Replaced three copy-pasted no-drop mixins with `PlayerBoundWeaponDropMixin` + `item/bound/BoundWeapons`. The old code re-entered `Player.drop` via `Inventory.placeItemBackInInventory` on a full inventory (verified in vanilla sources) → `StackOverflowError`.
- Bound weapons (Yamato, Royal Icicle, Rem's morning star) now carry a `superheroes:bound_weapon {owner, issue}` data component; the current issue lives in the non-persistent `BOUND_WEAPON_ISSUES` attachment. Stale copies (stashed in a chest then reissued, held by another player, left over from before a relog, `/give`) disappear on their first inventory tick. The three weapons extend `BoundWeaponItem`.
- Issuing into a full inventory now fails cleanly: Raiden's Sword Draw does not start and shows `ability.superheroes.bound_weapon.no_room`; Reinhard's ceremony completes but warns; Rem retries each tick as before.
- Added the GameTest lane: `src/gametest` source set, `superheroes-gametest` dev mod, Loom `gametest` run (`runGametest`), wired into `qualityGate`. Veil is excluded from the GameTest server classpath (it hard-depends on client-only Fabric modules).

## Completed this session (stage 2)

- `transform/HeroDataStore` is the only writer of `HERO_DATA`: `update(player, fn)` does read-modify-write; full syncs go out immediately, energy/mana-only changes are coalesced into one `ResourceUpdateS2CPayload` per player at the `hero_data_flush` END_SERVER_TICK phase (ordered after the default phase).
- `ResourceController` re-reads `HeroData` before each active ability, pays with the pure `ResourcePayment` (JUnit-tested), and deactivates through `AbilityRouter.deactivate`. `AbilityRouter.deactivate` clears the flag before `onDeactivate`; failed activations refund the exact charge (`ResourceController.charge/refund`).
- Removed the dead `DeactivateAbilityC2SPayload` endpoint and redundant `server().execute` hops in C2S handlers.
- `ProjectSanityTest.assertHeroDataHasSingleWriter` guards the single-writer rule.

## Completed this session (stage 3)

- New `lifecycle/` package: `PlayerLifecycle` is the single dispatch hub — hooks for join/leave/death/respawn/server-stopped wired to server-thread events (`ServerPlayerEvents.LEAVE` fires from `PlayerList.remove`, before save; `DISCONNECT` can run on the Netty thread — no longer used for game state). Every controller cleanup in the mod routes through it via `SuperheroesMod.registerPlayerLifecycle`.
- `lifecycle/EntityControlLock` — refcounted locks over the four NBT-persisted entity flags (NoAI, NoGravity, noPhysics, invulnerable). First owner records the previous flag value into a persistent `CONTROL_LOCK_SHADOW`; last release restores it. If a locked entity unloads while the live (non-persistent) lock map is gone, `reconcile` on `ENTITY_LOAD` restores flags from the shadow. DoomGrip, Think Mark, Regulus freeze/counter, and Reinhard's ceremony all hold locks instead of writing flags directly — stale NoAI/NoGravity after relog can no longer happen (B4).
- Ability-scoped attribute buffs (Kratos rage, Regulus madness, Reinhard draw, Raiden burst, Naruto/Goku/Rem stances, BattleBeast curse, Doomsday berserk, Thanos stone deltas) now apply as **transient** modifiers via `AttributeModifierSet.Builder.abilityScoped()` — they never serialize, so relog cannot leave zombie buffs (B3). Hero passives stay permanent on purpose (transient max-health clamps health on load). Controllers that re-derive session state on join re-apply: `BattleBeastCurseController.reapplyOnJoin`, `ThanosGauntletStateController.onPlayerReset`, `ReinhardController.onPlayerJoin`, `PandoraDeathController.reapplyState`.
- `HeroTransformService.clearHeroRuntimeState(player)` is the symmetric cleanup used by both `transform` and `doUntransform` — previously clearAdaptations/clearOnUntransform/Unibeam ran only on untransform (B23). `onPlayerLeave` clears runtime + active flags without running gameplay deactivate side-effects.
- Dead players are skipped in the per-player END_SERVER_TICK loop and `ThanosSnapWindupController` removes pending snaps for dead/absent casters; `ReinhardSwordDeathMarkController.cancelVictim` flushes marks on victim death/leave (B17).
- Transform cooldown moved from a `WeakHashMap` to the `TRANSFORM_TICK` attachment; ~20 controllers got `resetAll()`/`onPlayerGone`/`cancel` hooks wired into `SERVER_STOPPED` — static maps no longer leak into a reopened world on the same JVM (B8).
- Pandora revival state is now the persistent `PANDORA_REVIVED` attachment + an invulnerable control lock (was an NBT flag + in-memory set).

## Completed this session (stage 4)

- `ABILITY_COOLDOWNS` persistent attachment (`Map<ResourceLocation, Long>` of game-time deadlines) replaces the static `AbilityCooldowns` map — relog and hero swaps no longer reset cooldowns (B5). Not `copyOnDeath`: death still resets, matching the old semantics. `syncAll` on join resends live deadlines to the client; `clearAndSync` moved to the death hook.
- `transform()` no longer heals (`setHealth(min(health, maxHealth))`) and no longer refills energy — energy carries over like mana when swapping from a live hero (`d.hasHero() ? min(energy, max) : max`), first-time transforms still start full.
- Snap now burns the stones it actually checked: `InfinityGauntletData.clearStones` empties `InsertedStones` on the gauntlet and loose `InfinityStoneItem`s are removed — the snap is no longer infinite (B6).
- 3 new GameTests (`HeroSwapGameTests`): swap keeps cooldowns, no free heal/energy refill, snap consumes gauntlet + loose stones.

## Completed this session (stage 5)

- Damage accounting moved off `ALLOW_DAMAGE` onto `AFTER_DAMAGE` (`damageTaken`, post-armor/shield): RegulusMadness LAST_DAMAGER, DoomsdayAdaptation/EffectAdaptation accumulation, DoomsdayKryptonite, KratosRage, RemDemonism, GokuKiStack, KratosHandStrikeFx. `ALLOW_DAMAGE` remains only where `return false` is load-bearing (RegulusGreed queue, Reinhard adapted-immunity/riposte, SwordDeathMark suspension, Pandora gate, RegulusMadness reading block, DoomsdayAdaptation immunity). Saves moved to `ALLOW_DEATH`: Kawarimi now only fires on truly lethal hits (no more wasted substitution on non-lethal procs) (B9).
- `SungJinwooController` divert skips `#bypasses_invulnerability` sources — /kill and void damage can no longer be dumped onto a random shadow (audit's explicit check).
- 33 of 35 mod damage types added to `#minecraft:bypasses_cooldown` via generated tag (B20): ability damage no longer collides with the 10-tick i-frame window, so multi-hit abilities and rapid re-casts actually land. `SHADOW_ATTACK` and `HOMELANDER_MELEE` stay out — they are mob melee and keep vanilla i-frame rules.
- `ReinhardTimeSlowController` rewritten without `TickRateManager` (B11): the old code slowed the whole server's tick rate (breaking combat physics, cooldown pacing and redstone for everyone). Now `triggerAbilitySlow` freezes entities within 80 blocks — mobs via `EntityControlLock` (NO_AI + NO_GRAVITY + zeroed velocity), players via transient `MOVEMENT_SPEED`/`JUMP_STRENGTH` zero-modifiers plus attack/use-item/place block callback cancels — 170 ticks, released on expiry, owner death/leave or server stop. The owner walks at full speed inside the freeze, which preserves the power fantasy without corrupting the server clock.
- All `System.currentTimeMillis` timers in gameplay code replaced with level `gameTime` deadlines (B21): RegulusMadness reading/mana-lock (record fields renamed to `*_until_tick`, codec keys `mana_lock_until_tick`/`reading_until_tick`), DoomsdayAdaptation/EffectAdaptation first-hit/idle windows, `HeroLandingTracker`, `ReinhardSpeedJudgment` pending strikes. Single-player pause and TPS lag can no longer silently shorten or stretch these durations. Wire compatibility kept: `MadnessSyncS2CPayload` still carries remaining-milliseconds (converted from tick deadlines) so the client's wall-clock HUD math is unchanged.
- 3 new GameTests (`DamagePipelineGameTests`): ability damage bypasses the i-frame cooldown (control: mob attack does not), time slow freezes a nearby mob without touching the server tick rate and releases on owner leave, kawarimi saves from a lethal hit and goes on cooldown.

## Completed this session (stage 6)

- `world/WorldDestructionPolicy` (B10): единая точка прохода для всех способностей, меняющих мир — `tryBreak` (ванильная семантика `destroyBlock` + опциональные дропы), `tryCarve` (тихий вырез в AIR для путей с намеренным подавлением дропа), `tryPlace` (постановки без слома, например огонь). Гейты: `destroySpeed < 0` + тег `superheroes:ability_immune` (новый `src/main/generated/data/superheroes/tags/block/ability_immune.json`, написан руками + `ModBlockTagProvider` на будущее для runDatagen); для игроков — `Level.mayInteract` (защита спавна, граница мира) и `PlayerBlockBreakEvents` BEFORE/CANCELED/AFTER — claim-моды теперь видят все сломы; для мобов и без причины — `RULE_MOBGRIEFING`.
- Мигрированы все ability-точки: `RegulusMadnessController.carveCrater` (вынесен из `CounterState` на контроллер и получает игрока — раньше съедал бедрок конусом), `RemDemonismController`, `UnibeamController` (луч + кратер + огненное кольцо), `MadnessAftermathController`, `MadnessFlightController`, `RaidenMusouIsshinController.carveSlash` (игрок раньше не передавался), `HeavensStrikeController.carveCrater` (то же), `GuardiansBreakerAbility`, `RushTerrainBreaker`, `BallisticBodyTracker`, `ShockwaveUtil` (моб- и игрок-детонации), `HomelanderBlockThrowGoal` (подбор блока и сам бросок гейтятся — при отказе снаряд не спавнится), `EyeLasersAbility` (огненное кольцо).
- Дропы: `RushTerrainBreaker`, `BallisticBodyTracker` и `GuardiansBreakerAbility` теперь ломают с дропом — shulker box выскакивает с содержимым вместо исчезновения (регрессия из аудита). `RaidenMusouIsshin`/`HeavensStrike` сохранили свои списки «непробиваемого» в урезанном виде — только ценные блоки, которых нет в теге (обсидиан, crying obsidian, нетерит/железо/древние обломки/маяк/respawn anchor соответственно).
- `physics/BlockBreakPolicy` удалён — его роль выполняет новая политика. `ProjectSanityTest` запрещает прямые `destroyBlock`/`removeBlock`/`setBlock`/`setBlockAndUpdate` вне `WorldDestructionPolicy`.
- 7 новых GameTests (`DestructionPolicyGameTests`): кратер пропускает бедрок и тегированные блоки, вето claim-события отменяет слом и шлёт CANCELED, AFTER стреляет при реальном сломе, mobGriefing гейтит мобов и «без причины» для слома и постановки, shulker box дропается с содержимым, carve намеренно без дропа, контактный слом рывка дропает землю.
## Completed this session (stage 7)
- `ClientSessionState` — единый реестр сброса клиентской сессии (B15): каждый `Client*State`-холдер и сессионный синглтон (`ClientAbilityCooldowns`, `RemoteHeroSkins`, `JarvisDetectionHud`, `MirrorWarpFlashHud`, `RadialMenuHud`) регистрирует свой reset в static-блоке; `ClientPlayConnectionEvents.DISCONNECT` зовёт `resetAll()` вместо ручного списка — до этого сбрасывались только 10 из ~30 холдеров, и уход из мира во время time-slow навсегда глушил звуки мира в следующих мирах. `ProjectSanityTest.assertClientStatesRegisterReset` валится, если `Client*State`-класс не содержит `ClientSessionState.register(`.
- `ClientAbilityCooldowns` переведён с `LocalPlayer.tickCount` (обнуляется при респавне → HUD рисовал кулдауны в часы) на `ClientLevel#getGameTime()` — монотонные тики уровня; дедлайны в `long`, wire-формат пакета не тронут. Для JUnit — инжектируемый `clock` (`setClockForTesting`/`clearClockForTesting`, package-private).
- Миксины ролика Pandora (`PandoraCinematicKeyboardMixin`, `PandoraCinematicMousePressMixin`) больше не глотают `GLFW_RELEASE`: `KeyMapping.set(key,false)` приходит только с release, поэтому отпущенная во время ролика клавиша оставалась нажатой — игрок сам шёл/бил после катсцены. `InputFreeze`/`MouseTurn` миксины не тронуты — они не ломают трекинг кнопок.
- `IrisShaderBridge` стал resilient: `restore()` сбрасывает `activeSnapshot` и удаляет `MirrorRestoreFile` только после успешного restore (раньше снапшот терялся при первой неудаче); `restoreAfterCrashIfNeeded` больше не пытается ресторить в `onInitializeClient` (Iris ещё не готов → всегда фейл → файл удалялся без восстановления) — вместо этого `tickCrashRestore()` ретраит из клиент-тика до 200 тиков, файл удаляется только при успехе.
- `ChatComponentMixin`: хит-тест чата переведён на тот же сдвиг, что и рендер — `@ModifyVariable` на `screenToChatX`/`screenToChatY` вычитает `HudLayoutManager.offset(CHAT) + autoLift`, так что клики по ссылкам, ховер-теги и подсветка строки работают по реальному положению чата.
- JUnit: `ClientSessionStateResetTest` дёргает публичные сеттеры ~25 холдеров и проверяет, что `resetAll()` возвращает дефолты; `ClientAbilityCooldownsTest` гоняет дедлайны на инжектированных часах (респавн как скачок времени). `test` sourceSet получил `client.output + client.compileClasspath` (через `afterEvaluate`, т.к. `client` создаётся `splitEnvironmentSourceSets()`).## Important decisions

- Bound weapons are identified by type (`BoundWeaponItem`) and validated by token; untokened copies are treated as stale on purpose (none are obtainable legitimately; old saves could hold leaked copies).
- Bound weapons never become item entities: returned to the owner when valid and there is room, otherwise deleted (the ability can reissue).
- `HeroData` sync: writes are immediate; full syncs stay immediate to preserve packet ordering with other payloads; resource-only syncs are coalesced per tick.
- `AbilityRouter.deactivate` order is flag-off → `onDeactivate` (verified no `onDeactivate` callee reads the active set).
- Stage 3 lock design: `EntityControlLock` owns the flag transitions; controllers never touch the flags. Locks are re-established, not restored — join hooks re-apply what should still hold (Pandora revival), everything else is released. `RemDemonismController.onRespawn`/`ReinhardController.onDeath` exist but are dead code — intentionally unwired (death → forceUntransform → clear covers them).
- Lifecycle cleanup (stage 3) must not rely on `ServerPlayConnectionEvents.DISCONNECT` for game state: Fabric can fire it from the Netty thread (`Connection.channelInactive`). Use a main-thread hook before the player is saved (`PlayerList.remove`).
- Ability-scoped attribute buffs should become transient; hero base passives stay permanent (transient max-health modifiers would clamp health on load because `LivingEntity.readAdditionalSaveData` calls `setHealth` after loading attributes).

## Verification

- Stage 5: `qualityGate` green, 22/22 GameTests (3 new damage-pipeline tests). Test-only notes: mock players join with private `spawnInvulnerableTime` that blocks `hurt()` — cleared via reflection in the kawarimi test; the test structure spawns ~6M blocks from the mock-player spawn point, so proximity-sensitive asserts must teleport the player first.
- Stage 4: `qualityGate` green, 19/19 GameTests. Negative check — without the `hasHero()` guard, first-time transforms start with 0 energy and `windPrisonEndsWhenItsZoneExpires` fails.
- Stage 3: `qualityGate` green, 16/16 GameTests (7 new lifecycle regressions: owner-leave lock release, refcount, shadow reconcile, transient-vs-permanent NBT, attachment-backed transform cooldown, forceUntransform clears locks+cooldowns, dead-caster snap).
- Stage 2: `qualityGate` green, 9/9 GameTests; negative check — old tick write-back makes `windPrisonEndsWhenItsZoneExpires` fail.
- `./gradlew qualityGate --no-daemon` green on stage 1: JUnit, 8 `ProjectSanityTest` checks, assertion audit, jar isolation audit, 7/7 GameTests.
- Negative check: re-inserting the old mixin logic makes `droppingWithFullInventoryDoesNotRecurse` fail with `StackOverflowError`.
- No `runClient` in-game session: the sandbox has no display. Server behavior is covered by GameTests with real joined players.

## Known issues / follow-ups

- `runServer` in dev fails mod resolution while Veil is on the runtime classpath (audit N2); decide whether to make Veil `modCompileOnly` + client-only runtime.
- `auto-approve-pr.yml` still auto-approves green PRs (audit §3); left as a repository-owner decision, not changed by this work.

## Next session

1. Read this file, `AGENTS.md`, and the audit's «Статус исправлений» table.
2. Continue with the first stage whose PR is not yet open; re-verify each finding on current code first.
3. Keep `qualityGate` green; add GameTests for server behavior; update the audit tracker and this file per stage.
