package com.example.superheroes.ability;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public interface Ability {
	ResourceLocation getId();

	boolean isToggle();

	float costOnActivate();

	float costPerTick();

	boolean tryActivate(ServerPlayer player);

	/**
	 * Pre-check called by {@link AbilityRouter} before resource consumption.
	 * Return {@code false} to silently abort the activation without spending energy/mana.
	 */
	default boolean canActivate(ServerPlayer player) {
		return true;
	}

	default void onTickActive(ServerPlayer player) {
	}

	default void onDeactivate(ServerPlayer player) {
	}
}
