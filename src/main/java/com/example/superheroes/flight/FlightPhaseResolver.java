package com.example.superheroes.flight;

public final class FlightPhaseResolver {
	public static final int TAKEOFF_TICKS = 8;
	private static final double HOVER_HORIZONTAL_SPEED = 0.08;
	private static final double HOVER_VERTICAL_SPEED = 0.06;
	private static final double BOOST_HORIZONTAL_SPEED = 1.1;

	private FlightPhaseResolver() {
	}

	public static FlightPhase resolve(FlightMode mode, int activeTicks, boolean onGround,
			double horizontalSpeed, double verticalSpeed, boolean hasInput) {
		if (activeTicks < TAKEOFF_TICKS) {
			return FlightPhase.TAKEOFF;
		}
		if (onGround) {
			return FlightPhase.LANDING;
		}
		if (mode == FlightMode.SUPERSONIC || horizontalSpeed >= BOOST_HORIZONTAL_SPEED) {
			return FlightPhase.BOOST;
		}
		if (!hasInput && horizontalSpeed < HOVER_HORIZONTAL_SPEED && Math.abs(verticalSpeed) < HOVER_VERTICAL_SPEED) {
			return FlightPhase.HOVER;
		}
		return FlightPhase.CRUISE;
	}
}
