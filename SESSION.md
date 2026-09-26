# SESSION.md

## Active work

- Goal: the bugfix backlog (`docs/audits/2026-09-25-opus-architecture-audit.md`) is integrated on `main`; active work is the architecture-migration program in `docs/design/architecture-migration/` — `00-overview.md` is the map (stage graph §2.1, orchestration §11), plans `01`–`06` are executed stage by stage.
- Delivery: a stack of PRs, one per migration stage. Orchestrator (main session) assigns file ownership, integrates worker branches, runs every `Run:` command and `qualityGate`, owns `archunit_store/**`, `package-cycles-baseline.txt`, golden files, status tables and this file.
- The «Статус стадий» table inside each plan is the live tracker; update it with every stage.

## Stage roadmap (audit §5)

| Stage | Scope | Branch | State |
| :-- | :-- | :-- | :-- |
| 1 | B1 no-drop recursion crash, B7 bound-weapon dup, GameTest lane | `hoplite/kroton-d9205130` | PR open |
| 2 | B2 stale `HeroData` write-back; single `HeroData` writer (debt 2) | `hoplite/kroton-d9205130--herodata-writer` | PR open (stacked on 1) |
| 3 | Lifecycle: B3, B4, B8, B17, B23, N1 (main-thread leave hook) | `hoplite/kroton-d9205130--lifecycle` | PR open (stacked on 2) |
| 4 | B5 cooldown/heal reset on hero swap, B6 Snap stones | `hoplite/kroton-d9205130--cooldowns-snap` | PR open (stacked on 3) |
| 5 | Damage pipeline B9, B20, B21; B11 global tick rate | `hoplite/kroton-d9205130--damage-pipeline` | PR open (stacked on 4) |
| 6 | B10 `WorldDestructionPolicy` | `hoplite/kroton-d9205130--destruction-policy` | PR open (stacked on 5) |
| 7 | B15 client state/session | `hoplite/kroton-d9205130--client-state-reset` | PR open (stacked on 6) |
| 8 | B13 House of Vanity server authority | `hoplite/kroton-d9205130--vanity-authority` | PR open (stacked on 7) |
| 9 | B12 passive reconciler, B16 fall immunity | `hoplite/kroton-d9205130--passives-fall` | PR open (stacked on 8) |
| 10 | B14 synced public hero attachment | `hoplite/kroton-d9205130--public-hero-sync` | PR open (stacked on 9) |
| 11 | Hero hooks / lifecycle events / tick dispatcher (debt 1–4) | `hoplite/kroton-d9205130--hero-modularity` | PR open (stacked on 10) |
| 11b | Migrate ~50 self-registered `ServerTickEvents` onto the dispatcher | child session | in flight |
| 12 | Hygiene: deps, docs, missing model/lang | `hoplite/kroton-d9205130--hygiene` | PR open (stacked on 11) |
| 13 | B18 Sung shadows survive restart; B22 teleport collision checks | `hoplite/kroton-d9205130--shadows-teleports` | PR open (stacked on 12) |
| 14 | B19 shared target predicate honoring PvP/teams | `hoplite/kroton-d9205130--target-predicate` | PR open (stacked on 13) |
| 15 | Potential findings (4) + §3 network improvements | `hoplite/kroton-d9205130--potential-net` | PR open (stacked on 14) |
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
- JUnit: `ClientSessionStateResetTest` дёргает публичные сеттеры ~25 холдеров и проверяет, что `resetAll()` возвращает дефолты; `ClientAbilityCooldownsTest` гоняет дедлайны на инжектированных часах (респавн как скачок времени). `test` sourceSet получил `client.output + client.compileClasspath` (через `afterEvaluate`, т.к. `client` создаётся `splitEnvironmentSourceSets()`).
## Completed this session (stage 8)
- House of Vanity trap is now server-authoritative (B13): `MirrorDimensionController.enforceZone` runs unconditionally — the yank and `VanityAuthority` debuffs no longer wait for a client ACK (`VictimState.applied` removed). A modified or silent client stays inside the zone exactly like a confirmed one.
- `absorbNearby` lost its `canSend` gate: victims without the mod (or with networking filtered) are still trapped — the S2C payloads are now purely a cosmetic hint sent via `sendToVictim` (shader warp + cipher flag), so a missing reply no longer weakens containment.
- `handleStatus` is reduced to caster feedback (it relays the localized status message to Pandora only); `NO_IRIS`/`NO_PACK`/`IRIS_API_FAIL` no longer release the victim — previously a failed or absent warp silently dropped the victim while leaving her client ciphered forever (the deadman only watched `active`).
- Client cipher scope fixed in `ClientMirrorDimensionState.tick`: the deadman now tracks `trapped` (the cipher) rather than only `active` (the shader), so a victim whose warp never applied still releases the font cipher ~5s after keepalives stop instead of holding it until disconnect.
- Lifecycle wiring: `MirrorDimensionController.onPlayerGone` and `SpatialBindController.onPlayerGone` release victims/ropes on leave via `PlayerLifecycle.onLeave`; both controllers' `resetAll()` run from `onServerStopped` so no trap state leaks across world restarts.
- 4 new GameTests (`HouseOfVanityGameTests`): victim trapped without any client confirmation and yanked back when walking out, NO_IRIS status keeps full containment, debuffs applied without confirmation, caster leave closes the House and frees victims (effects cleared via `PlayerLifecycle` hook). Mock players double as the "no client ACK" regression case since `canSend` is false for them.
## Completed this session (stage 9)

