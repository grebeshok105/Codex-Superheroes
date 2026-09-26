package io.github.grebeshok105.codex.core.ability;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public interface Ability {
	ResourceLocation getId();

	boolean isToggle();

	float costOnActivate();

	float costPerTick();

	/**
	 * Runs activation side effects on the server.
	 * Return {@code false} when the activation did not actually start; {@link AbilityRouter}
	 * will not spend the activation cost for that attempt.
	 */
	boolean tryActivate(ServerPlayer player);

	/**
	 * Pre-check called by {@link AbilityRouter} before resource consumption.
	 * Return {@code false} to silently abort the activation without spending energy/mana.
	 */
	default boolean canActivate(ServerPlayer player) {
		return true;
	}

	/**
	 * Whether the admin mob-targeting debug toggle may aim this ability at mobs (default false).
	 * Read by {@code debug/AdminAbilityDebug} through the registry — debug code must not name
	 * concrete hero abilities.
	 */
	default boolean debugTargetsMobs() {
		return false;
	}

	default void onTickActive(ServerPlayer player) {
	}

	default void onDeactivate(ServerPlayer player) {
	}
}
