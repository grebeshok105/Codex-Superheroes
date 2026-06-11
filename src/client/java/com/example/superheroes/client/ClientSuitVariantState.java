package com.example.superheroes.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Варианты костюма Железного Человека по UUID игрока на клиенте.
 * Используется скин-миксином, чтобы каждый видел актуальный костюм.
 */
public final class ClientSuitVariantState {
	private static final Map<UUID, Integer> VARIANTS = new HashMap<>();

	private ClientSuitVariantState() {
	}

	public static synchronized void update(UUID playerId, int variant) {
		if (variant == 0) {
			VARIANTS.remove(playerId);
		} else {
			VARIANTS.put(playerId, variant);
		}
	}

	public static synchronized int variantFor(UUID playerId) {
		Integer v = VARIANTS.get(playerId);
		return v == null ? 0 : v;
	}

	public static synchronized void clear() {
		VARIANTS.clear();
	}
}
