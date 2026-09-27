---
name: testing-runclient-hud
description: How to runtime-verify heroes/abilities/HUD in this Minecraft Fabric mod via `./gradlew runClient` on a real X display — the dev-client quirks, input workarounds, debug flags, and per-hero checklists learned across the architecture-migration stages.
---

# runClient HUD / ability runtime testing

Applies to Codex-Superheroes (Fabric 1.21.1). Use for any stage that touches input, render, HUD, networking, VFX, or entities — the user's standing rule: «если стадия меняет input, render, HUD, networking, VFX или entities — выполни runtime-проверку; если runClient доступен — пройди runtime checklist».

## Launch / window

- Worktree: always the dedicated `wt-*` worktree, NOT the main checkout. Launch must be its OWN command (fails inside `&&`):
  `cd /home/ubuntu/wt-X && DISPLAY=:0 nohup ./gradlew runClient --no-daemon > /path/boot.log 2>&1 & disown`
- `runClient` = Veil present (default). `runClientNoVeil` exists (separate runDir `build/clientnoveil`); veil-absence is provable via the "recommends veil which is missing" warning + the ResourceManager mod list.
- Window id ~`0x03600007` (check `wmctrl -l | grep -i mine`). Maximize for recording: `wmctrl -i -r 0x03600007 -b add,maximized_vert,maximized_horz` (NOT `xdotool super+Up` — that half-tiles).
- Screenshots: `import -window 0x03600007 /tmp/x.png` (ImageMagick) on the right window — more reliable than the computer-tool screenshot for capturing the exact client frame.
- Kill: `pkill -f "wt-X.*KnotClient|wt-X.*gradlew"`. Runtime log: `wt-X/run/logs/latest.log`.
- First join of a fresh world takes ~10-15s; dev username is loom-randomized per launch (Player96/Player715/etc.) BUT the underlying .dat persists — transform/state relog correctly across relaunches (same player).

## Input quirks (IMPORTANT — the dev client is hostile to synthetic input)

- **Ability-slot keys are edge-detected per tick** — a quick tap is dropped. Hold the key ~500-600ms (`xdotool keydown z && sleep 0.6 && xdotool keyup z`). Reinhard order: Z draw, X air_slash, C sword_wave, V counter_riposte, B divine_aura, 3 speed_judgment, 4 judgment_mark, 5 wish.
- **Mouse-look is broken.** `xdotool mousemove`/`mousemove_relative` do NOT rotate the camera. The `computer` tool's `mouse_move`/`left_click` DO rotate but accumulate pitch to max-down 90° and it gets STUCK there. Fix rotation via chat command `/tp @s ~ ~ ~ <yaw> 0` (pitch 0 = level). `/data modify entity @s Rotation` fails ("Unable to modify player") — `/tp` is the way.
- **Mouse clicks don't reliably attack.** `xdotool click 1` swings the arm animation but deals NO damage (the attack packet isn't sent). `computer` `left_click` DOES attack but rotates the camera first, and clicks landing on the ability panel hit the tooltip not the world. **Best melee fix: rebind attack to a keyboard key** — quit, `sed -i 's|key_key.attack:key.mouse.left|key_key.attack:key.keyboard.p|' run/options.txt`, relaunch, then `xdotool key p` attacks with NO camera rotation. Pick 'p' (unused); do NOT use 'g' — it's already bound to `superheroes:super_jump` (conflict). Restore `key_key.attack:key.mouse.left` after.
- Chat commands: `xdotool key t`, `xdotool type --delay 15 "/cmd"`, `xdotool key Return`. Verify each via the log — stray Escape/Game Menu eats commands. `/data get entity @s "fabric:attachments"."superheroes:<key>"` reads persisted state (quote the inner key with `\"`).
- `positioned ^ ^ ^N` / `summon ^ ^ ^N` uses the CURRENT look-ray — with pitch 90 mobs spawn underground. Set pitch 0 first.
- `NoAI` does NOT stop gravity — pin test mobs with `{NoAI:1b,NoGravity:1b,Tags:["t"]}` or they fall/despawn. Tag them to re-query reliably.
- The ability panel is a HUD overlay — H cycles full→compact→hidden; Escape opens Game Menu, doesn't close it. F1 hides vanilla HUD but the mod panel persists. The panel covers the right ~60% of view — aim test mobs into the clear left third.
- First-person is often unreachable (clients get stuck cycling third-person after a transform; F5 never returns to FP despite modifier-key resets / gamemode / respawn / relaunch). A dev-client camera-state quirk, not a mod bug — don't fake a first-person screenshot.

