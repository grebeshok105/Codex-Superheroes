package com.example.superheroes.flight;

public final class FlightProfiles {
	public static final double BASE_MAX_HORIZONTAL_SPEED = 1.5;
	public static final double BASE_MAX_VERTICAL_SPEED = 1.0;
	public static final double BASE_ACCELERATION = 0.12;
	public static final double MIN_ENERGY_MULTIPLIER = 0.5;
	public static final double MADNESS_MULTIPLIER = 1.5;
	public static final double NORMAL_FLIGHT_MULTIPLIER = 1.5;
	public static final double IRON_MAN_MULTIPLIER = 0.56;
	public static final double SUPERSONIC_MULTIPLIER = 2.6;
	public static final double HORIZONTAL_FRICTION = 0.92;
	public static final double VERTICAL_FRICTION = 0.90;

	private FlightProfiles() {
	}

	public static FlightTuning tuning(FlightMode mode, float energy, float energyMax, boolean madness) {
		double multiplier = energyMultiplier(energy, energyMax);
		if (madness) {
			multiplier *= MADNESS_MULTIPLIER;
		} else if (mode == FlightMode.NORMAL) {
			multiplier *= NORMAL_FLIGHT_MULTIPLIER;
		}
		if (mode == FlightMode.IRON_MAN || mode == FlightMode.SUPERSONIC) {
			multiplier *= IRON_MAN_MULTIPLIER;
			if (mode == FlightMode.SUPERSONIC) {
				multiplier *= SUPERSONIC_MULTIPLIER;
			}
		}
		return new FlightTuning(
				BASE_MAX_HORIZONTAL_SPEED * multiplier,
				BASE_MAX_VERTICAL_SPEED * multiplier,
				BASE_ACCELERATION * multiplier,
				HORIZONTAL_FRICTION,
				VERTICAL_FRICTION,
				mode == FlightMode.SUPERSONIC);
	}

	private static double energyMultiplier(float energy, float energyMax) {
		if (energyMax <= 0f) {
			return 1.0;
		}
		double fraction = Math.max(0.0, Math.min(1.0, energy / energyMax));
		return MIN_ENERGY_MULTIPLIER + (1.0 - MIN_ENERGY_MULTIPLIER) * fraction;
	}
}
