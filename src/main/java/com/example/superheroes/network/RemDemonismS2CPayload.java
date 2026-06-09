package com.example.superheroes.network;

import com.example.superheroes.ModId;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record RemDemonismS2CPayload(UUID playerId, float charge, boolean active, boolean permanent) implements CustomPacketPayload {
	public static final Type<RemDemonismS2CPayload> TYPE = new Type<>(ModId.of("rem_demonism"));

	public static final StreamCodec<ByteBuf, RemDemonismS2CPayload> STREAM_CODEC = StreamCodec.composite(
			UUIDUtil.STREAM_CODEC, RemDemonismS2CPayload::playerId,
			ByteBufCodecs.FLOAT, RemDemonismS2CPayload::charge,
			ByteBufCodecs.BOOL, RemDemonismS2CPayload::active,
			ByteBufCodecs.BOOL, RemDemonismS2CPayload::permanent,
			RemDemonismS2CPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
