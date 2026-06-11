package com.example.superheroes.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Активные нано-формы Mark 85 по UUID игрока на клиенте
 * (0 — нет, 1 — клинок, 2 — супермолот, 3 — щит).
 * Используется {@code IronManNanoFormLayer} для рендера оружия на руке.
 */
public final class ClientNanoFormState {
	private static final Map<UUID, Integer> FORMS = new HashMap<>();

	private ClientNanoFormState() {
	}

	public static synchronized void update(UUID playerId, int form) {
		if (form == 0) {
			FORMS.remove(playerId);
		} else {
			FORMS.put(playerId, form);
		}
	}

	public static synchronized int formFor(UUID playerId) {
		Integer f = FORMS.get(playerId);
		return f == null ? 0 : f;
	}

	public static synchronized void clear() {
		FORMS.clear();
	}
}
