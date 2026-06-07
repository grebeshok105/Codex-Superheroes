---
name: project-profile
description: Use when working in this repo — provides current identifiers, versions, content map, and architectural decisions for the Superheroes Fabric mod.
triggers: ["model"]
---

# Project Profile — Superheroes Mod

## Идентификаторы

- Mod ID: `superheroes`
- Display Name: `Superheroes Mod`
- Java package: `com.example.superheroes`
- Main class: `com.example.superheroes.SuperheroesMod`
- Client class: `com.example.superheroes.client.SuperheroesClient`
- Datagen class: `com.example.superheroes.datagen.SuperheroesDataGenerator`
- Branch: `work/v3.12.2` in worktree `grebeshok105-v3.12.2`

## Версии

- Current mod version: `3.12.2`
- Base tag: `v3.12.2`
- Minecraft: `1.21`
- Loader: Fabric (`fabric-loader 0.19.2`, `fabric-api 0.102.0+1.21`)
- Java target: 21
- Mappings: official Mojang mappings (Yarn не используется)
- Loom: `1.16-SNAPSHOT`
- GeckoLib: `geckolib-fabric-1.21:4.5.8`

## Текущее содержимое

Зарегистрировано 13 героев в `Heroes.java`:

`Homelander`, `Iron Man`, `Regulus`, `Sung Jin-Woo`, `Doomsday`, `Goku`, `Naruto`, `Captain America`, `Kratos`, `Loki`, `Thanos`, `Reinhard van Astrea`, `Raiden Shogun`.

Зарегистрировано 79 abilities в `AbilityRegistry.java` на базе `v3.12.2`.

## Главные системы

- **Hero / HeroData / HeroTransformService** — лайфцикл трансформации, passives, dimensions, sync.
- **Ability + AbilityRegistry + AbilityIds + AbilityRouter** — модульные toggle/active способности.
- **ResourceController** — двойной ресурс Energy/Mana с ability bindings и fallback.
- **ModNetworking** — typed `CustomPayload` C2S/S2C пакеты Minecraft 1.21.
- **effect/*Controller** — server-side tick-системы: ульты, состояния, прогрессия, слабости.
- **RadialMenuHud / ResourceBarHud / HeroTheme** — real-time UI способностей и ресурсов.
- **client/render + client/fx** — beams, overlays, lightning, ESP, screen shake.
- **api/** — публичный addon API; детали в `docs/api.md`.

## Чего быть НЕ должно

- Лазеры не разрушают блоки; специальные исключения должны быть явно заложены в ability.
- Меню способностей не паузит мир.
- Ванильный glowing для Iron Man BoxESP не используется.
- Дефолтные звуки молнии не заменяются напрямую; кастомные звуки миксуются через `assets/minecraft/sounds.json`.
- Старый `PacketByteBuf`-style networking не использовать; только typed payloads + `StreamCodec`.

## Запрещённые без явного разрешения файлы

Если пользователь не сказал явно — НЕ модифицировать:

- `Hero.java` (interface, ломает все реализации)
- `HeroData.java` (изменения требуют attachment migration)
- `ResourceController.java` (центральная балансировка ресурсов)
- `*Hud.java` (HUD-рендер)
- `HeroTheme.java`
- `LivingEntityFallDamageMixin.java`
- `HeroLandingTracker.java` (исключение: точечный skip для busy-state, если пользователь просил)

## Паттерны добавления

- Новая способность: `AbilityIds` + файл `ability/<Name>Ability.java` + регистрация в `AbilityRegistry.init()` + добавить в `Hero.getAbilities()`.
- Новый controller: `effect/<Name>Controller.java` + обязательный `<Name>Controller.init()` в `SuperheroesMod.onInitialize()`.
- Новый предмет: `item/<Name>Item.java` + `ModItems.java` + модели/текстуры/lang.
- Новый звук: `assets/superheroes/sounds/<group>/<file>.ogg` + `assets/superheroes/sounds.json` + `ModSounds.register("group.event")`.
- Новая локализация: всегда обновлять и `en_us.json`, и `ru_ru.json`.
