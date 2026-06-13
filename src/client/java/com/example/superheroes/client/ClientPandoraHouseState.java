package com.example.superheroes.client;

/**
 * Client-side mirror of whether the local Pandora's House of Vanity is open (#2). Drives which
 * Pandora abilities are visible — see {@link ClientAbilityFilter}.
 */
public final class ClientPandoraHouseState {
	private static volatile boolean open;

	private ClientPandoraHouseState() {
	}

	public static void set(boolean value) {
		open = value;
	}

	public static boolean isOpen() {
		return open;
	}
}
