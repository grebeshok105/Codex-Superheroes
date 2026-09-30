package io.github.grebeshok105.codex.hero.homelander;

import io.github.grebeshok105.codex.hero.homelander.block.LaserScorchBlock;
import net.minecraft.world.level.block.Block;

/** Module-owned blocks; each leaf class owns its registration, this only aliases them. */
public final class HomelanderBlocks {
	public static final Block LASER_SCORCH = LaserScorchBlock.INSTANCE;

	private HomelanderBlocks() {
	}

	/** Forces class initialization so every block is registered. */
	public static void init() {
	}
}
