package io.github.grebeshok105.codex.client.core.render;

import net.minecraft.client.player.AbstractClientPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Hero-registered veto for the vanilla {@code PlayerModel}: while any
 * registered predicate says {@code true}, {@code PlayerModelPoseMixin} hides
 * {@code model.root()} for that player so a hero-owned substitute (e.g. the
 * Homelander EMF model layer) can draw in its place. Mirrors
 * {@link SkinSuppressions}: shared core holds the gate, hero modules own the
 * predicate — no hero knowledge enters client core.
 */
public final class PlayerModelSuppressions {
	private static final List<Predicate<AbstractClientPlayer>> SUPPRESSIONS = new ArrayList<>();

	private PlayerModelSuppressions() {
	}

	public static void register(Predicate<AbstractClientPlayer> suppression) {
		SUPPRESSIONS.add(suppression);
	}

	public static boolean suppresses(AbstractClientPlayer player) {
		for (Predicate<AbstractClientPlayer> suppression : SUPPRESSIONS) {
			if (suppression.test(player)) {
				return true;
			}
		}
		return false;
	}
}
