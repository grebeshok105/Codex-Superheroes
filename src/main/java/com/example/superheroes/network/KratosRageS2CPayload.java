package com.example.superheroes.network;

import com.example.superheroes.ModId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record KratosRageS2CPayload(float rage, boolean active) implements CustomPacketPayload {
	public static final Type<KratosRageS2CPayload> TYPE = new Type<>(ModId.of("kratos_rage"));

	public static final StreamCodec<RegistryFriendlyByteBuf, KratosRageS2CPayload> STREAM_CODEC =
			StreamCodec.composite(
					ByteBufCodecs.FLOAT, KratosRageS2CPayload::rage,
					ByteBufCodecs.BOOL, KratosRageS2CPayload::active,
					KratosRageS2CPayload::new
			);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