## Debug / state flags

- **Mob-targeting debug:** abilities that only target players need `/superheroes debug mob-targets on` (`AdminAbilityDebug.playerOnlyAbilitiesTargetMobs`) to hit mobs — e.g. speed_judgment's `findFastestDebugMobTarget`. A MOVING mob is required (`no_target` msg otherwise).
- **Wish damage-immunity:** `adapted_damage_types` grants LIVE immunity — `/damage @s N <wished-type>` is REFUSED ("invulnerable") afterward. `recent_damage_types` also blocks recently-taken types. Use a type not in either list (`minecraft:mob_attack`, `minecraft:cactus` worked; cycle if refused).
- **Reinhard gate:** `ReinhardSwordDrawGateController.recordHit` needs ONE attacker to deal ≥30 dmg in 600t → READY (Worthy! + chime) → Z consumes → ceremony. READY is a transient flag set on the crossing hit — a persisted `worthy_accumulated=30` alone does NOT re-arm it after relog; deal a fresh 30 hit. Gate is gated by `!swordDrawn && attacker!=player` → must `/damage @s 30 <type> by @e[<mob>]`.
- **`/give` on bound weapons gives `[Air]`** — bound items need `BoundWeapons.ensureHeld` (bound_weapon token); unbound copies are discarded by `discardIfInvalid` in inventoryTick. The royal_icicle (Reid) is correctly obtained ONLY via the sword-draw ceremony — `/give` is the wrong path.
- Energy gate: `AbilityRouter` blocks if `energy < cost + reserve`. judgment_mark costs 220; energy regens to 1000. sword_draw toggle drains 1.5/tick → auto-sheathes on depletion.
- Raycast abilities (judgment_mark, spear) need the mob ON the crosshair ray (cone ~37°). Summon at `^ ^ ^2.5` dead-ahead at pitch 0; terrain-clip causes silent misses (`hit=null`).
- Untransform = **Shift + right-click** the transform item (`isShiftKeyDown` branch), not plain use.

## Instrumentation pattern

- For invisible lifecycle cleanup (lock release, session drop, mark detonation), add a temporary `System.out.println("[TAG] ...")` in the worktree source, prove the path, then `git checkout` to revert — `git status` must end clean. One println at the shared choke point (e.g. `OwnedSessionMap.removeOwnedBy`, `ReinhardTimeSlowController.releaseSlow`/`onPlayerGone`, raycast `hit=`) catches all callers.

## Per-hero quick reference

- **Reinhard** (`/superheroes hero superheroes:reinhard`): state attachment `superheroes:reinhard_state` (persistent+copyOnDeath) — fields sword_drawn, wishes_used, adapted_damage_types, judgment_target+expire, phoenix_used, in_second_coming, worthy_accumulated_damage, recent_damage_types, riposte_expire_tick, last_wish_tick, last_insta_regen_tick. Survives death AND relog.
- Ability cooldowns (ticks): air_slash 30, sword_wave 60, counter_riposte 240, speed_judgment 160, judgment_mark 200, wish 600; divine_aura/sword_draw = toggles.
- Icicle empowered branches (`inSecondComing`→1000+cleave7, `swordDrawn`→bonus+cleave5+blind10) are transient-state-gated — persisted flags alone may not fire them live; don't treat their absence as regression without a baseline diff.

## Deliverable

- Per-item PASSED/FAILED/UNTESTED + evidence paths + a suggested PR comment (embedded screenshots via `![x](/abs/path.png)`, collapsible evidence). Never fake PASS — flag environment-blocked items honestly as UNTESTED or PASSED-bounded with the caveat.

## Launch

- The box runs a real X desktop (KDE Plasma) on `DISPLAY=:0` — no xvfb needed; the game window opens on the desktop.
- `cd <checkout> && DISPLAY=:0 ./gradlew runClient --no-daemon` as a background shell. First boot reaches title in ~60-120s (loom deps are cached in ~/.gradle, shared across checkouts/worktrees).
- Poll for the window: `DISPLAY=:0 wmctrl -l | grep -i minecraft`, then `wmctrl -i -r <wid> -b add,maximized_vert,maximized_horz` for a deterministic 1600x1127 geometry.
- Screenshots: `DISPLAY=:0 import -window <wid> out.png` captures only the game window (better than scrot -u for cross-build diffing).
- Keyboard/mouse work via the computer tool; `xdotool mousedown 3`/`mouseup 3` is the reliable way to hold RMB (e.g. melee-charge gauge).

