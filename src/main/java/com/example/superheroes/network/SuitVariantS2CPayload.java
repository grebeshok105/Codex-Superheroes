package com.example.superheroes.network;

import com.example.superheroes.ModId;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

/**
 * Текущий вариант костюма Железного Человека конкретного игрока.
 * Рассылается всем клиентам, чтобы смена костюма была видна каждому
 * (скин игрока подменяется в {@code AbstractClientPlayerSkinMixin}).
 */
public record SuitVariantS2CPayload(UUID playerId, int variant) implements CustomPacketPayload {
	public static final Type<SuitVariantS2CPayload> TYPE = new Type<>(ModId.of("suit_variant"));

	public static final StreamCodec<ByteBuf, SuitVariantS2CPayload> STREAM_CODEC =
			StreamCodec.composite(
					UUIDUtil.STREAM_CODEC, SuitVariantS2CPayload::playerId,
					ByteBufCodecs.VAR_INT, SuitVariantS2CPayload::variant,
					SuitVariantS2CPayload::new
			);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
