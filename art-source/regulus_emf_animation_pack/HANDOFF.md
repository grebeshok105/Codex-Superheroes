---
status: authoring-complete
mc: 1.21.1
loader: fabric
animation_target: EMF
last_verified: 2026-09-30
tags:
  - regulus
  - emf
  - animation
  - handoff
---
# Regulus — EMF animation pack handoff

Дата: 2026-09-30. Target: Minecraft 1.21.1 / Fabric / EMF.
Source: `D:/WorkFlow/BlockBench 3d models/workbench/regulus_emf_animation_pack/source/Regulus_All_Animations.bbmodel`.
EMF source: `workbench/regulus_emf_animation_pack/export/Regulus_EMF_Authoring.json`; та же информация встроена в source как `emf_lab`.
Риг, текстуры и все 10 clips находятся в одном master. Полные animation IDs имеют префикс `animation.regulus.`.

| Animation | Duration | Loop | Important timestamps | Notes |
|---|---:|---|---|---|
| combat_idle | 4.00s | loop | — | Спокойная асимметрия; исходная обычная стойка N |
| lion_heart_activation | 1.60s | once | activation_time 0.70s; activation_frame 42 @60fps | Короткий жест правой рукой к груди |
| lion_roar | 2.60s | once | start 0.82s; peak 0.98s; end 1.62s | Breath/отведение → release → живой sustain → recovery; рот — cavity overlay |
| debris_kick | 2.20s | once | kick_sweep_time 0.55s; impact 0.70s | Сильный задний замах правой ногой → удар вперёд по земле; бедро следует за тазом; левая стопа planted; recovery в N |
| mania_of_greed_cast | 2.10s | once | effect_trigger_time 0.94s | Широкий приказ правой рукой, поворот корпуса |
| greeds_embrace_cast | 2.20s | once | acquire 0.66s; effect 0.90s; release 1.62s | Прицельный жест левой рукой и закрытие кисти/предплечья |
| counter_attack | 0.90s | once | counter_impact_time 0.32s | Ready до 0.18s; release 0.18–0.32s; возврат в active stance A |
| evangelium_activation | 3.40s | once | begin 0.32s; major 1.66s; complete 3.00s | Сжатие/поворот → асимметричное раскрытие → settling в A |
| evangelium_active_idle | 4.00s | loop | — | Более напряжённая, раскрытая стойка A; breathing/overlap |
| evangelium_deactivation | 2.00s | once | state_end_time 0.96s | Отдельно authored, не reverse; A → снятие напряжения → N |

## Integration events

- `lion_heart_activation`: trigger **0.70s** (frame **42** при 60fps).
- `lion_roar`: roar start **0.82s**, peak **0.98s**, end **1.62s**.
- `mania_of_greed_cast`: effect trigger **0.94s**.
- `greeds_embrace_cast`: acquire target **0.66s**, effect trigger **0.90s**, release **1.62s**.
- `counter_attack`: counter impact **0.32s**.
- `debris_kick` («дробь», замена roar для будущего ability mapping): `kick_sweep_time` **0.55s**, frame **33**; `impact` **0.70s**, frame **42**. К этим authored events implementation agent привязывает залп; текущий runtime не изменён.
- `evangelium_activation`: activation begin **0.32s**, major visual trigger **1.66s**, transformation complete **3.00s**; последние 0.40s — settling.
- `evangelium_deactivation`: state end **0.96s**.

Секунды от начала clip — канон. Native timeline snapping = 60fps, отдельные keys/timestamps могут быть subframe. Это authored event map, не текущие balance/gameplay timings.

## Blends и interruption

