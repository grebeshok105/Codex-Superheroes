package com.example.superheroes.network;

import com.example.superheroes.ModId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ThanosStonesS2CPayload(int bitmask) implements CustomPacketPayload {
	public static final Type<ThanosStonesS2CPayload> TYPE = new Type<>(ModId.of("thanos_stones"));

	public static final StreamCodec<RegistryFriendlyByteBuf, ThanosStonesS2CPayload> STREAM_CODEC =
			StreamCodec.composite(
					ByteBufCodecs.VAR_INT, ThanosStonesS2CPayload::bitmask,
					ThanosStonesS2CPayload::new
			);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
