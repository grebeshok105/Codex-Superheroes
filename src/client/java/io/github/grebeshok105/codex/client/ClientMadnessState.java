package io.github.grebeshok105.codex.client;

public final class ClientMadnessState {
	private static volatile boolean madness = false;
	private static volatile boolean bonusLifeAvailable = false;
	private static volatile long readingUntilMs = 0L;
	private static volatile long manaLockUntilMs = 0L;
	private static volatile long madnessStartedAtMs = 0L;

	static {
		ClientSessionState.register(ClientMadnessState::reset);
	}

	private ClientMadnessState() {
	}

	public static void update(boolean madnessNew, boolean bonusLife, long readingRemainingMs, long manaLockRemainingMs) {
		if (madnessNew && !madness) {
			madnessStartedAtMs = System.currentTimeMillis();
		}
		if (!madnessNew) {
			madnessStartedAtMs = 0L;
		}
		madness = madnessNew;
		bonusLifeAvailable = bonusLife;
		long now = System.currentTimeMillis();
		readingUntilMs = readingRemainingMs > 0L ? now + readingRemainingMs : 0L;
		manaLockUntilMs = manaLockRemainingMs > 0L ? now + manaLockRemainingMs : 0L;
	}

	public static boolean isMadness() {
		return madness;
	}

	public static boolean isReading() {
		return readingUntilMs > System.currentTimeMillis();
	}

	public static long readingUntilMs() {
		return readingUntilMs;
	}

	public static boolean isBonusLifeAvailable() {
		return bonusLifeAvailable;
	}

	public static boolean isManaLocked() {
		return manaLockUntilMs > System.currentTimeMillis();
	}

	public static long manaLockUntilMs() {
		return manaLockUntilMs;
	}

	public static long madnessStartedAtMs() {
		return madnessStartedAtMs;
	}

	/** Drop all session state (registered with {@code ClientSessionState}). */
	public static void reset() {
		madness = false;
		bonusLifeAvailable = false;
		readingUntilMs = 0L;
		manaLockUntilMs = 0L;
		madnessStartedAtMs = 0L;
	}
}