## Deterministic world for HUD screenshots

1. Singleplayer → Create New World → name it → Game Mode Creative → **Allow Commands: ON** (button is easy to miss; a difficulty tooltip overlaps it — hover first) → World tab → World Type: Superflat.
2. In-world: `/time set noon`, `/gamerule doDaylightCycle false`, `/weather clear`, `/gamerule doWeatherCycle false`, `/effect give @s minecraft:speed infinite 0 true` (makes the top-right effects icons render).
3. Kill the tutorial toast BEFORE anything top-right — it covers the mod's "HUD" pause-menu button and the effects icons and never times out: quit, set `tutorialStep:none` in `run/options.txt`, relaunch. (GUI Options → Video Settings has no tutorial toggle.)
4. `/superheroes hero superheroes:<id>` (e.g. `homelander`, `iron_man`, `reinhard`, `regulus`) needs perm 2 — cheats-on singleplayer is enough.

## HUD editor specifics

- Open: Esc pause → purple "HUD" NeonButton top-right → "HUD Editor" (7 movable cards + Reset/Done + icon-style toggle).
- At the default GUI Scale (Auto→4 here) the cards are enormous and overlap: `hotbar` sits fully *inside* `chat`'s rect — a missing movable there is invisible in screenshots. Set **GUI Scale: 1** to separate all 7 cards for verification.
- Cards top-to-bottom hit order = reverse ELEMENTS order (tooltips … hero_panel). To grab a buried card, drag covering cards away first or pick a point outside the overlapping later-drawn cards; verify what you grabbed by reading `run/config/superheroes-hud-layout.json` (the dragged layoutId gets a `{x,y}` entry; Reset writes `{}`).
- Drag writes offsets live; they persist via that JSON and reload on boot — restart the client to test persistence.

## Cross-build screenshot parity

- Put both checkouts on the SAME run dir (e.g. `ln -sfn <main>/run <worktree>/run`) → identical options.txt (guiScale, keybinds), world save, hero-attachment state. Remember the worktree may lack `run/` entirely until first boot; remove the symlink afterwards.
- Pixel diffs of hero HUDs are dominated by animation (3D model pose, icon sweeps, world bg) — compare structurally (element set/anchors) or via the editor at GUI Scale 1, which is near pixel-identical when the bounds math matches.
- `compare -metric AE a.png b.png diff.png` works; AE counts animated content too, so use it for screens (editor/pause), not animated HUD regions.

## Hero abilities / keybinds (ModKeys)

- Radial wheel: hold **R** — `xdotool keydown r && sleep 1.5 && import ... && xdotool keyup r` captures it mid-bloom (wheel renders at z-order 700, UNDER the big abilities tooltip, so for heroes with many abilities only the arc/segment edges peek out — that is normal, not a bug).
- Super jump: single **G** press sends SuperJumpC2SPayload; server gates on `hero.canSuperJump()` (Regulus/Reinhard/Kratos/Thanos/Naruto/Doomsday true; others default false).
- Ambiguous "did it launch" shots: press **F3** first — `XYZ:` line gives exact Y. Super jump = JUMP_VELOCITY 2.7 → reads ~Y -47 at 0.4s, ~-32 at 1.4s from superflat ground -62. Grounded denial stays exactly `Y -62.00000`. Way more reliable than eyeballing the flat-world horizon.
- Other binds: H=tooltips toggle, comma=bindings screen, F8=VFX settings, F=raiden sword draw, N=nano weapon, K=ESP, Z/X/C/V/B/3/4/5=ability slots.
- Ability slots are edge-detected per client tick (`RawKeys.pressed`: `down && !was`, polled once per 50ms tick, gated by `screen==null && getOverlay()==null`). A quick `xdotool key x` tap (~30ms) falls between ticks and is silently dropped — ALWAYS fire slots with a hold: `xdotool keydown <k> && sleep 0.4 && xdotool keyup <k>`. If a held press still whiffs, check server-side refuse paths before retrying: energy (`/superheroes energy 150`), active cooldown, `canActivate` gates (e.g. scorpion breath blocks while `isBreathing`).
- Ability activation is proven by the tooltip row's cooldown label (e.g. "7s") plus FX/effect icons — a row staying READY means `tryActivate` never ran (refused or input dropped), not "fired with no effect".
- Scorpion sanity in superflat: the 22m-cone spear prefers ~any living non-player entity (pigs count) and flings/kills targets out of loaded chunks — "No entity found" after a summon usually means the spear killed/flung it, not a despawn. Use an invulnerable armor stand (`{Invulnerable:1b}`) as a control target.
- Hellport (and any `SafeTeleport.clamp` consumer) can report a successful cast (cooldown+FX) with ZERO displacement: `noCollision` includes entity collisions, so the caster's own box blocks the first sample step and `clamp` returns `from`. Ground-aimed blinks also self-block because `dest = hit - 1.5y` lands inside terrain. Before calling this a regression, `git diff origin/main` the ability + SafeTeleport — byte-identical means pre-existing dead-blink, not a stage bug.
- Tooltip pixel-diffing: hover the item at a FIXED mouse coordinate (tooltip anchors to cursor) on both builds, then crop and compare the TEXT region — don't trust a raw AE count. The vanilla tooltip box fill is only ~94% opaque, so a different animated background behind (inventory paper-doll pose, particles) produces thousands of sub-visible (<1% RGB) diffs that look like content drift. Decisive check: `convert a.png b.png -compose difference -composite d.png` then measure `d.png` max/percent delta — content bugs produce >5% deltas; bleed stays ~2/255. Side-by-side visual + delta magnitude = proof; AE count alone is misleading.

