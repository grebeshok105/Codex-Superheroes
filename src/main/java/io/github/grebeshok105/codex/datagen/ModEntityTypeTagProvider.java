package io.github.grebeshok105.codex.datagen;

import io.github.grebeshok105.codex.tag.ModEntityTypeTags;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;

import java.util.concurrent.CompletableFuture;

public final class ModEntityTypeTagProvider extends FabricTagProvider<EntityType<?>> {
	public ModEntityTypeTagProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
		super(output, Registries.ENTITY_TYPE, registriesFuture);
	}

	@Override
	protected void addTags(HolderLookup.Provider registries) {
		// Regulus's little-king scan never claims these vanilla types as heart bearers —
		// bosses and tech entities excluded by data, matching design §1.
		getOrCreateTagBuilder(ModEntityTypeTags.HEARTLESS).add(
				EntityType.WITHER,
				EntityType.WARDEN,
				EntityType.ENDER_DRAGON,
				EntityType.ARMOR_STAND
		);
	}
}