| Clip | Blend-in | Blend-out | Рекомендуемый interrupt-safe момент |
|---|---:|---:|---|
| combat_idle | 0.18s | 0.12s | Любой, через blend |
| lion_heart_activation | 0.10s | 0.16s | 1.08s |
| lion_roar | 0.12s | 0.18s | 1.84s |
| mania_of_greed_cast | 0.12s | 0.18s | 1.42s |
| greeds_embrace_cast | 0.12s | 0.18s | 1.62s |
| counter_attack | 0.06s | 0.10s | 0.50s |
| debris_kick | 0.11s | 0.17s | 1.60s |
| evangelium_activation | 0.18s | 0.22s | 3.00s |
| evangelium_active_idle | 0.22s | 0.16s | Любой, через blend |
| evangelium_deactivation | 0.16s | 0.18s | 1.20s |

Interrupt-safe здесь означает визуально подходящее место для blend; отмена эффектов/authority — решение будущего implementation agent. При завершении обычных casts переходить в `combat_idle`; после activation/counter — в `evangelium_active_idle`; после deactivation — в `combat_idle`. Конечные позы совпадают с фазой 0 соответствующих idle. На произвольной фазе idle использовать указанный blend.

## Debris kick — animation contract

- ID: `animation.regulus.debris_kick`; input: `var.regulus_debris_kick_time`.
- 0.00–0.55s: сильный замах **сзади**, почти выпрямленная правая нога; thigh X достигает −78°.
- 0.55–0.70s: большой быстрый задний → передний swing до +22.5°, дуга 100.5°; ударный контакт ровно на 0.70s.
- 0.70–1.60s: резкая остановка ноги, импульс корпуса/откат рук, затем head/forearm overlap и settle.
- 1.60–2.20s: плавный recovery в `combat_idle`, phase 0; start/end transform error 0.
- Root position постоянно `[0,0,0]`; left support chain сохраняет N, drift подошвы < 0.000001 model unit; `mouth_open` scale `[0,0,0]`.
- Right hip position следует за фиксированной local anchor на body; максимальная ошибка крепления 0.01722 model unit. Это локальная привязка к тазу, без world root movement.
- Native keys + EMF radians / `ordered_variables` → `part_expressions` / cubic Bezier; snapping 60fps. Ankle keys уточняют контакт, pose keys остальных частей редкие и редактируемые.
- Final evidence: `preview/debris_kick_review.png`, animated `preview/debris_kick_rear_swing_v3.gif`; два финальных native rounds front/side/3/4. OBB результаты и контакт adjacent thigh/pelvis см. REPORT.
- Девять прежних clips, геометрия, иерархия и embedded textures сохранены. `lion_roar` остаётся отдельным clip.

## Что implementation agent должен знать

- Работа выполнена через **Blockbench MCP**. EMF expressions проверены математическим preview adapter и сопоставлены с native timeline. Использованы `cubicbezier(t,p0,p1,p2,p3)`, `keyframeloop`, `torad`, `clamp`, `if`, `lerp`.
- `emf_lab` и `Regulus_EMF_Authoring.json` — authoring schema, **не готовый player.jem**. Blockbench `free` даёт editable native keys; EMF source сохранён отдельно и внутри файла.
- Для runtime требуется custom EMF player model с дополнительными shoulder/forearm/hand/shin/foot bones и `mouth_open`; проверить part IDs, оси/единицы и installed EMF release. `var.regulus_<clip>_time` — явный вход каждого clip, которому ещё нужен state/time driver.
- Сохранять порядок `ordered_variables` перед `part_expressions`. Rotation output — radians; translations — model units в authoring space. Future JEM binding требует проверки направления осей.
- Кисти кубические, отдельных пальцев нет. `mouth_open` — маленькая анимируемая плашка, не полноценная челюсть. В других clips она скрыта.
- Оригинальный Regulus skin embedded, UV открытых поверхностей сохранены. В checkout отдельного Regulus/Еvangelium master не найдено; использован фактический wide player skin из мода. Runtime item называется `evangelion`.
- Два полных native visual rounds на каждый clip: front/side/3/4; loops — по два цикла на ракурс. Evidence: `preview/*_review.png`, подробности в [[10 Analysis/regulus_emf_animation_pack/REPORT]].
- Minecraft не запускался. Runtime animation system, EMF integration, abilities, networking и баланс не менялись. Source находится в lab; мод-репозиторий остался read-only.
