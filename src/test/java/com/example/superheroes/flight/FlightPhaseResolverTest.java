package com.example.superheroes.flight;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FlightPhaseResolverTest {
	@Test
	void takeoffWinsDuringInitialTicks() {
		FlightPhase phase = FlightPhaseResolver.resolve(FlightMode.NORMAL, 4, false, 0.0, 0.0, false);

		assertEquals(FlightPhase.TAKEOFF, phase);
	}

	@Test
	void hoverIsUsedWhenAirborneWithoutInputOrSpeed() {
		FlightPhase phase = FlightPhaseResolver.resolve(FlightMode.NORMAL, 20, false, 0.02, 0.01, false);

		assertEquals(FlightPhase.HOVER, phase);
	}

	@Test
	void supersonicUsesBoostAfterTakeoff() {
		FlightPhase phase = FlightPhaseResolver.resolve(FlightMode.SUPERSONIC, 20, false, 0.2, 0.0, false);

		assertEquals(FlightPhase.BOOST, phase);
	}

	@Test
	void landingWinsWhenThePlayerTouchesGroundAfterTakeoff() {
		FlightPhase phase = FlightPhaseResolver.resolve(FlightMode.IRON_MAN, 20, true, 0.4, -0.2, false);

		assertEquals(FlightPhase.LANDING, phase);
	}
}
