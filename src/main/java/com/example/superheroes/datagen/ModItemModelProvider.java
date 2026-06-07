package com.example.superheroes.datagen;

import com.example.superheroes.item.ModItems;
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
		generator.generateFlatItem(ModItems.MILK_BOTTLE, ModelTemplates.FLAT_ITEM);
	}
}
