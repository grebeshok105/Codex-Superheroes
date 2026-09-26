package io.github.grebeshok105.codex.core.net;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public interface PayloadRegistrar {
	<T extends CustomPacketPayload> void s2c(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec);

	<T extends CustomPacketPayload> void c2s(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
			ServerPlayNetworking.PlayPayloadHandler<T> handler);

	PayloadRegistrar FABRIC = new PayloadRegistrar() {
		@Override
		public <T extends CustomPacketPayload> void s2c(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
			PayloadTypeRegistry.playS2C().register(type, codec);
		}

		@Override
		public <T extends CustomPacketPayload> void c2s(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
				ServerPlayNetworking.PlayPayloadHandler<T> handler) {
			PayloadTypeRegistry.playC2S().register(type, codec);
			ServerPlayNetworking.registerGlobalReceiver(type, handler);
		}
	};
}
