package io.github.grebeshok105.codex.mechanic.flight;

import io.github.grebeshok105.codex.core.model.HeroData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Per-hero hooks the flight mechanic consults. A hero module registers one
 * implementation through {@link FlightProfiles#registerModifier}; heroes without
 * a modifier get the unmodified default behavior. Kept in the mechanic package so
 * the flight classes never import a foreign hero.
 */
public interface FlightModifier {

	/** Activation gate for {@code mechanic.ability.FlightAbility} — true denies activation. */
	default boolean denyActivation(ServerPlayer player, HeroData data) {
		return false;
	}

	/** Restriction pressure while flying NORMAL (uranium-style forced off). */
	default boolean restrictsFlight(ServerPlayer player, HeroData data) {
		return false;
	}

	/** Boosted tuning flag (madness-style multiplier) read by the client travel hook. */
	default boolean boosted(Player player) {
		return false;
	}
}
