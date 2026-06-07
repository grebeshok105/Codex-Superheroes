---
trigger: model_decision
description: "Non-obvious подводные камни Fabric и NeoForge для MC 1.21+. Использовать когда модель собирается писать loader-specific код."
---

# Loader Gotchas

Только то, что **не** найти за один взгляд в код / Mojang docs / Fabric wiki / NeoForge docs.

## Fabric

- `@WrapOperation` (MixinExtras) предпочитать `@Redirect` — Redirect конфликтует с другими модами
- В миксинах все приватные поля и методы помечать `@Unique` — иначе скрытый конфликт байт-кода
- `net.fabricmc.fabric.impl.*` — internal, не импортировать никогда
- Для регистрации сетевых каналов **не** ставить `@Environment` — Fabric сам разделит client/server, иначе крах

## NeoForge

- `@SubscribeEvent` **удалён**, использовать `modEventBus.addListener(...)` или `NeoForge.EVENT_BUS.addListener(...)`
- `DistExecutor` устарел, заменён `EffectiveSide.get()` или `@EventBusSubscriber(value = Dist.CLIENT)`
- Конструктор `@Mod` принимает `IEventBus` параметром (1.20.5+), нельзя получать через `FMLJavaModLoadingContext.get()` как в legacy Forge
- Networking перешёл на `IPayloadHandler<T>` + `CustomPacketPayload`, старый `SimpleChannel` удалён

## Общее (Mojang mappings, 1.21+)

- `Identifier.of("modid", "name")` — конструктор `new Identifier(...)` удалён в 1.21
- `Component` (NeoForge) и `Text` (Fabric) — это один и тот же тип `net.minecraft.network.chat.Component`, разные YARN-обёртки больше не используются
- DataGen всегда генерируй в `src/generated/resources/`, не правь руками сгенерированное
