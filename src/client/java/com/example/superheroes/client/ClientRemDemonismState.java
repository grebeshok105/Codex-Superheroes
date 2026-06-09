package com.example.superheroes.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ClientRemDemonismState {
	private static final Map<UUID, State> STATES = new HashMap<>();

	private ClientRemDemonismState() {
	}

	public static void update(UUID playerId, float charge, boolean active, boolean permanent) {
		if (playerId == null) {
			return;
		}
		if (!active && charge <= 0.001f && !permanent) {
			STATES.remove(playerId);
			return;
		}
		STATES.put(playerId, new State(Math.max(0f, Math.min(100f, charge)), active, permanent));
	}

	public static boolean isActive(UUID playerId) {
		State state = STATES.get(playerId);
		return state != null && state.active();
	}

	public static float charge(UUID playerId) {
		State state = STATES.get(playerId);
		return state == null ? 0f : state.charge();
	}

	public static boolean isPermanent(UUID playerId) {
		State state = STATES.get(playerId);
		return state != null && state.permanent();
	}

	public static void clear(UUID playerId) {
		STATES.remove(playerId);
	}

	public static void clearAll() {
		STATES.clear();
	}

	private record State(float charge, boolean active, boolean permanent) {
	}
}
