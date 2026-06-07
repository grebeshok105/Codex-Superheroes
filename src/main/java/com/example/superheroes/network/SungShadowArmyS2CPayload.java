package com.example.superheroes.network;

import com.example.superheroes.ModId;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

/**
 * Состояние армии теней Сон Джи Ву для конкретного игрока.
 *  - playerId: UUID Сон Джи Ву
 *  - hasShadows: есть ли живые тени (для авто-свапа фазы скина)
 *  - count: сколько живых теней (для HUD-индикатора)
 */
public record SungShadowArmyS2CPayload(UUID playerId, boolean hasShadows, int count, boolean phase2) implements CustomPacketPayload {
	public static final Type<SungShadowArmyS2CPayload> TYPE = new Type<>(ModId.of("sung_shadow_army"));

	public static final StreamCodec<ByteBuf, SungShadowArmyS2CPayload> STREAM_CODEC = StreamCodec.composite(
			UUIDUtil.STREAM_CODEC, SungShadowArmyS2CPayload::playerId,
			ByteBufCodecs.BOOL, SungShadowArmyS2CPayload::hasShadows,
			ByteBufCodecs.VAR_INT, SungShadowArmyS2CPayload::count,
			ByteBufCodecs.BOOL, SungShadowArmyS2CPayload::phase2,
			SungShadowArmyS2CPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
