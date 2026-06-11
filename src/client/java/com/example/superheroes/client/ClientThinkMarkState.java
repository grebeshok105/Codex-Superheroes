package com.example.superheroes.client;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Игроки, выполняющие захват «Think, Mark!»: обе руки вытянуты вперёд
 * (поза переопределяется в {@code PlayerModelPoseMixin}).
 */
public final class ClientThinkMarkState {
	private static final Set<UUID> ACTIVE = new HashSet<>();

	private ClientThinkMarkState() {
	}

	public static synchronized void update(UUID playerId, boolean active) {
		if (active) {
			ACTIVE.add(playerId);
		} else {
			ACTIVE.remove(playerId);
		}
	}

	public static synchronized boolean isActive(UUID playerId) {
		return ACTIVE.contains(playerId);
	}

	public static synchronized void clear() {
		ACTIVE.clear();
	}
}
