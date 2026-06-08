package com.example.superheroes.client;

import com.example.superheroes.flight.FlightMode;
import com.example.superheroes.flight.FlightPhase;

import java.util.HashMap;
import java.util.Map;

public final class ClientFlightState {
	private static final Map<Integer, State> STATES = new HashMap<>();

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
