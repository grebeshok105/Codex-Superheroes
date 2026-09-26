package com.example.superheroes.client.core.module;

import com.example.superheroes.client.core.hud.HudLayer;
import com.example.superheroes.client.core.hud.HudLayers;
import com.example.superheroes.client.core.hud.MovableHud;
import com.example.superheroes.client.core.input.HeroActionKeys;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

/** Default {@link HeroClientContext}: delegates to the real registries. One instance per module — {@link #heroId} scopes action-key filtering. */
public final class CoreClientContext implements HeroClientContext {
	private final ResourceLocation heroId;

	public CoreClientContext(ResourceLocation heroId) {
		this.heroId = heroId;
	}

	@Override
	public <T extends CustomPacketPayload> void receive(CustomPacketPayload.Type<T> type, ClientPlayNetworking.PlayPayloadHandler<T> handler) {
		ClientPlayNetworking.registerGlobalReceiver(type, handler);
	}

	@Override
	public void hud(int order, ResourceLocation id, HudLayer layer) {
		HudLayers.register(order, id, layer);
	}

	@Override
	public void movableHud(int order, ResourceLocation id, HudLayer layer, MovableHud movable) {
		HudLayers.registerMovable(order, id, layer, movable);
	}

	@Override
	public KeyMapping actionKey(KeyMapping mapping, Consumer<Minecraft> onPress) {
		return HeroActionKeys.register(heroId, mapping, onPress);
	}
}
