---
name: loader-gotchas
description: Use when writing Fabric or NeoForge specific code, mixins, networking, or registration code in MC 1.21+. Non-obvious traps that aren't in standard docs.
triggers: ["model"]
---

# Loader Gotchas (MC 1.21+)

Только то, что **не** найти за один взгляд в Mojang docs / Fabric wiki / NeoForge docs.

## Fabric (этот проект)

- `@WrapOperation` (MixinExtras) предпочитать `@Redirect` — Redirect конфликтует с другими модами
- В миксинах все приватные поля и методы помечать `@Unique` — иначе скрытый конфликт байт-кода
- `net.fabricmc.fabric.impl.*` — internal, не импортировать никогда. Только `net.fabricmc.fabric.api.*`
- Для регистрации сетевых пакетов **не** ставить `@Environment` — Fabric сам разделит client/server
- `ServerTickEvents.END_SERVER_TICK` runs after entity ticks. Если ставишь `setDeltaMovement(0)` — гравитация всё равно успеет добавить -0.08 в следующем тике до твоего хука. Для жёсткого якоря используй `connection.teleport(...)` назад к точке
- HeroData с `.copyOnDeath()` сохраняет attachment через смерть — на death-event надо вручную чистить, иначе игрок «зомби-герой» без passives

## NeoForge (если когда-нибудь будет порт)

- `@SubscribeEvent` устарел в новых версиях, использовать `modEventBus.addListener(...)`
- `DistExecutor` устарел, заменён `EffectiveSide.get()` или `@EventBusSubscriber(value = Dist.CLIENT)`
- Конструктор `@Mod` принимает `IEventBus` параметром (1.20.5+)
- Networking перешёл на `IPayloadHandler<T>` + `CustomPacketPayload`, старый `SimpleChannel` удалён

## Общее (Mojang mappings, 1.21+)

- `ResourceLocation.fromNamespaceAndPath(...)` или `ResourceLocation.parse(...)` — конструктор `new ResourceLocation(...)` удалён
- `Component` — единый тип `net.minecraft.network.chat.Component` (Yarn-обёртки `Text` больше не используются)
- `Items.ELYTRA` is `ElytraItem` (`extends Item implements Equipable`); `ArmorItem` для брони. Чтобы детектить экипировку — `instanceof ArmorItem` + `instanceof ElytraItem`
- `Inventory.armor` — публичный `NonNullList<ItemStack>` слотов (boots/legs/chest/helmet)
- `LivingEntity` MobEffect API: `addEffect(new MobEffectInstance(MobEffects.X, duration, amplifier, ambient, showParticles, showIcon))`. Бесконечный эффект — `duration = -1`
- В этом репо DataGen выводится в `src/main/generated/` (см. `build.gradle`), не править generated JSON руками — менять DataProvider и запускать `runDatagen`
