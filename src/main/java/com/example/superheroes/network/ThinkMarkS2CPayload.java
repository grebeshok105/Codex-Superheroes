package com.example.superheroes.network;

import com.example.superheroes.ModId;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

/**
 * Состояние захвата «Think, Mark!» Омнимена: пока active — игрок держит цель
 * вытянутыми вперёд руками (клиентская поза рук).
 */
public record ThinkMarkS2CPayload(UUID playerId, boolean active) implements CustomPacketPayload {
	public static final Type<ThinkMarkS2CPayload> TYPE = new Type<>(ModId.of("think_mark"));

	public static final StreamCodec<ByteBuf, ThinkMarkS2CPayload> STREAM_CODEC =
			StreamCodec.composite(
					UUIDUtil.STREAM_CODEC, ThinkMarkS2CPayload::playerId,
					ByteBufCodecs.BOOL, ThinkMarkS2CPayload::active,
					ThinkMarkS2CPayload::new
			);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
