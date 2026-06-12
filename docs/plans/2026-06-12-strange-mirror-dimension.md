# Доктор Стрэндж — «Зеркальное измерение» (Acid warp) — план реализации

**Цель:** одна способность. Доктор Стрэндж кастует «Зеркальное измерение» на игрока-цель — у цели на клиенте автоматически включается готовый шейдерпак **Acid Shaders 1.5** (Iris) в режиме «круглый мир» (сферическая инверсия пространства). По окончании эффекта клиент цели **точно восстанавливает** свои прежние шейдерные настройки.

**Явно вне скоупа (по запросу пользователя):** скин/модель Стрэнджа, другие способности, купол/ограничение пространства (пользователь делает сам), порталы, звуки/анимации сверх минимума.

---

## 0. Исследованные факты (проверено 2026-06-12)

### 0.1 Acid Shaders 1.5 — внутренности

- Modrinth: `https://modrinth.com/shader/acidshaders15` (project id `Z8vHOw6N`), loaders: `iris`, `optifine`, game versions: **1.11 … 1.21.1** (наша 1.21.1 поддерживается).
- Лицензия: **All-Rights-Reserved** → ⚠️ **мы НЕ имеем права вшивать пак в jar или в репозиторий**. Пак ставится игроком вручную (или качается им самим с Modrinth). Это жёсткое юридическое ограничение.
- Деформация — **vertex deformation** в `gbuffers_*.vsh` через функцию `geomfunc(vec4 P)` в `shaders/common.glsl`.
- Управляется **двумя shader options** (стандартные Iris/OptiFine options, объявлены в `common.glsl`):
  - `#define MODE 0 // [0 1 2 3 4 5 6 7 8 9]` — режим деформации;
  - `#define J 16 // [1 2 4 8 16 32 64 128 256 512]` — масштаб мира («радиус» искривления, меньше = круче загиб).
- Режимы (из `shaders/lang/en_us.lang` и кода):
  - 0/1/2 — вращающаяся инверсия (zx/xy/yz) — «кислотное» кручение;
  - 3 — Relativity (зависит от скорости, jittery);
  - **4 — «Uptown» — `inv(p, vec3(0, J, 0))` — сферическая инверсия с центром НАД игроком → мир заворачивается ВВЕРХ вокруг тебя (концаво, «петля», похоже на референс-скрин пользователя);**
  - **5 — «Downtown» — `inv(p, vec3(0, -J, 0))` — инверсия с центром ПОД игроком → эффект «маленькой планеты» (выпукло);**
  - 6 — Solv-геодезики; 7/8/9 — кастомные (8 = тор-мир `torusify`).
- «Круглый мир» из запроса = **MODE 4 или 5** (что именно красивее — решается живым тестом, см. §7; по умолчанию берём 4, J=16, оба значения выносим в конфиг).
- Iris хранит выбранные options пака в файле **`shaderpacks/<имя_пака>.txt`** (формат `key=value`, OptiFine-совместимый). Т.е. `shaderpacks/Acid Shaders.zip.txt` с содержимым `MODE=4` и `J=16`. ✅ Это наш механизм «найти настройку круглого мира и выставить её программно».

### 0.2 Iris

- Для MC 1.21.1 Fabric актуален **Iris 1.8.8+1.21.1-fabric** (требует Sodium 0.6.x — ставится вместе).
- Публичный API: `net.irisshaders.iris.api.v0.IrisApi`:
  - `IrisApi.getInstance().getConfig().areShadersEnabled()`;
  - `IrisApi.getInstance().getConfig().setShadersEnabledAndApply(boolean)` — включает/выключает шейдеры **с применением** (перезагрузка пайплайна);
  - `isShaderPackInUse()`.
- **Выбор конкретного пака** в публичном API v0 отсутствует → используем внутренний класс `net.irisshaders.iris.Iris`: `Iris.getIrisConfig().setShaderPackName(String)` + `Iris.reload()`. Это **internal API** — главный технический риск (§8.1). Iris не обфусцирован, имена классов стабильны в пределах 1.8.x, но между мажорными версиями могут меняться → все вызовы оборачиваем в guard + try/catch (`NoClassDefFoundError`, `NoSuchMethodError`, любые `Throwable`) с graceful fallback.
- Iris — **client-only** мод. Сервер (включая dedicated) не должен классло́адить ничего из Iris → весь Iris-код строго в `src/client`, за проверкой `FabricLoader.getInstance().isModLoaded("iris")`.

