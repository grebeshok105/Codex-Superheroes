package io.github.grebeshok105.codex.mechanic.flight;

public record FlightTuning(
		double maxHorizontalSpeed,
		double maxVerticalSpeed,
		double acceleration,
		double horizontalFriction,
		double verticalFriction,
		boolean forcesForward
) {
}
