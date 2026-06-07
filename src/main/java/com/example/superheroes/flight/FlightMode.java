package com.example.superheroes.flight;

public enum FlightMode {
	NORMAL,
	IRON_MAN,
	SUPERSONIC;

	public static FlightMode byOrdinal(int ordinal) {
		FlightMode[] values = values();
		if (ordinal < 0 || ordinal >= values.length) {
			return NORMAL;
		}
		return values[ordinal];
	}
}
