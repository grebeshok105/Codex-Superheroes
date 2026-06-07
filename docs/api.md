# Superheroes Mod — Public Addon API

Стабильная точка расширения для других модов. Гарантируется обратная совместимость в пределах minor-версий.

## Что считается публичным API

| Где | Что |
|---|---|
| `com.example.superheroes.api.HeroApi` | facade для регистрации/получения героев и трансформации игроков |
| `com.example.superheroes.api.AbilityApi` | facade для регистрации/получения способностей и расхода ресурса |
| `com.example.superheroes.api.CreativeTabIds` | константы `ResourceKey<CreativeModeTab>` для встраивания в creative-вкладку |
| `com.example.superheroes.hero.Hero` (interface) | контракт героя — addon реализует свой класс |
| `com.example.superheroes.hero.HeroTheme` | цветовая тема героя |
| `com.example.superheroes.hero.LandingImpact` | DTO для `Hero.onLanded` |
| `com.example.superheroes.ability.Ability` (interface) | контракт способности |
| `com.example.superheroes.transform.HeroData` (record) | публичный data-class состояния игрока |
| `com.example.superheroes.resource.ResourceKind` (enum) | `ENERGY` / `MANA` |

## Что НЕ публично (internal)

Не ссылаться, не вызывать напрямую — может ломаться в любой минорной версии:

- `Heroes` (registry для встроенных героев), `AbilityRegistry` (registry встроенных способностей) — **используй `HeroApi.register` / `AbilityApi.register` вместо них**
- `HeroTransformService` — **используй `HeroApi.transform` / `HeroApi.untransform`**
- `ResourceController` — **используй `AbilityApi.tryConsume`**
- Все внутренние пакеты: `attachment`, `command`, `damage`, `datagen`, `effect.*Controller`, `entity`, `item`, `mixin`, `network`, `particle`, `physics`, `sound`
- Конкретные реализации героев (`HomelanderHero`, `IronManHero`, и т.д.) и способностей (`FlightAbility`, `EyeLasersAbility`, и т.д.) — это imp-detail
- `HeroAttributes` — список константных модификаторов для встроенных героев

## Минимальный пример addon-героя

`fabric.mod.json` аддона:

```json
{
  "depends": {
    "fabricloader": ">=0.19.2",
    "minecraft": "~1.21",
    "java": ">=21",
    "fabric-api": "*",
    "superheroes": "*"
  }
}
```

`build.gradle` аддона:

```gradle
dependencies {
    minecraft "com.mojang:minecraft:1.21"
    mappings loom.officialMojangMappings()
    modImplementation "net.fabricmc:fabric-loader:0.19.2"
    modImplementation "net.fabricmc.fabric-api:fabric-api:0.102.0+1.21"

    // Superheroes Mod jar (download from GitHub releases or local libs/)
    modImplementation files("libs/superheroes-2.3.0.jar")
}
```

Класс героя в аддоне:

```java
package com.youraddon.hero;

import com.example.superheroes.hero.Hero;
import com.example.superheroes.hero.HeroTheme;
import com.example.superheroes.resource.ResourceKind;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public final class MyHero implements Hero {
    public static final ResourceLocation ID =
        ResourceLocation.fromNamespaceAndPath("youraddon", "myhero");

    @Override public ResourceLocation getId() { return ID; }
    @Override public float getEnergyMax() { return 200f; }
    @Override public float getEnergyRegenPerTick() { return 1.0f; }
    @Override public float getManaMax() { return 100f; }
    @Override public net.minecraft.world.entity.EntityDimensions getDimensions(net.minecraft.world.entity.Pose pose) { return null; }
    @Override public List<ResourceLocation> getAbilities() {
        return List.of(/* your ability ids */);
    }
    @Override public ResourceKind getDefaultBinding(ResourceLocation abilityId) { return ResourceKind.ENERGY; }
    @Override public void applyPassives(Player player) { /* attribute modifiers, mob effects */ }
    @Override public void removePassives(Player player) { /* clean up */ }
    @Override public boolean cancelsFallDamage(Player player) { return false; }
    @Override public HeroTheme getTheme() { return HeroTheme.DEFAULT; }
}
```

Регистрация в `onInitialize` аддона:

```java
import com.example.superheroes.api.HeroApi;
import com.example.superheroes.api.AbilityApi;
import com.example.superheroes.api.CreativeTabIds;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;

public final class MyAddonMod implements ModInitializer {
    @Override
    public void onInitialize() {
        HeroApi.register(new MyHero());
        AbilityApi.register(new MyAbility());

        // Положить свои предметы в вкладку Superheroes:
        ItemGroupEvents.modifyEntriesEvent(CreativeTabIds.SUPERHEROES_TAB)
            .register(entries -> {
                entries.accept(MyItems.MY_TRANSFORM_ITEM);
                entries.accept(MyItems.MY_RESOURCE_ITEM);
            });
    }
}
```

Трансформация по предмету:

```java
@Override
public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
    if (player instanceof ServerPlayer sp) {
        if (HeroApi.hasHero(player) &&
                MyHero.ID.equals(HeroApi.getCurrentHeroId(player).orElse(null)) &&
                player.isShiftKeyDown()) {
            HeroApi.untransform(sp);
        } else {
            HeroApi.transform(sp, MyHero.ID);
        }
    }
    return InteractionResultHolder.success(player.getItemInHand(hand));
}
```

## Стабильность и версионирование

- Минорные версии (`2.3.x → 2.4.x`) — обратная совместимость API сохраняется. Можно будет добавить новые методы / классы; старые остаются.
- Major версии (`2.x → 3.x`) — потенциальные breaking changes; все они будут перечислены в release notes. Аддонам стоит закрепляться на конкретную major-версию через `gradle.properties` либо `"superheroes": ">=3 <4"` в fabric.mod.json.
- Эта документация и пакет `com.example.superheroes.api` — **источник истины**. Если чего-то нет в `api/`, не используй это в аддоне без обсуждения.

## Текущие ограничения

- Кастомные heroes сейчас вынуждены сами реализовывать `applyPassives(Player)` через прямые вызовы `player.getAttribute(...).addOrUpdateTransientModifier(...)` и `player.addEffect(...)`. Хелпера `HeroAttributesBuilder` пока нет — будет добавлен в API когда дозреют требования (пока лучше скопировать паттерн из `HomelanderHero.applyPassives` через рефлексию или вручную).
- Нет публичного hook-а для damage-pipeline (Divine Protection-стайл блокировка урона). Используй `ServerLivingEntityEvents.ALLOW_DAMAGE` (Fabric API) — там можно проверить `HeroApi.getCurrentHeroId(victim)` и вернуть false.
- Нет публичного API для ressetting transform-cooldown (используй `forceUntransform` если нужно обойти).
- Кастомные controllers (tick-based effect controllers основного мода) не описаны в API. Аддон может ставить свои controllers через стандартный Fabric `ServerTickEvents.END_SERVER_TICK.register(...)`.
