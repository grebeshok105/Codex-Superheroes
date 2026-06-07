package com.example.superheroes.client;

public final class ClientMadnessState {
	private static volatile boolean madness = false;
	private static volatile boolean bonusLifeAvailable = false;
	private static volatile long readingUntilMs = 0L;
	private static volatile long manaLockUntilMs = 0L;
	private static volatile long madnessStartedAtMs = 0L;

	private ClientMadnessState() {
	}

	public static void update(boolean madnessNew, boolean bonusLife, long readingUntil, long manaLock) {
		if (madnessNew && !madness) {
			madnessStartedAtMs = System.currentTimeMillis();
		}
		if (!madnessNew) {
			madnessStartedAtMs = 0L;
		}
		madness = madnessNew;
		bonusLifeAvailable = bonusLife;
		readingUntilMs = readingUntil;
		manaLockUntilMs = manaLock;
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
}
