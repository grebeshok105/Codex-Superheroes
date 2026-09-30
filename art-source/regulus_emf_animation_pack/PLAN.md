# Regulus EMF animation pack — authoring plan

Дата: 2026-09-30. Target: Fabric 1.21.1, EMF; runtime integration запрещена заданием.

## Audit

Repository checkout: `D:/WorkFlow/codex-superheroes-fresh`, remote `grebeshok105/Codex-Superheroes`.
`RegulusHero.SKIN` указывает на `src/main/resources/assets/superheroes/textures/entity/hero/regulus.png`. Отдельных Regulus bbmodel/geo/JEM и Regulus clips в art-source/player_animations не найдено. `SkinResolver`/`SkinProvider`: default wide vanilla player. Evangelium в коде назван Evangelion: generated item, без отдельного skeletal model. Поэтому использовать существующие vanilla player proportions с настоящим Regulus skin; аккуратно сегментировать локти/колени в authoring copy. Не копировать чужого персонажа.

## Steps

1. Через native Blockbench MCP создать skin-derived rig Regulus, сохранить UV/bind pose и embedded original texture. Source живёт в `workbench/regulus_emf_animation_pack/source/Regulus_All_Animations.bbmodel`.
2. Создать calm asymmetric combat idle (4s), Lion Heart (1.6s), Roar (2.6s), Mania (2.1s), Embrace (2.2s), Counter (0.9s), Evangelium activation (3.4s), active idle (4s), deactivation (2s), в указанном порядке.
3. EMF expressions — authoring source для curves; native timeline представляет математический preview. Сохранить curve source/EMF expressions в custom `emf_lab` и отдельном authoring JSON, без player.jem/runtime edits. Использовать проверенные signatures официальной EMF docs; не предполагать version по исторической заметке.
4. Разные силуэты: Heart — короткий chest/self gesture; Roar — грудь/голова и breath/release; Mania — широкий одноручный приказ; Embrace — прицельное вытягивание/сжатие; Counter — короткий pivot/удар; Evangelium — смена всей стойки.
5. Для каждого clip native playback front/side/3/4, key moments + correction/replay. Loop boundary и переходы ordinary↔active idle; не выдавать screenshots за gameplay verification.
6. Сохранить один master, timings/blends/interrupt moments в [[10 Analysis/regulus_emf_animation_pack/HANDOFF]], evidence в [[10 Analysis/regulus_emf_animation_pack/REPORT]]. Native JSON roundtrip + lab validator.

## Limits

Game не запускать. Репозиторий используется read-only, текущие Homelander modifications не затрагивать. EMF part mapping, state/time driver и axis/translation verification в Minecraft остаются задачей отдельного implementation agent.

## Related

- [[30 Solutions/EMF player animation visual authoring]]
- [[40 Codebase/EMF/Blockbench native animation primitives]]