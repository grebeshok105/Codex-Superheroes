package com.example.superheroes.client;

public final class ClientKratosRageState {
	private static volatile float rage;
	private static volatile boolean active;

	private ClientKratosRageState() {
	}

	public static void update(float rage, boolean active) {
		ClientKratosRageState.rage = Math.max(0f, Math.min(100f, rage));
		ClientKratosRageState.active = active;
	}

	public static float rage() {
		return rage;
	}

	public static boolean active() {
		return active;
	}

	public static boolean isFull() {
		return rage >= 99.99f;
	}

	public static void clear() {
		rage = 0f;
		active = false;
	}
}
