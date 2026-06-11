package com.example.superheroes.network;

import com.example.superheroes.ModId;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Состояние админ-билда игрока. Клиент по нему показывает/прячет
 * админ-предметы в креатив-вкладке Superheroes.
 */
public record AdminBuildS2CPayload(boolean enabled) implements CustomPacketPayload {
	public static final Type<AdminBuildS2CPayload> TYPE = new Type<>(ModId.of("admin_build"));

	public static final StreamCodec<ByteBuf, AdminBuildS2CPayload> STREAM_CODEC =
			StreamCodec.composite(
					ByteBufCodecs.BOOL, AdminBuildS2CPayload::enabled,
					AdminBuildS2CPayload::new
			);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
