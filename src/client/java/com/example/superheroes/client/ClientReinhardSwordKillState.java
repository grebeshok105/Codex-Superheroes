package com.example.superheroes.client;

public final class ClientReinhardSwordKillState {
	private static volatile boolean active;
	private static volatile long activatedAtMs;

	private ClientReinhardSwordKillState() {
	}

	public static void update(boolean newActive) {
		if (newActive && !active) {
			activatedAtMs = System.currentTimeMillis();
		}
		active = newActive;
	}

	public static boolean active() {
		return active;
	}

	public static long activatedAtMs() {
		return activatedAtMs;
	}
}
