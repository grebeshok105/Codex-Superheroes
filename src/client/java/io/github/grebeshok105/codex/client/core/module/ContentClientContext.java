package io.github.grebeshok105.codex.client.core.module;

import io.github.grebeshok105.codex.client.core.hud.HudLayer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public interface ContentClientContext {
	<T extends CustomPacketPayload> void receive(CustomPacketPayload.Type<T> type, ClientPlayNetworking.PlayPayloadHandler<T> handler);

	void hud(int order, ResourceLocation id, HudLayer layer);

	/** Registers the renderer of an entity type owned by this module's content slice. */
	<T extends Entity> void entityRenderer(EntityType<T> type, EntityRendererProvider<T> provider);
}
