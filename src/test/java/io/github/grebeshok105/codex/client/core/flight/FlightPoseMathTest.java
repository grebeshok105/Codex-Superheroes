package io.github.grebeshok105.codex.client.core.flight;

import io.github.grebeshok105.codex.client.core.vfx.params.VfxParams;
import io.github.grebeshok105.codex.mechanic.flight.FlightPhase;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the flight-pose smoothing contract (plan Review Focus 3): phase flips
 * must move the body continuously — never snap — and the pose targets stay
 * inside the tuning limits.
 */
class FlightPoseMathTest {
	private static final VfxParams DEFAULTS = VfxParams.EMPTY;
	private static final float HALF_LIFE = 3f;

	@Test
	void phaseFlipDoesNotJumpPose() {
		// Converged BOOST (80°) flips to HOVER for one tick: the exponential
		// step removes only the 1−2^(−dt/halfLife) fraction of the gap.
		FlightBodyTransform converged = new FlightBodyTransform(80f, 0f);
		FlightBodyTransform hover = FlightPoseMath.target(FlightPhase.HOVER, Vec3.ZERO, 0f, DEFAULTS);
		FlightBodyTransform next = FlightPoseMath.step(converged, hover, 1f, HALF_LIFE);
		float change = Math.abs(next.pitchDeg() - converged.pitchDeg());
		float maxStep = 80f * (1f - (float) Math.pow(2.0, -1.0 / 3.0)) + 0.01f;
		assertTrue(change <= maxStep,
				"one tick after the BOOST→HOVER flip pitch moved " + change + "° (limit " + maxStep + ")");
		assertTrue(next.pitchDeg() >= hover.pitchDeg(),
				"step overshot the hover target: " + next.pitchDeg());
	}

	@Test
	void stepIsContinuousForLargeDt() {
		// A 40-tick stall must land on the target side of the current pose —
		// never past it — no matter how large dt gets.
		FlightBodyTransform current = new FlightBodyTransform(80f, 25f);
		FlightBodyTransform target = new FlightBodyTransform(-10f, -25f);
		FlightBodyTransform next = FlightPoseMath.step(current, target, 40f, HALF_LIFE);
		assertTrue(next.pitchDeg() <= current.pitchDeg() && next.pitchDeg() >= target.pitchDeg(),
				"pitch overshot after large dt: " + next.pitchDeg());
		assertTrue(next.rollDeg() <= current.rollDeg() && next.rollDeg() >= target.rollDeg(),
				"roll overshot after large dt: " + next.rollDeg());
	}

	@Test
	void boostPitchWithinLimit() {
		FlightBodyTransform boost = FlightPoseMath.target(
				FlightPhase.BOOST, new Vec3(1.2, 0.0, 0.0), 0f, DEFAULTS);
		assertTrue(Math.abs(boost.pitchDeg()) <= 80f + 0.001f,
				"boost pitch exceeded the 80° limit: " + boost.pitchDeg());
		assertEquals(80f, boost.pitchDeg(), 0.001f, "boost defaults to a full 80° head-first pitch");
		// Pitch never flips belly-up: diving keeps the positive head-first lean.
		FlightBodyTransform diving = FlightPoseMath.target(
				FlightPhase.BOOST, new Vec3(1.2, -0.5, 0.0), 0f, DEFAULTS);
		assertEquals(80f, diving.pitchDeg(), 0.001f,
				"descending boost must not flip pitch sign, got " + diving.pitchDeg());
	}

	@Test
	void boostTargetPrefersEmfRootPitchWhenPresent() {
		// Owned (EMF) presentations carry the authored BOOST root rotation as
		// emfBoostRootPitch in their poseParams overlay (§7 stage 4).
		VfxParams emf = new VfxParams(java.util.Map.of(
				"emfBoostRootPitch", 86f, "boostPitch", 80f), java.util.Map.of());
		FlightBodyTransform boost = FlightPoseMath.target(
				FlightPhase.BOOST, new Vec3(1.2, 0.0, 0.0), 0f, emf);
		assertEquals(86f, boost.pitchDeg(), 0.001f,
				"owned players boost at the authored 86° root pitch");
	}

