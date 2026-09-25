# Codex Superheroes — аудит локальности героев (feature locality / hero modularity)

> Read-only аудит Opus 5.5 (Hoplite), 2026-09-25. Снимок кода — `main` @ `625e9bc`; все ссылки `Класс:строка` относятся к нему.
> Дополняет [`2026-09-25-opus-architecture-audit.md`](2026-09-25-opus-architecture-audit.md): там баги и их корневые причины, здесь только структура — насколько каждый герой размазан по проекту, сколько стоит целиком понять или изменить одного персонажа и как прийти к hero-first модулям без бессмысленного rewrite.

## Итог

Сейчас герой в Codex не является модулем. Его код разложен по техническим слоям: `ability`, `effect`, `network`, `item`, `mixin`, `client/hud`, `render`. Данные героя разнесены по 12 с лишним общим таблицам. Поэтому даже у самого простого героя 12 точек касания с общим кодом, а Iron Man занимает 11 пакетов и упоминается в 36 общих файлах. Хорошая новость: в проекте уже есть работающие точки расширения, и переход к структуре «герой = модуль» можно делать постепенно, без переписывания механик.

## Как мерил

- Прочитал ядро: реестры, bootstrap, сеть, миксины, `ProjectSanityTest`.
- Скриптом (в репозиторий не входит) построил граф ссылок по 560 Java-файлам, около 54,7 тыс. строк.
- Принадлежность класса герою определял по имени и поправлял вручную. Например, `RoyalIcicle` — меч Рейнхарда, `Uranium` — механика против Хоумлендера, `Evangelion` — предмет Регулуса, `DoctorStrange` — Пандора.
- Отдельно посмотрел в git, какие файлы трогало добавление Scorpion и Pandora. Ключевые места перепроверил чтением кода. Код не менялся, субагенты не использовались.
- Имена классов уникальны, поэтому ссылки даны как `Класс:строка`. В списках файлов `C/` = `src/client/java/com/example/superheroes/client/`, всё остальное лежит в `src/main/java/com/example/superheroes/`.

## 1. Как устроен герой сейчас

**Добавление способности** требует правок минимум в четырёх местах:

- класс в `ability/`;
- константа в `AbilityIds`;
- поле и вызов `register` в `AbilityRegistry` (285 строк на 118 способностей);
- id в `XxxHero.getAbilities()`;
- плюс ключи в двух lang-файлах.

Реестры уже расходятся с кодом:

- пять id объявлены, но не зарегистрированы: `VILTRUMITE_THUNDER_CLAP`, `IRON_MAN_NANO_REPAIR`, `NARUTO_KURAMA_CLOAK`, `NARUTO_TAILED_BEAST_BOMB`, `NARUTO_FLYING_RAIJIN`;
- `METEOR_SLAM` и `SHOCKWAVE_PULSE` зарегистрированы, но не входят ни в одного героя, поэтому активировать их нельзя. При этом `MeteorSlamAbility.serverTick` вызывается для каждого игрока каждый тик (`SuperheroesMod:110`), а `InvincibleHero:106` чистит его состояние. У этого кода нет владельца, поэтому никто не видит, что он мёртвый.

**Добавление героя** на примере Scorpion (коммит `ab26340`): 9 общих Java-файлов (`SuperheroesMod`, `AbilityIds`, `AbilityRegistry`, `HeroAttributes`, `HeroHudConfig`, `HeroTheme`, `Heroes`, `ModItems`, `ModItemGroups`), 2 lang-файла и ресурсы. Позже добавились `ModNetworking`, `ClientNetworking` и `ModSounds`. Про часть таблиц никто не напомнил, и два новейших героя из них выпали:

| Таблица | Где | Кого нет |
|---|---|---|
| Уровень угрозы Jarvis | `JarvisThreatClass:41-66` | Scorpion, Pandora (получают значение по умолчанию) |
| Сила и стиль удара | `CombatImpactEngine:289-323` | Scorpion, Pandora |
| Число пассивок | `AbilityDescriptions:29-50` | Scorpion, Pandora |
| Иконки пассивок | `PassiveIcons:17-37` | Scorpion, Pandora |
| Конфиг HUD | `HeroHudConfig:29-49` | Pandora (конфиг по умолчанию) |