## Old-vs-new build matrix

- For a baseline build use a detached worktree pinned to the merge-base (`git worktree add ../wt-<stage>-main <sha>`), compile it once (`./gradlew compileJava compileClientJava`) before launching, and symlink its `run/` to the main checkout's `run/` for identical options/world/config.
- Sequence: boot OLD → all captures → quit client fully → boot NEW → identical captures → `compare -metric AE` + side-by-sides (`convert a b -resize 800x +append`).

## Notes for this repo

- `hud.superheroes.edit.*` lang keys label the editor ("HUD", "HUD Editor", Reset, Done, card names).
- HudLayoutManager persists to `run/config/superheroes-hud-layout.json`; `superheroes-client.json` holds iconStyle/vfxMode (default ROUND).
- Chat + effects cards have no render layer of their own — mixins (`ChatComponentMixin`, `GuiEffectsMixin`) shift vanilla drawing by `HudLayoutManager.offset`; the editor still shows their cards via movable-only registry entries.
- Melee charge: hero active + empty main hand + hold RMB (keyUse) → "Charged Strike: Tier N" label + vertical gauge right of crosshair.

## Server-tick / module-refactor verification (D2b-style)

- **Tick-order proof without gameplay:** when the ordering-sensitive path can't be exercised live (e.g. Unibeam's `anchorPlayer` teleports every tick → a real landing-during-channel is structurally impossible), instrument BOTH the writer and reader in the same tick: `println` at the end of the producing tick (`[X] writer.tick t=<serverTicks>`) and at the top of the consuming tick (`[X] reader.tick t=<serverTicks> busy=<freshRead>`). Consecutive same-`t` lines prove ordering + a fresh read. Revert all println files before finishing.
- **`/data get entity @s abilities` is the unambiguous tick-alive check.** A status/effect granted by a moved tick (e.g. `VanityAuthority.applyToCaster` → `mayfly`) is indistinguishable from creative-mode mayfly. Switch to `/gamemode survival` (gamemode change resets `mayfly:0b`), re-read: `mayfly:1b` in survival can only come from the tick. `hasActiveHouse`/map-membership alone is NOT proof (the entry persists even if the tick never runs).
- **Ability slot order comes from `Hero.getAbilities()`, not the plan's guess** — invincible's Z is `FLIGHT`, `VILTRUMITE_CHARGE` is slot-1 (X). If a key fires the wrong ability id, read the hero's list order before assuming a bug.
- **The client relaunch truncates your stdout redirect.** Earlier evidence (unibeam channel, landing impacts, tier progression) survives in Minecraft's own rotation: `zcat run/logs/debug-*.log.gz run/logs/2026-*.log.gz | grep -a ...`. `latest.log`+`debug.log` = current boot; merge the gz archives into one searchable file.
- **Ability-input gotchas that silently eat a test:** commands typed on the death screen are dropped — click **Respawn** (~784,700 tool-px) before typing; `/damage` and `hurt` report "invulnerable" in creative — `/gamemode survival` first; Doomsday tier-gates block lower-tier casts (`tryActivate=true` only at the right tier) and death resets energy to 0 — `/superheroes energy 200` after respawn before casting.
- **Choke-points to instrument once for many receivers/activations:** `CoreClientContext.receive` logs every module-moved S2C receiver (`[RX] <type> via <hero>`); `AbilityRouter.activate` logs every C2S activate + which gate rejected it — one println each covers the whole hero set.

