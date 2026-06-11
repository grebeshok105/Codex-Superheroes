# HUD v5 Feedback — 12 items (PR continuing from #14 / v3.20.0)

Branch: viktor/hud-v5-feedback (from origin/viktor/hud-v3-reinhard-ram @ 7be67fe)

- [ ] 1. HP bar → hero theme color (like energy) — HeroInfoPanelHud, HeroTheme
- [ ] 2. Remove black square behind radial menu — RadialMenuHud
- [ ] 3. Hide locked/not-unlocked abilities (Doomsday-style) for Regulus, Rem + others — study unlock mechanics
- [ ] 4. Light highlight of active (toggled-on) ability in hero panel — HeroInfoPanelHud
- [ ] 5. Cooldown border thinner + nicer color — AbilityBarHud
- [ ] 6. Bugfix: costume off→on resets all cooldowns — HeroTransformService/AbilityCooldowns
- [ ] 7. Small icons for passives (now empty circles) — HeroInfoPanelHud + textures
- [ ] 8. Radial menu key labels in English (now Cyrillic А,Ч,И,С,М) — RadialMenuHud
- [ ] 9. Doomsday tier label → right corner — DoomsdayGlitchHud / wherever rendered
- [ ] 10. Remove LOCKED system entirely, default hotbar; keybinds may conflict — HotbarLockState, RawKeys, mixins, AbilityBarHud "LOCKED" label
- [ ] 11. Radial menu icons slightly bigger — RadialMenuHud
- [ ] 12. Iron Man: remove Целеуказатель (BoxEspAbility) fully from code

Then: bump version 3.21.0, build (gradle), push, PR to viktor/hud-v3-reinhard-ram? or main? -> PR base = viktor/hud-v3-reinhard-ram (same as PR#14 chain)
