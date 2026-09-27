package io.github.grebeshok105.codex.client.core.module;

import io.github.grebeshok105.codex.client.core.hud.HudLayer;
import io.github.grebeshok105.codex.client.core.hud.HudLayers;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/** Default {@link ContentClientContext}: delegates to the real registries. */
public final class CoreContentClientContext implements ContentClientContext {
	@Override
	public <T extends CustomPacketPayload> void receive(CustomPacketPayload.Type<T> type, ClientPlayNetworking.PlayPayloadHandler<T> handler) {
		ClientPlayNetworking.registerGlobalReceiver(type, handler);
	}

	@Override
	public void hud(int order, ResourceLocation id, HudLayer layer) {
		HudLayers.register(order, id, layer);
	}

	@Override
	public <T extends Entity> void entityRenderer(EntityType<T> type, EntityRendererProvider<T> provider) {
		EntityRendererRegistry.register(type, provider);
	}
}
