package io.github.grebeshok105.codex.flight;

public record FlightTuning(
		double maxHorizontalSpeed,
		double maxVerticalSpeed,
		double acceleration,
		double horizontalFriction,
		double verticalFriction,
		boolean forcesForward
) {
}
