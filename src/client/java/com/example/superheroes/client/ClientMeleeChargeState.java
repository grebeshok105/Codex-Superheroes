package com.example.superheroes.client;

public final class ClientMeleeChargeState {
	private static volatile boolean charging;
	private static volatile int chargeTicks;

	private ClientMeleeChargeState() {
	}

	public static void update(boolean newCharging, int ticks) {
		charging = newCharging;
		chargeTicks = ticks;
	}

	public static boolean charging() {
		return charging;
	}

	public static int chargeTicks() {
		return chargeTicks;
	}

	public static void clearAll() {
		charging = false;
		chargeTicks = 0;
	}
}
