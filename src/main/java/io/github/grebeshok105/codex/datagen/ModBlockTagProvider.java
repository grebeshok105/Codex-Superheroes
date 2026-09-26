package io.github.grebeshok105.codex.datagen;

import io.github.grebeshok105.codex.world.WorldDestructionPolicy;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.concurrent.CompletableFuture;

public final class ModBlockTagProvider extends FabricTagProvider<Block> {
	public ModBlockTagProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
		super(output, Registries.BLOCK, registriesFuture);
	}

	@Override
	protected void addTags(HolderLookup.Provider registries) {
		// Blocks abilities may never remove, on top of the vanilla
		// destroySpeed < 0 check (which already covers most of these).
		getOrCreateTagBuilder(WorldDestructionPolicy.ABILITY_IMMUNE).add(
				Blocks.BARRIER,
				Blocks.BEDROCK,
				Blocks.CHAIN_COMMAND_BLOCK,
				Blocks.COMMAND_BLOCK,
				Blocks.END_GATEWAY,
				Blocks.END_PORTAL,
				Blocks.END_PORTAL_FRAME,
				Blocks.JIGSAW,
				Blocks.LIGHT,
				Blocks.NETHER_PORTAL,
				Blocks.REINFORCED_DEEPSLATE,
				Blocks.REPEATING_COMMAND_BLOCK,
				Blocks.SPAWNER,
				Blocks.STRUCTURE_BLOCK,
				Blocks.STRUCTURE_VOID
		);
	}
}
