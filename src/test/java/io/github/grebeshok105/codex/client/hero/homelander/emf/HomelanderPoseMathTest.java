package io.github.grebeshok105.codex.client.hero.homelander.emf;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link HomelanderPoseMath}: the pure scalars behind the EMF variables —
 * the half-life-smoothed active weight and the free-running clip clocks the
 * jem's {@code keyframe(...)} tables index.
 */
class HomelanderPoseMathTest {

	@Test
	void activeWeightHalvesTheGapAfterOneHalfLife() {
		float after3 = HomelanderPoseMath.activeWeight(0f, true, 3f);
		assertEquals(0.5f, after3, 1e-4, "half-life 3 ticks: 0 -> ~0.5 after 3 ticks");
	}

	@Test
	void activeWeightConvergesToTarget() {
		float w = 0f;
		for (int i = 0; i < 60; i++) {
			w = HomelanderPoseMath.activeWeight(w, true, 1f);
		}
		assertEquals(1f, w, 1e-4);
		for (int i = 0; i < 60; i++) {
			w = HomelanderPoseMath.activeWeight(w, false, 1f);
		}
		assertEquals(0f, w, 1e-4);
	}

	@Test
	void activeWeightIsFrameRateIndependent() {
		// ten 0.1-tick steps land at the same value as one 1-tick step
		float fine = 0f;
		for (int i = 0; i < 10; i++) {
			fine = HomelanderPoseMath.activeWeight(fine, true, 0.1f);
		}
		float coarse = HomelanderPoseMath.activeWeight(0f, true, 1f);
		assertEquals(coarse, fine, 1e-4);
	}

	@Test
	void clipClockAccumulatesAndResets() {
		HomelanderPoseMath.ClipClock clock = new HomelanderPoseMath.ClipClock();
		assertEquals(0.25f, clock.advance(0.25f), 1e-6);
		assertEquals(0.50f, clock.advance(0.25f), 1e-6);
		clock.reset();
		assertEquals(0f, clock.time(), 1e-6);
		assertFalse(clock.finished(1.0f));
		clock.advance(1.1f);
		assertTrue(clock.finished(1.0f));
	}

	@Test
	void oneShotWeightIsFullInsideTheClipAndCleanZeroAfter() {
		float len = HomelanderPoseMath.MILK_CLIP_SECONDS;
		assertEquals(1f, HomelanderPoseMath.oneShotWeight(true, 0f, len), 1e-6);
		assertEquals(1f, HomelanderPoseMath.oneShotWeight(true, len - 0.01f, len), 1e-6);
		// the exact boundary already reads finished — the weight returns to 0
		assertEquals(0f, HomelanderPoseMath.oneShotWeight(true, len, len), 1e-6);
		assertEquals(0f, HomelanderPoseMath.oneShotWeight(true, len + 1f, len), 1e-6);
		// cancel drops the weight mid-clip
		assertEquals(0f, HomelanderPoseMath.oneShotWeight(false, 2f, len), 1e-6);
	}

	@Test
	void loopTimeWrapsIntoLength() {
		assertEquals(0.5f, HomelanderPoseMath.loopTime(3.7f, 3.2f), 1e-6);
		assertEquals(0f, HomelanderPoseMath.loopTime(6.4f, 3.2f), 1e-6);
		// a one-shot time past its length stays put (clamps, not wraps)
		assertEquals(1.9f, HomelanderPoseMath.loopTime(1.9f, 0f), 1e-6);
	}
}
