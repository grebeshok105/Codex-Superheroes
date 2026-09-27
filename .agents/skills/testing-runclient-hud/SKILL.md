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
