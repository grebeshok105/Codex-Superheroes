package com.example.superheroes.flight;

public record FlightControls(
		float yawDegrees,
		float pitchDegrees,
		float forward,
		float strafe,
		boolean jumping,
		boolean sneaking
) {
	public boolean hasHorizontalInput() {
		return Math.abs(forward) > 1.0e-4f || Math.abs(strafe) > 1.0e-4f;
	}

	public boolean hasVerticalInput() {
		return jumping || sneaking || Math.abs(forward) > 1.0e-4f;
	}
}
