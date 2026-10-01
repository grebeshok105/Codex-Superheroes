package io.github.grebeshok105.codex.client.hero.regulus.emf;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link RegulusPoseMath}: the pure scalars behind the EMF variables —
 * the half-life-smoothed master weight, the authored blend windows, and the
 * clip clocks the jem's {@code var.regulus_<clip>_time} channels index.
 */
class RegulusPoseMathTest {

	@Test
	void approachHalvesTheGapAfterOneHalfLife() {
		float after3 = RegulusPoseMath.approach(0f, 1f,
				RegulusPoseMath.ACTIVE_HALF_LIFE_TICKS, 3f);
		assertEquals(0.5f, after3, 1e-4, "half-life 3 ticks: 0 -> ~0.5 after 3 ticks");
	}

	@Test
	void blendRisesAndFallsAtAuthoredRates() {
		// 4-tick blend window: one tick moves the weight by exactly 1/4.
		assertEquals(0.25f, RegulusPoseMath.blend(0f, 1f, 4f, 4f), 1e-6f);
		assertEquals(0.75f, RegulusPoseMath.blend(1f, 0f, 4f, 4f), 1e-6f);
		assertEquals(1f, RegulusPoseMath.blend(0.9f, 1f, 4f, 4f), 1e-6f);
		assertEquals(0f, RegulusPoseMath.blend(0.1f, 0f, 4f, 4f), 1e-6f);
	}

	@Test
	void loopTimeWrapsTheTimeline() {
		assertEquals(0.5f, RegulusPoseMath.loopTime(4.5f, 4f), 1e-6f);
		assertEquals(0f, RegulusPoseMath.loopTime(4f, 4f), 1e-6f);
		assertEquals(1f, RegulusPoseMath.loopTime(1f, 4f), 1e-6f);
	}

	@Test
	void oneShotClockClampsAtDuration() {
		RegulusPoseMath.ClipClock clock = new RegulusPoseMath.ClipClock();
		clock.start();
		assertTrue(clock.playing());
		assertEquals(0f, clock.time(), 1e-6f);

		clock.advance(1.0f, 2.0f);
		assertTrue(clock.playing());
		assertEquals(1.0f, clock.time(), 1e-6f);

		clock.advance(1.5f, 2.0f);
		assertFalse(clock.playing());
		assertTrue(clock.finished());
		assertEquals(2.0f, clock.time(), 1e-6f, "a finished one-shot holds its final frame");

		// Restart returns to frame 0 — the `var.regulus_<clip>_time` reset.
		clock.start();
		assertTrue(clock.playing());
		assertEquals(0f, clock.time(), 1e-6f);
	}
}
