package com.example.superheroes.flight;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FlightMotionMathTest {
	@Test
	void takeoffAddsVisibleLiftAndForwardPush() {
		FlightTuning tuning = FlightProfiles.tuning(FlightMode.NORMAL, 100f, 100f, false);
		FlightControls controls = new FlightControls(0f, 0f, 1f, 0f, false, false);

		FlightVector next = FlightMotionMath.next(new FlightVector(0, 0, 0), controls, tuning, FlightPhase.TAKEOFF);

		assertTrue(next.y() >= 0.24);
		assertTrue(next.horizontalLength() >= 0.18);
	}

	@Test
	void hoverDampsLegacyDriftAggressively() {
		FlightTuning tuning = FlightProfiles.tuning(FlightMode.NORMAL, 100f, 100f, false);
		FlightControls controls = new FlightControls(0f, 0f, 0f, 0f, false, false);

		FlightVector next = FlightMotionMath.next(new FlightVector(1.0, -0.20, 0.0), controls, tuning, FlightPhase.HOVER);

		assertTrue(next.horizontalLength() <= 0.75);
		assertTrue(Math.abs(next.y()) <= 0.08);
	}

	@Test
	void boostForcesForwardMotionEvenWithoutInput() {
		FlightTuning tuning = FlightProfiles.tuning(FlightMode.SUPERSONIC, 100f, 100f, false);
		FlightControls controls = new FlightControls(0f, 0f, 0f, 0f, false, false);

		FlightVector next = FlightMotionMath.next(new FlightVector(0, 0, 0), controls, tuning, FlightPhase.BOOST);

		assertTrue(next.z() >= 0.35);
		assertTrue(next.horizontalLength() >= 0.35);
	}
}
