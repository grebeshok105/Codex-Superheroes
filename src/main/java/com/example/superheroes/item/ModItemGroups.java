package com.example.superheroes.item;

import com.example.superheroes.ModId;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public final class ModItemGroups {
	public static final ResourceKey<CreativeModeTab> SUPERHEROES_TAB_KEY = ResourceKey.create(
			Registries.CREATIVE_MODE_TAB, ModId.of("superheroes"));

	public static final CreativeModeTab SUPERHEROES_TAB = FabricItemGroup.builder()
			.icon(() -> new ItemStack(ModItems.HOMELANDER_SUIT))
			.title(Component.translatable("itemGroup.superheroes"))
			.displayItems((params, output) -> {
				output.accept(ModItems.HOMELANDER_SUIT);
				output.accept(ModItems.COMPOUND_V);
				output.accept(ModItems.MILK_BOTTLE);
				output.accept(ModItems.VOUGHT_SIGNAL);
				output.accept(ModItems.HOMELANDER_BOSS_SPAWN_EGG);

				output.accept(ModItems.IRON_MAN_SUIT);
				output.accept(ModItems.IRON_MAN_REACTOR);
				output.accept(ModItems.URANIUM_ISOTOPE);
				output.accept(ModItems.URANIUM_DAGGER);

				output.accept(ModItems.REGULUS_SUIT);
				output.accept(ModItems.EVANGELION);

				output.accept(ModItems.SHADOW_MONARCHS_CLOAK);

				output.accept(ModItems.DOOMSDAY_GENOME);
				output.accept(ModItems.KRYPTONITE_SHARD);

				output.accept(ModItems.GOKU_GI);

				output.accept(ModItems.NARUTO_HEADBAND);

				output.accept(ModItems.CAPTAIN_AMERICA_SUIT);
				output.accept(ModItems.VIBRANIUM_SHIELD);

				output.accept(ModItems.BLADE_OF_CHAOS);

				output.accept(ModItems.LOKI_SCEPTER);

				output.accept(ModItems.INFINITY_GAUNTLET);
				output.accept(ModItems.POWER_STONE);
				output.accept(ModItems.SPACE_STONE);
				output.accept(ModItems.REALITY_STONE);
				output.accept(ModItems.SOUL_STONE);
				output.accept(ModItems.TIME_STONE);
				output.accept(ModItems.MIND_STONE);

				output.accept(ModItems.REINHARD_SUIT);

				output.accept(ModItems.RAIDEN_SUIT);

				output.accept(ModItems.INVINCIBLE_SUIT);
				output.accept(ModItems.OMNIMAN_SUIT);
			})
			.build();

	private ModItemGroups() {
	}

	public static void init() {
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, SUPERHEROES_TAB_KEY, SUPERHEROES_TAB);
	}
}
