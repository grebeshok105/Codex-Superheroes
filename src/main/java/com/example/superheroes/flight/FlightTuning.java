package com.example.superheroes.flight;

public record FlightTuning(
		double maxHorizontalSpeed,
		double maxVerticalSpeed,
		double acceleration,
		double horizontalFriction,
		double verticalFriction,
		boolean forcesForward
) {
}
