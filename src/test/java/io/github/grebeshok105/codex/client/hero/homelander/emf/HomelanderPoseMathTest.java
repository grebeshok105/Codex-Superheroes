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
	void advanceRisesActiveWeightWhileFlyingAndFallsAfterLanding() {
		HomelanderPoseState.Entry entry = new HomelanderPoseState.Entry();
		for (int i = 0; i < 3; i++) {
			entry.advance(true, true);
		}
		assertEquals(0.5f, entry.active, 1e-4, "half-life 3 ticks while flying");
		for (int i = 3; i < 21; i++) {
			entry.advance(true, true);
		}
		assertEquals(1f, entry.active, 0.01f, "rises to full weight");
		for (int i = 0; i < 24; i++) {
			entry.advance(false, false);
		}
		assertEquals(0f, entry.active, 0.01f, "returns to vanilla after landing");
	}

	@Test
	void weightReadInterpolatesBetweenTickValues() {
		HomelanderPoseState.Entry entry = new HomelanderPoseState.Entry();
		entry.advance(true, true);
		assertEquals(0f, entry.weight(0f), 1e-6, "read opens at the previous tick value");
		assertEquals(entry.active, entry.weight(1f), 1e-6);
		float boundary = entry.weight(1f);
		entry.advance(true, true);
		assertEquals(boundary, entry.weight(0f), 1e-6,
				"no step at the tick boundary during the ramp");
	}

	@Test
	void hoverTimeWrapsAtLoopLength() {
		HomelanderPoseState.Entry entry = new HomelanderPoseState.Entry();
		for (int i = 0; i < 63; i++) {
			entry.advance(false, false);
		}
		assertEquals(3.15f, entry.hoverTime(0f), 5e-4, "3.15 s of 60 client ticks");
		entry.advance(false, false);
		float wrapped = entry.hoverTime(0f);
		assertTrue(wrapped < 5e-4
						|| Math.abs(wrapped - HomelanderPoseMath.HOVER_LENGTH_SECONDS) < 5e-4,
				"3.2 s sits on the loop boundary (float accumulation may land a hair under)");
		entry.advance(false, false);
		assertEquals(0.05f, entry.hoverTime(0f), 5e-4);
		assertEquals(0.07f, entry.hoverTime(0.4f), 5e-4, "wraps mid-interpolation too");
	}

	@Test
	void hoverClockNeverResetsAcrossFlightPhaseFlips() {
		HomelanderPoseState.Entry entry = new HomelanderPoseState.Entry();
		for (int i = 0; i < 10; i++) {
			entry.advance(true, true);
		}
		float before = entry.hover.time();
		for (int i = 0; i < 20; i++) {
			entry.advance(false, false); // landed
		}
		for (int i = 0; i < 10; i++) {
			entry.advance(true, true); // flying again
		}
		assertEquals(before + 30f * 0.05f, entry.hover.time(), 1e-4,
				"the loop clock keeps running through land/re-fly flips — no snap");
	}

	@Test
	void clipClockAccumulatesRealDeltasAtAnyFrameRate() {
		// advance(dt) is fed real frame deltas: 2 s of 60 fps frames and
		// 2 s of 144 fps frames must land on the same clock value.
		HomelanderPoseMath.ClipClock c60 = new HomelanderPoseMath.ClipClock();
		HomelanderPoseMath.ClipClock c144 = new HomelanderPoseMath.ClipClock();
		for (int i = 0; i < 120; i++) {
			c60.advance(1f / 60f);
		}
		for (int i = 0; i < 288; i++) {
			c144.advance(1f / 144f);
		}
		assertEquals(c60.time(), c144.time(), 1e-4, "2 s wall at 60 vs 144 fps");
	}

	@Test
	void hoverTimeIsFrameRateIndependent() {
		// The read is tick-clock + partial-tick interpolation, so at any wall
		// instant it reproduces wall time regardless of render rate.
		for (double fps : new double[]{60.0, 144.0}) {
			HomelanderPoseState.Entry entry = new HomelanderPoseState.Entry();
			int ticks = 0;
			for (int f = 1; f <= 4 * fps; f++) {
				double wall = f / fps;
				int due = (int) Math.floor(wall * 20.0 + 1e-9);
				for (; ticks < due; ticks++) {
					entry.advance(true, true);
				}
				float partial = (float) (wall * 20.0 - due);
				float expected = (float) (wall % HomelanderPoseMath.HOVER_LENGTH_SECONDS);
				// compare in loop space: a hair under 3.2 is identical to a hair over 0
				float diff = Math.abs(expected - entry.hoverTime(partial));
				float loopDist = Math.min(diff, HomelanderPoseMath.HOVER_LENGTH_SECONDS - diff);
				assertTrue(loopDist < 1e-3,
						fps + " fps, wall=" + wall + " expected=" + expected
								+ " actual=" + entry.hoverTime(partial));
			}
		}
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
	void loopTimeWrapsIntoLength() {
		assertEquals(0.5f, HomelanderPoseMath.loopTime(3.7f, 3.2f), 1e-6);
		assertEquals(0f, HomelanderPoseMath.loopTime(6.4f, 3.2f), 1e-6);
		// a one-shot time past its length stays put (clamps, not wraps)
		assertEquals(1.9f, HomelanderPoseMath.loopTime(1.9f, 0f), 1e-6);
	}

	@Test
	void takeoffStartsAtZeroWhenActivatedOnGround() {
		HomelanderPoseState.Entry entry = new HomelanderPoseState.Entry();
		entry.advance(true, true);
		assertEquals(0.05f, entry.takeoff.time(), 1e-6,
				"grounded activation resets the clip to 0, then one tick advances it");
		entry.advance(true, true);
		assertEquals(0.10f, entry.takeoff.time(), 1e-6);
	}

	@Test
	void takeoffSkipsCrouchDipWhenActivatedAirborne() {
		HomelanderPoseState.Entry entry = new HomelanderPoseState.Entry();
		entry.advance(true, false);
		assertEquals(0.28f + 0.05f, entry.takeoff.time(), 1e-6,
				"airborne activation starts past the 0.28 s crouch dip");
	}

	@Test
	void groundedFlagOnlyCountsOnTheActivationEdge() {
		HomelanderPoseState.Entry entry = new HomelanderPoseState.Entry();
		entry.advance(true, true);
		entry.advance(true, false);
		assertEquals(0.10f, entry.takeoff.time(), 1e-6,
				"already-flying ticks never re-open the takeoff");
	}

	@Test
	void flightOffDuringTakeoffFadesWeightsWithoutResettingHoverClock() {
		HomelanderPoseState.Entry entry = new HomelanderPoseState.Entry();
		for (int i = 0; i < 8; i++) {
			entry.advance(true, true); // 0.4 s into the takeoff
		}
		float hoverBefore = entry.hover.time();
		float takeoffBefore = entry.takeoffWeight;
		assertTrue(takeoffBefore > 0.5f, "takeoff nearly full mid-clip");
		entry.advance(false, false);
		assertTrue(entry.takeoffWeight < takeoffBefore, "takeoff weight decays on flight-off");
		assertTrue(entry.takeoffWeight > 0f, "decay is continuous, not a snap");
		assertTrue(entry.active < 1f, "active weight decays on flight-off");
		for (int i = 0; i < 45; i++) {
			entry.advance(false, false);
		}
		assertEquals(0f, entry.takeoffWeight, 1e-4);
		assertEquals(0f, entry.active, 1e-4);
		assertEquals(hoverBefore + 46f * 0.05f, entry.hover.time(), 1e-4,
				"hover clock free-runs through the flight-off window");
	}

	@Test
	void reactivationWithinClipRestartsTakeoffOnce() {
		HomelanderPoseState.Entry entry = new HomelanderPoseState.Entry();
		for (int i = 0; i < 10; i++) {
			entry.advance(true, true); // t = 0.5 s
		}
		for (int i = 0; i < 3; i++) {
			entry.advance(false, false);
		}
		entry.advance(true, true);
		assertEquals(0.05f, entry.takeoff.time(), 1e-6,
				"re-activation restarts the single takeoff clock — no second takeoff");
		float faded = entry.takeoffWeight;
		for (int i = 0; i < 5; i++) {
			entry.advance(true, true);
		}
		assertTrue(entry.takeoffWeight > faded, "weight ramps back toward 1 after restart");
	}

	@Test
	void takeoffWeightHoldsDuringClipAndPinsZeroAtClipEnd() {
		HomelanderPoseState.Entry entry = new HomelanderPoseState.Entry();
		for (int i = 0; i < 12; i++) {
			entry.advance(true, true); // t = 0.60 s, inside the 0.65 s hold
		}
		assertTrue(entry.takeoffWeight > 0.9f, "weight ~1 through the hold window");
		for (int i = 0; i < 3; i++) {
			entry.advance(true, true); // t = 0.75 s, inside the ease-out
		}
		assertTrue(entry.takeoffWeight < 0.5f, "weight eases out with 2-tick half-life");
		entry.advance(true, true); // t = 0.80 s, clip end
		assertEquals(0f, entry.takeoffWeight, 1e-6,
				"pinned to 0 at 0.8 s — hover owns the pose from here");
	}

	@Test
	void takeoffWeightReadInterpolatesBetweenTicks() {
		HomelanderPoseState.Entry entry = new HomelanderPoseState.Entry();
		entry.advance(true, true);
		float boundary = entry.takeoffWeight(1f);
		entry.advance(true, true);
		assertEquals(boundary, entry.takeoffWeight(0f), 1e-6,
				"no step at the tick boundary during the ramp");
	}

	@Test
	void takeoffWeightApproachesOneDuringHoldAndDecaysAfter() {
		assertEquals(1f - (float) Math.pow(0.5, 0.5),
				HomelanderPoseMath.takeoffWeight(0f, true, 0.30f, 1f), 1e-4,
				"hold target 1, half-life 2 ticks");
		assertEquals(0.5f, HomelanderPoseMath.takeoffWeight(1f, true, 0.70f, 2f), 1e-4,
				"after the hold the target is 0: two ticks = one half-life");
		assertEquals(0f, HomelanderPoseMath.takeoffWeight(0.9f, true, 0.8f, 1f),
				"pinned to 0 at clip end");
		assertEquals(0f, HomelanderPoseMath.takeoffWeight(0.9f, true, 2.0f, 1f));
		assertEquals(0.5f, HomelanderPoseMath.takeoffWeight(1f, false, 0.3f, 2f), 1e-4,
				"not flying: decays regardless of clip time");
	}
}
