package com.example.superheroes.client;

public final class ClientUraniumThreatState {
	private static volatile boolean self = false;
	private static volatile int sourceCount = 0;
	private static volatile long lastEntryMs = 0L;

	static {
		ClientSessionState.register(ClientUraniumThreatState::reset);
	}

	private ClientUraniumThreatState() {
	}

	public static void update(boolean newSelf, int count) {
		boolean was = self;
		self = newSelf;
		sourceCount = count;
		if (newSelf && !was) {
			lastEntryMs = System.currentTimeMillis();
		}
	}

	public static boolean isSelfThreatened() {
		return self;
	}

	public static int sourceCount() {
		return sourceCount;
	}

	public static long lastEntryMs() {
		return lastEntryMs;
	}

	/** Drop all session state (registered with {@code ClientSessionState}). */
	public static void reset() {
		self = false;
		sourceCount = 0;
		lastEntryMs = 0L;
	}
}
