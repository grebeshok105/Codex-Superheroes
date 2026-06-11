package com.example.superheroes.network;

import com.example.superheroes.ModId;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

/**
 * Текущая нано-форма Железного Человека конкретного игрока
 * (0 — нет, 1 — клинок, 2 — супермолот, 3 — щит).
 * Рассылается всем клиентам, чтобы нано-оружие на руке видел каждый.
 */
public record NanoFormS2CPayload(UUID playerId, int form) implements CustomPacketPayload {
	public static final Type<NanoFormS2CPayload> TYPE = new Type<>(ModId.of("nano_form"));

	public static final StreamCodec<ByteBuf, NanoFormS2CPayload> STREAM_CODEC =
			StreamCodec.composite(
					UUIDUtil.STREAM_CODEC, NanoFormS2CPayload::playerId,
					ByteBufCodecs.VAR_INT, NanoFormS2CPayload::form,
					NanoFormS2CPayload::new
			);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