### 0.3 Принципиальные ограничения (физика вопроса)

- Шейдер — **клиентский**. Сервер не может включить шейдер игроку с ванильным клиентом. Чтобы жертва увидела искривление, на её клиенте должны стоять: **наш мод + Iris (+Sodium) + пак Acid Shaders**. Это требование к установке, обойти нельзя.
- Шейдер действует **глобально на весь экран клиента** жертвы (не «зона купола»). Ограничение пространства — отдельная механика (вне скоупа, делает пользователь).

---

## 1. Геймплейный контракт способности

| Поле | Значение |
| --- | --- |
| Герой | Doctor Strange (`superheroes:doctor_strange`) — новый, минимальный shell |
| Ability ID | `superheroes:mirror_dimension` |
| Имя | «Зеркальное измерение» / "Mirror Dimension" |
| Тип | active, toggle (`isToggle()=true`) |
| Ресурс | Mana (Стрэндж — маг): max 200, реген как у магов-аналогов |
| Стоимость | 60 маны активация + 0.5/тик пока активна (≈10 маны/сек) |
| Кулдаун | 10 сек после деактивации (`AbilityCooldowns.setCooldownTicks`) |
| Дальность | raycast 32 блока по прицелу; цель — **игрок**; если игрока в прицеле нет → **self-cast** (режим самотеста, удобно проверять в одиночке) |
| Длительность | пока toggle активен и хватает маны; деактивация — повторное нажатие/смерть/растрансформация/выход маны в 0 |
| Эффект на цели | клиенту цели включается Acid Shaders с MODE/J из конфига; при снятии — точное восстановление прежнего состояния шейдеров |
| Контр-игра | выйти из зоны нельзя «через настройки» — но жертва может физически убить кастера; деактивация при смерти кастера |
| Цели | только игроки (у мобов нет клиента, шейдер бессмыслен). Мобы в прицеле игнорируются |
| VFX/SFX | минимум: частицы portal/reverse_portal вокруг цели при старте (серверные), без новых звуков |
| Network | S2C `MirrorDimensionS2CPayload` (вкл/выкл + mode + scale + keepalive), C2S `MirrorDimensionStatusC2SPayload` (результат применения на клиенте цели → фидбек кастеру) |
| Cleanup | деактивация при: смерти кастера, смерти цели, дисконнекте любого, растрансформации кастера, нехватке маны; на клиенте — deadman-switch и restore-файл (§4.4) |

Фидбек кастеру в чат (actionbar/чат): «цель не игрок», «у цели нет мода», «у цели нет Iris», «у цели не найден пак Acid», «эффект применён», «эффект снят».

---

## 2. Карта файлов

### Новые (main, серверная логика)

| Файл | Ответственность |
| --- | --- |
| `hero/DoctorStrangeHero.java` | Минимальный shell героя: id, mana 200, реген, abilities=[MIRROR_DIMENSION], dimensions стандартные, скин — заглушка (см. §6) |
| `item/DoctorStrangeSuitItem.java` | `extends TransformationItem` (копия паттерна `RegulusSuitItem`), tooltip-лор 2 строки |
| `ability/MirrorDimensionAbility.java` | toggle ability: raycast цели, валидации, запуск/остановка `MirrorDimensionController` |
| `effect/MirrorDimensionController.java` | Серверное состояние: `Map<UUID caster, UUID victim>`; тик: keepalive каждые 20т, проверки жизни/онлайна/маны; полный cleanup |
| `network/MirrorDimensionS2CPayload.java` | `{byte action (ON/OFF/KEEPALIVE), int mode, int scale}` |
| `network/MirrorDimensionStatusC2SPayload.java` | `{byte status}` → контроллер пересылает кастеру текст |

### Новые (client)

