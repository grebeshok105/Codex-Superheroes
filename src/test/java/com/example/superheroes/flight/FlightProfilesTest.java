package com.example.superheroes.flight;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FlightProfilesTest {
	@Test
	void normalFlightNoLongerKeepsLegacyTopSpeed() {
		FlightTuning tuning = FlightProfiles.tuning(FlightMode.NORMAL, 100f, 100f, false);

		assertTrue(tuning.maxHorizontalSpeed() >= 2.75);
		assertTrue(tuning.maxVerticalSpeed() >= 1.30);
		assertTrue(tuning.acceleration() >= 0.22);
		assertTrue(tuning.horizontalFriction() <= 0.86);
	}

	@Test
	void supersonicForcesForwardCruiseAndIsFasterThanIronManFlight() {
		FlightTuning ironMan = FlightProfiles.tuning(FlightMode.IRON_MAN, 1000f, 1000f, false);
		FlightTuning supersonic = FlightProfiles.tuning(FlightMode.SUPERSONIC, 1000f, 1000f, false);

		assertTrue(supersonic.forcesForward());
		assertTrue(supersonic.maxHorizontalSpeed() > ironMan.maxHorizontalSpeed());
	}

	@Test
	void supersonicIsClearlyFasterThanNormalFlight() {
		FlightTuning normal = FlightProfiles.tuning(FlightMode.NORMAL, 100f, 100f, false);
		FlightTuning supersonic = FlightProfiles.tuning(FlightMode.SUPERSONIC, 100f, 100f, false);

		assertTrue(supersonic.maxHorizontalSpeed() >= normal.maxHorizontalSpeed() * 1.75);
		assertTrue(supersonic.acceleration() >= normal.acceleration() * 1.50);
	}
}