## Moved keybinds / client-input verification (CL3b-style)

- **A dead bind on a CONFLICTING key is usually vanilla, not a mod bug.** Vanilla `KeyMapping.click()` dispatches a press to ONE mapping per key via `MAP.get(boundKey)` — `resetMapping()` rebuilds MAP from `ALL.values()` (a HashMap keyed by the translation-key STRING, so iteration order is registration-independent). Two binds on the same key → only the HashMap-late one drains `consumeClick`; the other is dead forever. `key.superheroes.raiden_sword_draw` defaults to F, which `key_key.swapOffhand` wins — the default raiden bind has never worked by keypress on ANY build (only ability wheel). Proof: `javap -c` the mapped jar `KeyMapping.class` for the single-`MAP.get` dispatch; verify structurally identical on merge-base (same translation string → same ALL set → same winner) instead of booting the baseline.
- **`consumeClick` counts each key-press event — X auto-repeat produces a SECOND click ~0.5s into a hold.** Toggle binds (raiden draw/sheath, ESP mode) double-fire on a 0.4-0.6s held press → draw+sheath in one "press" → looks like nothing happened. Use ~0.2-0.25s holds for toggles.
- **Instrument the drain loop once to prove EVERY moved bind.** `HeroActionKeys.installTick`'s `while (entry.mapping().consumeClick()) { ... }` — println `key=<name> hero=<currentHero> owner=<entryHero> gated=<bool>` covers all 3+ moved actionKeys at once: gated=true = press fired as owner; gated=false = press consumed-but-suppressed for non-owner (the airtight negative proof).
- **The hero ability panel covers the center-right ~2/3 of the 1600x1200 frame — world entities/ESP render BEHIND it.** Spawned test entities keep landing hidden. Get world-rendered visuals in the panel-free left ~25% strip, look at sky, or put the entity dead-center via fixed coords + `/tp @s x y z <yaw> <pitch>` (yaw 0 faces +Z/south, 180 faces -Z/north).
- **ESP (IronManEspRenderer) only boxes transformed hero players (wallhack) + hostile `Enemy`/angry-golems in FOV+LOS** — peaceful mobs (pig) never get a box; summon a zombie. The floating label `NAME / HOSTILE // Nm HP%` (font.drawInBatch) is easier to spot than the thin red box edges.
- **Key Binds screen: a row must be SELECTED before Play/Bind commits** — single-click highlights, click the `[ key ]` button to enter rebind, press the new key. The Options→Controls→Key Binds path is Escape → "Options..." (~520,737) → "Controls..." (~1170,594) → "Key Binds" (~1170,231), then scroll ~80-90 clicks of `click 5` to reach the Superheroes section.

## Server-computed ability visibility (C4-style)

