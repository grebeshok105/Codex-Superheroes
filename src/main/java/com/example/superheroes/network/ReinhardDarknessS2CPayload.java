package com.example.superheroes.network;

import com.example.superheroes.ModId;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ReinhardDarknessS2CPayload(int durationTicks) implements CustomPacketPayload {
	public static final Type<ReinhardDarknessS2CPayload> TYPE = new Type<>(ModId.of("reinhard_darkness"));

	public static final StreamCodec<ByteBuf, ReinhardDarknessS2CPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, ReinhardDarknessS2CPayload::durationTicks,
			ReinhardDarknessS2CPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
