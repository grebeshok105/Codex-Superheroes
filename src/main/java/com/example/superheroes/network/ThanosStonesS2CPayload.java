package com.example.superheroes.network;

import com.example.superheroes.ModId;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

/**
 * Набор камней бесконечности конкретного игрока-Таноса (битовая маска по
 * ординалам {@code InfinityStoneType}). Рассылается всем клиентам, чтобы
 * скин-перчатка каждого Таноса показывала только собранные им камни.
 */
public record ThanosStonesS2CPayload(UUID playerId, int bitmask) implements CustomPacketPayload {
	public static final Type<ThanosStonesS2CPayload> TYPE = new Type<>(ModId.of("thanos_stones"));

	public static final StreamCodec<ByteBuf, ThanosStonesS2CPayload> STREAM_CODEC =
			StreamCodec.composite(
					UUIDUtil.STREAM_CODEC, ThanosStonesS2CPayload::playerId,
					ByteBufCodecs.VAR_INT, ThanosStonesS2CPayload::bitmask,
					ThanosStonesS2CPayload::new
			);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
