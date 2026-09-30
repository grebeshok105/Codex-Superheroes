package io.github.grebeshok105.codex.client.hero.homelander.flight;

import io.github.grebeshok105.codex.client.hero.homelander.flight.HomelanderFlightLean.Params;
import io.github.grebeshok105.codex.client.hero.homelander.flight.HomelanderFlightLean.Targets;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HomelanderFlightLeanTest {
	private final HomelanderFlightLean lean = new HomelanderFlightLean(Params.defaults());

	@Test
	void forwardSpeedPitchesForward() {
		Targets t = lean.target(0.6, 0, 0, 0);
		assertEquals(Math.toRadians(35.0), t.pitchRad(), 1e-4);
		Targets half = lean.target(0.3, 0, 0, 0);
		assertEquals(Math.toRadians(17.5), half.pitchRad(), 1e-4);
	}

	@Test
	void backwardSpeedAppliesSmallBackwardLean() {
		Targets t = lean.target(-0.6, 0, 0, 0);
		assertEquals(Math.toRadians(-10.0), t.pitchRad(), 1e-4);
		// Much faster backward travel still caps at the small lean.
		Targets capped = lean.target(-5.0, 0, 0, 0);
		assertEquals(Math.toRadians(-10.0), capped.pitchRad(), 1e-4);
	}

	@Test
	void strafeBanksTowardMotion() {
		Targets right = lean.target(0, 0.5, 0, 0);
		assertEquals(Math.toRadians(20.0), right.rollRad(), 1e-4);
		Targets left = lean.target(0, -0.5, 0, 0);
		assertEquals(Math.toRadians(-20.0), left.rollRad(), 1e-4);
	}

	@Test
	void verticalSpeedAdjustsPitchAndOffset() {
		Targets up = lean.target(0, 0, 0.4, 0);
		assertEquals(Math.toRadians(-8.0), up.pitchRad(), 1e-4);
		assertEquals(1.6, up.yPx(), 1e-4); // 0.4/0.5 * 2px
		Targets down = lean.target(0, 0, -0.4, 0);
		assertEquals(Math.toRadians(8.0), down.pitchRad(), 1e-4);
		assertEquals(-1.6, down.yPx(), 1e-4);
	}

	@Test
	void boostWeightDampsLean() {
		Targets none = lean.target(0.6, 0.5, 0, 0);
		Targets full = lean.target(0.6, 0.5, 0, 1.0);
		assertTrue(Math.abs(full.pitchRad()) < Math.abs(none.pitchRad()));
		assertTrue(Math.abs(full.rollRad()) < Math.abs(none.rollRad()));
	}

	@Test
	void approachIsExponential() {
		HomelanderFlightLean l = new HomelanderFlightLean(
				new Params(35, 0.6, 20, 0.5, 8, 0.4, 10, 2, 0.5, 4.0, 0.7));
		double v = l.approach(0, 100, 4);
		assertEquals(50.0, v, 1e-4);
	}
}