| Файл | Ответственность |
| --- | --- |
| `client/ClientMirrorDimensionState.java` | Состояние на клиенте жертвы: active, lastKeepaliveTime; deadman-switch (нет keepalive 100 тиков → restore) |
| `client/iris/IrisShaderBridge.java` | ЕДИНСТВЕННОЕ место, где трогаем классы Iris. `isIrisLoaded()`, `findAcidPack()`, `snapshot()`, `applyAcid(mode, scale)`, `restore(snapshot)`. Все методы безопасны при отсутствии Iris |
| `client/iris/MirrorRestoreFile.java` | Крэш-стойкость: json-снапшот прежнего состояния на диске; восстановление при старте клиента (§4.4) |

### Изменяемые

| Файл | Правка |
| --- | --- |
| `hero/Heroes.java` | поле + `register(DOCTOR_STRANGE)` |
| `hero/HeroAttributes.java` | `STRANGE` — скромные атрибуты (HP 30, без буйства: герой «техничный», упор на способность) |
| `ability/AbilityIds.java` | `MIRROR_DIMENSION` |
| `ability/AbilityRegistry.java` | регистрация |
| `item/ModItems.java` | `DOCTOR_STRANGE_SUIT` + creative tab |
| `network/ModNetworking.java` | регистрация двух payload (typed `CustomPacketPayload` + `StreamCodec`, по образцу существующих) |
| `client/SuperheroesClient.java` | client receiver S2C + тик deadman + `ClientPlayConnectionEvents.DISCONNECT` → restore |
| `SuperheroesMod.java` | `MirrorDimensionController.init()` |
| `build.gradle` | `maven { url "https://api.modrinth.com/maven" }` + `modCompileOnly "maven.modrinth:iris:1.8.8+1.21.1-fabric"` (**compileOnly! не runtime, не include**) |
| `assets/superheroes/lang/en_us.json` + `ru_ru.json` | имя героя, имя/описание способности, лор предмета, тексты фидбека (паритет ключей) |
| модель/текстура предмета | простая 2D-иконка предмета (плейсхолдер, §6) |

---

## 3. Логика сервера (подробно)

### 3.1 Активация (`MirrorDimensionAbility.tryActivate`)

1. `canActivate`: не на кулдауне (`AbilityCooldowns.isOnCooldown`).
2. Raycast по взгляду 32 блока (`ProjectileUtil`/AABB-скан, как в `ScorpionFireTeleportAbility`): ищем `ServerPlayer` (не spectator, жив). Нет игрока → цель = сам кастер (self-test).
3. Проверка канала: `ServerPlayNetworking.canSend(victim, MirrorDimensionS2CPayload.TYPE)`. `false` → фидбек «у цели нет мода (Codex-Superheroes)», активация отклоняется (`return false` — мана не списывается).
4. `MirrorDimensionController.start(caster, victim)`:
   - если у кастера уже есть активная жертва → сначала корректно останавливаем старую (OFF + cleanup);
   - шлём жертве `ON{mode, scale}` из конфиг-констант;
   - серверные частицы `REVERSE_PORTAL` вокруг жертвы (1 раз);
   - запоминаем пару в map.
5. Toggle активен; `costPerTick=0.5` списывает `AbilityRouter`/ResourceController штатно; мана в 0 → штатная деактивация toggle.

### 3.2 Тик контроллера (`END_SERVER_TICK`)

- каждые 20 тиков: `KEEPALIVE` жертве;
- проверки каждый тик: кастер жив/онлайн/всё ещё Стрэндж/toggle активен; жертва жива/онлайн; иначе → `stop(caster)`.

### 3.3 Деактивация (`onDeactivate` + `stop`)

- шлём жертве `OFF`;
- удаляем пару из map;
- `AbilityCooldowns.setCooldownTicks(caster, id, 200)`;
- если жертва оффлайн — OFF не дойдёт: на её клиенте сработает deadman-switch / restore-файл (§4.4).

### 3.4 Статусы от клиента жертвы (C2S)