Для тем героев одновременно живут две конвенции:

- 11 тем лежат в общем файле `HeroTheme:23-233`; тема Хоумлендера заодно служит темой по умолчанию (`:254`);
- ещё 10 — в классах самих героев (`ATrainHero:18`, `DoomsdayHero:25`, `KazuhaHero:17` и другие);
- Pandora заимствует тему Регулуса (`PandoraHero:110-111`).

Правила нет: даже новейший Scorpion попал в общий файл, хотя половина тем уже живёт в классах героев.

## 2. Разброс по всем героям

| Герой | Свои файлы / строк | Пакетов | Общих файлов со ссылками |
|---|---|---|---|
| Iron Man | 45 / 5146 | 11 | 36 |
| Reinhard | 37 / 3436 | 10 | 20 |
| Pandora | 29 / 2528 | 9 | 18 |
| Homelander | 26 / 2109 | 8 | 27 |
| Thanos | 23 / 2124 | 9 | 24 |
| Doomsday | 20 / 1995 | 8 | 23 |
| Regulus | 19 / 2218 | 7 | 23 |
| Rem · Raiden | 17 / 2400 · 17 / 1835 | 8 · 5 | 13 · 14 |
| Sung Jinwoo | 14 / 1542 | 8 | 15 |
| Kratos · Naruto · Scorpion | 11–12 / 1000–1213 | 6–7 | 12–17 |
| Goku · Omni-Man · Captain America · Invincible | 8–10 / 802–1201 | 4–6 | 9–17 |
| Loki · Battle Beast · A-Train · Kazuha · Scaramouche | 6–7 / 548–741 | 3–4 | 7–11 |

Обе числовые колонки — нижние границы:

- «Общих файлов со ссылками» считает упоминания классов и констант героя. Сверх этого почти каждый герой записан строковым id в `PassiveIcons` и `AbilityDescriptions`, и у всех есть ключи в двух lang-файлах.
- «Свои файлы» не учитывает файлы с общими именами, которые на деле работают только для одного героя. Например, `BloodRainHud`, `ClientHudGlitch`, `GuiVanillaGlitchMixin` и `GameRendererFovMixin` (безумие Регулуса), `SunWindupHud` (безумие Хоумлендера), `SoundEngineMixin` (Рейнхард) засчитаны как общие.

## 3. Карты зависимостей по 8 героям

Базовый пример — **Kazuha**, самый простой герой. У него 6 своих файлов в 3 пакетах: 4 способности, класс героя, предмет. Касаний общего кода 12: `Heroes`, `AbilityIds`, `AbilityRegistry`, `ModItems`, `ModItemGroups`, `HeroHudConfig`, `CombatImpactEngine`, `JarvisThreatClass`, `PassiveIcons`, `AbilityDescriptions` и оба lang-файла.

**Homelander** — самый старый герой.

```text
Свои: ability/ EyeLasers HandClap IronFists StunningRoar XRay
      effect/ HomelanderRegen IronFists UraniumDefense UraniumOffhand MadnessMobEffect MadnessAftermathMobEffect
              MadnessFlight MadnessAftermath
      item/ HomelanderSuit MilkBottle UraniumDagger UraniumIsotope · network/ 3 payload
      C/ ClientUraniumPressureState ClientUraniumThreatState · hud/ UraniumThreatHud · render/ LaserBeamRenderer LocalLaserOverlay
Регистрации: Heroes:12,39 · AbilityIds:8-12 · AbilityRegistry:14-18,154-158 · SuperheroesMod:43-44,58,60,68-69
      ModEffects:11-18 · ModNetworking:35,43-44 · ClientNetworking:77-78,145-149 · ModSounds:17-21,26
      SuperheroesClient:59,63,180
Данные в общих файлах: HeroAttributes:9-14,142-148 · HeroTheme:23,254 · HeroHudConfig:29 · CombatImpactEngine:297,311
      JarvisThreatClass:49 · ModDamageTypes:16,60 (EYE_LASER)
Логика в общих системах: FlightController:147,182-210 (уран и «молочное безумие») · FlightAbility:37-38 · LocalPlayerFlightMixin:44
      AbilityRouter:20 (блокировка в aftermath), 66 (блокировка на время IRON_FISTS)
      AbilityRouter:134 + ResourceController:88 (в безумии способности бесплатны) · ResourceController:48
      HeroReactionController:24-29 · AbstractClientPlayerSkinMixin:26,41-42 · LowResourceVignetteHud:24 · SunWindupHud:22
Чужие модули знают о нём: InvincibleCombatController:55 (проверяет IRON_FISTS), 117 (его звук)
      ReinhardController:419 (EYE_LASER) · HeavensStrikeController:228 (его звук)
```

