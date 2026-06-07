package com.example.superheroes.network;

import com.example.superheroes.ModId;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ReinhardSwordGateS2CPayload(boolean ready, float progress) implements CustomPacketPayload {
	public static final Type<ReinhardSwordGateS2CPayload> TYPE = new Type<>(ModId.of("reinhard_sword_gate"));

	public static final StreamCodec<ByteBuf, ReinhardSwordGateS2CPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, ReinhardSwordGateS2CPayload::ready,
			ByteBufCodecs.FLOAT, ReinhardSwordGateS2CPayload::progress,
			ReinhardSwordGateS2CPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
