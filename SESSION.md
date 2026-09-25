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
| 3 | Lifecycle: B3, B4, B8, B17, B23, N1 (main-thread leave hook) | | todo |
| 4 | B5 cooldown/heal reset on hero swap, B6 Snap stones | | todo |
| 5 | Damage pipeline B9, B20, B21; B11 global tick rate | | todo |
| 6 | B10 `WorldDestructionPolicy` | | todo |
| 7 | B15 client state/session | | todo |
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

## Important decisions

- Bound weapons are identified by type (`BoundWeaponItem`) and validated by token; untokened copies are treated as stale on purpose (none are obtainable legitimately; old saves could hold leaked copies).
- Bound weapons never become item entities: returned to the owner when valid and there is room, otherwise deleted (the ability can reissue).
- `HeroData` sync: writes are immediate; full syncs stay immediate to preserve packet ordering with other payloads; resource-only syncs are coalesced per tick.
- `AbilityRouter.deactivate` order is flag-off → `onDeactivate` (verified no `onDeactivate` callee reads the active set).
- Lifecycle cleanup (stage 3) must not rely on `ServerPlayConnectionEvents.DISCONNECT` for game state: Fabric can fire it from the Netty thread (`Connection.channelInactive`). Use a main-thread hook before the player is saved (`PlayerList.remove`).
- Ability-scoped attribute buffs should become transient; hero base passives stay permanent (transient max-health modifiers would clamp health on load because `LivingEntity.readAdditionalSaveData` calls `setHealth` after loading attributes).

## Verification

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