Здесь есть ловушка в именах: префикс `Madness*` делят два героя. `ModEffects.MADNESS`, `MadnessMobEffect`, `MadnessFlightController` и `MadnessAftermath*` — режим Хоумлендера после молока (`MilkBottleItem:84-87`). А `MadnessSyncS2CPayload`, `MadnessVisualS2CPayload`, `ClientMadnessState`, `MadnessHudOverlay` и все `RegulusMadness*` — совсем другая механика Регулуса. Поиск по имени путает и людей, и скрипты.

Рядом живёт **босс Хоумлендер** — отдельная фича с тем же именем: сущность, 10 AI-целей в `entity/ai/`, рендерер, предмет Vought Signal и свои типы урона `HOMELANDER_*` (`ModDamageTypes:42-49,86-93,201-230`). О боссе тоже знают чужие модули: `DoomsdayAdaptationController:71-81` и `ReinhardController:422-423` перечисляют эти типы урона.

**Iron Man** — самый большой герой.

```text
Свои: ability/ ×6 + ability/ironman/ ×8 (единственный герой со своим подпакетом)
      effect/ IronManAutoEject IronManJarvis IronManReactorTracker UnibeamController(560)
      entity/ IronLegionDrone(359) SmartMissile · jarvis/ ×2 · item/ ×2 · network/ ×5
      C/ 6 Client*State · hud/ JarvisOverlayHud(527) JarvisDetectionHud ReactorOverlayHud
      render/ ×6 (IronManEspRenderer 432)
Регистрации: Heroes:13,40 · AbilityIds:23-31 · AbilityRegistry:27-34,167-174 · SuperheroesMod:45,48-51,81,113,120
      ModNetworking:36,40,49-51 · ClientNetworking:80-81,128-129,161-173 · ModEntities:59,62,69,72,87
      ModSounds:14-16,27-32 · ModParticles:12-13 · ModAttachments:61 · ModKeys:17-18,69-78
Данные в общих файлах: HeroAttributes:16-20,151-177 · HeroTheme:44 · HeroHudConfig:30 · ModDamageTypes:17-18,61-62,102,106
Логика в общих системах: FlightMode:5-6 (IRON_MAN и SUPERSONIC прямо в enum) · FlightAbilityState:13-14,26-33
      FlightProfiles:10-11,25-28 · FlightController:34,173-177,214-232 · CombatImpactEngine:99-108 (нано-молот)
      AbilityRouter:137 (резерв энергии под Unibeam) · HeroLandingTracker:113 · HeroMeleeImpactController:184-185
      HeroTransformService:99 · HeroInfoPanelHud:97,157-161,232,252 (отдельная панель статуса костюма)
      GuiHotbarMixin:48 · AbstractClientPlayerSkinMixin:33,54-56 · PlayerRendererMixin:39,51-53
      SuperheroesClient:60-61,69,71,103-104,109-110,167-168,176,203-207,279,281,295-320
Чужие модули знают о нём: RegulusMadnessController:138-146 (выключает IRON_MAN_FLIGHT и SUPERSONIC по id)
      ReinhardController:420-421
```

По факту подсистема полёта наполовину принадлежит Iron Man.

**Reinhard** — самый сложный по числу сценариев.

