package com.example.superheroes.flight;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlightProfilesTest {
	@Test
	void normalFlightScalesWithEnergyAndKeepsLegacyTopSpeed() {
		FlightTuning tuning = FlightProfiles.tuning(FlightMode.NORMAL, 50f, 100f, false);

		assertEquals(1.6875, tuning.maxHorizontalSpeed(), 0.0001);
		assertEquals(1.125, tuning.maxVerticalSpeed(), 0.0001);
		assertEquals(0.135, tuning.acceleration(), 0.0001);
	}

	@Test
	void supersonicForcesForwardCruiseAndIsFasterThanIronManFlight() {
		FlightTuning ironMan = FlightProfiles.tuning(FlightMode.IRON_MAN, 1000f, 1000f, false);
		FlightTuning supersonic = FlightProfiles.tuning(FlightMode.SUPERSONIC, 1000f, 1000f, false);

		assertTrue(supersonic.forcesForward());
		assertTrue(supersonic.maxHorizontalSpeed() > ironMan.maxHorizontalSpeed());
	}
}
