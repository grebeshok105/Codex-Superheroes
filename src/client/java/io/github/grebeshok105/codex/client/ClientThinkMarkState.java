package io.github.grebeshok105.codex.client;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Игроки, выполняющие захват «Think, Mark!»: обе руки вытянуты вперёд
 * (поза переопределяется в {@code PlayerModelPoseMixin}).
 */
public final class ClientThinkMarkState {
	private static final class Maps {
		final Set<UUID> active = new HashSet<>();
	}

	private static final Maps MAPS = new Maps();

	static {
		ClientSessionState.register(ClientThinkMarkState::clear);
	}

	private ClientThinkMarkState() {
	}

	public static synchronized void update(UUID playerId, boolean active) {
		if (active) {
			MAPS.active.add(playerId);
		} else {
			MAPS.active.remove(playerId);
		}
	}

	public static synchronized boolean isActive(UUID playerId) {
		return MAPS.active.contains(playerId);
	}

	public static synchronized void clear() {
		MAPS.active.clear();
	}
}