```text
Свои: ability/ ×8 · effect/ ×8 (ReinhardController 601) · network/ ×7 · item/ ReinhardSuit RoyalIcicle (его меч Reid)
      mixin/ RoyalIcicleNoDrop · C/ 5 Client*State · hud/ ×3 · render/ ReinhardScabbardLayer · screen/ ReinhardWishScreen
Регистрации: Heroes:23,50 · AbilityIds:95-102 · AbilityRegistry:95-102,225-232 · SuperheroesMod:87-92
      ModNetworking:28,57-62,86-88 · ClientNetworking:194-216 · ModItems:150-157 · ModItemGroups:61
      ModAttachments:36-38 · ModSounds:24-25
Данные в общих файлах: HeroAttributes:107-137,305-348 · HeroTheme:191 · HeroHudConfig:37 · CombatImpactEngine:316
      JarvisThreatClass:46
Логика в общих системах: AbilityIds.isReinhardSwordOnly:153-158 · HeroTransformService:102,174 · SuperJumpController:38
      RadialMenuHud:276-278 · SoundEngineMixin:16 · SuperheroesClient:102,183,195-196,284,380 · AdminAbilityDebug:11
Кросс-геройское: ReinhardController:419-424 (вручную перечислены лучевые типы урона Homelander, Iron Man, Goku и босса)
      HeavensStrikeController:38 (мёртвый Variant.REINHARD внутри механики Raiden)
Ресурсы: 58 lang-ключей в каждом из двух языков, sounds/reinhard/, 8 иконок, royal_icicle.json
```

Чтобы целиком понять Рейнхарда, нужно открыть 37 своих и 20 общих файлов, то есть около 57.

**Regulus** — старый и сложный.

```text
Свои: ability/ ×5 · effect/ GreedCage RegulusGreed(282) RegulusMadness(488) RegulusMadnessState RegulusTotem
      item/ RegulusSuit Evangelion · network/ MadnessSync MadnessVisual
      C/ ClientMadnessState · hud/ MadnessHudOverlay CracksOverlayHud EvangelionZoomHud
Регистрации: Heroes:14,41 · AbilityIds:33-37 · AbilityRegistry:36-40,175-179 · SuperheroesMod:52-55 · ModNetworking:41-42
      ClientNetworking:131-141 · ModAttachments:20-21,24 · SuperheroesClient:177-179,181
Данные в общих файлах: HeroAttributes:22-26,180-188 · HeroHudConfig:43 · CombatImpactEngine:313 · JarvisThreatClass:47
      ModDamageTypes:19-20,63-64,110,114
Логика в общих системах: HeroTransformService:65-66,100-101 · LivingEntityFallDamageMixin:26 · ClientAbilityFilter:36
      SuperJumpController:33 · AbilitiesTooltipHud:225 · SuperheroesClient:229
      файлы с общими именами, работающие только для него: BloodRainHud ClientHudGlitch GuiVanillaGlitchMixin GameRendererFovMixin
Кросс-геройское: ThanosStoneRewardController:41 · PandoraHero:111 берёт RegulusHero.THEME
```

Эффекты безумия Регулуса (глитчи, трещины, кровавый дождь) встроены в общий HUD и миксины, а не подключаются из модуля героя.

**Pandora** — самый новый герой, переименован из Doctor Strange.

```text
Свои: ability/ ×5 · effect/ MirrorDimension(451) PandoraDeath(255) SpatialBind VanityAuthority VanityStrippedMobEffect
      item/ DoctorStrangeSuitItem · network/ ×4 · C/ 3 состояния · hud/ ×3 · iris/ ×2
      mixin/ ×5 (4 блокировки ввода + FontVanityCipher)
Регистрации: Heroes:33,60 · AbilityIds:144-150 (комментарий «// Doctor Strange») · AbilityRegistry:143-147,266-270
      SuperheroesMod:39-40,103,138,141-151 (tick, disconnect, ALLOW_DAMAGE, ALLOW_DEATH) · ModNetworking:65-68,98-100
      ClientNetworking:88-115 · ModEffects:36-38,65-66 · ModItems:205-207 · ModItemGroups:72 · ModSounds:34-35
Данные в общих файлах: HeroAttributes:431,445-446 · ModDamageTypes:50,94,234
Логика в общих системах: AbilityRouter:28-32,50-57 · ClientAbilityFilter:24-26,30,33,41 · HeroTransformService:63
      SuperheroesClient:55-57,185,198,276-277 · HeroComponentStripMixin:29-43 (совместимость с другим модом)
```

Следы переименования так и остались: класс `DoctorStrangeSuitItem`, id предмета `doctor_strange_suit` (`ModItems:206`), его модель и 5 lang-ключей, а текстура `textures/entity/hero/doctor_strange.png` вообще ни на что не ссылается. Это прямое следствие того, что у героя нет единого модуля.

