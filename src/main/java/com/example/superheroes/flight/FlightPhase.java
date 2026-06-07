package com.example.superheroes.flight;

public enum FlightPhase {
	IDLE,
	TAKEOFF,
	HOVER,
	CRUISE,
	BOOST,
	LANDING;

	public static FlightPhase byOrdinal(int ordinal) {
		FlightPhase[] values = values();
		if (ordinal < 0 || ordinal >= values.length) {
			return IDLE;
		}
		return values[ordinal];
	}
}
