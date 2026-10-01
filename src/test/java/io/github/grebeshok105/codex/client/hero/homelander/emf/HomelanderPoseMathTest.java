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

	@Test
	void takeoffStartsAtZeroWhenActivatedOnGround() {
		HomelanderPoseState.Entry entry = new HomelanderPoseState.Entry();
		entry.advance(false, true); // standing on the ground the tick before activation
		// on the activation tick the client-side TAKEOFF lift has already run, so
		// the current onGround reads airborne — the previous tick's flag decides
		entry.advance(true, false);
		assertEquals(0.05f, entry.takeoff.time(), 1e-6,
				"grounded activation resets the clip to 0, then one tick advances it");
		entry.advance(true, false);
		assertEquals(0.10f, entry.takeoff.time(), 1e-6);
	}

	@Test
	void takeoffSkipsCrouchDipWhenActivatedAirborne() {
		HomelanderPoseState.Entry entry = new HomelanderPoseState.Entry();
		entry.advance(false, false); // airborne the tick before activation
		entry.advance(true, false);
		assertEquals(0.28f + 0.05f, entry.takeoff.time(), 1e-6,
				"airborne activation starts past the 0.28 s crouch dip");
	}

	@Test
	void firstObservedTickFlyingStaysInHover() {
		// entry created while the player is already flying (observer joined or
		// the player entered render distance mid-flight): late tracking lands
		// in HOVER — the TAKEOFF one-shot is skipped like every other missed
		// start event (§7 stage 15), never replayed from the airborne start.
		HomelanderPoseState.Entry entry = new HomelanderPoseState.Entry();
		entry.advance(true, false);
		assertEquals(0.8f, entry.takeoff.time(), 1e-6,
				"takeoff clock stays parked at clip end — no replay for late trackers");
	}

	@Test
	void groundedFlagOnlyCountsOnTheActivationEdge() {
		HomelanderPoseState.Entry entry = new HomelanderPoseState.Entry();
		entry.advance(false, true);
		entry.advance(true, false);
		entry.advance(true, false);
		assertEquals(0.10f, entry.takeoff.time(), 1e-6,
				"already-flying ticks never re-open the takeoff");
	}

	@Test
	void flightOffDuringTakeoffFadesWeightsWithoutResettingHoverClock() {
		HomelanderPoseState.Entry entry = new HomelanderPoseState.Entry();
		entry.advance(false, true);
		for (int i = 0; i < 8; i++) {
			entry.advance(true, false); // 0.4 s into the takeoff
		}
		float hoverBefore = entry.hover.time();
		float takeoffBefore = entry.takeoffWeight;
		assertEquals(1f, takeoffBefore, 1e-6, "takeoff weight pinned through the hold");
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
		entry.advance(false, true);
		for (int i = 0; i < 10; i++) {
			entry.advance(true, false); // t = 0.5 s
		}
		for (int i = 0; i < 3; i++) {
			entry.advance(false, true); // landed between activations
		}
		entry.advance(true, false);
		assertEquals(0.05f, entry.takeoff.time(), 1e-6,
				"re-activation restarts the single takeoff clock — no second takeoff");
		for (int i = 0; i < 5; i++) {
			entry.advance(true, false);
		}
		assertEquals(1f, entry.takeoffWeight, 1e-6,
				"weight pinned back to 1 through the restarted hold");
	}

	@Test
	void takeoffWeightHoldsDuringClipAndPinsZeroAtClipEnd() {
		HomelanderPoseState.Entry entry = new HomelanderPoseState.Entry();
		entry.advance(false, true);
		for (int i = 0; i < 12; i++) {
			entry.advance(true, false); // t = 0.60 s, inside the 0.65 s hold
		}
		assertEquals(1f, entry.takeoffWeight, 1e-6, "weight pinned at 1 through the hold window");
		for (int i = 0; i < 3; i++) {
			entry.advance(true, false); // t = 0.75 s, inside the ease-out
		}
		assertTrue(entry.takeoffWeight < 0.5f, "weight eases out with 2-tick half-life");
		entry.advance(true, false); // t = 0.80 s, clip end
		assertEquals(0f, entry.takeoffWeight, 1e-6,
				"pinned to 0 at 0.8 s — hover owns the pose from here");
	}

	@Test
	void takeoffWeightReadInterpolatesBetweenTicks() {
		HomelanderPoseState.Entry entry = new HomelanderPoseState.Entry();
		entry.advance(false, true);
		entry.advance(true, false);
		float boundary = entry.takeoffWeight(1f);
		entry.advance(true, false);
		assertEquals(boundary, entry.takeoffWeight(0f), 1e-6,
				"no step at the tick boundary during the ramp");
	}

	@Test
	void takeoffWeightPinsOneDuringHoldAndDecaysAfter() {
		assertEquals(1f, HomelanderPoseMath.takeoffWeight(0f, true, 0.30f, 1f), 1e-6,
				"weight is literally 1 through the hold — no ramp, plan §7 stage 3");
		assertEquals(1f, HomelanderPoseMath.takeoffWeight(0.4f, true, 0.20f, 1f), 1e-6,
				"a partially-faded weight snaps back to 1 inside the hold");
		assertEquals(0.5f, HomelanderPoseMath.takeoffWeight(1f, true, 0.70f, 2f), 1e-4,
				"after the hold the target is 0: two ticks = one half-life");
		assertEquals(0f, HomelanderPoseMath.takeoffWeight(0.9f, true, 0.8f, 1f),
				"pinned to 0 at clip end");
		assertEquals(0f, HomelanderPoseMath.takeoffWeight(0.9f, true, 2.0f, 1f));
		assertEquals(0.5f, HomelanderPoseMath.takeoffWeight(1f, false, 0.3f, 2f), 1e-4,
				"not flying: decays regardless of clip time");
	}

	// --- Stage 4: BOOST hysteresis latch (§7 stage 4) ---

	@Test
	void boostEngagesAtEnterThreshold() {
		assertTrue(HomelanderPoseMath.boostEngaged(false, 0.9f, false, 0.9f, 0.6f),
				"forward == emfBoostEnter engages");
		assertFalse(HomelanderPoseMath.boostEngaged(false, 0.8999f, false, 0.9f, 0.6f),
				"a hair under enter does not");
	}

	@Test
	void boostHoldsInsideTheHysteresisBand() {
		assertTrue(HomelanderPoseMath.boostEngaged(true, 0.75f, false, 0.9f, 0.6f),
				"engaged boost keeps boosting between exit and enter");
		assertTrue(HomelanderPoseMath.boostEngaged(true, 0.6f, false, 0.9f, 0.6f),
				"exit boundary itself still holds");
	}

	@Test
	void boostDisengagesOnlyBelowExit() {
		assertFalse(HomelanderPoseMath.boostEngaged(true, 0.5999f, false, 0.9f, 0.6f),
				"under emfBoostExit the latch releases");
	}

	@Test
	void boostDoesNotReengageInsideTheBand() {
		assertFalse(HomelanderPoseMath.boostEngaged(false, 0.75f, false, 0.9f, 0.6f),
				"rising into the band without crossing enter stays disengaged — no flapping");
	}

	@Test
	void supersonicModeEngagesAboveLowForwardThreshold() {
		assertTrue(HomelanderPoseMath.boostEngaged(false, 0.31f, true, 0.9f, 0.6f),
				"SUPERSONIC + forward > 0.3 boosts");
		assertFalse(HomelanderPoseMath.boostEngaged(false, 0.3f, true, 0.9f, 0.6f),
				"the supersonic threshold is strict");
	}

	@Test
	void backwardSpeedNeverEngagesBoost() {
		assertFalse(HomelanderPoseMath.boostEngaged(false, -5f, false, 0.9f, 0.6f),
				"fast backward never enters boost");
		assertFalse(HomelanderPoseMath.boostEngaged(false, -5f, true, 0.9f, 0.6f),
				"fast backward never enters boost, even supersonic");
	}

	// --- Stage 4: forward/strafe projection on body yaw ---

	@Test
	void forwardComponentProjectsOntoBodyYaw() {
		// yaw 0 faces +Z (south): +Z motion is forward, -Z is backward.
		assertEquals(1f, HomelanderPoseMath.forwardComponent(0f, 1f, 0f), 1e-6);
		assertEquals(-1f, HomelanderPoseMath.forwardComponent(0f, -1f, 0f), 1e-6);
		// yaw 180 faces -Z: -Z motion is forward.
		assertEquals(1f, HomelanderPoseMath.forwardComponent(0f, -1f, 180f), 1e-4);
		// yaw 90 faces -X (west): -X motion is forward.
		assertEquals(1f, HomelanderPoseMath.forwardComponent(-1f, 0f, 90f), 1e-4);
		// pure sideways motion carries no forward component.
		assertEquals(0f, HomelanderPoseMath.forwardComponent(1f, 0f, 0f), 1e-6);
		// diagonal: projection, not raw speed — 0.5 forward + 0.5 strafe at
		// yaw 0 projects to 0.5, and yaw 45 faces (−0.707, 0.707) where the
		// same (0.5, 0.5) motion is purely sideways.
		assertEquals(0.5f, HomelanderPoseMath.forwardComponent(0.5f, 0.5f, 0f), 1e-6);
		assertEquals(0f, HomelanderPoseMath.forwardComponent(0.5f, 0.5f, 45f), 1e-4);
	}

	@Test
	void strafeComponentProjectsOntoBodyRight() {
		// facing +Z the body-right axis is -X: -X motion is positive strafe.
		assertEquals(1f, HomelanderPoseMath.strafeComponent(-1f, 0f, 0f), 1e-4);
		assertEquals(-1f, HomelanderPoseMath.strafeComponent(1f, 0f, 0f), 1e-4);
		// pure forward motion carries no strafe component.
		assertEquals(0f, HomelanderPoseMath.strafeComponent(0f, 1f, 0f), 1e-6);
	}

	// --- Stage 4: boost weight smoothing ---

	@Test
	void boostWeightHalvesTheGapAfterOneHalfLife() {
		assertEquals(0.5f, HomelanderPoseMath.boostWeight(0f, true, 4f), 1e-4,
				"boost weight half-life is 4 ticks");
	}

	@Test
	void boostWeightRisesOnForwardSpeedAndHoldsThroughTheBand() {
		HomelanderPoseState.Entry entry = new HomelanderPoseState.Entry();
		double z = 0;
		// 1.2 b/t forward facing +Z — smoothed velocity crosses emfBoostEnter.
		for (int i = 0; i < 30; i++) {
			z += 1.2;
			entry.advance(true, 0, 0, z, 0f, false, false, 0.9f, 0.6f, false);
		}
		assertTrue(entry.boostWeight > 0.9f, "boost converged near 1 at 1.2 b/t: " + entry.boostWeight);
		// slow into the hysteresis band (0.7 b/t): the latch holds, weight stays.
		for (int i = 0; i < 30; i++) {
			z += 0.7;
			entry.advance(true, 0, 0, z, 0f, false, false, 0.9f, 0.6f, false);
		}
		assertTrue(entry.forward > 0.6f && entry.forward < 0.9f,
				"smoothed forward sits in the band: " + entry.forward);
		assertTrue(entry.boostWeight > 0.9f,
				"hysteresis keeps boost inside the band: " + entry.boostWeight);
		// under the exit threshold the weight decays to 0.
		for (int i = 0; i < 60; i++) {
			z += 0.4;
			entry.advance(true, 0, 0, z, 0f, false, false, 0.9f, 0.6f, false);
		}
		assertEquals(0f, entry.boostWeight, 0.02f, "below exit the weight releases");
	}

	@Test
	void backwardSpeedKeepsBoostWeightAtZero() {
		HomelanderPoseState.Entry entry = new HomelanderPoseState.Entry();
		for (int i = 0; i < 40; i++) {
			entry.advance(true, 0, 0, -(i + 1) * 2.0, 0f, false, false, 0.9f, 0.6f, false);
		}
		assertTrue(entry.forward < -1.9f, "moving backward fast: " + entry.forward);
		assertEquals(0f, entry.boostWeight, 1e-6, "backward flight stays upright — no boost");
	}

	@Test
	void sidewaysSpeedKeepsBoostWeightAtZero() {
		HomelanderPoseState.Entry entry = new HomelanderPoseState.Entry();
		for (int i = 0; i < 40; i++) {
			entry.advance(true, (i + 1) * 2.0, 0, 0, 0f, false, false, 0.9f, 0.6f, false);
		}
		assertTrue(Math.abs(entry.strafe) > 1.9f, "pure strafe at speed: " + entry.strafe);
		assertEquals(0f, entry.boostWeight, 1e-6, "sideways speed never enters boost");
	}

	@Test
	void supersonicModeBoostsAtLowForwardSpeed() {
		HomelanderPoseState.Entry entry = new HomelanderPoseState.Entry();
		for (int i = 0; i < 40; i++) {
			entry.advance(true, 0, 0, (i + 1) * 0.4, 0f, true, false, 0.9f, 0.6f, false);
		}
		assertTrue(entry.forward > 0.3f, "supersonic forward: " + entry.forward);
		assertTrue(entry.boostWeight > 0.9f,
				"SUPERSONIC + forward > 0.3 converges to boost: " + entry.boostWeight);
	}

	@Test
	void boostWeightSurvivesOneTickFlightFlicker() {
		HomelanderPoseState.Entry entry = new HomelanderPoseState.Entry();
		double z = 0;
		for (int i = 0; i < 40; i++) {
			z += 1.5;
			entry.advance(true, 0, 0, z, 0f, false, false, 0.9f, 0.6f, false);
		}
		assertTrue(entry.boostWeight > 0.95f, "fully boosted: " + entry.boostWeight);
		// one tick where the synced flight state drops out — still moving.
		z += 1.5;
		entry.advance(false, 0, 0, z, 0f, false, false, 0.9f, 0.6f, false);
		assertTrue(entry.boostWeight > 0.8f,
				"a single-tick drop does not reset the smoothed weight: " + entry.boostWeight);
		for (int i = 0; i < 20; i++) {
			z += 1.5;
			entry.advance(true, 0, 0, z, 0f, false, false, 0.9f, 0.6f, false);
		}
		assertTrue(entry.boostWeight > 0.95f, "recovers once flying again: " + entry.boostWeight);
	}

	// --- Stage 6: third-person framing body centre (§7 stage 6) ---

	@Test
	void bodyCentreHeightAddsPixelLiftToStandingChest() {
		assertEquals(1.1f, HomelanderPoseMath.bodyCentreHeight(0f), 1e-6,
				"no root lift keeps the standing chest height");
		assertEquals(1.1f + 6.619f / 16f,
				HomelanderPoseMath.bodyCentreHeight(HomelanderPoseMath.DEFAULT_HOVER_ROOT_TY),
				1e-6, "feet + 1.1 + hoverRootTy/16 from the plan");
		assertEquals(2.1f, HomelanderPoseMath.bodyCentreHeight(16f), 1e-6,
				"16 model pixels are one block");
	}

	@Test
	void boostWeightReadInterpolatesBetweenTickValues() {
		HomelanderPoseState.Entry entry = new HomelanderPoseState.Entry();
		double z = 0;
		for (int i = 0; i < 40; i++) {
			z += 1.5;
			entry.advance(true, 0, 0, z, 0f, false, false, 0.9f, 0.6f, false);
		}
		float boundary = entry.boostWeight(1f);
		z += 1.5;
		entry.advance(true, 0, 0, z, 0f, false, false, 0.9f, 0.6f, false);
		assertEquals(boundary, entry.boostWeight(0f), 1e-6,
				"read opens at the previous tick value — no step at the boundary");
		assertEquals(entry.boostWeight, entry.boostWeight(1f), 1e-6);
	}
}