	@Test
	void boostTargetFallsBackToBoostPitch() {
		// Params without the EMF key keep the pre-existing 80° default —
		// byte-for-byte parity for non-owned players.
		VfxParams plain = new VfxParams(java.util.Map.of("boostPitch", 72f), java.util.Map.of());
		FlightBodyTransform boost = FlightPoseMath.target(
				FlightPhase.BOOST, new Vec3(1.2, 0.0, 0.0), 0f, plain);
		assertEquals(72f, boost.pitchDeg(), 0.001f,
				"non-owned params still read boostPitch");
	}

	@Test
	void rollClampedTo25() {
		FlightBodyTransform banking = FlightPoseMath.target(
				FlightPhase.CRUISE, new Vec3(0.5, 0.0, 0.0), 100f, DEFAULTS);
		assertEquals(-25f, banking.rollDeg(), 0.001f,
				"roll past the clamp should pin at -25°, got " + banking.rollDeg());
		FlightBodyTransform opposite = FlightPoseMath.target(
				FlightPhase.CRUISE, new Vec3(0.5, 0.0, 0.0), -100f, DEFAULTS);
		assertEquals(25f, opposite.rollDeg(), 0.001f,
				"roll past the clamp should pin at +25°, got " + opposite.rollDeg());
	}

	// --- Stage 5: directional() for EMF-owned players (§7 stage 5) ---

	private static final VfxParams EMF = new VfxParams(
			java.util.Map.of("emfBoostRootPitch", 86f), java.util.Map.of());

	@Test
	void forwardFlightPitchesIntoTravel() {
		// Signed forward speed maps linearly onto the hover lean cap (+12).
		assertEquals(12f, FlightPoseMath.directional(0.3f, 0f, 0f, 0f, 0f, DEFAULTS)
				.pitchDeg(), 0.001f, "full lean at hoverRefSpeed");
		assertEquals(6f, FlightPoseMath.directional(0.15f, 0f, 0f, 0f, 0f, DEFAULTS)
				.pitchDeg(), 0.001f, "half lean at half reference speed");
		assertEquals(12f, FlightPoseMath.directional(2f, 0f, 0f, 0f, 0f, DEFAULTS)
				.pitchDeg(), 0.001f, "past reference speed the lean clamps, never exceeds");
	}

	@Test
	void backwardFlightLeansBackWithinEightDegrees() {
		// Backward speed yields negative pitch, clamped to −hoverBackwardPitch.
		for (float forward = -0.05f; forward > -2f; forward -= 0.25f) {
			float pitch = FlightPoseMath.directional(
					forward, 0f, 0f, 0f, 0f, DEFAULTS).pitchDeg();
			assertTrue(pitch >= -8f - 1e-4 && pitch <= 0f + 1e-4,
					"backward pitch " + pitch + "° left [-8, 0] at forward=" + forward);
		}
		assertEquals(-8f, FlightPoseMath.directional(-0.3f, 0f, 0f, 0f, 0f, DEFAULTS)
				.pitchDeg(), 0.001f, "full backward lean at hoverRefSpeed");
	}

	@Test
	void pureStrafeBanksOppositeForLeftAndRight() {
		float left = FlightPoseMath.directional(0f, 0.5f, 0f, 0f, 0f, DEFAULTS).rollDeg();
		float right = FlightPoseMath.directional(0f, -0.5f, 0f, 0f, 0f, DEFAULTS).rollDeg();
		assertEquals(-14f, left, 0.001f);
		assertEquals(14f, right, 0.001f);
		assertTrue(Math.signum(left) == -Math.signum(right),
				"left and right strafe must bank to opposite sides");
	}

	@Test
	void strafeRollCombinesWithYawRateInsideClamp() {
		// strafe + yaw terms add, then pin at ±rollMax.
		assertEquals(-25f, FlightPoseMath.directional(0f, 5f, 0f, 100f, 0f, DEFAULTS)
				.rollDeg(), 0.001f);
		assertEquals(25f, FlightPoseMath.directional(0f, -5f, 0f, -100f, 0f, DEFAULTS)
				.rollDeg(), 0.001f);
	}

