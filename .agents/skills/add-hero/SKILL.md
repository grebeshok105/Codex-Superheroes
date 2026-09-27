---
name: add-hero
description: Checklist for adding a new hero (server+client module pair, registration, resources, golden rows, GameTests) or a new ability to an existing hero in the post-migration module architecture. Use whenever a hero is created or extended.
---

# add-hero — checklist

Architecture context: `AGENTS.md` §2 and §7; dependency table in `docs/design/architecture-migration/00-overview.md` §5.2. A hero is a **module pair** — `hero/<id>/<Id>Module` + `client/hero/<id>/<Id>ClientModule` — registered by **one new line in each composition root**. Reference implementations: `hero/doomsday/` (most server seams: attachments, damage types, commands, tiered `visibility`), `client/hero/rem/` (hud + render + state + S2C receiver), `hero/scorpion/` (Veil-gated `fx/veil/`).

`<id>` = lowercase snake id (`doomsday`, `iron_man`); `<Id>` = CamelCase prefix (`Doomsday`, `IronMan`).

## 1. Server module — `hero/<id>/`

- `<Id>Hero implements Hero` — the contract: `getId()` (`ModId.of("<id>")`), energy/mana caps, `getDimensions(Pose)`, `getAbilities()`, `getDefaultBinding()`; override hooks only where the hero deviates (`passiveAttributes`/`applyPassives`/`removePassives`, `keepsHeroOnDeath`, `reapplyPassivesAfterRespawn`, `visibility`, `canUseAbility`/`onAbilityDenied`, `onLanded`, `getTheme`, `getHudConfig`, `getPassiveGlyphs`, `getImpactStyle`/`getImpactPower`, `getThreatClass`, `canSuperJump`, `getMeleeBleed`, `cancelsFallDamage`, `getSkinTexture`, …).
- `<Id>Module implements HeroModule` — `hero()` + `register(HeroModuleContext ctx)`; optional `damageTypes()` returning `DamageTypeSpec`s for datagen.
- `ability/` — one `implements Ability` class per ability (`getId`, `isToggle`, `costOnActivate`, `costPerTick`, `tryActivate`, `canActivate`, `onTickActive`, `onDeactivate`). Each owns `public static final ResourceLocation ID = ModId.of("<ability_id>")`.
- `runtime/` — controllers and state records. Wire through `ctx.ticks()` (phases: `start`/`early`/`global`/`level`/`player`/`hero`/`activeAbility`) and `ctx.lifecycle()` (`onJoin`/`onLeave`/`onDeath`/`onRespawn`/`onHeroClear`/`onHeroTransformed`/`onServerStopped`) in `register`. Per-player session state = `OwnedSessionMap.create(ctx.lifecycle(), Set.of(ClearOn.…))` or a transient attachment — never a static `Map<UUID,…>`. Controllers get no static `init` (ArchUnit-forbidden); the module's `register` wires them.
- `net/` — payload records with `TYPE` + `STREAM_CODEC`; register `ctx.payloads().s2c(...)` / `c2s(...)`. Every C2S handler starts with `C2SGuards.requireHero` / `requireActiveAbility` — guard-first, else no-op.
- `item/` — item classes; `<Id>Items` registers via `ModContent.item(path, item)`; the transformation item is `new TransformationItem(ModId.of("<id>"), props, lore)`; creative tab via `ctx.content().creativeTab(item)`.
- `sound/` — `SoundEvent`s via `ModContent.sound("<id>.<name>")` + `init()` to force class init.
- `entity/` — entity types via direct `Registry.register` + `FabricDefaultAttributeRegistry` (no shared registrar — intentional, R23). If entity ↔ runtime references would form a bidirectional package pair, keep the entities class inside `runtime/` (RemEntities precedent).
- `registry/` — damage types etc.; expose specs through `module.damageTypes()` so datagen never names hero classes.
- `<Id>Abilities` / `<Id>Items` / `<Id>Attachments` at the root are **aliases over leaf constants** + `init()` — never the other way around.
- Attachments live on a leaf record: `AttachmentType<X> X = AttachmentRegistrar.FABRIC.persistent(path, codec, copyOnDeath[, initializer])` or `.transientType(path)`; the root alias is what gametests/out-of-module code import.
- Hero mixin only when no shared hook covers it: `mixin/hero/<id>/` + `superheroes.mixins.json` entry under the hero's id.