`OK_APPLIED`, `NO_IRIS`, `NO_PACK`, `IRIS_API_FAIL`, `OK_RESTORED`. Контроллер мапит на локализованные сообщения кастеру. При `NO_IRIS`/`NO_PACK`/`IRIS_API_FAIL` контроллер **сам останавливает** эффект (не жжём ману впустую).

---

## 4. Логика клиента жертвы (подробно)

### 4.1 Приём `ON{mode, scale}` (только client env)

1. `if (!FabricLoader.isModLoaded("iris"))` → ответ `NO_IRIS`, выход. (Никаких импортов Iris вне `IrisShaderBridge`.)
2. `IrisShaderBridge.findAcidPack()`: скан `<gameDir>/shaderpacks/` на файл/папку, имя содержит `acid` (case-insensitive). Не найден → `NO_PACK`.
3. `snapshot()` — запомнить ПОЛНОЕ прежнее состояние:
   - `shadersEnabled` (`IrisApi.getConfig().areShadersEnabled()`);
   - имя текущего пака (`Iris.getIrisConfig().getShaderPackName()` — Optional);
   - байты текущего `shaderpacks/<acidPack>.txt`, если существует (мы его перезапишем).
4. Записать снапшот в `MirrorRestoreFile` (`config/superheroes/mirror_restore.json`) — флаг «грязного» состояния на случай крэша.
5. Записать options: `shaderpacks/<acidPack>.txt` ← `MODE=<mode>\nJ=<scale>\n` (поверх прежнего содержимого, прежнее лежит в снапшоте).
6. Выбрать пак + включить: `Iris.getIrisConfig().setShaderPackName(acidPack)`, `setShadersEnabledAndApply(true)` / `Iris.reload()`. Всё в try/catch → при любой ошибке откат по снапшоту + ответ `IRIS_API_FAIL`.
7. Ответ `OK_APPLIED`. `ClientMirrorDimensionState.active=true`.

### 4.2 Приём `KEEPALIVE` — обновить `lastKeepaliveTime`.

### 4.3 Приём `OFF` → `restore()`:

- вернуть прежний `<acidPack>.txt` (или удалить, если его не было);
- вернуть прежний пак (`setShaderPackName(old)`);
- вернуть `shadersEnabled` как было + apply/reload;
- удалить restore-файл; `active=false`; ответ `OK_RESTORED`.

### 4.4 Стойкость к обрывам (критично, чтобы не «сломать» человеку шейдеры навсегда)

- **Deadman-switch:** client tick: если `active` и нет keepalive >100 тиков (5 сек) → самостоятельный `restore()`. Покрывает: крэш сервера, кик, потерю пакетов.
- **Дисконнект:** `ClientPlayConnectionEvents.DISCONNECT` → если active → `restore()`.
- **Крэш клиента во время эффекта:** при следующем старте (client init) если `mirror_restore.json` существует → восстановить из него и удалить. Iris на старте уже прочитал свои конфиги — наш restore выполнить ДО первого мира (в `ClientLifecycleEvents.CLIENT_STARTED` достаточно: правим конфиг Iris и txt до того, как игрок включит мир; если шейдеры уже поднялись — `reload()`).

---

## 5. Сетевые пакеты

По существующему паттерну (typed `CustomPacketPayload` + `StreamCodec`, регистрация в `ModNetworking`; **без** `@Environment` на регистрации — Fabric сам разделит):

```
MirrorDimensionS2CPayload { byte action; int mode; int scale; }  // ON=0, OFF=1, KEEPALIVE=2
MirrorDimensionStatusC2SPayload { byte status; }                 // OK_APPLIED=0, NO_IRIS=1, NO_PACK=2, IRIS_API_FAIL=3, OK_RESTORED=4
```

Receiver S2C регистрируется в client networking (как остальные S2C), C2S — в `ModNetworking` рядом с `ActivateAbilityC2SPayload`.

---

## 6. Герой-shell и предмет (минимум, без арта)

