package io.github.grebeshok105.codex.hero.homelander.runtime;

import net.minecraft.core.BlockPos;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bounded per-position re-mark cooldown for laser scorch placement. A beam
 * parked on one block-face for minutes would otherwise write a block every
 * tick; the journal rate-limits re-marks of a cell to one per
 * {@code cooldownTicks}. Capacity-bound LRU so long beams over large walls
 * cannot grow it without limit — evicted cells simply become markable again,
 * which is fine because the world itself still rejects already-scorched cells.
 */
final class ScorchJournal {
	private final int capacity;
	private final long cooldownTicks;
	private final Map<Long, Long> marks = new LinkedHashMap<>(64, 0.75f, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<Long, Long> eldest) {
			return size() > capacity;
		}
	};

	ScorchJournal(int capacity, long cooldownTicks) {
		this.capacity = capacity;
		this.cooldownTicks = cooldownTicks;
	}

	/**
	 * {@code true} when the cell may be (re)marked now; the first call for a
	 * position always succeeds and starts its cooldown.
	 */
	boolean tryMark(BlockPos pos, long gameTime) {
		return tryMark(pos.asLong(), gameTime);
	}

	boolean tryMark(long posKey, long gameTime) {
		Long last = marks.get(posKey);
		if (last != null && gameTime - last < cooldownTicks) {
			return false;
		}
		marks.put(posKey, gameTime);
		return true;
	}
}
