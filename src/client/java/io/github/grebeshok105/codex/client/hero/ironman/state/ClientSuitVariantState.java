package io.github.grebeshok105.codex.client.hero.ironman.state;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import io.github.grebeshok105.codex.client.ClientSessionState;

/**
 * Варианты костюма Железного Человека по UUID игрока на клиенте.
 * Используется скин-миксином, чтобы каждый видел актуальный костюм.
 */
public final class ClientSuitVariantState {
	private static final class Maps {
		final Map<UUID, Integer> variants = new HashMap<>();
	}

	private static final Maps MAPS = new Maps();

	static {
		ClientSessionState.register(ClientSuitVariantState::clear);
	}

	private ClientSuitVariantState() {
	}

	public static synchronized void update(UUID playerId, int variant) {
		if (variant == 0) {
			MAPS.variants.remove(playerId);
		} else {
			MAPS.variants.put(playerId, variant);
		}
	}

	public static synchronized int variantFor(UUID playerId) {
		Integer v = MAPS.variants.get(playerId);
		return v == null ? 0 : v;
	}

	public static synchronized void clear() {
		MAPS.variants.clear();
	}
}
