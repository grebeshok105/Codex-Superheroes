package com.example.superheroes.client;

public final class HotbarLockState {
	private static boolean locked = false;
	private static long lastToggleTime = 0L;

	private HotbarLockState() {
	}

	public static boolean isLocked() {
		return locked;
	}

	public static void toggle() {
		locked = !locked;
		lastToggleTime = System.currentTimeMillis();
	}

	public static boolean showIndicator() {
		return System.currentTimeMillis() - lastToggleTime < 2000L;
	}
}