- **One println proves every hero's gate.** `AbilityAvailabilitySync.tickPlayer` recomputes `AbilityAvailability{entries: map of only NON-AVAILABLE ids→Visibility}` each PLAYERS tick and writes the attachment only on change — `println("[V] hero=" + data.heroId() + " nonVisible=" + next.entries())` inside the write branch logs the exact hidden/locked set per transition. Empty `{}` = whole list visible; an id ABSENT from the map = it became AVAILABLE. The client `ClientAbilityVisibility` keeps only AVAILABLE (HIDDEN+LOCKED both filtered); absent attachment → all-visible fail-open (≤1 tick window).
- **Slot keys read the visibility-filtered list** (`SuperheroesClient`: `ClientAbilityVisibility.visible().get(i)` → Z/X/C/V/B). So a hero's slot→ability mapping CHANGES as gates open: pandora outside has only `mirror_dimension` = Z; inside the House all 5 are visible.
- **Gives/effects that drive the gates** (all solo-reachable):
  - Doomsday tiers: `/superheroes doomsday tier <1-7>` — no need to die; tier1=all hidden, +smash@2,roar@3,bone_spike@4,charge_tackle@5,berserk@6,doom_grip@7.
  - Thanos stones: gauntlet stones live in `CUSTOM_DATA.InsertedStones` (a list of "power"/"space"/"reality"/"soul"/"time"/"mind"). `/give` REQUIRES component syntax — `/give @s superheroes:infinity_gauntlet[minecraft:custom_data={InsertedStones:["power"]}]`; the bare `{...}` NBT shorthand fails "Expected whitespace to end one argument" and silently gives nothing. snap needs ≥6.
  - Pandora: Z casts `mirror_dimension` → opens/closes House (`MirrorDimensionController.SESSIONS` toggles); the 4 house-keyed abilities gate on `hasActiveHouse`.
  - Rem demonism: toggle needs 100% charge (passive ~0.45/s is slow). **Fast path: `ALLOW_DEATH → tryDeathSave` auto-triggers PERMANENT demonism on lethal damage** — `/gamemode survival` + `/damage @s 100` → demon form + demon-only abilities + `rem_oni_rage` self-hides. `removePassives`/`clear` resets charge to 0 on transform.
  - Regulus madness (counter_strike gate): `/item replace entity @s weapon.mainhand with superheroes:evangelion` + hold RMB ~0.6s → `startReading` → 200 ticks → `finishReading` sets `REGULUS_MADNESS.madness` → counter_strike appears. **Careful: `infinity_gauntlet` is ALSO a transform item** — right-clicking it while holding it transforms you into thanos; use `/item replace ... weapon.mainhand` to force the evangelion into hand before clicking, never rely on hotbar order.
  - Vanity-strip = the `superheroes:vanity_stripped` MOB EFFECT (`/effect give @s superheroes:vanity_stripped 60`) — `isVanityStripped` → every ability HIDDEN regardless of hero/madness.
- **Transforms write the attachment same-tick** — every `/superheroes hero` immediately produced a `[V]` line with the new hero's gate set; no perceptible flash of the full list in any capture (the fail-open window is bounded ≤1 tick by the per-tick sync).

## Lifecycle reset verification (D2b-2-style: leave/death/respawn release)

