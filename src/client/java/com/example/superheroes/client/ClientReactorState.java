package com.example.superheroes.client;

public final class ClientReactorState {
	private static volatile boolean active = false;
	private static volatile int progress = 0;
	private static volatile int total = 100;
	private static volatile boolean hasStock = true;

	private ClientReactorState() {
	}

	public static void update(boolean active, int progress, int total, boolean hasStock) {
		ClientReactorState.active = active;
		ClientReactorState.progress = progress;
		ClientReactorState.total = total;
		ClientReactorState.hasStock = hasStock;
	}

	public static boolean active() {
		return active;
	}

	public static int progress() {
		return progress;
	}

	public static int total() {
		return total;
	}

	public static boolean hasStock() {
		return hasStock;
	}
}
