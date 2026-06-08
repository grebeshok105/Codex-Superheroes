package com.example.superheroes.network;

import com.example.superheroes.ModId;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record FlightStateS2CPayload(
		int entityId,
		boolean active,
		int mode,
		int phase,
		float horizontalSpeed
) implements CustomPacketPayload {
	public static final Type<FlightStateS2CPayload> TYPE = new Type<>(ModId.of("flight_state"));

	public static final StreamCodec<ByteBuf, FlightStateS2CPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, FlightStateS2CPayload::entityId,
			ByteBufCodecs.BOOL, FlightStateS2CPayload::active,
			ByteBufCodecs.VAR_INT, FlightStateS2CPayload::mode,
			ByteBufCodecs.VAR_INT, FlightStateS2CPayload::phase,
			ByteBufCodecs.FLOAT, FlightStateS2CPayload::horizontalSpeed,
			FlightStateS2CPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