	@Test
	void boostWeightBlendsPitchToAuthoredRoot() {
		// w=1 lands exactly on the authored BOOST root pitch; w=0.5 is the
		// midpoint between the hover lean and it.
		assertEquals(86f, FlightPoseMath.directional(1.2f, 0f, 0f, 0f, 1f, EMF)
				.pitchDeg(), 0.001f);
		assertEquals(49f, FlightPoseMath.directional(1.2f, 0f, 0f, 0f, 0.5f, EMF)
				.pitchDeg(), 0.001f);
		// without the emf overlay the blend falls back to boostPitch (80).
		assertEquals(80f, FlightPoseMath.directional(1.2f, 0f, 0f, 0f, 1f, DEFAULTS)
				.pitchDeg(), 0.001f);
	}

	@Test
	void verticalResponseGatedBelowHalfBoost() {
		// Descending while hovering leans the nose down, climbing leans back —
		// both capped by verticalPitch.
		assertEquals(6f, FlightPoseMath.directional(0f, 0f, -0.6f, 0f, 0f, DEFAULTS)
				.pitchDeg(), 0.001f, "full descent term at verticalRef");
		assertEquals(-6f, FlightPoseMath.directional(0f, 0f, 0.6f, 0f, 0f, DEFAULTS)
				.pitchDeg(), 0.001f, "climbing leans back");
		// At boostWeight >= 0.5 the vertical term drops out: pure lerp to 86.
		assertEquals(43f, FlightPoseMath.directional(0f, 0f, -0.6f, 0f, 0.5f, EMF)
				.pitchDeg(), 0.001f, "w=0.5 suppresses the vertical term");
	}

	@Test
	void fullSpeedDescentNeverInvertsOrExceedsNinety() {
		// The PR #138 descent rule stays: at full forward speed a hard descent
		// keeps the head-first lean positive and inside the upright cone for
		// every boost blend.
		for (float w : new float[]{0f, 0.49f, 0.5f, 1f}) {
			float pitch = FlightPoseMath.directional(1.2f, 0f, -3f, 0f, w, EMF).pitchDeg();
			assertTrue(pitch > 0f && pitch <= 86f + 1e-4,
					"descent at boost " + w + " pitched to " + pitch + "°");
		}
		// Backward descent leans the nose toward the dive, never belly-up.
		assertEquals(-2f, FlightPoseMath.directional(-1.2f, 0f, -3f, 0f, 0f, EMF)
				.pitchDeg(), 0.001f);
	}

	@Test
	void stepConvergesAtThirtyAndTwoFortyFps() {
		// One simulated second of step() must land on the same pose at any
		// frame rate — the exponential approach is dt-exact.
		FlightBodyTransform target = new FlightBodyTransform(60f, 15f);
		FlightBodyTransform low = FlightBodyTransform.IDENTITY;
		FlightBodyTransform high = FlightBodyTransform.IDENTITY;
		for (int i = 0; i < 30; i++) {
			low = FlightPoseMath.step(low, target, 20f / 30f, HALF_LIFE);
		}
		for (int i = 0; i < 240; i++) {
			high = FlightPoseMath.step(high, target, 20f / 240f, HALF_LIFE);
		}
		assertEquals(low.pitchDeg(), high.pitchDeg(), 0.1f,
				"30 fps and 240 fps diverged after 1 s");
		assertEquals(low.rollDeg(), high.rollDeg(), 0.1f,
				"30 fps and 240 fps diverged after 1 s");
	}

	@Test
	void alternatingDirectionEveryTickStaysBounded() {
		// Forward/backward flapping every tick must never grow the smoothed
		// pose — it settles into a bounded oscillation inside the hover
		// envelope, not a runaway.
		FlightBodyTransform cur = FlightBodyTransform.IDENTITY;
		for (int i = 0; i < 400; i++) {
			float forward = (i & 1) == 0 ? 1.2f : -1.2f;
			FlightBodyTransform target = FlightPoseMath.directional(
					forward, 0f, 0f, 0f, 0f, DEFAULTS);
			cur = FlightPoseMath.step(cur, target, 1f, HALF_LIFE);
			assertTrue(Math.abs(cur.pitchDeg()) <= 12f + 1e-3,
					"pitch escaped the hover envelope at tick " + i + ": " + cur.pitchDeg());
			assertTrue(Math.abs(cur.rollDeg()) <= 25f + 1e-3,
					"roll escaped the clamp at tick " + i + ": " + cur.rollDeg());
			assertFalse(Float.isNaN(cur.pitchDeg()) || Float.isNaN(cur.rollDeg()),
					"NaN in the smoothed pose at tick " + i);
		}
	}
}
