package com.example.superheroes.flight;

public record FlightVector(double x, double y, double z) {
	public double horizontalLength() {
		return Math.sqrt(x * x + z * z);
	}
}
