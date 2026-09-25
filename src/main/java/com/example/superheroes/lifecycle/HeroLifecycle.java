package com.example.superheroes.lifecycle;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Hero-level transition events, distinct from {@link PlayerLifecycle}'s player-level
 * join/leave/death hooks (audit debt 1).
 *
 * <p>{@link #onClear} subscribers drop a player's hero-scoped session state when the
 * hero runtime is torn down — hero swap and untransform. (Leave/death cleanup is a
 * lighter path that goes through {@link PlayerLifecycle}.) Hooks are registered in
 * {@code SuperheroesMod.registerPlayerLifecycle} beside the {@link PlayerLifecycle}
 * wiring so the full cleanup set stays visible in one ordered table (audit debt 1:
 * cleanup used to live in a hardcoded call list in
 * {@code HeroTransformService.clearHeroRuntimeState}).
 */
public final class HeroLifecycle {
	private static final List<Consumer<ServerPlayer>> CLEAR = new ArrayList<>();
	private static final List<BiConsumer<ServerPlayer, ResourceLocation>> TRANSFORMED = new ArrayList<>();

	private HeroLifecycle() {
	}

	/** Drop hero-scoped session state for this player (swap/untransform/leave/death). */
	public static void onClear(Consumer<ServerPlayer> hook) {
		CLEAR.add(hook);
	}

	/** Fired after a successful transform, with the new hero id. */
	public static void onTransformed(BiConsumer<ServerPlayer, ResourceLocation> hook) {
		TRANSFORMED.add(hook);
	}

	public static void fireClear(ServerPlayer player) {
		for (Consumer<ServerPlayer> hook : CLEAR) {
			hook.accept(player);
		}
	}

	public static void fireTransformed(ServerPlayer player, ResourceLocation heroId) {
		for (BiConsumer<ServerPlayer, ResourceLocation> hook : TRANSFORMED) {
			hook.accept(player, heroId);
		}
	}
}
