package io.github.grebeshok105.codex.client.render;

import io.github.grebeshok105.codex.client.core.render.BeamRenderer;
import io.github.grebeshok105.codex.client.core.render.PlayerLayers;
import io.github.grebeshok105.codex.client.render.lightning.SuperheroLightningRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.EntityType;

/**
 * Shared render wiring: the Wild shader instances, the beam renderer's world-render hook,
 * the lightning-bolt override, and the player-layer callback every hero module feeds through
 * {@code HeroClientContext.playerLayer}.
 */
public final class CoreRenderers {
	private CoreRenderers() {
	}

	public static void init() {
		WildShaders.register();
		BeamRenderer.register();
		EntityRendererRegistry.register(EntityType.LIGHTNING_BOLT, SuperheroLightningRenderer::new);
		LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, entityRenderer, registrationHelper, context) -> {
			if (entityRenderer instanceof PlayerRenderer playerRenderer) {
				PlayerLayers.registerAll(playerRenderer, registrationHelper);
			}
		});
	}
}
