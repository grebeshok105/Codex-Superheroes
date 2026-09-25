package com.example.superheroes.client;

public final class ClientReinhardCeremonyState {
	private static volatile boolean active;
	private static volatile float progress;

	static {
		ClientSessionState.register(ClientReinhardCeremonyState::reset);
	}

	private ClientReinhardCeremonyState() {
	}

	public static void update(boolean newActive, float newProgress) {
		active = newActive;
		progress = Math.max(0f, Math.min(1f, newProgress));
	}

	public static boolean active() {
		return active;
	}

	public static float progress() {
		return progress;
	}

	/** Drop all session state (registered with {@code ClientSessionState}). */
	public static void reset() {
		active = false;
		progress = 0f;
	}
}
