Ревью плана Regulus rework (Codex Superheroes, Fabric 1.21.1).

Прочитай полностью:
1. `docs/superpowers/plans/2026-09-30-regulus-rework.md` — план, 9 тасков.
2. `docs/design/2026-09-30-regulus-rework-design.md` — спека (истина по WHAT/почему).
3. `AGENTS.md` — контракт проекта (§2 hero-module, §7 hard rules, §10 verification).

Контекст: план уже прошёл 5 ревью и пропатчен. Ищи ТОЛЬКО остаточное:
- противоречия между тасками (файл создаётся/удаляется дважды, контракт ≠ использование);
- ссылки на несуществующие символы/сигнатуры (проверяй grep/javap по реальному коду, не по памяти);
- требования спеки §1–§9 без таска/теста;
- шаги, которые не скомпилируются или сломают `qualityGate` по порядку коммитов.

Вывод: короткий список находок в формате `[priority] file:line — что сломано — как починить`. Не дублируй: trackingAndSelf, syncWith, clipId, EMF dep, releaseUsingItem, per-tick velocity, OwnableEntity-гейт, world-render — уже внесены.
