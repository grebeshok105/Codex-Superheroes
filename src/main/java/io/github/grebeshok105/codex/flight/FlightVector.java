package io.github.grebeshok105.codex.flight;

public record FlightVector(double x, double y, double z) {
	public double horizontalLength() {
		return Math.sqrt(x * x + z * z);
	}
}
