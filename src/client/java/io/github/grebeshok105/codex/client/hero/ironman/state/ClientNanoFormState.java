package io.github.grebeshok105.codex.client.hero.ironman.state;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import io.github.grebeshok105.codex.client.ClientSessionState;

/**
 * Активные нано-формы Mark 85 по UUID игрока на клиенте
 * (0 — нет, 1 — клинок, 2 — супермолот, 3 — щит).
 * Используется {@code IronManNanoFormLayer} для рендера оружия на руке.
 */
public final class ClientNanoFormState {
	private static final class Maps {
		final Map<UUID, Integer> forms = new HashMap<>();
	}

	private static final Maps MAPS = new Maps();

	static {
		ClientSessionState.register(ClientNanoFormState::clear);
	}

	private ClientNanoFormState() {
	}

	public static synchronized void update(UUID playerId, int form) {
		if (form == 0) {
			MAPS.forms.remove(playerId);
		} else {
			MAPS.forms.put(playerId, form);
		}
	}

	public static synchronized int formFor(UUID playerId) {
		Integer f = MAPS.forms.get(playerId);
		return f == null ? 0 : f;
	}

	public static synchronized void clear() {
		MAPS.forms.clear();
	}
}