## 2. Client module — `client/hero/<id>/`

- `<Id>ClientModule implements HeroClientModule` — `heroId()` + `register(HeroClientContext ctx)`.
- `ctx` seams: `receive` (**mandatory for every `*S2CPayload` the server module registers** — `everyHeroS2CPayloadHasARegisteredReceiver` fails otherwise), `hud`/`movableHud`, `actionKey`, `entityRenderer`, `particle`, `skin`, `playerLayer`, `soundFilter`, `abilityDecoration`, `fovModifier`, `hudGlitchSource`, `beamStyle`, `skinSuppression`, `crosshairSuppression`, `heroPanelSection`, `clientTick`.
- `state/`, `hud/`, `render/`, `screen/`, `fx/` as needed. A `Client*State` static must register its reset via `ClientSessionState` (`assertClientStatesRegisterReset`).
- Veil-backed FX go in `fx/veil/` behind `FabricLoader.isModLoaded("veil")` — `veil.*` never leaves `src/client` (see AGENTS.md §7).
- Client mixins only when no shared hook covers it: `client/hero/<id>/mixin/` + a dedicated `*.mixins.json` config (the existing configs cover `client.mixin` and `client.core.mixin` only — they don't see `client.hero` packages).

## 3. Registration — two single lines

- `bootstrap/HeroModules.ALL`: `new io.github.grebeshok105.codex.hero.<id>.<Id>Module()`. Position in the list = registry order → hero order in goldens and lists.
- `client/bootstrap/HeroClientModules.ALL`: `new io.github.grebeshok105.codex.client.hero.<id>.<Id>ClientModule()`, same position.

Nothing else names the hero. If a design seems to need another shared file, the design is wrong — express it through a `Hero` hook or a `ctx` registrar.

## 4. Package rules (ArchUnit-enforced)

- A leaf never imports its module root — it owns `ModId.of(...)` literals; the root aliases them.
- Zero class references hero→hero: other heroes are string ids (`ModId.of("homelander")`) or entity/tag checks.
- `hero.<id>` classes are referenced only by `hero.<id>`, `client.hero.<id>` and `bootstrap.HeroModules`; `client.hero.<id>` only by itself and `client.bootstrap.HeroClientModules`.
- Composition roots (`SuperheroesMod`, `SuperheroesClient`, `bootstrap.*`, `client.bootstrap.*`, `datagen`) are never imported — use `LoggerFactory.getLogger(ModId.MOD_ID)`, not `SuperheroesMod.LOGGER`.
- Server ticks only via `ctx.ticks()`; lifecycle only via `ctx.lifecycle()`; never `ServerTickEvents`/`DISCONNECT` directly (DISCONNECT can fire off the server thread — `onLeave` is the safe hook).

## 5. Resources checklist

- `assets/superheroes/lang/en_us.json` **and** `ru_ru.json` together (sanity check enforces sync): `hero.superheroes.<id>`; `hero.superheroes.<id>.passive.<n>` for each `getPassiveGlyphs` entry, in order; `ability.superheroes.<ability_id>` + `.desc` per ability; `item.superheroes.<item>` (+ `.lore.lineN` / `.lore.usage` / `.lore.untransform` on the transformation item); `entity.superheroes.<entity>`; `subtitles.superheroes.<sound>`.
- `assets/superheroes/models/item/<item>.json` per item.
- `assets/superheroes/textures/entity/hero/<id>.png` — the skin `getSkinTexture` returns.
- `assets/superheroes/textures/gui/abilities/<ability_id>.png` per ability — radial-menu icon, filename = ability id path.
- `sounds.json` entries + `sounds/<id>/*.ogg` — OGG Vorbis only.
- Check `art-source/` for existing source assets before creating any texture/sound/model.
- Datagen: hero-owned damage types flow through `module.damageTypes()` → `HeroModules.damageTypeSpecs()`; run `./gradlew runDatagen --no-daemon` and review the `src/main/generated/` diff.

## 6. Tests (mandatory)

- Golden rows in `src/gametest/resources/golden/` — a hero without a row fails the gate:
  - `hero_presentation.txt` — one line, exactly the `HeroPresentationGameTests.hookLine` format (theme, HUD config, impact style/power, threat class, super jump, melee bleed). Run `./gradlew runGametest --no-daemon` and copy the `actual:` line from the mismatch, or write it in that format.
  - `passive_glyphs.txt` — `<id>|GLYPH,GLYPH,…` matching `getPassiveGlyphs()`.
  - `passive_modifiers.txt` — one row per attribute modifier when the hero declares passives.
  - `transformation_lore.txt` — a row when the hero adds a transformation item (order-sensitive: append in module order).
- `HeroCompletenessGameTests` auto-covers every hero: `getAbilities()` ids must be registered, hero+ability lang must exist in both languages.
- Behavior gets its own `<Id>GameTests` in `src/gametest` for anything non-obvious — gates, denials, cleanup, C2S guards, lifecycle. Helpers: `TestPlayers.join`, `TestHeroes.transform`.

## 7. Adding an ability to an existing hero

Stays inside `hero/<id>/` + resources — **zero shared Java files**:

1. `hero/<id>/ability/<Name>Ability.java` — owns `ID = ModId.of("<new_ability>")`.
2. Alias row in `<Id>Abilities`; `ctx.abilities().register(new <Name>Ability())` in `<Id>Module.register`; the id into `<Id>Hero.getAbilities()` + `getDefaultBinding()`.
3. `en_us.json` + `ru_ru.json`: `ability.superheroes.<new_ability>` + `.desc`; icon `textures/gui/abilities/<new_ability>.png`.
4. Client bits only inside `client/hero/<id>/` (`ctx.abilityDecoration`, HUD, `ctx.receive` for a new S2C payload).
5. Goldens unchanged unless `Hero` hooks moved: `hero_presentation.txt` only if presentation hooks changed; `passive_glyphs.txt` only if glyphs changed. A plain new ability needs no golden edit.

## 8. Verification

- `./gradlew qualityGate --no-daemon` — the only finishing gate (build + JUnit + sanity + gametests + ArchUnit + baseline audit). `./gradlew build -x test` is a mid-work check, not the gate.
- Seam greps for hero `<id>`:
  - `grep -rliE '<id>' src/main/java | grep -v '/hero/<id>/'` → only `bootstrap/HeroModules.java`
  - `grep -rliE '<id>' src/client/java | grep -v '/hero/<id>/'` → only `client/bootstrap/HeroClientModules.java`
  - `grep -rn 'veil' src/main/java` → empty
  Any other hit is an architecture violation — fix before PR.
- Hero with HUD/input/VFX: in-game runClient checklist — see the `testing-runclient-hud` skill.

## Known traps

- Module list order shifts goldens (`transformation_lore.txt` compares the whole file) — regen rows, don't hand-order.
- `OwnedSessionMap` only removes entries — a `clear`/`resetAll` with side effects (withdrawn weapons, released locks, summoned entities, client packets) stays an explicit `ctx.lifecycle()` hook.
- `AbilityRouter` owns the self-cooldown check — an ability never calls `AbilityCooldowns.isOnCooldown` on its own id (checking another ability's is allowed).
- `keepsHeroOnDeath` skips the global untransform — a hero whose death pipeline adapts through death (Doomsday) needs it plus an apply-only `reapplyPassivesAfterRespawn`.
- Edge-detected ability keys need ~500ms holds in runtime tests; `/give` on bound weapons gives Air — bound items come from `BoundWeapons.ensureHeld`.