- `DoctorStrangeHero`: mana-герой (energy 0 / mana 200, реген ~1.0/т), стандартные dimensions, `getAbilities() = [MIRROR_DIMENSION]`, `getDefaultBinding -> MANA`.
- Скин: **арт не рисуем** (явное требование). `getSkinTexture()` → плейсхолдер `textures/entity/hero/doctor_strange.png` — копия самой нейтральной существующей текстуры (проверить `art-source/` и `textures/entity/hero/`; в `art-source/marvel/` Стрэнджа нет — подтверждено). Заменим, когда пользователь даст арт.
- Предмет `DoctorStrangeSuitItem` («Плащ левитации» как трансформ-итем): простая 2D-иконка-плейсхолдер (генеренная 16×16 «мантия», в `art-source/` положить оригинал + source.txt), модель через datagen по паттерну `add-item`.
- `HeroAttributes.STRANGE`: HP 30, без модификаторов атаки/скорости — герой про способность, не про статы (tier C по `balance-check`; ОП-механик нет — эффект чисто визуально-психологический).

---

## 7. Поиск «того самого» круглого мира (тюнинг)

1. Дефолт: `MODE=4 (Uptown), J=16`.
2. Живой тест в одиночке self-cast'ом: сравнить `MODE=4` vs `MODE=5` и `J=8/16/32`:
   - 4 — мир заворачивается вверх «вокруг неба» (ближе к референс-скрину с чёрным небом в центре);
   - 5 — «маленькая планета» под ногами;
   - меньший J — туже петля, сильнее дезориентация (но больше клиппинга геометрии).
3. Выбранные значения фиксируются как константы `MirrorDimensionConfig` (`MODE`, `J`, `KEEPALIVE_TICKS`, `RANGE`); при желании пользователя позже выносим в json-конфиг.

---

## 8. Риски и узкие места (полный список)

| # | Риск | Серьёзность | Митигация |
| --- | --- | --- | --- |
| 1 | **Internal API Iris** (`Iris.getIrisConfig`, `Iris.reload`) может отличаться между версиями Iris | ВЫСОКАЯ | Пин на 1.8.x в доке для друга; все вызовы в одном классе-мосте; try/catch всех Throwable; fallback: если выбор пака упал, но шейдеры уже на Acid вручную — просто `setShadersEnabledAndApply(true)` (публичный API); статус `IRIS_API_FAIL` кастеру |
| 2 | **Лицензия Acid = All-Rights-Reserved** — нельзя бандлить пак | ВЫСОКАЯ (юр.) | Пак НЕ кладём ни в jar, ни в repo. Жертва ставит сама (Modrinth). Мод только находит пак в `shaderpacks/` и управляет им |
| 3 | У жертвы нет нашего мода (ванильный клиент) | ВЫСОКАЯ (по сути фичи) | Технически непреодолимо. `canSend()`-проверка + честный фидбек кастеру. Требование к установке задокументировать в README/Slack |
| 4 | У жертвы нет Iris/Sodium или пака | СРЕДНЯЯ | Статусы NO_IRIS/NO_PACK, автоостановка эффекта, сообщение кастеру что поставить |
| 5 | **Восстановление прежних шейдеров жертвы** (у неё мог стоять свой пак/настройки) | ВЫСОКАЯ (доверие игрока) | Полный снапшот (enabled+pack+options txt) в память И на диск; restore на OFF, deadman, disconnect, рестарт после крэша (§4.4) |
| 6 | Крэш/кик/обрыв во время эффекта → жертва «застревает» в кислоте | СРЕДНЯЯ | Deadman-switch 5с без keepalive + restore-файл при старте клиента |
| 7 | `Iris.reload()` — глобальная перезагрузка пайплайна: фриз ~1-3 c на клиенте жертвы при вкл/выкл | НИЗКАЯ | Неустранимо, приемлемо для «ловушки»; задокументировать |
| 8 | Имя файла options (`<pack>.txt`) — если Iris изменит конвенцию | НИЗКАЯ | После записи перечитать файл; в тесте проверить, что MODE реально применился (визуально) |
| 9 | Неопределённость «какой MODE = его картинка» | НИЗКАЯ | Тюнинг-сессия §7, константы меняются в одном месте |
| 10 | Dedicated server: классы Iris не должны грузиться на сервере | СРЕДНЯЯ | Iris-код только в `src/client`; payload-классы без Iris-импортов; smoke-тест на dedicated |
| 11 | Сервер-авторитарность: жертва может «отключить» эффект модами клиента | НИЗКАЯ | Эффект чисто визуальный; читер сам себе портит шоу. Не боремся |
| 12 | Несколько Стрэнджей кастуют на одну жертву | НИЗКАЯ | На клиенте state одиночный: второй ON перезаписывает mode, OFF любого снимает; в контроллере victim→caster map, второй каст на занятую жертву отклоняется с фидбеком |
| 13 | Производительность жертвы: Acid + Sodium на слабом ПК | НИЗКАЯ | Vertex-деформация дешёвая; Sodium компенсирует; не наша зона |
| 14 | Creative-режим (пользователь играет в креативе) | НИЗКАЯ | Ресурсы/кулдауны работают как у остальных героев (как ведёт себя ResourceController в креативе — проверить в тесте; поведение не меняем) |
| 15 | `modCompileOnly` Iris через Modrinth maven может не резолвиться у Loom | СРЕДНЯЯ | Проверено сообществом (maven.modrinth работает с Loom); если нет — fallback: полный reflection без compile-dep (мост и так изолирован) |

