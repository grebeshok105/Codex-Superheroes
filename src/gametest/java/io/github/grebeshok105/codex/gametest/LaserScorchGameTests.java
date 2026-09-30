package io.github.grebeshok105.codex.gametest;

import io.github.grebeshok105.codex.hero.homelander.HomelanderBlocks;
import io.github.grebeshok105.codex.hero.homelander.runtime.LaserBurnMarks;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Laser burn marks ({@link LaserBurnMarks}): the eye beam leaves persistent
 * {@code laser_scorch} decals attached to the face it terminated on —
 * never on unbreakable / {@code superheroes:ability_immune} blocks or inside
 * fluids — with spacing + per-cell cooldown so a parked beam cannot spam marks.
 */
public final class LaserScorchGameTests implements FabricGameTest {

	@GameTest(template = EMPTY_STRUCTURE)
	public void markAttachesOppositeTheHitFace(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		BlockPos support = new BlockPos(4, 2, 4);
		helper.setBlock(support, Blocks.STONE);

		markTopFace(helper, support, player);

		BlockPos cell = support.above();
		helper.assertBlockPresent(HomelanderBlocks.LASER_SCORCH, cell);
		helper.assertBlockProperty(cell, MultifaceBlock.getFaceProperty(Direction.DOWN), true);
		helper.assertBlockProperty(cell, MultifaceBlock.getFaceProperty(Direction.UP), false);
		helper.assertBlockProperty(cell, MultifaceBlock.getFaceProperty(Direction.NORTH), false);
		TestPlayers.leave(player);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void markAttachesToSideFace(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		BlockPos support = new BlockPos(4, 3, 4);
		helper.setBlock(support, Blocks.STONE);
		BlockPos abs = helper.absolutePos(support);
		BlockHitResult hit = new BlockHitResult(
				Vec3.atCenterOf(abs.north()), Direction.NORTH, abs, false);

		LaserBurnMarks.tryMark(helper.getLevel(), hit, player);

		BlockPos cell = support.north();
		helper.assertBlockPresent(HomelanderBlocks.LASER_SCORCH, cell);
		helper.assertBlockProperty(cell, MultifaceBlock.getFaceProperty(Direction.SOUTH), true);
		TestPlayers.leave(player);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void skipsUnbreakableAndImmuneBlocks(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		BlockPos bedrock = new BlockPos(4, 2, 4);
		BlockPos spawner = new BlockPos(6, 2, 4);
		helper.setBlock(bedrock, Blocks.BEDROCK);
		helper.setBlock(spawner, Blocks.SPAWNER);

		markTopFace(helper, bedrock, player);
		markTopFace(helper, spawner, player);

		helper.assertBlockNotPresent(HomelanderBlocks.LASER_SCORCH, bedrock.above());
		helper.assertBlockNotPresent(HomelanderBlocks.LASER_SCORCH, spawner.above());
		TestPlayers.leave(player);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void skipsFluidCells(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		BlockPos support = new BlockPos(4, 2, 4);
		helper.setBlock(support, Blocks.STONE);
		helper.setBlock(support.above(), Blocks.WATER);

		markTopFace(helper, support, player);

		helper.assertBlockNotPresent(HomelanderBlocks.LASER_SCORCH, support.above());
		TestPlayers.leave(player);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void spacingSuppressesNearbyMarks(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		// Marks within 2 blocks (Chebyshev) of an existing scorch are rejected;
		// a third support far enough away still gets marked.
		BlockPos near = new BlockPos(4, 2, 4);
		BlockPos tooClose = new BlockPos(6, 2, 4);
		BlockPos farEnough = new BlockPos(4, 2, 8);
		helper.setBlock(near, Blocks.STONE);
		helper.setBlock(tooClose, Blocks.STONE);
		helper.setBlock(farEnough, Blocks.STONE);

		markTopFace(helper, near, player);
		markTopFace(helper, tooClose, player);
		markTopFace(helper, farEnough, player);

		helper.assertBlockPresent(HomelanderBlocks.LASER_SCORCH, near.above());
		helper.assertBlockNotPresent(HomelanderBlocks.LASER_SCORCH, tooClose.above());
		helper.assertBlockPresent(HomelanderBlocks.LASER_SCORCH, farEnough.above());
		TestPlayers.leave(player);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void cooldownBlocksImmediateRemark(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		BlockPos support = new BlockPos(4, 2, 4);
		helper.setBlock(support, Blocks.STONE);
		markTopFace(helper, support, player);
		BlockPos cell = support.above();
		helper.assertBlockPresent(HomelanderBlocks.LASER_SCORCH, cell);

		// Scorch removed between beam passes: the per-cell cooldown still
		// suppresses an immediate re-mark.
		helper.setBlock(cell, Blocks.AIR);
		markTopFace(helper, support, player);

		helper.assertBlockNotPresent(HomelanderBlocks.LASER_SCORCH, cell);
		TestPlayers.leave(player);
		helper.succeed();
	}

	private static void markTopFace(GameTestHelper helper, BlockPos support, ServerPlayer player) {
		BlockPos abs = helper.absolutePos(support);
		BlockHitResult hit = new BlockHitResult(
				Vec3.atCenterOf(abs.above()), Direction.UP, abs, false);
		LaserBurnMarks.tryMark(helper.getLevel(), hit, player);
	}
}