- **Instrument the reset choke-points once to cover every lock-holder + session map.** Two printlns in `EntityControlLock` cover all lock-based cleanup: `release()` logs `[X-UNLOCK] <entity> kind=<NO_AI|NO_GRAVITY|INVULNERABLE> owner=<id>` (per-entity unlock) and `releaseOwnedBy()` logs `[X-RELEASE-OWNED] owner=<p> held=<N>` (an owner's HELD_LOCKS dropped). `held=<N>` is the leak meter — a large N on death means many owned entity-locks were correctly released in one shot (timeslow's ~59 frozen-mob locks dropped on the owner's death).
- **Controllers with release obligations keep static maps + explicit onLeave/onDeath→`onPlayerGone`/`clear` hooks** (ReinhardTimeSlow, DoomGrip, MirrorDimension). Instrument each hook head (`[X-GONE] hadSlow=<bool>` / `hadGrip=<bool>` / `ownedHouse=<bool>`): a `true` on death/leave proves the hook ran with live state to release; `false` just means nothing was active. OwnedSessionMap-driven state (unibeam charging/firing) drops silently per its ClearOn policy — prove it behaviorally (die mid-channel → clean respawn, no stuck state) since there's no hook to log.
- **Death-save heroes intercept `/kill`** — plan the "die while ability active" shot around it. Reinhard `Second Coming` revives ONCE per session (`phoenixUsed`); the first `/kill` revives, the SECOND is a real death (fires onDeath→onPlayerGone→releaseSlow). Pandora literally `never dies` (ALLOW_DEATH→deny comment) — `/kill` returns "Killed" but she's alive; her house simply can't trigger a death path while she's pandora. Test pandora's house cleanup on the LEAVE path instead.
- **`ServerPlayerEvents.LEAVE` fires on quit-to-title in singleplayer** — Save and Quit runs the onLeave hook chain for the local player (house closes, locks drop) BEFORE the server-stop resetAll. So "leave" is testable solo: open the house → quit → `[X-HOUSE-GONE] ownedHouse=true` → rejoin → no auto-reopen, clean re-open.
- **Summon targets that SURVIVE to the kill moment.** Grip/timeslow drains + throws kill low-HP mobs before you can die — a 16hp spider dies to the first 18-dmg grip-tick and releases early (looks like "grip didn't work"). Use `{Health:900f,PersistenceRequired:1b,attributes:[{id:"minecraft:generic.max_health",base:900}]}`. Zombies still BURN in daylight (PersistenceRequired only stops despawn) — prefer spider/husk, or high HP to outlive the burn.
- **speed_judgment's debug-mob target must be MOVING** (`findFastestDebugMobTarget` scores deltaMovement > 0.03/tick) — a freshly-summoned stationary mob scores ~0 → `no_target`. Use a hostile that CHASES you: `/gamemode survival` + summon a zombie within its 50-block RADIUS → it pursues → speedScore>0 → judgment triggers timeslow → `freezeAround` locks NO_AI+NO_GRAVITY on everything in the 80-block FREEZE_RADIUS.
- **A stray `Escape` opens the Game Menu and swallows your next command** — `/kill` typed while the menu is up is silently lost. After any Escape, screenshot to confirm you're back in-world before typing `/kill`; click "Back to Game" (~800,380) to dismiss.

## Two-client LAN / skin-resolution verification (CL4-style: owner view vs observer view)

- **Why a second client:** `AbstractClientPlayerSkinMixin.getSkin`/`PlayerRendererMixin.renderHand` use `SkinResolver` per-UUID — an observer sees the owner's skin independently of the owner's own F5/self view. Plan requires an Observer over LAN for "вид других игроков"; own third-person F5 is NOT the same render path.
- **Second client via a temporary Loom run-config (NOT committed):** add inside `loom { runs { ... } }`:
  ```
  client2 {
      inherit client
      name "Observer Client"
      programArgs '--username', 'Observer'
      runDir "run-observer"
  }
  ```
  Launch: `DISPLAY=:0 nohup ./gradlew runClient2 --no-daemon > /tmp/observer.log 2>&1 & disown` — **nohup+disown is required**; a plain `&` dies with the one-shot shell. Revert the build.gradle block when done (`git diff build.gradle` must be empty).
- **Join LAN:** on the owner, Esc → "Open to LAN" → Start LAN World. On the observer: Multiplayer → Proceed → it auto-scans and lists "LAN World / <owner>" → **double-click the entry** (single "Join Server" clicks don't register). The observer spawns as a separate player named `Observer`.
- **Owner drives the observer's hero without it pressing keys:** `/execute as Observer run superheroes hero superheroes:<id>` — transforms the observer remotely (logged "Transformed Observer into ..."). Same for energy/effects.
- **Input/screenshot discipline with two windows:** `xdotool windowactivate --sync <winid>` before every keystroke; the observer window's title is "Observer Client". Screenshots: `import -window <winid>` captures window-local pixels. Find winids via `wmctrl -l` (owner vs observer differ by title).
- **F3+P on EACH client** disables pause-on-lost-focus — without it, switching focus between the two windows throws a stray Game Menu that eats the next command.
- **`RawKeys.pressed` (ability slots Z/X/C/V/B/3/4/5) is edge-detected per tick AND gated by `screen==null && getOverlay()==null`** — a quick `key 4` tap falls between ticks and is missed. **Hold the key ~500ms** (`keydown 4; sleep 0.5; keyup 4`) so a tick catches the press. IronMan suit_switch = "4" (cycled to STEALTH on the observer this way).
- **KNOWN BLOCKER — first-person unreachable on the dev clients:** in this session BOTH clients got stuck cycling third-person views and `F5` never returned to first-person (verified via keyup-modifiers, gamemode spectator→survival toggle, kill+respawn, and a fresh relaunch — a fresh client IS first-person right after join but drifts to third-person after a hero transform). The first-person ARM via `renderHand` was still proven for thanos (armored fist, not vanilla arm), but a specific hero's first-person arm shot may be uncapturable — report it as environment-blocked, not a code bug. `renderHand` returns null for non-local players, so the arm can only be seen on the owning client anyway.
- **Skin/hook evidence sources:** `SkinProvider.skin()` = body texture (observer + F5 both use it); `handSkin()` = first-person arm; `slimModel()` = model. The sung phase-1↔phase-2 divergence is `skin()=hasShadows?PHASE2:null` (body) vs `handSkin()=isPhase2?PHASE2:null` (hand) — intentional, `isPhase2` is server-synced via `ClientShadowArmyState.update`.