- Hero passives no longer die to effect wipes (B12). New `lifecycle/PassiveReconciler`: `capture()` arms a ThreadLocal around `hero.applyPassives` while `LivingEntityPassiveEffectsMixin` records every *infinite* `MobEffectInstance` added in that scope — that's the hero's declared passive set, stored per player (with the heroId). `onEffectRemoved` (fired by milk `removeAllEffects`, death-save wipes, `LionHeart`, `ThanosTimeRewind`, `RegulusGreedController.removeEffect(JUMP)`, targeted removes and natural expiry alike) only marks the player dirty; the actual re-assert runs deferred on `END_SERVER_TICK`, skipping effects the player still has — so vanilla `MobEffectInstance.update` nesting (a temporary stronger instance hiding a nested passive) is preserved. A `heroId` guard drops stale records on swap/untransform, `PassiveReconciler.clear` on `doUntransform`.
- Capture wired at every real `applyPassives` site: `HeroTransformService.transform` and `reapplyLifecyclePassives` (join/respawn), `DoomsdayTierController.applyProgress` (tier-dependent re-derive). Doomsday's `reapplyLifecyclePassives` keeps its no-removePassives quirk.
- `RegulusMadnessController.clearMadness` is scoped to madness-owned instances (B12): it used to `removeEffect` SPEED/STRENGTH/JUMP/RESISTANCE unconditionally on every join/leave/death/respawn for ANY player — deleting potion, beacon, and hero passive effects server-wide. Now `removeMadnessEffect` removes an instance only if it matches the madness signature (duration ≤ 60, matching amplifier, ambient, not visible, showIcon); `applyMadnessEffects` builds via `madnessEffect()` so the signature can't drift. A madness instance merged over an infinite passive is still removed whole — the reconciler re-asserts the passive next tick.
- Fall immunity is per-participant again (B16): `LivingEntityFallDamageMixin` asked global `isAnyCounterActive()`, so one Regulus counter anywhere stripped `SuperJumpController`/hero `cancelsFallDamage` immunity from every hero. Now `RegulusMadnessController.isCounterInvolved(entity)` (matches the counter's `playerId`/`attackerId`); `isAnyCounterActive` deleted.
- 4 new GameTests (`PassivesFallGameTests`): milk-style `removeAllEffects` restores all five Regulus infinites; hero swap replaces the declared set (regulus→kratos restores RESISTANCE, not REGENERATION); `clearMadness` keeps a visible potion, a beacon-style ambient+visible effect and an untouched infinite passive while dropping madness-signed SPEED/JUMP even with a passive nested under them; counter strips fall immunity for owner/victim but not for an uninvolved hero.

## Completed this session (stage 10)

- New synced attachment `PUBLIC_HERO` (B14): `persistent(ResourceLocation.CODEC)` + `copyOnDeath()` + `syncWith(ResourceLocation.STREAM_CODEC, AttachmentSyncPredicate.all())` — a narrow public projection carrying only the hero id; energy/mana and everything else in `HERO_DATA` stays owner-only. Written once inside `HeroDataStore.update` (the single writer, so it cannot drift), plus `syncPublicHero(ServerPlayer)` back-fill on JOIN for pre-existing saves.
- `PlayerDimensionsMixin` now reads `PUBLIC_HERO` instead of `HERO_DATA` — remote players on clients get the real hero hitbox (Battle Beast etc.) instead of the vanilla one; the local player still refreshes dims via the `HeroDataSyncS2CPayload` receiver.
- All remote-hero plumbing deleted: `RemoteHeroSkinS2CPayload` + registration + client receiver, `RemoteHeroSkins` map, `broadcastRemoteHeroSkin`, `sendRemoteHeroSkinTo`, and the `EntityTrackingEvents.START_TRACKING` block. Attachment sync covers tracking start, dimension change and respawn for free — `sendRemoteHeroSkinTo`'s early `hero == null` return (the stale-skin-after-relog bug) is gone with it.
- Client consumers migrated to `player.getAttached(PUBLIC_HERO)`: `AbstractClientPlayerSkinMixin` (hero skins for other players), `IronManEspRenderer` (hero detection + id), `ReinhardScabbardLayer`, `ClientNanoSuitUpState`.
- New `ClientHeroDimsWatcher`: attachment sync has no client-side change callback, so an END_CLIENT_TICK watcher diffs each tracked player's synced hero id and calls `refreshDimensions()` on change (clears its map when `client.level == null`).
- 2 new GameTests (`PublicHeroSyncGameTests`): transform writes / untransform clears `PUBLIC_HERO`; join back-fill derives the projection from a legacy directly-written `HERO_DATA`.

## Completed this session (stage 11)

- `lifecycle/HeroTickDispatcher` (debt 3): the only `END_SERVER_TICK` consumer for gameplay ticks — ordered phases GLOBAL → LEVELS → PLAYERS → ABILITY_ACTIVE, the dead-player skip happens once centrally (B17), and `HERO_DATA` is fetched once per player. `SuperheroesMod`'s 100-line inline lambda became `registerTickHandlers()`, a flat table keeping the exact prior order. The ~50 controllers that self-register `ServerTickEvents` stay as they are — migrating them onto the dispatcher is the follow-up hygiene stage.
- `lifecycle/HeroLifecycle` (debt 1): `onClear`/`onTransformed` listener events fired from `HeroTransformService.clearHeroRuntimeState`/`transform` — the hard-coded cleanup call list (Unibeam, Regulus totem/madness, Reinhard adaptations, Raiden, Rem, Pandora, DoomGrip, Omniman think mark, control locks) is now a registration table in `registerPlayerLifecycle()`.
- `Hero` default hooks (debt 4): `getImpactStyle`/`getImpactPower`, `getThreatClass`, `canUseAbility`/`onAbilityDenied`, `isAbilitySuppressedBy`, `getEnergyReserveFor`, `isUraniumWeak`. `CombatImpactEngine` deleted its 20-hero import table (style/power now come from the hero); `JarvisThreatClass` moved `jarvis/` → `hero/` and `forHero` reads `getThreatClass()` — `HERO_THREATS` gone, jarvis↔hero package cycle broken; `AbilityRouter`'s three `instanceof` branches + the IRON_FISTS check + the UNIBEAM reserve are hook calls; `FlightController` reads `isUraniumWeak()` instead of `HomelanderHero.ID`.
- `ClientAbilityFilter` reuses the server tables `DoomsdayHero.isUnlockedAtTier` and `RemHero.isVisibleIn` — the client-side duplicate lists are deleted, so HUD and router can no longer drift apart.
- `ProjectSanityTest.assertNoHeroTypeDispatch` forbids `instanceof *Hero` outside the hero package; 3 new GameTests (`HeroTickDispatcherGameTests`): dead skip, isActive gating, phase order.

## Completed this session (stage 11b)


- Migrated all 47 self-registering `ServerTickEvents.END_SERVER_TICK` controllers onto `HeroTickDispatcher` — 25 `onGlobalTick` + 33 `onPlayerTick` registrations appended to `registerTickHandlers()` in `init()` order. Per-player loops became `PlayerTask`s (central dead-skip, prefetched `HeroData`); offline-player prunes and entity-map sweeps split into `GLOBAL` methods (`pruneGonePlayers`, `tickFreezes`, `tickCounters`, `serverTick`); verbatim global bodies kept where the loop wasn't per-player work (Jarvis scans, Kratos drain, DoomsdayKryptonite interval pass, Raiden/Heavens/ThanosSnap windups).
- Controllers whose `init()` only held the tick lost it (call removed from `onInitialize`); `init()`s with other registrations (`AttackEntityCallback`, `ALLOW_DAMAGE`, `DISCONNECT`, `START_SERVER_TICK`) kept — only the END registration moved.
- Deliberately left self-registered: `HeroDataStore` (two `hero_data_flush` phase-boundary registrations by design), `MirrorDimensionController` (owned by stage 8), `MadnessFlightController`/`KratosRageController`/`ThanosGauntletStateController` START_SERVER_TICK registrations (the dispatcher is END-only), `src/client`, `transform/`, `lifecycle/`, `WorldDestructionPolicy` files (stages 6/7/9).
- Ordering notes: phase split means GLOBAL tasks now run before all per-player work; verified each split pair is data-independent (offline prunes commute; `RegulusMadness` COUNTERS vs `tickPlayer` don't touch shared state; `RegulusGreed` freezes/casters independent). One acknowledged delta: `KratosRageController` rage drain now runs before player tasks — same-tick deactivation timing shifts within the tick, not across ticks.


## Completed this session (stage 12)

- `fabric.mod.json` deps tightened to what the build actually targets: `minecraft ~1.21.1`, `fabric-api >=0.116.12`.
- `horde_crystal` got a model (`item/generated` + vanilla `echo_shard` texture — no new art needed for a command-issued item).
- 5 missing entity display names added to en+ru: `horde_acid_bomb`, `horde_fire_bomb`, `kage_bunshin`, `shield_projectile`, `smart_missile`.
- 21 cyrillic `Component.literal` sites converted to `Component.translatable` (+22 new lang keys in both files): commands (horde/admin-build/state), horde crystal + boss bar + Infected Homelander lines, Omniman hint, abilities HUD seconds.
- `ModAttachments` rewritten from deprecated `AttachmentRegistry.builder()...buildAndRegister` to `AttachmentRegistry.create(id, b -> ...)` (13 attachments, same semantics).
- `ProjectSanityTest` gained `assertNoCyrillicLiterals` (no `Component.literal` with cyrillic in main/client) and `assertEntityLangNames` (every entity id registered in `ModEntities`/`HordeEntities` has an `entity.superheroes.*` key in en_us.json).

## Completed this session (stage 13)

- B18: армия Sung вынесена из статических `ARMY`/`SUMMONED`/`PHASE2` в persistent-attachment `SUNG_SHADOW_ARMY` (record `attachment/SungShadowArmy`: `List<UUID> shadowIds`, `summoned`, `phase2`). После рестарта UUID сохранённых теней перелинковываются — повторный призыв не случается, т.к. `summoned` переживает рестарт. В тике снимаются только «разрешившиеся и мёртвые» UUID; невыгруженные остаются в списке и подхватываются при загрузке чанка. `ShadowSoldierEntity.aiStep` сам решает свою судьбу: владелец офлайн → сон (цель и месть очищены, не атакует); владелец онлайн, но не Sung или тени нет в армии → `discard()` — так закрываются и снятие героя, и сироты, загрузившиеся после disband.
- B22: `util/SafeTeleport.clamp(level, entity, dest)` шагает по отрезку (шаг ≈ половина меньшего размера хитбокса, мин. 0.2) и возвращает последнюю точку, где хитбокс свободен от блоков; невыгруженный чанк = препятствие; проверка только по блокам, чтобы «за спину» не упиралось в самого моба. Переведены Goku IT, Loki Tesseract, Scorpion, Kratos God Slayer, Reinhard Speed Judgment, Kawarimi, Shadow Exchange (оба направления), Doom Grip lunge, Thanos Space Portal. Не тронуты (by design): hold/anchor/lockPos/return-телепорты (DoomGrip hold, SpatialBind, RegulusGreed, Unibeam, IronManReactor, Pandora, MirrorDimension, lockPos у HeavensStrike/RaidenMusou/Regulus counter-катсцена) и `DoomsdayTierController.tryRelocate` (точка уже проверяется на свободу).
- Новый `ShadowsTeleportsGameTests` (4 теста): disband при forceUntransform (нет ни записей, ни сущностей), тень-сирота удаляет себя, телепорт клэмпится у каменной стены, свободный телепорт доходит до точки.

## Completed this session (stage 14)

- `combat/TargetFilters` — единый предикат враждебного выбора целей (B19): `harmableBy(target, attacker)` (alive, не сам атакующий, не spectator, плюс `victim.canHarmPlayer(attacker)` когда обе стороны игроки) и `hostileTo(attacker)` — то же плюс пропуск creative-игроков (наиболее частое условие копий). Ванильная семантика проверена по байткоду: `ServerPlayer.canHarmPlayer` = `isPvpAllowed() && super.canHarmPlayer`, где super покрывает команды и friendly-fire, а `ServerPlayer.hurt` применяет эту же проверку когда источник урона несёт Player-атакующего.
- Заменено ~25 приватных `isValidTarget`/`validTarget`-хелперов и ~60 инлайн-лямбд в ability/effect/item/entity/physics на `TargetFilters`; лишние условия сохранены через `.and(...)` (дистанция, `!= primary`, исключение ShadowSoldier). Френдли-селекторы (shadows, призывы, владельцы, кинематик-фризы) оставлены как были.
- `magic()` без атакующего теперь атрибутирован (`indirectMagic(attacker, attacker)`), так что `pvp=false`/команды работают и на источнике урона: ScaramoucheWindPrisonAbility, ThanosSoulPulseAbility, MonarchsDomainController, UraniumDaggerItem. DoT-эффекты (Bleeding/Snapped) и fallback-без-владельца (ShieldProjectile generic, SungJinwoo divert) намеренно оставлены unattributed.
- Побочные правки поведения по сути фикса: creative-игроки и зрители теперь единообразно пропускаются всеми враждебными сканами; DoomGrip вместо фейкового `isAlly` (uuid==uuid) использует настоящие team-правила; кинематик-фризы SwordDrawCeremony (`e -> true`) и TimeSlow-цикл игроков оставлены/переведены осознанно (TimeSlow больше не замораживает тиммейтов без friendly fire).

## Completed this session (stage 15)

- All 4 "потенциальные" findings verified real against current code and fixed:- Architecture audit 2 is preserved as two complementary files: `docs/audits/2026-09-25-hero-modularity-audit.md` (hero locality, `HeroModule`/`HeroProfile`, Scorpion pilot, Reinhard stress-test) and `docs/audits/2026-09-25-hoplite-structural-audit.md` (package cycles, registries, router contract, services, payloads).
- The migration that synthesizes both audits is split into six executable plans in `docs/design/architecture-migration/` (`00-overview.md` is the map; `01`–`06` are the plans). They were revised after an external review and rebased on the bugfix stages 4–12 (#40, #42–#49): plans extend the BF11 seams (`HeroTickDispatcher`, `HeroLifecycle`, `Hero` hooks), BF7 `ClientSessionState` and BF10 `PUBLIC_HERO` instead of adding parallel ones. The monolithic `docs/design/2026-09-25-architecture-migration-plan.md` is an unchanged archive of the pre-review version and is not executed or updated.

## Architecture migration — stage CL2 (plan 04)

- New `client/core/hud/` registry: `HudLayer` (functional iface), `HudBounds`, `MovableHud` (`layoutId()` + `bounds(w,h)`), `HudLayers` — the mod's single `HudRenderCallback` (spectator gate lives inside it), `register`/`registerMovable`, `movables()`. All 24 HUD renders in `SuperheroesClient` are now `HudLayers.register(order, id, X::render)` with `order` = former position × 100.
- The 7 draggable elements implement `MovableHud` — each `bounds()` holds the exact math deleted from `HudEditScreen`'s switch. `chat`/`effects` (mixin-shifted vanilla, no HUD class) got movable-only adapters (`ChatHudMovable`/`EffectsHudMovable`) registered with no-op layers so the editor still shows all 7 cards.
- `HudEditScreen` iterates `HudLayers.movables()` re-sorted by its retained `ELEMENTS` order (`orderedMovables()`) — card paint order and reversed topmost-first hit-test unchanged; `HudLayoutManager.ALL` deleted.
- Runtime checklist PASSED: hero HUDs (homelander/iron_man/reinhard/regulus) identical pre/post on the same run dir; editor shows the same 7 cards in the same frames (≈pixel-identical at GUI scale 1); dragged `ability_bar` offset survives a full client restart. Evidence in `~/cl2-runtime/`.
- Reviewer nits logged (not fixed — latent design traps, not regressions): `orderedMovables()` silently drops movables lacking an `ELEMENTS` row (future CL3b HUDs must add ELEMENTS rows), `HudLayers.init()` has no idempotence guard, `orderedMovables()` allocates per frame.

## Architecture migration — stage A1 (plan 01)

- ArchUnit guardrails landed: `src/test/java/.../architecture/` — `CodexClasses` imports main/client class dirs from Gradle sysprops; `ArchitectureRulesTest` holds 5 frozen rules (shared→concrete-hero deps, main→client, ServerTickEvents outside the dispatcher, lifecycle hooks outside registrars, depends-on-composition-roots) and 6 strict rules for the future layered packages (core/mechanic/hero-module rules + module-list construction/reference rules); `ClientArchitectureRulesTest` mirrors it for `src/client`; `PackageCycleRatchetTest` ratchets bidirectional package pairs against `src/test/resources/architecture/package-cycles-baseline.txt` (`-Dcodex.writeCycleBaseline=true` regenerates).
- Freeze store lives in `src/test/resources/archunit_store/` (`archunit.properties`: `allowStoreCreation=false`, `allowStoreUpdate=true` — new violations fail, shrinking violations update the store). Committed baseline: 32 cycle pairs, 134 shared→concrete-hero deps, 33 client→hero, 74 lifecycle-hook call sites, 4 composition-root deps, 5 remaining tick-field accesses (most were already migrated in stage 11b), 0 main→client.
- `qualityGate` now depends on `verifyArchitectureBaseline` — a PR fails if the store/baseline drift uncommitted.
- Negative probes (temporary `core/module`/`hero/scorpion` stubs + a synthetic cycle) failed in exactly the expected rules, confirming the strict rules really fire.
- `TestHeroes.transform(ServerPlayer, ResourceLocation)` replaced 17 copy-pasted transform calls in 10 GameTest files.
- `build.gradle` gained a backup Central mirror (`CentralAliyunMirror`) — the local repo1 redirect mirror does not carry `com.tngtech.archunit` and shared CI egress IPs get 429s; the extra repo keeps the new dependency resolvable everywhere.

## Architecture migration — stage C2 (plan 02)

- New `core/ability/` trio verbatim per plan: `AbilityDenial` (record, `SILENT` + `notify`), `AbilityBlocker` (functional interface), `AbilityRules` (`blocker`/`freeCost` registries, `firstBlock`, `isFree` — registration order = evaluation order).
- `AbilityRouter.activate` — the first three `ModEffects` checks replaced by `AbilityRules.firstBlock(...)` + `blocked.notify(player)`; `canPayActivationCost` uses `AbilityRules.isFree`. `ResourceController` — both `isMadness` sites → `isFree` (tick local still named `madness`, semantically "free cost" now). Acceptance grep `ModEffects` in both files → empty.
- `SuperheroesMod` registers the 4 rules right after `ModEffects.init()` (aftermath → Snap → Vanity → `freeCost(ModEffects::isMadness)`) with the plan's D2b note; two imports added (`Component`, `ChatFormatting`) since the moved code is spelled unqualified there.
- Characterize-first: 8 `AbilityGateGameTests` PASSED on old code and again post-migration (65/65 gametests in gate). Cycle baseline shrank 31→30 (`effect <-> resource` pair dropped — `ResourceController` no longer imports `ModEffects`; orchestrator regenerated).

## Architecture migration — stage C1 (plan 02)

- 81 duplicate `AbilityCooldowns.isOnCooldown(player, getId())` calls removed from `Ability.canActivate` implementations (plan said 85; actual inventory was 82 sites in `ability/` = 81 own-id + 1 router check). Deletion shapes: `return !cd` → `return true` (64), conjunct removal preserving the rest (13), whole `if (cd)` statement removal (4 — the two Raiden ones dropped a cooldown message that was already unreachable: the router rejects before `canActivate` runs). Cooldown SET sites (`setCooldownTicks`) untouched per «Нельзя менять».
- New frozen ArchUnit rule `abilitiesDoNotCheckTheirOwnCooldown` in `ArchitectureRulesTest` — store generated post-cleanup, empty (no `Ability` implementor calls it).
- Characterize-first per §11.1.6: `CooldownGateGameTests.routerRejectsAbilityOnCooldown` (Scaramouche Electro Swirl — plain cast, no toggle/preconditions; Wind Prison deliberately avoided since its toggle-off branch precedes the cooldown check) PASSED before and after the deletions.
- SESSION.md merge-conflict markers from the N3 union merge cleaned up in this branch.

## Architecture migration — stage N3 (plan 01)

- Deleted the fake `api/` package entirely (`HeroApi`, `AbilityApi`, `CreativeTabIds`, `package-info`; −240 lines). The only consumer was `RepulsorChargeController`: `HeroApi.getCurrentHeroId(player).orElse(null)` → `HeroDataStore.get(player).heroId()` (identical semantics — `heroId()` is `currentHero.orElse(null)`).
- `CreativeTabIds`'s id `superheroes:superheroes` already lived in `ModItemGroups.SUPERHEROES_TAB_KEY` — no move needed; new GameTest `superheroesTabIdIsStable` pins it.
- External-consumer check via `gh search code` returned zero before deletion.
- `package-cycles-baseline.txt` shrank 32→31 pairs (the `ability <-> api` bidirectional pair dropped out — orchestrator regenerated).

## Architecture migration — stage N2 (plan 01)

- Java-only rename of Doctor Strange leftovers: `ModItems.DOCTOR_STRANGE_SUIT` → `PANDORA_SUIT` (registered id stays `doctor_strange_suit` — persisted), `HeroAttributes.STRANGE_HP` → `PANDORA_HP` (value `modifiers/pandora/max_health` unchanged — persisted modifier id), misleading comments fixed in `AbilityIds`/`MirrorModeCycleAbility`/`MirrorDimensionS2CPayload`/`PandoraHero`. `DoctorStrangeSuitItem` class NOT renamed — B3 deletes it.
- `textures/entity/hero/doctor_strange.png` deleted (grep-verified unreferenced); the item model/texture/lang keys stay.
- New GameTest `pandoraSuitIdIsStable` (in `HeroCompletenessGameTests`) pins the persisted registry id so a future rename can never drift it.
- `Madness*`/`RegulusMadness*` renames deliberately skipped — waves I5/I6.

## Architecture migration — stage N1 (plan 01)

- Removed dead code: `ViltrumiteThunderClapAbility` (never registered), `MeteorSlamAbility` + `ShockwavePulseAbility` (registered but listed by no hero — unreachable through the AbilityRouter hero-list gate), 2 never-registered HUDs (`LowResourceVignetteHud`, `ResourceBarHud`), `HordeGeoRenderer`+`HordeGeoModel` (unregistered geo renderer; `HordeGeoAssets` stays — still used by `BaseHordeEntity`), 7 `AbilityIds` constants, and the 4 `MeteorSlamAbility` call sites in `SuperheroesMod` (onLeave/onDeath/resetAll/playerTick).
- `archunit_store` shrank 74→73 lifecycle-hook rows (orchestrator regenerated — the deletions removed entries in file `6ab35c1a`).
- New GameTests guard the contract the deletions relied on: `decodesUnknownAbilityIds` (CODEC tolerates persisted ids the registry no longer knows) and `activatingAnUnlistedAbilityIdIsANoOp` (hero-list gate rejects unlisted-but-registered ids without charging resources).
- Orphan left deliberately (outside the stage's listed scope): `textures/gui/vignette.png` — its only consumer was `LowResourceVignetteHud`.

## Architecture migration — stage A2 (plan 01)

- `HeroCompletenessGameTests` (entrypoint #14): `everyListedAbilityIsRegistered` fails on any ability id a hero lists that `AbilityRegistry.get` can't resolve; `everyHeroAndAbilityHasLangInBothLanguages` requires `hero.<ns>.<id>`, `ability.<ns>.<ability>`, `ability.<ns>.<ability>.desc` in both lang files — the exact three key shapes the HUD reads (verified against `AbilityDescriptions.nameKey/descKey`). `lang(String)` is package-accessible for B1 reuse.
- `assertControllersAreWired` now builds its wiring source from `SuperheroesMod` + every `*Module.java` — a controller may be wired by a hero/shared module instead of the composition root; the check is removed in D2b when static `init()`s disappear.
- Negative probe confirmed: unregistering `SCORPION_SPEAR` fails `everyListedAbilityIsRegistered` with `superheroes:scorpion lists unregistered superheroes:scorpion_spear`. All 22 heroes pass — no missing lang keys.

## Important decisions
- Bound weapons are identified by type (`BoundWeaponItem`) and validated by token; untokened copies are treated as stale on purpose (none are obtainable legitimately; old saves could hold leaked copies).
- Bound weapons never become item entities: returned to the owner when valid and there is room, otherwise deleted (the ability can reissue).
- `HeroData` sync: writes are immediate; full syncs stay immediate to preserve packet ordering with other payloads; resource-only syncs are coalesced per tick.
- `AbilityRouter.deactivate` order is flag-off → `onDeactivate` (verified no `onDeactivate` callee reads the active set).
- Stage 3 lock design: `EntityControlLock` owns the flag transitions; controllers never touch the flags. Locks are re-established, not restored — join hooks re-apply what should still hold (Pandora revival), everything else is released. `RemDemonismController.onRespawn`/`ReinhardController.onDeath` exist but are dead code — intentionally unwired (death → forceUntransform → clear covers them).
- Lifecycle cleanup (stage 3) must not rely on `ServerPlayConnectionEvents.DISCONNECT` for game state: Fabric can fire it from the Netty thread (`Connection.channelInactive`). Use a main-thread hook before the player is saved (`PlayerList.remove`).
- Stage 10 projection design: `PUBLIC_HERO` mirrors the hero id only — widening it to resources/passives would re-create the privacy surface of `HERO_DATA` broadcast; anything client-side needing more can read other synced attachments or payloads. `copyOnDeath` mirrors `HERO_DATA` lifecycle (hero persists across death), persistent mirrors save data so no respawn hooks are needed.
- Ability-scoped attribute buffs should become transient; hero base passives stay permanent (transient max-health modifiers would clamp health on load because `LivingEntity.readAdditionalSaveData` calls `setHealth` after loading attributes).

## Verification

- Migration A1: `qualityGate` green (ArchUnit tests + `verifyArchitectureBaseline` included); negative probes trip exactly the five expected rules; baseline files committed. `runClient` launches to the title screen under xvfb on the orchestration VM.
- Stage 15: `qualityGate` green, 24/24 GameTests (2 new: horde audience pause/resume, ram adopt/dedupe).
- Stage 14: `qualityGate` green, 24/24 GameTests (2 новых `TargetPredicateGameTests`: AoE бьёт врага и пропускает тиммейта без friendly fire — через `DoomsdayRoar` end-to-end; `pvp=false` убирает игроков из сканов). Готча: `makeMockServerPlayerInLevel` хардкодит `isCreative()=true` и общее имя `test-mock-player` — для pvp/team-тестов нужен `TestPlayers.join(helper, name)` с реальным ServerPlayer.
- Stage 11: `qualityGate` green, 27/27 GameTests (3 new dispatcher tests). Intentionally kept: `CombatImpactEngine`'s nano-hammer branch (reads the `NANO_FORM` attachment — an ability-state rule, not a lookup table) and client-side statics (`ThanosHero`, `PandoraHero` tables used by HUD filters).
- Stage 10: `qualityGate` green, 24/24 GameTests (2 new public-hero-sync tests). First run caught a test bug: `untransform` is cooldown-gated right after `transform` — the test uses `forceUntransform`.
- Stage 8: `qualityGate` green, 26/26 GameTests (4 new House of Vanity tests). Test-only notes: victims must be joined+teleported inside `PULL_RADIUS` BEFORE `AbilityRouter.activate` — latecomer absorb only runs on keepalive ticks (every 20), so post-activation joins race the assert delays; mock players hardcode `isCreative()==true` (bytecode-verified `GameTestHelper$2`), so `VanityAuthority` `mayfly` grant/clear is unreachable in gametest — assert `hasEffect` probes instead.- Stage 5: `qualityGate` green, 22/22 GameTests (3 new damage-pipeline tests). Test-only notes: mock players join with private `spawnInvulnerableTime` that blocks `hurt()` — cleared via reflection in the kawarimi test; the test structure spawns ~6M blocks from the mock-player spawn point, so proximity-sensitive asserts must teleport the player first.- Stage 4: `qualityGate` green, 19/19 GameTests. Negative check — without the `hasHero()` guard, first-time transforms start with 0 energy and `windPrisonEndsWhenItsZoneExpires` fails.
- Stage 3: `qualityGate` green, 16/16 GameTests (7 new lifecycle regressions: owner-leave lock release, refcount, shadow reconcile, transient-vs-permanent NBT, attachment-backed transform cooldown, forceUntransform clears locks+cooldowns, dead-caster snap).
- Stage 2: `qualityGate` green, 9/9 GameTests; negative check — old tick write-back makes `windPrisonEndsWhenItsZoneExpires` fail.
- `./gradlew qualityGate --no-daemon` green on stage 1: JUnit, 8 `ProjectSanityTest` checks, assertion audit, jar isolation audit, 7/7 GameTests.
- Negative check: re-inserting the old mixin logic makes `droppingWithFullInventoryDoesNotRecurse` fail with `StackOverflowError`.
- `runClient` reaches the title screen on the orchestration VM (DISPLAY=:0 + xvfb-run); runtime checklists are feasible for client-facing stages. Server behavior is covered by GameTests with real joined players.

## Known issues / follow-ups

- `runServer` in dev fails mod resolution while Veil is on the runtime classpath (audit N2); decide whether to make Veil `modCompileOnly` + client-only runtime.
- `auto-approve-pr.yml` still auto-approves green PRs (audit §3); left as a repository-owner decision, not changed by this work.
- Execute the plans in `docs/design/architecture-migration/` stage by stage; the canonical stage graph is `00-overview.md` §2.1 and each plan's «Статус стадий» table is its tracker. First stage: plan 1 `A1` once every bugfix PR (#37–#40, #42–#49) and #41 are on `main`.
- The bugfix stack merged as a single linear history (`integrate/main` → `main`): the hazards called out here were real (`fabric.mod.json` GameTest unions, `RemoteHeroSkins` deletion vs stage-7 references, `ServerTickEvents` removal vs later imports) and were resolved during the restack — `RemoteHeroSkins` references dropped, GameTest entrypoints unioned to 13, `TargetFilters`/`SafeTeleport`/`WorldDestructionPolicy` imports kept where still used.
- Owner decision needed before plan 5 stage `E1`: new root package name (proposed `io.github.grebeshok105.codex`, decision R14).
- Rebuild only the project skills that prove useful for the new workflow.
- Design a new release/versioning workflow after the verification baseline is stable.
- Use the VFX research to decide the Codex 5.0 rendering foundation.

## Next session

1. Read this file, `AGENTS.md`, and the plan's «Статус стадий» tables — migration stage `A1` is done on `main`; the next legal stages per `00-overview.md` §2.1 are `A2`, `N1`/`N2`/`N3`, `CL2` (three tracks in parallel).
2. For architecture work: `docs/design/architecture-migration/00-overview.md` plus the one plan whose stage you execute (stage graph §2.1).
3. Re-verify findings while implementing; do not assume subagent-only findings are proven until checked.
4. Keep `qualityGate` green; add GameTests for server behavior; update the audit tracker and this file per stage.


## Architecture migration — stage B1 (plan 02)

- Remaining hero-owned data moved onto the hero: `Hero` gained `getPassiveGlyphs()` / `canSuperJump()` / `getMeleeBleed(ServerPlayer)` defaults; all 22 heroes declare their `THEME`/`HUD` literals and values (Pandora THEME = Regulus-palette copy with the mandated comment; Doomsday bleed gates on `getTier(attacker) >= 3` — clamp-equivalent to the old controller read).
- New types: `hero/BleedProfile` record, `hero/PassiveGlyph` enum (19 constants, `HudIcons.PassiveGlyph` moved verbatim); `AbilityDescriptions.passiveCount` reads the hero; `SuperJumpController`/`HeroBleedingController`/`HeroMeleeImpactController` consume hooks only.
- Legacy tables deleted: `HeroTheme.<HERO>`×11 + `HeroHudConfig.<HERO>`×21 constants (DEFAULT kept), `PassiveIcons`, `HERO_PASSIVE_COUNT`, `ALLOWED_HEROES`/`isAllowed`/`bleedFor`/`getDoomsdayTier` temp lookups.
- Characterize-first: goldens `hero_presentation.txt` + `passive_glyphs.txt` captured on old code; `presentationMatchesGolden`/`passiveGlyphsMatchGolden` green post-migration. qualityGate green (59/59 gametests on branch); freeze store shrank by the expected SuperJumpController/getDoomsdayTier/Pandora→Regulus entries.
- Runtime checklist (§11.1.11): hero panel + radial + energy HUD pixel-identical old-vs-new for homelander/iron_man/pandora/scorpion on a shared run dir; super jump launches on Regulus (F3 Y −62→−45.8) and stays denied on Scorpion. Evidence under `/home/ubuntu/b1-runtime/`.


## Architecture migration — stage D1 (plan 03)

- `HeroTickDispatcher` gained `Phase.START` (START_SERVER_TICK) and `Phase.EARLY` (END tick, before GLOBAL — empty until D2b) plus `onServerTickStart`/`onEarlyTick`/`onHeroTick` and `init()` (registers START+END listeners at the former `END_SERVER_TICK` call site in `SuperheroesMod`). `registrar()` exposes a private `enum Registrar implements TickRegistrar` for module-facing registration.
- New `lifecycle/` types verbatim per plan: `LifecycleRegistrar` (7 hooks + `global()`), `GlobalLifecycleRegistrar` delegating to `PlayerLifecycle` (BF3) + `HeroLifecycle` (BF11), `OwnedSessionMap` (`create(lifecycle, Set<ClearOn>)`, `ClearOn{LEAVE,DEATH,HERO_CLEAR}`, null-rejecting `put`) + `OwnedSessionMapTest` (3 JUnit tests).
- First consumer migrated: `CapShieldSlamAbility` `WeakHashMap` → `OwnedSessionMap` (LEAVE+DEATH + always server-stop clear; its `clear`'s "untransform" javadoc was stale — no `onClear` registration ever existed, behavior preserved exactly). The three registrations dropped from `SuperheroesMod`.
- Integration fix: `startRunsBeforeEndPhasesAndEarlyBeforeGlobal` asserted absolute index order — made robust to mid-tick hook registration (search relative to first "start"). qualityGate green: 67/67 gametests; worker's hand-shrunk freeze-store entries verified = canonical `allowStoreUpdate` output.

## Architecture migration — stage B3 (plan 02)

- `TransformationLore` record (`transform/`) + `TransformationItem(heroId, props, lore)` ctor; new `appendHoverText` emits openDivider→flavor→empty→bullets→closeDivider via `TooltipFrame`, null-guarded so the 7 remaining subclasses keep working.
- 15 lore-only subclasses deleted; `ModItems` now constructs them inline with identical keys/colors/order/props. Hero ids passed as `ModId.of("…")` literals — `<Hero>.ID` class reads would have added 15 new frozen-rule violations (`sharedCodeDoesNotDependOnConcreteHeroes` can't grow); convention confirmed by plan 06 I5a («id героев строками»). Pandora keeps `doctor_strange_suit` id + comment.
- Integration fixes: `item.TooltipFrame` moved to `transform/` (TransformationItem's tooltip dep created a new `item <-> transform` package cycle — ratchet caught it; move is acyclic since `item.infinity` never imports `transform`). Freeze store −16 (all removed entries = deleted subclasses' hero-id reads, zero additions).
- Golden `transformation_lore.txt` (22 items) captured pre-migration; `transformationItemLoreIsStable` now compares against it — green post-migration. qualityGate: 70/70 gametests.
- Runtime cross-build verification (old subclasses vs new, same world/GUI scale, fixed hover coords): `goku_gi`/`scorpion_kunai` tooltips pixel-perfect; `doctor_strange_suit` content identical (≤0.78% px residual = tooltip-fill bleed); `blade_of_chaos` stone hint identical; kunai right-click → scorpion transform + hellfire HUD PASS. Post-merge qualityGate 71/71.

## Architecture migration — stage B2 (plan 02)

- `Hero.passiveAttributes()` returns the hero's `AttributeModifierSet`; default `applyPassives`/`removePassives` route through it. 6 fully-reducible heroes dropped both overrides (captain_america, goku, loki, naruto, scorpion, kazuha); 5 partially (atrain, rem, pandora, raiden, reinhard keep the override with extra side effects); 11 custom keep overrides but route the passive set through `PASSIVES`.
- `HeroAttributes.java` deleted: per-hero passive sets → `PASSIVES` fields on heroes; the ability-scoped/transient members (KRATOS_RAGE, NANO_*, RAIDEN_BURST, REINHARD_*, REGULUS_MADNESS, DOOMSDAY_*, thanosClearStoneModifiers, buildReinhardPhaseSet, buildDoomsdayTierSet) moved verbatim to `hero/AbilityScopedModifiers.java` — same package, so zero new package edges and zero new frozen violations (not a concrete-hero class). `ability/` host rejected: hero files read those members → would create new `ability<->hero` cycle pair.
- `DoomsdayHero.PASSIVES` = `AbilityScopedModifiers.DOOMSDAY` (one definition); id/value tuples diff-verified identical old-vs-new.
- Golden `passive_modifiers.txt` (171 applied-modifier records) captured pre-migration; `passiveModifiersAreStable` compares — green. qualityGate 70/70; baseline/store delta zero.

## Architecture migration — stage D2a-1 (plan 03)

- Scorpion migrated to the module pipeline: `HeroModules.bootstrap(CoreModuleContext.INSTANCE)` runs a two-pass ctor over `HeroModules.ALL` = [ScorpionModule]; `Heroes.SCORPION` + 4 `SCORPION_*` ability constants deleted — `AbilityIds` referenced instead; SuperheroesMod line 56 identical callsite preserved.
- Contracts: `HeroModule` (id + ctor taking `HeroModuleContext`), `AbilitySink` (register(cb)), `CoreModuleContext` singleton. Strict ArchUnit rules verified non-empty via temporary `allowEmptyShould(false)` flip (13 rules pass).
- Store shrank exactly 2 lines (Heroes.SCORPION field + clinit call); cycle baseline unchanged.
- Gametest `scorpionIsRegisteredThroughItsModule` asserts registry↔module list identity + all 4 ability ids. qualityGate 70/70.
- Runtime (PR head @742ecda): kunai transform, all 4 scorpion abilities (spear/eruption/breath/hellport), suggestion list, regulus sanity — PASS. Pre-existing bug found (NOT regression): Hellport never displaces — `SafeTeleport.clamp` self-collides on the caster's bounding box; files byte-identical to main. Logged for a separate fix ticket.

## Architecture migration — stage D2a-2 (plan 03)

- All 22 heroes registered through modules: 21 new `hero/<id>/<Id>Module.java` (package = hero id w/o underscores; `final class`, own hero field, `register(ctx)` = hero's abilities in `getAbilities()` order). `HeroModules.ALL` = 22 in original `Heroes.init()` order; `bootstrap` = heroes → `SharedAbilities` → module register (two-pass preserved). `bootstrap/SharedAbilities` owns FLIGHT + VILTRUMITE_RECOVERY (≥2-hero abilities) in old init order.
- `Heroes`/`AbilityRegistry` fields + `init()` deleted; `register`/`get`/`all` kept. `SuperheroesMod`: `HeroModules.bootstrap(CoreModuleContext.INSTANCE)` at the old init site. 114 module abilities + 2 shared = 116 ids — id set identical to old init; each old ability owned by exactly one module/shared.
- Gametest `modulesCoverEveryHeroInRegistryOrder` (verbatim from plan) + index-independent `scorpionIsRegisteredThroughItsModule`. Cycle baseline: `ability<->ability.ironman` pair resolved and removed; store auto-shrank ~42 stale `Heroes.*`/`AbilityRegistry.*` entries. qualityGate 71/71.


## Architecture migration — stage D2b-1 (plan 03)

- 22 controllers renamed `init()` → `register(HeroModuleContext ctx)` verbatim (init bodies — UseItemCallback/AttackEntity/JOIN/DISCONNECT listeners — kept identical inside register). MirrorDimension self-END → `ctx.ticks().early`, MadnessFlight self-START → `ctx.ticks().start`; both lost `ServerTickEvents` imports.
- New `bootstrap/SharedMechanics.register(ctx)` = old shared rows in old relative order: HeroLandingTracker, HeroEquipmentLock, SuperJump, AutoSaturation, HeroPassiveRegen, MeleeImpact serverTick, BallisticBodyTracker, FlightController cleanup, HeavensStrike + content rows HordeManager(level), AdminBuildSync. Called from `HeroModules.bootstrap` between SharedAbilities and the per-module pass; `SharedMechanics.registerPost(ctx)` after the module pass holds the shared wiring that must stay after every hero listener/tick: `HeroMeleeImpactController.register` (AttackEntity: IronFists@64 consumed before MeleeImpact@67 — uniform module order can't place it between Omniman@14 and TimeSlow@11, post-pass keeps all hero listeners ahead; residual edge: a time-frozen attacker previously got MeleeImpact push+FX on the cancelled hit, now gets only the cancel), `Landing.tickPlayer`, `Flight.tickPlayer`.
- Hero modules extended with their controllers' register + tick rows (old table order within each hero): Scorpion 1; Pandora 3 (Mirror early, Death global, SpatialBind global); Homelander 6; IronMan 7; Regulus 4; Invincible 1 + ViltrumiteCharge tick (sole-Invincible); Omniman 1 + Rush/Think ticks; BattleBeast 1; Rem 2; SungJinwoo 2; Doomsday 4 + DoomGrip/ChargeTackle/Footsteps ticks; Goku 2 + Kamehameha/SpiritBomb ticks + SAIYAN_AURA activeAbility; Naruto 2 + 3 Rasengan ticks + SAGE_MODE activeAbility.
- `SuperheroesMod`: my init() calls + tick rows removed; kept core inits, worker-B init()s (Thanos×2, Kratos×2, Reinhard×3, RaidenPlunging) + B's tick rows + PassiveReconciler/ResourceController rows. registerPlayerLifecycle + inline damage lambdas untouched (D2b-2).
- Tests: `assertControllersAreWired` deleted (STATIC_INIT scan, SUPERHEROES_MOD field); `controllersHaveNoStaticInit` strict rule added verbatim — RED until worker B removes the last 8 `*Controller.init()`.
- Freeze store: `4c613794` −2 (my self-registered tick listeners); `8c45b479` — init()→register(ctx) descriptor rewrites (Invincible/Kawarimi/RegulusTotem) + +1 line shifts for import additions; `6ab35c1a` regenerated at current SuperheroesMod line numbers (uniform −22 from HEAD for onLeave/onDeath/onRespawn/onServerStopped; −15 vs stale store for earlier blocks).
- Tick cross-reads (сверка): LandingTracker.tickPlayer reads Unibeam.isBusy and ViltrumiteCharge/Rush read FlightController.isFlightActive — both order flips were caught at integration and fixed by `SharedMechanics.registerPost` (Landing/Flight player ticks moved after the module pass, restoring Unibeam@345→Landing@347→Flight@380 freshness and charge/rush's pre-refresh isFlightActive read); reviewer additionally caught HeroMeleeImpactController's AttackEntity listener needing the same post-pass (IronFists consume-order). UraniumDefense.isUnderUraniumThreat read stays phase-safe (GLOBAL precedes PLAYERS both schemes). Residual documented above.
- qualityGate NOT run here (orchestrator runs the gate); `controllersHaveNoStaticInit` expected-green after worker B's init() removals landed (aab21ad) — verified at gate.

## Architecture migration — stage CL3a-1 (plan 04)

- Client module contracts landed: `client/core/module/{HeroClientModule,HeroClientContext,CoreClientContext}` (plan-verbatim) + `client/bootstrap/HeroClientModules` + `client/core/input/HeroActionKeys` (one END_CLIENT_TICK drains `consumeClick()` always, fires `onPress` only when local `PUBLIC_HERO` matches; no key mappings registered yet — CL3b owns them).
- `ScorpionClientModule` owns the `ScorpionFxS2CPayload` receiver (handler byte-identical; `ClientNetworking` lost only that receiver + import — 35→34 in-file + 1 via `ctx.receive`). `heroId() = ScorpionHero.ID` — IN_CLIENT_HERO_MODULE is excluded from the sharedClientCode rule. `HeroClientModules.bootstrap()` runs right after `ClientNetworking.init()`.
- New ArchUnit client rules (`clientHeroModulesAreReferencedOnlyByThemselvesAndTheModuleList`, `clientCoreDoesNotKnowHeroModules`, `sharedClientCodeDoesNotDependOnConcreteHeroes`) now non-vacuous. qualityGate 71/71.
- Runtime: receiver proven to fire via temporary `[CL3A1-FX]` println (kind=2 hellfire pillar, kind=1 spear harpoon) — Veil 4.1.2 in classpath, handler dispatches to `VeilScorpionFx`. Kunai→scorpion transform, regulus regression PASS.


## Architecture migration — stage D2b-2 worker A (plan 03)

- `SuperheroesMod.registerPlayerLifecycle`/`registerTickHandlers` deleted (onInitialize 222→61 lines): core rows → `SharedMechanics`, hero rows → owning modules via `ctx.lifecycle()` or the controller's `register(ctx)`. Split by position inside the old table: rows that OPENED an event list (HTS::onPlayerJoin/HDS::syncPublicHero onJoin; HTS::onPlayerLeave+ECL::releaseOwnedBy onLeave; ECL onDeath; HTS::onPlayerRespawn; HeroReactionController::onTransformed — global cross-hero broadcast, parked shared; Horde/EnergyLocks resetAll) sit in `SharedMechanics.register` (pre-modules), rows that CLOSED a list (AbilityCooldowns::syncAll onJoin, AC::clearAndSync onDeath, ECL::releaseOwnedBy onHeroClear) sit in `registerPost` (post-modules) — documented in javadoc.
- Per-hero hooks moved: Regulus 10 (Greed onLeave/onDeath/stop; Madness join/leave/death/respawn/heroClear/stop; Totem heroClear), Doomsday 4 (DoomGrip leave/death/heroClear/stop), Omniman 4 (ThinkMark leave/death/heroClear/stop — ACTIVE stays a plain map: clear() has unlockTarget+setPose side effects), BattleBeast 2 (Curse reapplyOnJoin + stop), Rem 2 (Demonism heroClear+stop), SungJinwoo 1 (stop; DEATH_ECHOES keyed by ServerLevel — not convertible), Pandora 3 (MirrorDim leave+stop in its register; PandoraDeath.register adds ALLOW_DAMAGE→ALLOW_DEATH in old order + onHeroClear resetOnHeroTaken).
- OwnedSessionMap conversions (13 maps, 11 files): all cleared-only `Map<UUID,…>` — 9 charge abilities (ChargeTackle, ViltrumiteCharge, OmnimanViltrumiteRush, GokuKamehameha, GokuSpiritBomb, NarutoRasengan/Oodama/Rasenshuriken, RepulsorCharge) got ClearOn{LEAVE,DEATH}; Unibeam 3 maps ClearOn{LEAVE,DEATH,HERO_CLEAR} (covers the old heroClear row verbatim — clearState was pure drops); MonarchsDomain ClearOn{LEAVE,DEATH}; PandoraDeath.ACTIVE + SpatialBind.BOUND ClearOn{LEAVE}. Owners = the keyed player everywhere except SpatialBind (victim uuid — map is victim-keyed). Auto-drop registers at class-init; every converted class is touched at bootstrap via `new XAbility()`/`X::tick` in its module, except RepulsorChargeController (lambda body → first player tick — safe: map is empty before any use and no leave can precede a tick).
- AbilityRules attribution (old order preserved as module order): isAftermath blocker + isMadness freeCost → HomelanderModule (MADNESS is applied by Homelander-only MilkBottleItem — сверка correction, not Regulus); VANITY_STRIPPED → PandoraModule; DISABLED_ABILITIES (applied by ThanosSnap/SoulPulse) → ThanosModule — B's push missed it; restored by worker A post-rebase.
- §6.3 GameTests (`LifecycleSideEffectsGameTests`, registered in gametest fabric.mod.json): DoomGrip heroClear releases victim NO_AI+caster INVULNERABLE; ThinkMark heroClear restores victim AI/gravity; madness heroClear resets REGULUS_MADNESS + strips only madness-owned effect instances (foreign DAMAGE_RESISTANCE amp-5 survives); Greed owner-leave frees frozen victim; MirrorHouse caster-leave closes + victim-leave releases; Pandora heroClear drops PANDORA_REVIVED + permanent INVULNERABLE; relog reapplies BattleBeast curse (real save/load path via new `TestPlayers.rejoin` — same GameProfile). DoomGrip/ThinkMark start() are unguarded — tests drive them without transforms.
- Cross-read сверка: none — every hook touches only its own hero's state. Order flips vs old table (all plan-accepted module order): respawn ThanosGauntlet→RegulusMadness became Regulus→Thanos (independent attachments); onLeave/onDeath hero rows now run in HeroModules.ALL position instead of table order.
- qualityGate NOT run (orchestrator runs the gate). Frozen store 6ab35c1a untouched (orchestrator-owned, §380) — its 70 stale lines all reference the deleted method; orchestrator regenerates/shrinks.
- Integration: B pushed first (contrary to plan order); rebased worker-A commit onto `origin/arch-mig/D2b-2` — one conflict (SuperheroesMod: his truncated table vs my full rewrite → took the rewrite) + `fabric.mod.json` auto-merged (both test classes registered). B's commit did NOT carry the `DISABLED_ABILITIES` rule — restored it in `ThanosModule.register` post-rebase (thanos sits at module position 10, keeping the old blocker order aftermath → disabled → vanity).
