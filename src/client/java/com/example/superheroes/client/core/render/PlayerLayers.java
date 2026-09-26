package com.example.superheroes.client.core.render;

import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Registry of hero-owned {@link PlayerRenderer} feature layers. Client modules queue a factory via
 * {@code HeroClientContext.playerLayer(...)} during bootstrap; {@code SuperheroesClient} attaches every
 * queued layer inside {@link LivingEntityFeatureRendererRegistrationCallback} instead of naming concrete
 * layer classes itself.
 */
public final class PlayerLayers {
	private static final List<Function<PlayerRenderer, RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>>> FACTORIES = new ArrayList<>();

	private PlayerLayers() {
	}

	public static void register(Function<PlayerRenderer, RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>> factory) {
		FACTORIES.add(factory);
	}

	/** Instantiates every queued layer for this renderer; called once per {@link PlayerRenderer} construction. */
	public static void registerAll(PlayerRenderer renderer, LivingEntityFeatureRendererRegistrationCallback.RegistrationHelper helper) {
		for (Function<PlayerRenderer, RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>> factory : FACTORIES) {
			helper.register(factory.apply(renderer));
		}
	}
}
