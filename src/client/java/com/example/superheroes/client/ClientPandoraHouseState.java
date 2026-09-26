package com.example.superheroes.client;

/**
 * Client-side mirror of whether the local Pandora's House of Vanity is open (#2). Fed to
 * Pandora's client module and HUD effects; the ability list itself follows the server-synced
 * {@code ability_availability} attachment — see {@link ClientAbilityVisibility}.
 */
public final class ClientPandoraHouseState {
	private static volatile boolean open;

	static {
		ClientSessionState.register(ClientPandoraHouseState::reset);
	}

	private ClientPandoraHouseState() {
	}

	public static void set(boolean value) {
		open = value;
	}

	public static boolean isOpen() {
		return open;
	}

	/** Drop all session state (registered with {@code ClientSessionState}). */
	public static void reset() {
		open = false;
	}
}