**Thanos** — герой, построенный на предметах.

```text
Свои: ability/ ×7 · effect/ ThanosGauntletState ThanosSnapWindup ThanosStoneReward ThanosCrossModSnapHook SnappedMobEffect
      item/ InfinityGauntlet · item/infinity/ ×3 · mixin/ ThanosBlockBreaking · network/ ×2
      C/ ClientThanosState ThanosSkinTextures · render/ CosmicBeamRenderer
Регистрации: Heroes:22,49 · AbilityIds:87-93 · AbilityRegistry:87-93,218-224 · SuperheroesMod:80,86,98 · ModItems:110-142
      ModEffects:26-28 · ModNetworking:37,48 · ClientNetworking:83-84,166-167 · ModSounds:23 · SuperheroesClient:62,278
Данные в общих файлах: HeroAttributes:96-105,292-302,450 · HeroTheme:170 · HeroHudConfig:36 · CombatImpactEngine:307
      JarvisThreatClass:43 · ModDamageTypes:36-39,80-83,178-190
Логика в общих системах: AbilityRouter:23-27 (DISABLED_ABILITIES от Snap и Soul Pulse), 45-48 · ClientAbilityFilter:28,38,51-56
      RadialMenuHud:194,271-272,489-532 · PlayerRendererMixin:56-58 · AbstractClientPlayerSkinMixin:47-51
      SuperJumpController:36 · TooltipFrame:41
Кросс-геройское: ThanosStoneRewardController:36-41 (какой герой какой камень даёт); то же соответствие продублировано
      в предметах шести других героев (BladeOfChaosItem:26, NarutoHeadbandItem:26 и т. д., вызов TooltipFrame.containsStone(...))
```

Итого одно и то же соответствие «герой → камень» записано в 7 местах.

**Doomsday** — доступ к способностям зависит от уровня.

```text
Свои: ability/ ×6 (ChargeTackle и DoomGrip без префикса героя) · effect/ ×7 · item/ DoomsdaySuit KryptoniteShard
      mixin/ KryptoniteShardPickup · network/ DoomsdayProgress · C/ ClientDoomsdayState · hud/ DoomsdayGlitchHud
Регистрации: Heroes:16,43 · AbilityIds:46-51 · AbilityRegistry:49-54,186-191 · SuperheroesMod:73-75,85,102,108
      ModAttachments:30-32 · ModNetworking:46 · ClientNetworking:155-156 · ModItems:75-77,145-147 · ModSounds:22
      SuperheroesClient:182
Данные в общих файлах: HeroAttributes:35-49,200-211,462-476 · HeroHudConfig:35 · CombatImpactEngine:293,304
      JarvisThreatClass:44 · ModDamageTypes:21-25,65-69,118-134
Логика в общих системах: AbilityRouter:41-44 · таблица уровней продублирована в ClientAbilityFilter:58-64 и DoomsdayHero:129-136
      SuperheroesMod:157-158 (исключение в AFTER_DEATH) · HeroTransformService:179 · HeroMeleeImpactController:252-257
      LivingEntityEffectMixin:21-22 · SuperheroesCommands:63-67,283-298 · LowResourceVignetteHud:26
```

**Sung Jinwoo** — герой с призываемыми сущностями, самый изолированный из сложных.

```text
Свои: ability/ ×6 (ни одна не содержит имени героя) · effect/ SungJinwooController(329) MonarchsDomainController
      entity/ ShadowSoldier(387) · item/ ShadowMonarchsCloak · network/ SungShadowArmy
      C/ ClientShadowArmyState · render/ ShadowSoldierRenderer
Регистрации: Heroes:15,42 · AbilityIds:39-44 · AbilityRegistry:42-47,180-185 · SuperheroesMod:71-72 · ModEntities:20,23,84
      ModNetworking:45 · ClientNetworking:151-152 · ModItems:70-72 · SuperheroesClient:66
Данные в общих файлах: HeroAttributes:28-33,191-197 · HeroHudConfig:34 · CombatImpactEngine:312 · JarvisThreatClass:51
Дубли: второй скин выбирается в двух миксинах по разным условиям: AbstractClientPlayerSkinMixin:44 (hasShadows)
      и PlayerRendererMixin:47 (isPhase2)
```

