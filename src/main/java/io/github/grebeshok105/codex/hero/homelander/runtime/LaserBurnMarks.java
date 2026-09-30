package io.github.grebeshok105.codex.hero.homelander.runtime;

import io.github.grebeshok105.codex.hero.homelander.block.LaserScorchBlock;
import io.github.grebeshok105.codex.mechanic.world.WorldDestructionPolicy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Leaves persistent {@link LaserScorchBlock} burn marks where the Homelander
 * eye beam terminates on a block face. The mark is a real block state (never
 * expires, survives chunk unload and restarts) placed in the air cell in
 * front of the hit block, attached to the face the beam touched.
 *
 * <p>Spam control: a bounded {@link ScorchJournal} per-cell re-mark cooldown
 * ({@value #REMARK_COOLDOWN_TICKS} ticks) plus a minimum spacing
 * ({@value #MIN_SPACING} blocks) so a sweeping or parked beam cannot write
 * thousands of marks. Never marks unbreakable / {@code ability_immune}
 * blocks or fluid cells. Placement goes through
 * {@link WorldDestructionPolicy#tryPlace} so spawn protection and claim
 * plugins can veto.
 */
public final class LaserBurnMarks {
	private static final int REMARK_COOLDOWN_TICKS = 200;
	private static final int JOURNAL_CAPACITY = 512;
	private static final int MIN_SPACING = 2;

	private static final ScorchJournal JOURNAL = new ScorchJournal(JOURNAL_CAPACITY, REMARK_COOLDOWN_TICKS);

	private LaserBurnMarks() {
	}

	/**
	 * Attempts to scorch the face of {@code blockHit}; no-ops (silently)
	 * whenever a rule says no — this runs every tick the beam touches blocks.
	 */
	public static void tryMark(ServerLevel level, BlockHitResult blockHit, @Nullable Entity cause) {
		BlockPos hitPos = blockHit.getBlockPos();
		BlockState hitState = level.getBlockState(hitPos);
		if (!WorldDestructionPolicy.mayDestroy(level, hitPos, hitState)) {
			return;
		}
		Direction face = blockHit.getDirection();
		Direction attachFace = face.getOpposite();
		BlockPos cell = hitPos.relative(face);
		BlockState cellState = level.getBlockState(cell);
		if (!cellState.getFluidState().isEmpty()) {
			return;
		}
		BlockState scorch = LaserScorchBlock.INSTANCE.defaultBlockState();
		if (cellState.is(scorch.getBlock())) {
			if (MultifaceBlock.hasFace(cellState, attachFace)) {
				return;
			}
			scorch = cellState;
		} else if (!cellState.isAir() && !cellState.canBeReplaced()) {
			return;
		}
		if (!JOURNAL.tryMark(cell, level.getGameTime())) {
			return;
		}
		if (scorchNearby(level, cell)) {
			return;
		}
		BlockState newState = LaserScorchBlock.INSTANCE
				.getStateForPlacement(scorch, level, cell, attachFace);
		if (newState != null) {
			WorldDestructionPolicy.tryPlace(level, cell, newState, cause);
		}
	}

	/** Any existing scorch within {@link #MIN_SPACING} blocks suppresses a new mark. */
	private static boolean scorchNearby(ServerLevel level, BlockPos cell) {
		BlockState scorch = LaserScorchBlock.INSTANCE.defaultBlockState();
		for (BlockPos pos : BlockPos.betweenClosed(
				cell.offset(-MIN_SPACING, -MIN_SPACING, -MIN_SPACING),
				cell.offset(MIN_SPACING, MIN_SPACING, MIN_SPACING))) {
			if (!pos.equals(cell) && level.getBlockState(pos).is(scorch.getBlock())) {
				return true;
			}
		}
		return false;
	}
}
