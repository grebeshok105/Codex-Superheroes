---
name: migrate-hero
description: Procedure for migrating one hero into the hero-module architecture (characterize → move → clean shared rows). Use during П6 waves I1–I6 and for any new hero migration.
---

# migrate-hero — procedure

Distilled from stage F (Scorpion, #85) and G1–G3 (Reinhard, #86/#87/#88). Follow in order; each numbered item is a commit boundary.

## 0. Pre-checks
- Read the hero's row in plan 06's wave table + do «Сверка»: re-grep the hero's actual files on current `main` — the wave table may be stale.
- Confirm dependencies: wave order (H→I1/I2/I3/IC; I2+I3→I4; I3+I5→I6; all→O), ≤3 stages in flight.
- Reference modules: `hero/scorpion/` (simple vertical) and `hero/reinhard/` (max seams — attachments, bound weapon, control locks, C2S+screen, sound filter, decorations).

## 1. Characterize BEFORE moving (mandatory)
- Write gametests characterizing current behavior on OLD code (state persistence, denials, gates, cooldown edges). Orchestrator runs them on old code; only after green baseline does the move start.
- Look for hidden dead code (`git log -S` + zero call sites) — migrate dead code DISCONNECTED, don't wire it live (see isReinhardSwordOnly precedent).

## 2. Layout
```
hero/<id>/            — <Hero>Module, <Hero>Hero, <Hero>Abilities, <Hero>Items, <Hero>Attachments (root: wiring only)
hero/<id>/ability/    — ability classes, each owns `public static final ResourceLocation ID = ModId.of("<id>")`
hero/<id>/runtime/    — controllers, state records, modifiers
hero/<id>/item/       — hero items (bound weapons extend mechanic.boundweapon.BoundWeaponItem)
hero/<id>/net/        — payload classes (TYPE ids byte-identical)
hero/<id>/sound/      — hero sounds leaf (avoids hero→sound edge)
client/hero/<id>/     — <Hero>ClientModule + state/, hud/, screen/, render/, fx/
```
Intra-module DAG: root→leaf one-way only. Leaves never import root module classes — local `private static final ResourceLocation` constants instead. `net/` handlers that would create ability↔net cycles live as `public static` on the ability class (see ReinhardWishAbility.handleWishConfirm).

## 3. Available seams (HeroModuleContext / HeroClientContext)
- attachments: `ctx.attachments()` → AttachmentRegistrar (`persistent(path,codec,copyOnDeath[,initializer])`, `transientType`)
- ticks: TickRegistrar phases; lifecycle: LifecycleRegistrar (onJoin/onLeave/onDeath/onRespawn/onClear/onServerStopped)
- damage pipeline: ALLOW_DAMAGE/AFTER_DAMAGE listeners via ctx
- sessions: OwnedSessionMap (auto-clear on leave/death/hero-change/SERVER_STOPPED) — replaces static Map<UUID>
- payloads: `ctx.receive(TYPE, handler)`; C2S guards: `C2SGuards.requireHero/requireActiveAbility` (guard-first, else no-op)
- client: `ctx.hud(order)` , `ctx.playerLayer`, `ctx.soundFilter(predicate)`, `ctx.abilityDecoration(id, decoration)`, `ctx.receive`
- services: mechanic.Motion / FxBroadcast / Targeting; client.SkinResolver, HudLayers, ClientSoundFilters, AbilityDecorations, ClientSessionState.register

## 4. Shared-row cleanup
Delete hero rows from: AbilityIds, AbilityRegistry, Heroes, ModItems, ModItemGroups, ModAttachments, ModNetworking, ModSounds, SuperJumpController, HeroTransformService, AdminAbilityDebug, SuperheroesClient/ClientNetworking (client side), RadialMenuHud, mixin configs. One file = one owner.

## 5. Invariants (never break)
- All ids byte-identical: attachments, items, abilities, sounds, payload TYPE ids, lang keys (en_us + ru_ru together).
- No behavior changes (plan-sanctioned only): C2S guard additions are sanctioned; registration ORDER changes golden files — regen via orchestrator.
- Hero→hero class deps: 0 (string ids + tags only).

## 6. Orchestrator-owned (workers never touch)
- src/test/resources/archunit_store/** (refreeze: `-Darchunit.freeze.refreeze=true`)
- src/test/resources/architecture/package-cycles-baseline.txt (regen: `-Dcodex.writeCycleBaseline=true`; new bidirectional pairs are FORBIDDEN — §8.12 = 0 at end)
- src/gametest/resources/golden/** (regen on registration-order change)
- status tables, SESSION.md, 00-overview.md, all Gradle runs.

## 5b. Entities / particles / sounds / items
Hero-owned entities and particles register DIRECTLY via `Registry`/`FabricParticleTypes` in module-owned `*Entities`/`*Particles` classes — intentional design (H gate): do NOT invent a registrar seam for them. Sounds go to `hero/<id>/sound/` leaf via direct `SoundEvent` registration.

## 5c. Commit hygiene
`behavior:`/`policy:` prefix on every runtime-visible change — including new guards/validations (a dropped packet, a new denial). Refactors that only move code stay `refactor:`; golden/store/baseline regens stay `test:`.

## 7. Verification sequence
1. Worker: implement, `javac`-check optional, push branch, report acceptance greps.
2. Orchestrator: inspect diff → refreeze+regen → `./gradlew qualityGate --no-daemon` → fresh reviewer → runtime checklist via runClient (HUD/input/render/network/VFX/entity) → PR → merge.
3. Acceptance greps (hero `<id>`): `grep -rliE '<id>' src/main/java | grep -v '/hero/<id>/'` → HeroModules.java only; same for client → HeroClientModules.java only.

## 8. Known traps
- Registration order shifts goldens (kunai/reinhard_suit moved to front on module bootstrap) — regen, don't fight it.
- `sourceSets.client` doesn't exist inside `sourceSets {}` block — eager declare + afterEvaluate (see build.gradle clientnoveil).
- Edge-detected ability keys need ~500ms holds in runtime tests.
- `/give` on bound weapons gives Air — they need BoundWeapons.ensureHeld (ceremony-issued).
- Gate READY flags are transient — persisted counters alone don't re-arm after relog.
- No melee in solo test env — forced client-state hooks prove render paths.
