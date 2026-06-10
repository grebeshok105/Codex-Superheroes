package com.example.superheroes.client;

public final class ClientReinhardDarknessState {
	private static volatile long activatedAtMs;
	private static volatile long deadlineMs;

	private ClientReinhardDarknessState() {
	}

	public static void activate(int durationTicks) {
		long now = System.currentTimeMillis();
		long newDeadline = now + durationTicks * 50L;
		if (newDeadline > deadlineMs) {
			if (!active()) {
				activatedAtMs = now;
			}
			deadlineMs = newDeadline;
		}
	}

	public static boolean active() {
		return System.currentTimeMillis() < deadlineMs;
	}

	public static long activatedAtMs() {
		return activatedAtMs;
	}

	public static long deadlineMs() {
		return deadlineMs;
	}

	public static void clearAll() {
		deadlineMs = 0L;
		activatedAtMs = 0L;
	}
}
