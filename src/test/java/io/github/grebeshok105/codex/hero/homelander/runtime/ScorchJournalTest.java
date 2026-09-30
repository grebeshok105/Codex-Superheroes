package io.github.grebeshok105.codex.hero.homelander.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ScorchJournal} — the bounded per-position re-mark cooldown so a
 * beam parked on one spot cannot write a new scorch block every tick.
 */
final class ScorchJournalTest {
	@Test
	void positionBlocksRemarkInsideCooldown() {
		ScorchJournal journal = new ScorchJournal(64, 200);

		assertTrue(journal.tryMark(1L, 1000));
		assertFalse(journal.tryMark(1L, 1100), "100 ticks later is inside the 200-tick cooldown");
		assertTrue(journal.tryMark(1L, 1200), "exactly at the cooldown boundary it re-marks");
	}

	@Test
	void positionsAreIndependent() {
		ScorchJournal journal = new ScorchJournal(64, 200);

		assertTrue(journal.tryMark(1L, 1000));
		assertTrue(journal.tryMark(2L, 1000), "a different position has its own cooldown");
	}

	@Test
	void oldestEntryEvictedPastCapacity() {
		ScorchJournal journal = new ScorchJournal(2, 200);

		assertTrue(journal.tryMark(1L, 1000));
		assertTrue(journal.tryMark(2L, 1000));
		assertTrue(journal.tryMark(3L, 1000), "third mark evicts the oldest entry");
		assertTrue(journal.tryMark(1L, 1001),
				"the evicted position is free to mark again immediately");
		assertFalse(journal.tryMark(3L, 1001), "the still-tracked position remains in cooldown");
	}
}
