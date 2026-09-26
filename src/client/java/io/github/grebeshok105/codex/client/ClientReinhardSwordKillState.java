package io.github.grebeshok105.codex.client;

public final class ClientReinhardSwordKillState {
	private static volatile boolean active;
	private static volatile long activatedAtMs;

	static {
		ClientSessionState.register(ClientReinhardSwordKillState::reset);
	}

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

	/** Drop all session state (registered with {@code ClientSessionState}). */
	public static void reset() {
		active = false;
		activatedAtMs = 0L;
	}
}
