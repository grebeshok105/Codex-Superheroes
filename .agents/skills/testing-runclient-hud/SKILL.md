---
name: testing-runclient-hud
description: How to run the Codex-Superheroes Minecraft client on this box and get deterministic HUD/screenshot evidence (runClient, world setup, HUD editor, cross-build comparison).
---

# Runtime-testing the client (runClient)

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
