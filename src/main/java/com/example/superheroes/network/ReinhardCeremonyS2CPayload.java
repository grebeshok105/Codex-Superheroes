package com.example.superheroes.network;

import com.example.superheroes.ModId;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ReinhardCeremonyS2CPayload(boolean active, float progress) implements CustomPacketPayload {
	public static final Type<ReinhardCeremonyS2CPayload> TYPE = new Type<>(ModId.of("reinhard_sword_ceremony"));

	public static final StreamCodec<ByteBuf, ReinhardCeremonyS2CPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, ReinhardCeremonyS2CPayload::active,
			ByteBufCodecs.FLOAT, ReinhardCeremonyS2CPayload::progress,
			ReinhardCeremonyS2CPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
