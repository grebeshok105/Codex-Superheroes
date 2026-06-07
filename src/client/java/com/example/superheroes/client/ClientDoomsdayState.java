package com.example.superheroes.client;

public final class ClientDoomsdayState {
	private static int tier = 1;
	private static int adaptations = 0;

	private ClientDoomsdayState() {
	}

	public static int tier() {
		return Math.max(1, Math.min(7, tier));
	}

	public static int adaptations() {
		return adaptations;
	}

	public static float glitchPct() {
		int t = tier();
		return Math.max(0f, Math.min(0.80f, (t - 1) / 6.0f * 0.80f));
	}

	public static void update(int tier, int adaptations) {
		ClientDoomsdayState.tier = tier;
		ClientDoomsdayState.adaptations = adaptations;
	}

	public static void clear() {
		tier = 1;
		adaptations = 0;
	}
}