---

## 9. Порядок работ (bite-sized)

1. [ ] `build.gradle`: modrinth maven + `modCompileOnly` iris → `./gradlew compileJava` зелёный.
2. [ ] `AbilityIds.MIRROR_DIMENSION` + пустая `MirrorDimensionAbility` + регистрация → компиляция.
3. [ ] Payload'ы + регистрация в `ModNetworking` → компиляция.
4. [ ] `MirrorDimensionController` (start/stop/tick/keepalive/статусы) + init в `SuperheroesMod`.
5. [ ] Raycast и валидации в `MirrorDimensionAbility` (по образцу `ScorpionFireTeleportAbility`).
6. [ ] Клиент: `ClientMirrorDimensionState` + receiver'ы + deadman + DISCONNECT-хук.
7. [ ] `IrisShaderBridge` (find/snapshot/apply/restore) + `MirrorRestoreFile` + restore-on-start.
8. [ ] Герой-shell + suit item + плейсхолдер-текстуры + lang en/ru + datagen модели.
9. [ ] `./gradlew build` (тесты), фикс ворнингов **без** запрещённых deprecated API.
10. [ ] Smoke-тест чеклист §10; тюнинг MODE/J (§7).
11. [ ] Bump `mod_version=3.37.0` (minor: новая фича), PR, jar в Slack.

## 10. Чеклист проверки (в игре)

- [ ] Установка на клиент: мод + Iris 1.8.8 + Sodium + `Acid Shaders.zip` в `shaderpacks/`.
- [ ] Трансформация в Стрэнджа, способность видна в радиальном меню.
- [ ] Self-cast: шейдер включился, мир «круглый» (MODE применился, не дефолтный rotation!).
- [ ] Деактивация: вернулось ровно прежнее состояние (3 кейса: шейдеры были выключены; был другой пак; был Acid с другими options).
- [ ] Мана тикает вниз, при 0 — авто-выключение + кулдаун 10с.
- [ ] LAN/сервер: каст на второго игрока — жертва видит, кастер нет; фидбек «применён».
- [ ] Жертва без Iris → сообщение кастеру, эффект не «висит».
- [ ] Жертва вышла из игры во время эффекта → при перезаходе шейдеры в норме (restore-файл).
- [ ] Убить клиент жертвы (крэш) → перезапуск → шейдеры восстановлены.
- [ ] Смерть кастера / растрансформация → эффект снят у жертвы.
- [ ] Dedicated server стартует без Iris в classpath (нет NoClassDefFoundError).

## 11. Что нужно от пользователя

1. Подтвердить дефолт MODE=4 (или выбрать 5 после теста) и J.
2. Другу (жертве) поставить: наш мод, Iris+Sodium, Acid Shaders 1.5 в `shaderpacks/`. Больше ничего: режим/включение мод сделает сам.
3. Позже (опционально): арт скина Стрэнджа и иконки предмета — сейчас плейсхолдеры.
