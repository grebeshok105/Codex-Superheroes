---
status: authoring-complete
mc: 1.21.1
loader: fabric
animation_target: EMF
last_verified: 2026-09-30
tags:
  - regulus
  - emf
  - blockbench
  - animation
---
# Regulus EMF animation pack — REPORT

## Summary

Создан master `workbench/regulus_emf_animation_pack/source/Regulus_All_Animations.bbmodel`: 10 animations, 18 groups, 29 cubes, 2 embedded textures, 2170 native keys. Authoring готов; runtime integration отсутствует.
Timings/events/blends: [[10 Analysis/regulus_emf_animation_pack/HANDOFF]].
Plan/audit: [[10 Analysis/regulus_emf_animation_pack/PLAN]].

## Provenance / Audit

Read-only checkout `D:/WorkFlow/codex-superheroes-fresh`, remote `grebeshok105/Codex-Superheroes`.
RegulusHero.java: `SKIN = ModId.of("textures/entity/hero/regulus.png")`; SkinProvider.java default wide model; RegulusClientModule не регистрирует альтернативную skin model.
Regulus bbmodel/geo/JEM и отдельные Regulus clips в просмотренных art-source/runtime assets отсутствуют. Найден `models/item/evangelion.json` с `minecraft:item/generated`; отдельной skeletal Evangelium модели нет.
Skin SHA256: `4d8732e1c94a1d101725a12794251aaffe5f9054011461a6ba7679dff6d993e5`.
Repository LICENSE проверен отдельно; происхождение skin в отдельной asset license не установлено. Работа для authoring данного персонажа, без public release/export в мод.

## Hierarchy / Bind pose

```text
root
  body (pivot 0,12,0)
    head
      mouth_open
    right_shoulder -> right_arm -> right_forearm -> right_hand
    left_shoulder -> left_arm -> left_forearm -> left_hand
  right_leg -> right_shin -> right_foot
  left_leg -> left_shin -> left_foot
```

Существующий вид восстановлен native wide skin model с фактическим Regulus skin. Head/body и original layers сохранены.
Arms разбиты по y=18 и y=14; legs по y=6 и y=2. Joint pivots заданы на cuts. Сохранены source bounds и proportional crops исходных side-face UV; internal caps — authoring geometry. No fingers.
Никаких крыльев/нового внешнего дизайна Evangelium не добавлено.
Имена `snake_case`; full clip prefix `animation.regulus.`. Root translations малы (максимум 0.35 model unit на одной оси), world entity movement отсутствует.

## Textures

- `textures/regulus_skin.png`: оригинальный skin 64×64, embedded в master.
- `textures/regulus_mouth_palette.png`: отдельный 4×4 dark palette для скрытой cavity overlay.
- `mouth_open` scale animated только в Roar; в остальных clips scale=0.
- Дополнительная плашка не является полноценной челюстью. Texture mapping одежды/лица не перерисован.

## Animations / Authored decisions

Точный список и events — [[10 Analysis/regulus_emf_animation_pack/HANDOFF]].
Normal idle — высокомерная, спокойная асимметрия. Active idle — более раскрытая, напряжённая стойка.
Heart — chest gesture; Roar — inhale/back extension и резкий forward release; Mania — широкий правый жест власти; Embrace — левый прицельный захват; Counter — короткая full-body контратака; Evangelium — сжатие/поворот, асимметричное раскрытие и смена стойки; deactivation authored отдельно.
Bezier handles поддерживают непрерывную скорость там, где действие продолжается; rests только в заданных фазах. Loops — phased Catmull-Rom, shoulder/head/arm overlap. Mouth scale — linear.
Основные выражения EMF: `cubicbezier` и `keyframeloop`; native keys являются эквивалентным editable preview.

## Code linkage / Runtime boundary

