package com.example.superheroes.client;

public final class ClientReinhardSwordGateState {
	private static volatile boolean ready;
	private static volatile float progress;

	private ClientReinhardSwordGateState() {
	}

	public static void update(boolean newReady, float newProgress) {
		ready = newReady;
		progress = Math.max(0f, Math.min(1f, newProgress));
	}

	public static boolean ready() {
		return ready;
	}

	public static float progress() {
		return progress;
	}
}