## 4. Где общий код знает конкретных героев

1. **Ручные реестры.** По смыслу знают всех, но заполняются руками:
   - `Heroes`, `AbilityIds`, `AbilityRegistry`, `ModItems`, `ModItemGroups` (22 героя каждый);
   - `ModEntities`, `ModSounds`, `ModParticles`, `ModEffects`, `ModAttachments`, `ModDamageTypes` вместе с datagen;
   - сеть: `ModNetworking:24-68` (44 payload) и `ClientNetworking:40-216` (36 обработчиков);
   - bootstrap: `SuperheroesMod:28-99` (около 70 вызовов `init()`) и `:107-128` (15 per-player тиков);
   - клиент: `SuperheroesClient` (24 вызова HUD в `:161-199`, 32 фабрики частиц в `:107-158`), `ModKeys`, два mixin-конфига.
2. **Данные героя лежат в общих файлах:**
   - `HeroAttributes` (483 строки, 17 героев), `HeroTheme`, `HeroHudConfig`;
   - таблицы в `CombatImpactEngine`, `JarvisThreatClass`, `PassiveIcons`, `AbilityDescriptions`;
   - `HeroBleedingController:29-33`, список `SuperJumpController:33-38`, `HeroReactionController:24-29`, `LowResourceVignetteHud:24-26`.

   Рейтинг силы героев вообще записан дважды: `CombatImpactEngine:303-323` и `JarvisThreatClass:41-66`.
3. **Ветки под героев внутри общих систем:**
   - `AbilityRouter:20-32` (эффекты-блокировки Homelander, Thanos и Pandora проверяются поимённо), `:41-57,66,134,137`;
   - `ClientAbilityFilter` целиком;
   - вручную перечисленные очистки в `HeroTransformService:63-66,99-104,174,179,191`;
   - подсистема полёта (`FlightMode`, `FlightController`, `FlightProfiles`, `FlightAbilityState`, `FlightAbility`);
   - skin-миксины;
   - HUD: `HeroInfoPanelHud` (панель Iron Man), `RadialMenuHud` (Thanos, Reinhard), `AbilitiesTooltipHud` (Regulus);
   - `AbilityIds.isReinhardSwordOnly`.
4. **Миксины.** Из 32 миксинов 15 обслуживают ровно одного героя:
   - Pandora: 4 блокировки ввода, `FontVanityCipher`, `HeroComponentStrip` (совместимость с другим модом);
   - Regulus: `GameRendererFov`, `GuiVanillaGlitch`;
   - Doomsday: `LivingEntityEffect`, `KryptoniteShardPickup`;
   - Reinhard: `SoundEngine`; Thanos: `ThanosBlockBreaking`;
   - 3 миксина «оружие нельзя выбросить» (Reinhard, Raiden, Rem).

   Ещё 6 общих миксинов содержат ветки под героев: два skin-миксина, `GuiHotbar`, `PlayerModelPose`, `LivingEntityFallDamage`, `LocalPlayerFlight`. На момент снимка открыт PR #37: он заменяет три no-drop миксина одним общим `PlayerBoundWeaponDropMixin` и пакетом `item/bound/`, что совпадает с рекомендацией раздела 6.
5. **Герои знают друг о друге.** Большую часть этих связей лучше выразить тегами или данными, а не импортами классов:
   - `ReinhardController:419-424` и `DoomsdayAdaptationController:71-81` перечисляют чужие типы урона (Homelander, Iron Man, Goku, босс Хоумлендер) — нужны теги типов урона;
   - `RegulusMadnessController:138-146` выключает полёт по конкретным id, хотя в `FlightAbilityState.isFlightAbility` уже есть такая проверка;
   - `InvincibleCombatController:55,117`, `HeavensStrikeController:38`, `ThanosStoneRewardController`.

Что стоит сохранить, потому что это уже здоровые механизмы:

- хуки `Hero.onLanded` (`HeroLandingTracker:140`), `cancelsFallDamage`, `getTheme`, `getHudConfig`, `getSkinTexture`;
- поиск иконок по пути с отдельной иконкой для героя (`AbilityIcons:26-42`);
- параметризованная механика `HeavensStrikeController.Variant`;
- подпакет `ability/ironman/`.