Code read только для source/model/ability names. Java, PlayerAnimator, EMF integration, mixins, FlightPoseTracker, animation core, abilities, networking, VFX, sounds, HUD не изменялись.
`emf_lab` и authoring JSON содержат ordered variables, per-part expressions, tracks, events, recommendations и validation evidence. Это не runtime JEM.
Функции проверены по official EMF source commit `84df532f70fc58f998795899904826a065cafff2`:
[ASMHelper.java](https://github.com/Traben-0/Entity_Model_Features/blob/84df532f70fc58f998795899904826a065cafff2/src/main/java/traben/entity_model_features/models/animation/math/asm/ASMHelper.java),
[KeyframeloopMethod.java](https://github.com/Traben-0/Entity_Model_Features/blob/84df532f70fc58f998795899904826a065cafff2/src/main/java/traben/entity_model_features/models/animation/math/methods/emf/KeyframeloopMethod.java).
`cubicBezier(t,p0,p1,p2,p3)` фактически использует обычный polynomial control-point order, несмотря на неоднозначную прозу animation docs.
Installed release / Minecraft binding не проверялись.

## Verification / Iterations

Blockbench 5.2.1 desktop / MCP plugin 1.9.3. Применены: get_capabilities, risky_eval native APIs с Undo, set_mode, create_offscreen_view, list_export_formats. Сохранение native project codec, embedded textures, JSON readback.
Первоначальные 9 clips × 2 rounds × 3 ракурса = **54 полных playbacks**; idle в каждом playback проигран дважды. Всего **11217** native preview updates за **185.38s**. Node timers исключили hidden-window throttle; camera offscreen, aspect ratio сохранён.
Превью: `preview/<clip>_review.png`; обзор `preview/regulus_emf_animation_pack_preview.png`.

Исправления:
- Heart опущен от лица к груди; elbow/forearm путь вынесен перед torso.
- Roar head компенсирует наклон body; добавлен скрываемый mouth cavity.
- Mania: более широкий правый gesture.
- Embrace: wrist curl смягчён; исправлен recovery between keys.
- Counter: голова/возврат и guard clearance.
- Evangelium: между ключами forearm проходил через head. Перестроен shoulder/arm путь через наружный боковой arc; фиксировались не только screenshots ключей.
- Deactivation: снятие напряжения в локтях без копии activation backwards.
- Idle: различные фазы shoulder/head/arm и проверка нескольких циклов.

Численные проверки:
- EMF preview expressions vs native на 60fps сетке: max rotation error **0.001006°**, position error **0.00002766 model unit**, scale error около machine epsilon.
- 9 endpoint/loop comparisons: exact pose error **0**. Это сравнение transform values, не обещание runtime blending.
- OBB scan hands/forearms vs head/torso на 60fps: крупные пересечения устранены. Остались малые контакты inflated layers: максимум torso overlap **0.10426 model unit**, Embrace forearm/head **0.08399**. На финальных previews заметного проваливания частей не обнаружено; не заявляется полное zero-overlap всех cube pairs.
- Planted foot floor min drift: максимум **0.06057 model unit** (Counter), без fake locomotion.
- Native key numbers plain decimal; source JSON перечитан, 9 clips/embedded textures/EMF metadata подтверждены.
- `python tools/validate_workbench_asset.py regulus_emf_animation_pack`: OK, без ERROR/WARNING после уточнения names head_skin/torso_skin.
- Saved-source structural check: 9 clips, 18 groups, 29 cubes, 1807 keys, 2 embedded textures, 9 review sheets; native key values finite/plain decimal; EMF companion JSON идентичен embedded metadata; исходный skin byte-for-byte неизменен.
- Repository checkout: `feat/homelander-omp-animations`, HEAD `a90a6f2f31e2d239012afb26e3bf0b64626c68b9`. Итоговый git status совпадает с начальным списком существующих Homelander/mahoraga изменений; эта работа туда не записывала файлы.

## Debris kick — approved rear swing, 2026-09-30

Добавлен `animation.regulus.debris_kick`, 2.20s, once, **363 native keys**. Финальный вариант принят пользователем и сохранён в существующий master; девять прежних animation objects сохранены без изменений. Geometry/outliner/embedded textures идентичны checkpoint.

| Clip | Duration | Events | Blends in/out | Interrupt-safe | Preview |
|---|---:|---|---|---:|---|
| debris_kick | 2.20s once | kick_sweep_time 0.55s (33); impact 0.70s (42) @60fps | 0.11s / 0.17s | 1.60s | debris_kick_review.png; debris_kick_rear_swing_v3.gif |

Дизайн: сильный **задний** замах почти прямой правой ногой (thigh X −78°), затем разгон вперёд до +22.5° за 0.15s. Hip anchor следует за body; руки сначала балансируют, после удара откатываются назад; head/forearms догоняют импульс. Sustain до 1.60s; recovery до 2.20s в N. Root position 0; mouth scale 0. Input `var.regulus_debris_kick_time`; native keys и equivalent EMF `cubicbezier`/radians сохранены в том же schema. Старые `keyframeloop` clips не менялись.

Итерации: удалён драйвер геометрической автокоррекции, который давал ~4.2-unit прыжки всей ноги на первом/последнем кадре. Вместо него — плавная привязка бедра к тазу. Усилен rear swing, исправлен ankle/contact path. Отклонённые drafts не записывались в master.

Финальные проверки:
- Два native visual rounds × front/side/3/4 = **6 полных playbacks**, **807** preview updates за **13.28s**. GIF — 24fps, полный 2.20s clip с тремя ракурсами. Все камеры private offscreen, без изменения пользовательского ракурса.
- Full-path geometry scan **480Hz / 1057 samples**, включая sweep/contact/recovery. Left sole corner drift **4.97e−16** model unit; right hip attachment error максимум **0.01722** model unit.
- OBB right shin/foot vs torso/head и hands/forearms vs head: **0 overlap** на проверенной сетке. Left sleeve/torso: inherited layer contact **0.005174** model unit.
- **Adjacent right thigh/pelvis OBB contact остаётся**: максимум **2.69846** model units на 0.5625s при сильном заднем замахе. Это пересечение кубических объёмов в месте крепления бедра; не заявляется zero-overlap всех leg/torso pairs. Полные результаты сохранены в `emf_lab.validation.debris_kick`.
- Right sole min residual **−0.004914** model unit между 60fps ankle keys; на authored `impact` контакт у пола. Native hip corrections имеют плавный путь, без отрыва/телепортации конечности.
- EMF vs native, 133 samples @60fps: rotation error **0.00037040°**, position **0.000003335** model unit, scale **0**. Start/end vs `combat_idle` phase 0: exact error **0**.
- Saved source readback: **10 clips / 2170 keys / 18 groups / 29 cubes / 2 embedded textures**; embedded `emf_lab` идентичен companion JSON; прежние 9 clips и source geometry/textures unchanged.
- `python tools/validate_workbench_asset.py regulus_emf_animation_pack`: **OK, no issues found**; 11 preview PNGs. Новые runtime/Java/gameplay правки не выполнялись.

Checkpoint: `workbench/regulus_emf_animation_pack/checkpoints/before_debris_kick/`. HANDOFF обновлён через подтверждённый `mcpvault_blockbench`.

## Lessons / Promote

1. Проверять весь путь между authored poses: красивый keyframe не гарантирует clearance при Euler interpolation.
2. Forward/head compensation зависит от rig; для этого source forward = negative Z, Euler = ZYX.
3. EMF mathematical preview/native agreement доказывает curve agreement в lab; не подтверждает EMF parser/model binding в Minecraft.
4. Source of truth один bbmodel; expressions сохранены внутри и в authoring JSON.

Связанные каноны:
- [[30 Solutions/EMF player animation visual authoring]]
- [[40 Codebase/EMF/Blockbench native animation primitives]]
- [[20 Knowledge/BlockBench/MCP workflows]]

Обновить knowledge note о реальном signature cubicBezier и междуключевом clearance, без копирования character-specific clips в общую codebase.
