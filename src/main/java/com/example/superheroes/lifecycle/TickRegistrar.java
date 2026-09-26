package com.example.superheroes.lifecycle;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Where modules subscribe to dispatcher tick phases; backed by {@link HeroTickDispatcher} (BF11). */
public interface TickRegistrar {
	/** {@link HeroTickDispatcher.Phase#START} — {@code START_SERVER_TICK}. */
	void start(Consumer<MinecraftServer> task);

	/** {@link HeroTickDispatcher.Phase#EARLY} — first inside the END tick, before GLOBAL. */
	void early(Consumer<MinecraftServer> task);

	/** {@link HeroTickDispatcher.Phase#GLOBAL}. */
	void global(Consumer<MinecraftServer> task);

	/** {@link HeroTickDispatcher.Phase#LEVELS}. */
	void level(BiConsumer<MinecraftServer, ServerLevel> task);

	/** {@link HeroTickDispatcher.Phase#PLAYERS}. */
	void player(HeroTickDispatcher.PlayerTask task);

	/** {@link HeroTickDispatcher.Phase#PLAYERS} gated on the player's current hero. */
	void hero(ResourceLocation heroId, HeroTickDispatcher.PlayerTask task);

	/** {@link HeroTickDispatcher.Phase#ABILITY_ACTIVE}. */
	void activeAbility(ResourceLocation abilityId, HeroTickDispatcher.PlayerTask task);
}