## 5. Целевая структура

```text
<root>/
  core/      hero (Hero, HeroProfile, HeroModule, реестр) · ability (контракт, авто-реестр, AbilityRouter без веток)
             resource · transform/HeroData · lifecycle (события и диспетчер тиков) · net (общие payload)
  mechanic/  flight · impact · shockwave · strike · beam · charge · summon · damage (pipeline и теги)
             · общие способности (FLIGHT у 4 героев, VILTRUMITE_RECOVERY у 2)
  hero/<id>/ <Id>Module · <Id>Hero · ability/ · runtime/ (бывшие controllers) · item/ · entity/ · net/ · <Id>Abilities (id)
  content/   horde/ · boss/homelander/ · commands
  compat/    iris · falbiks · veil
client/
  core/      HUD-фреймворк (реестр слоёв) · input · SkinResolver · fx · config
  hero/<id>/ <Id>ClientModule · hud/ · render/ · state/ · input/ · screen/
```

```java
public interface HeroModule {
    Hero hero();                                        // статы, пассивки, HeroProfile
    void registerAbilities(AbilitySink sink);           // вместо AbilityIds/AbilityRegistry
    default void registerContent(ContentSink sink) {}   // предметы, сущности, звуки, эффекты, attachments, типы урона
    default void registerNetworking(PayloadSink sink) {}
    default void registerHooks(HeroHooks hooks) {}      // тики, lifecycle, условия доступа, фазы урона
}
```

- Модули перечисляются явным списком: одна строка на героя, это легко найти поиском и проверить тестом. Позже для аддонов можно добавить Fabric-entrypoint через `HeroApi`.
- Слушатели событий регистрируются в явных фазах, а не в порядке строк `init()`.
- Геройские миксины по возможности заменяются общими хуками: `ClientInputLock`, реестры модификаторов FOV, звука и HUD-джиттера, единый no-drop механизм уберут 7–9 миксинов, а `SkinResolver` очистит от веток оба skin-миксина. Оставшиеся лягут в `mixin/hero/<id>/`.
- Ресурсы физически в модуль не переезжают: большинство типов ресурсов Minecraft ищет по фиксированным путям (`lang/`, `models/`, `sounds.json`, `particles/`, `data/<ns>/<тип>/`), а lang — один файл на язык. Вместо этого нужны строгие подпапки по id героя и sanity-проверка полноты. Каталоги нужно нормализовать: сейчас `sounds/ironman`, `sounds/unibeam` и `textures/entity/hero/ironman*.png` не совпадают с id `iron_man`.

## 6. Что оставить общим, а что перенести в модуль героя

| Остаётся общим | Переезжает в модуль героя | Пограничные случаи |
|---|---|---|
| Контракты `Hero` и `Ability`, реестры, поток `AbilityRouter`, общая проверка «эффект блокирует способности» по тегу | Класс героя, его статы, пассивки, тема, конфиг HUD, глифы пассивок, боевой профиль, уровень угрозы | `JarvisThreatClass`: HUD Jarvis остаётся у Iron Man, а уровень угрозы объявляет каждый герой в `HeroProfile` |
| `ResourceController` (откат с Energy на Mana), `HeroData`, трансформация, lifecycle, кулдауны | Способности героя, их id и runtime-контроллеры | Соответствие «герой → камень»: данные по id героев, но владеет ими модуль Thanos; подсказки в предметах через client tooltip-хук |
| Физика полёта (математика, фазы, настройки), движок удара, заряд ближнего боя, landing, shockwave, разрушение блоков | Уникальные сущности: ShadowSoldier, IronLegionDrone, SmartMissile, KageBunshin, ShieldProjectile, Ram | Режимы полёта Iron Man и лимиты Хоумлендера — профили и модификаторы, которые предоставляет герой |
| Каркас для урона: pipeline, теги, политика разрушения мира | Декларации своих типов урона, звуков, частиц, эффектов (включая Snap и Vanity Strip), attachments | `HeavensStrike` и подобные: механика общая, параметры (варианты) задаёт герой |
| Сетевая инфраструктура и общие payload (активация, привязка, синхронизация, кулдауны, тряска экрана, обломки) | Свои payload вместе с клиентскими обработчиками, Client*State, оверлеи, рендереры, экраны, клавиши | Безумие Хоумлендера и безумие Регулуса — каждое в свой модуль, с разными именами |
| HUD-фреймворк и конвенция иконок `AbilityIcons` | Миксины, нужные только одному герою (если их нельзя заменить общим хуком) | Босс Хоумлендер и орда — отдельные content-модули |
| Один механизм «оружие нельзя выбросить», база для компаньонов, блокировка управления сущностью | Урановые предметы (Homelander), Kryptonite (Doomsday), сет камней (Thanos) | Общие способности (FLIGHT, VILTRUMITE_RECOVERY) — в `mechanic/`, иконка под героя уже поддерживается |

