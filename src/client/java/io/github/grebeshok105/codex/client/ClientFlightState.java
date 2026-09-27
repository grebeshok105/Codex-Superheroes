package io.github.grebeshok105.codex.client;

import io.github.grebeshok105.codex.flight.FlightMode;
import io.github.grebeshok105.codex.flight.FlightPhase;

import java.util.HashMap;
import java.util.Map;

public final class ClientFlightState {
	private static final Map<Integer, State> STATES = new HashMap<>();

	static {
		ClientSessionState.register(ClientFlightState::clearAll);
	}

	private ClientFlightState() {
	}

	public static synchronized void update(int entityId, boolean active, FlightMode mode, FlightPhase phase, float horizontalSpeed) {
		if (!active) {
			STATES.remove(entityId);
			return;
		}
		STATES.put(entityId, new State(mode, phase, horizontalSpeed));
	}

	public static synchronized void clear(int entityId) {
		STATES.remove(entityId);
	}

	public static synchronized void clearAll() {
		STATES.clear();
	}

	public static synchronized State get(int entityId) {
		return STATES.get(entityId);
	}

	public record State(FlightMode mode, FlightPhase phase, float horizontalSpeed) {
	}
}
