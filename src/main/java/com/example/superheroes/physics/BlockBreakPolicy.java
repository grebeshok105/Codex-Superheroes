package com.example.superheroes.physics;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class BlockBreakPolicy {
	private BlockBreakPolicy() {
	}

	public static boolean canImpactBreak(ServerLevel level, BlockPos pos, BlockState state, float maxHardness) {
		if (state.isAir() || state.liquid() || isProtected(state)) {
			return false;
		}
		float hardness = state.getDestroySpeed(level, pos);
		return hardness >= 0f && hardness <= maxHardness;
	}

	public static boolean isProtected(BlockState state) {
		return state.is(Blocks.BEDROCK)
				|| state.is(Blocks.BARRIER)
				|| state.is(Blocks.COMMAND_BLOCK)
				|| state.is(Blocks.CHAIN_COMMAND_BLOCK)
				|| state.is(Blocks.REPEATING_COMMAND_BLOCK)
				|| state.is(Blocks.STRUCTURE_BLOCK)
				|| state.is(Blocks.JIGSAW)
				|| state.is(Blocks.END_PORTAL)
				|| state.is(Blocks.END_PORTAL_FRAME)
				|| state.is(Blocks.OBSIDIAN)
				|| state.is(Blocks.CRYING_OBSIDIAN)
				|| state.is(Blocks.REINFORCED_DEEPSLATE);
	}
}