## 7. План миграции

Каждый шаг — отдельный небольшой PR с зелёным `qualityGate`. Строковые id (способностей, attachments, предметов, звуков) не меняются, поэтому сохранения и lang не затрагиваются. Начинать стоит после багфикс-PR №1–3 из базового аудита, иначе баги переедут в новую структуру.

0. **Страховка, размер S.**
   - Проверка полноты героя в `ProjectSanityTest`. Она сразу покажет пропуски у Scorpion и Pandora.
   - `assertControllersAreWired` (`ProjectSanityTest:195-213`) сейчас принудительно держит всю инициализацию в `SuperheroesMod`. Её нужно заменить проверкой списка модулей.
   - Добавить source-check «общие пакеты не импортируют `hero.*`» со списком исключений, который со временем только сокращается.
   - Переименование корневого пакета `com.example` лучше сделать до физических переносов.
1. **Данные в класс героя, поведение не меняется, размер S.** Константы `HeroTheme`, `HeroHudConfig` и наборы из `HeroAttributes` переезжают в классы героев. В `Hero` появляется `HeroProfile`: стиль и сила удара, угроза, пассивки, кровотечение, суперпрыжок. После этого исчезают 7 общих таблиц (`HeroTheme`, `HeroHudConfig`, наборы `HeroAttributes`, таблицы `CombatImpactEngine`, `JarvisThreatClass`, `PassiveIcons`, `AbilityDescriptions`) и списки в `HeroBleedingController` и `SuperJumpController`. Перед удалением — snapshot-тест, что значения совпадают со старыми таблицами.
2. **Хуки вместо веток в ядре, размер M.** Совпадает с п. 11 из §5 базового аудита (`HeroLifecycleEvents`, `HeroTickDispatcher`, хуки `Hero`) — их стоит делать вместе.
   - Условие доступа к способности на сервере плюс синхронизированный набор разблокированных способностей. Это убирает ветки в `AbilityRouter` и дубли в `ClientAbilityFilter`.
   - Lifecycle-хуки вместо ручных списков очистки.
   - Диспетчер тиков вместо `SuperheroesMod:107-128`.
   - Теги типов урона и способностей.
   - Профили полёта, реестры HUD-слоёв и клавиш, `SkinResolver`, авто-сброс клиентских состояний.
3. **Физический перенос, по одному PR на героя.**
   - Пилот — Scorpion (11 файлов, почти без связей с другими героями).
   - Затем Reinhard: на нём проверяются все точки расширения.
   - Дальше — по возрастанию связей с другими героями; простых героев можно объединять.
4. **Документация.**
   - Обновить AGENTS.md §2: сейчас он прямо описывает старую раскладку (`AbilityIds`, `AbilityRegistry`, `effect/*Controller` в `SuperheroesMod`).
   - Добавить skill `add-hero` по шаблону модуля.
   - Убрать остатки Doctor Strange; переименовать вводящие в заблуждение Java-классы, не меняя их id.

Отдельные Gradle-модули на каждого героя не советую: для одного jar это дорого (Loom, datagen, run-конфигурации), а пакеты плюс sanity-правила дают почти всю пользу.

Критерии успеха:

- 2 каталога на героя (`hero/<id>/` в main и в client) вместо 3–11 пакетов;
- ноль общих файлов с данными героя;
- добавление героя — одна строка в списке модулей плюс lang и ресурсы.
