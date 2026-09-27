package io.github.grebeshok105.codex.item;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.content.admin.AdminBuildVisibility;
import io.github.grebeshok105.codex.core.content.CreativeTabContents;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.List;

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

				output.accept(ModItems.IRON_MAN_SUIT);
				output.accept(ModItems.IRON_MAN_REACTOR);
				output.accept(ModItems.URANIUM_ISOTOPE);
				output.accept(ModItems.URANIUM_DAGGER);

				output.accept(ModItems.REGULUS_SUIT);
				output.accept(ModItems.EVANGELION);


				// Hero modules append their items here, in module registration order.
				for (ItemLike item : CreativeTabContents.all()) {
					output.accept(item);
				}

				// Админ-предметы показываются в этой же вкладке, когда у игрока
				// включён /superheroes admin (клиент пересобирает вкладку по пакету)
				if (AdminBuildVisibility.isClientVisible()) {
					for (Item adminItem : ModItemGroups.ADMIN_ONLY_ITEMS) {
						output.accept(adminItem);
					}
				}
			})
			.build();

	/**
	 * Предметы, доступные только через админ-билд ({@code /superheroes admin}).
	 * Не появляются в обычном креативе.
	 */
	public static final List<Item> ADMIN_ONLY_ITEMS = List.of(
			io.github.grebeshok105.codex.content.boss.homelander.HomelanderBossItems.HOMELANDER_BOSS_SPAWN_EGG,
			io.github.grebeshok105.codex.content.horde.HordeItems.HORDE_CRYSTAL
	);

	private ModItemGroups() {
	}

	public static void init() {
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, SUPERHEROES_TAB_KEY, SUPERHEROES_TAB);
	}
}
