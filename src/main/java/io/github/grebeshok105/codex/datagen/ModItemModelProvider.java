package io.github.grebeshok105.codex.datagen;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.item.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.minecraft.data.models.BlockModelGenerators;
import net.minecraft.data.models.ItemModelGenerators;
import net.minecraft.data.models.model.ModelTemplates;

public final class ModItemModelProvider extends FabricModelProvider {
	public ModItemModelProvider(FabricDataOutput output) {
		super(output);
	}

	@Override
	public void generateBlockStateModels(BlockModelGenerators generator) {
	}

	@Override
	public void generateItemModels(ItemModelGenerators generator) {
		generator.generateFlatItem(ModItems.HOMELANDER_SUIT, ModelTemplates.FLAT_ITEM);
		generator.generateFlatItem(ModItems.IRON_MAN_SUIT, ModelTemplates.FLAT_ITEM);
		generator.generateFlatItem(ModItems.IRON_MAN_REACTOR, ModelTemplates.FLAT_ITEM);
		generator.generateFlatItem(ModItems.COMPOUND_V, ModelTemplates.FLAT_ITEM);
		// Hero-module items are resolved by id — datagen must not import hero packages.
		generator.generateFlatItem(item("milk_bottle"), ModelTemplates.FLAT_ITEM);
	}

	private static Item item(String path) {
		return BuiltInRegistries.ITEM.get(ModId.of(path));
	}
}
