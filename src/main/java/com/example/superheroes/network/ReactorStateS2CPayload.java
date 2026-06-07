package com.example.superheroes.network;

import com.example.superheroes.ModId;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ReactorStateS2CPayload(boolean active, int progressTicks, int totalTicks, boolean hasStock) implements CustomPacketPayload {
	public static final Type<ReactorStateS2CPayload> TYPE = new Type<>(ModId.of("reactor_state"));

	public static final StreamCodec<ByteBuf, ReactorStateS2CPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, ReactorStateS2CPayload::active,
			ByteBufCodecs.VAR_INT, ReactorStateS2CPayload::progressTicks,
			ByteBufCodecs.VAR_INT, ReactorStateS2CPayload::totalTicks,
			ByteBufCodecs.BOOL, ReactorStateS2CPayload::hasStock,
			ReactorStateS2CPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
