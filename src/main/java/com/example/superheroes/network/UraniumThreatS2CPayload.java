package com.example.superheroes.network;

import com.example.superheroes.ModId;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Личный сигнал клиенту-Хоумлендеру: рядом ли носители уран-кинжала.
 *  - self: чувствует ли локальный игрок давление урана прямо сейчас
 *  - sourceCount: сколько носителей кинжала рядом
 */
public record UraniumThreatS2CPayload(boolean self, int sourceCount) implements CustomPacketPayload {
	public static final Type<UraniumThreatS2CPayload> TYPE = new Type<>(ModId.of("uranium_threat"));

	public static final StreamCodec<ByteBuf, UraniumThreatS2CPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, UraniumThreatS2CPayload::self,
			ByteBufCodecs.VAR_INT, UraniumThreatS2CPayload::sourceCount,
			UraniumThreatS2CPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
