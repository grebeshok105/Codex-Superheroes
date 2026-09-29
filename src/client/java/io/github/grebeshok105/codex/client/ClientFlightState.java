package io.github.grebeshok105.codex.client;

import io.github.grebeshok105.codex.mechanic.flight.FlightMode;
import io.github.grebeshok105.codex.mechanic.flight.FlightPhase;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class ClientFlightState {
	private static final Map<Integer, State> STATES = new HashMap<>();
	private static final Set<Integer> PRESENTATION_OWNED = new HashSet<>();

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
		PRESENTATION_OWNED.clear();
	}

	/**
	 * Marks a player whose flight visuals are owned by a {@code FlightPresentation}
	 * ({@code FlightPoseTracker}); legacy visual managers such as
	 * {@code FlightTrailManager} stand down for marked players.
	 */
	public static synchronized void markPresentationOwned(int entityId) {
		PRESENTATION_OWNED.add(entityId);
	}

	public static synchronized void unmarkPresentationOwned(int entityId) {
		PRESENTATION_OWNED.remove(entityId);
	}

	public static synchronized boolean isPresentationOwned(int entityId) {
		return PRESENTATION_OWNED.contains(entityId);
	}

	public static synchronized State get(int entityId) {
		return STATES.get(entityId);
	}

	public record State(FlightMode mode, FlightPhase phase, float horizontalSpeed) {
	}
}
