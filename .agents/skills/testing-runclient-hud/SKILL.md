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
