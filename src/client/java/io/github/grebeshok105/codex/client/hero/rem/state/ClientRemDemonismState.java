package io.github.grebeshok105.codex.client.hero.rem.state;

import io.github.grebeshok105.codex.client.ClientSessionState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ClientRemDemonismState {
	private static final class Maps {
		final Map<UUID, State> states = new HashMap<>();
	}

	private static final Maps MAPS = new Maps();

	static {
		ClientSessionState.register(ClientRemDemonismState::clearAll);
	}

	private ClientRemDemonismState() {
	}

	public static void update(UUID playerId, float charge, boolean active, boolean permanent) {
		if (playerId == null) {
			return;
		}
		if (!active && charge <= 0.001f && !permanent) {
			MAPS.states.remove(playerId);
			return;
		}
		MAPS.states.put(playerId, new State(Math.max(0f, Math.min(100f, charge)), active, permanent));
	}

	public static boolean isActive(UUID playerId) {
		State state = MAPS.states.get(playerId);
		return state != null && state.active();
	}

	public static float charge(UUID playerId) {
		State state = MAPS.states.get(playerId);
		return state == null ? 0f : state.charge();
	}

	public static boolean isPermanent(UUID playerId) {
		State state = MAPS.states.get(playerId);
		return state != null && state.permanent();
	}

	public static void clear(UUID playerId) {
		MAPS.states.remove(playerId);
	}

	public static void clearAll() {
		MAPS.states.clear();
	}

	private record State(float charge, boolean active, boolean permanent) {
	}
}
