package com.example.superheroes.network;

import com.example.superheroes.ModId;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ResourceUpdateS2CPayload(float energy, float mana) implements CustomPacketPayload {
	public static final Type<ResourceUpdateS2CPayload> TYPE = new Type<>(ModId.of("resource_update"));

	public static final StreamCodec<ByteBuf, ResourceUpdateS2CPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.FLOAT, ResourceUpdateS2CPayload::energy,
			ByteBufCodecs.FLOAT, ResourceUpdateS2CPayload::mana,
			ResourceUpdateS2CPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
