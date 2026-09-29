package io.github.grebeshok105.codex.client.core.flight;

import io.github.grebeshok105.codex.client.core.vfx.params.VfxParams;
import io.github.grebeshok105.codex.mechanic.flight.FlightPhase;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
		// Pitch sign follows vertical velocity: descending flips the lean.
		FlightBodyTransform diving = FlightPoseMath.target(
				FlightPhase.BOOST, new Vec3(1.2, -0.5, 0.0), 0f, DEFAULTS);
		assertEquals(-80f, diving.pitchDeg(), 0.001f,
				"descending boost should flip pitch sign, got " + diving.pitchDeg());
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
}
