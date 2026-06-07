package com.example.superheroes.hero;

public record LandingImpact(float fallDistance, double verticalSpeed, double horizontalSpeed, float intensity, Tier tier) {
	public enum Tier { WEAK, NORMAL, STRONG, EPIC }

	public static LandingImpact compute(float fallDistance, double verticalSpeed, double horizontalSpeed) {
		float heightFactor = clamp01((fallDistance - 10.0f) / 50.0f);
		float vSpeedFactor = (float) clamp01((Math.abs(verticalSpeed) - 0.6) / 2.4);
		float hSpeedFactor = (float) clamp01((horizontalSpeed - 0.4) / 1.6);
		float speedFactor = Math.max(vSpeedFactor, hSpeedFactor * 0.85f);
		float intensity = clamp01(0.55f * heightFactor + 0.45f * speedFactor);
		Tier tier;
		if (intensity < 0.20f) {
			tier = Tier.WEAK;
		} else if (intensity < 0.50f) {
			tier = Tier.NORMAL;
		} else if (intensity < 0.80f) {
			tier = Tier.STRONG;
		} else {
			tier = Tier.EPIC;
		}
		return new LandingImpact(fallDistance, verticalSpeed, horizontalSpeed, intensity, tier);
	}

	private static float clamp01(float v) {
		return v < 0f ? 0f : (v > 1f ? 1f : v);
	}

	private static double clamp01(double v) {
		return v < 0.0 ? 0.0 : (v > 1.0 ? 1.0 : v);
	}
}
