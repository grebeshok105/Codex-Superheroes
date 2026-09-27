package io.github.grebeshok105.codex.client.core.render;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Predicates that veto the hero-skin override for a player (e.g. Iron Man's
 * nano suit-up keeps the vanilla skin while the armor materialises as a layer).
 * Consulted by the skin/hand mixins before {@link SkinResolver}.
 */
public final class SkinSuppressions {
	private static final List<Predicate<UUID>> SUPPRESSIONS = new ArrayList<>();

	private SkinSuppressions() {
	}

	public static void register(Predicate<UUID> suppression) {
		SUPPRESSIONS.add(suppression);
	}

	public static boolean suppresses(UUID playerId) {
		for (Predicate<UUID> suppression : SUPPRESSIONS) {
			if (suppression.test(playerId)) {
				return true;
			}
		}
		return false;
	}
}
