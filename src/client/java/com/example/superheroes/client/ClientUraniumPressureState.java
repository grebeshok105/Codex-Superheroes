package com.example.superheroes.client;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class ClientUraniumPressureState {
	private static volatile Set<UUID> pressured = Collections.emptySet();

	private ClientUraniumPressureState() {
	}

	public static void update(List<UUID> ids) {
		pressured = ids.isEmpty() ? Collections.emptySet() : new HashSet<>(ids);
	}

	public static boolean isPressured(UUID id) {
		return pressured.contains(id);
	}

	public static boolean anyPressured() {
		return !pressured.isEmpty();
	}
}
