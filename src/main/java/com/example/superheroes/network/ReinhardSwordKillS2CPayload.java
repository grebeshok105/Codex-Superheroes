package com.example.superheroes.network;

import com.example.superheroes.ModId;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ReinhardSwordKillS2CPayload(boolean active) implements CustomPacketPayload {
	public static final Type<ReinhardSwordKillS2CPayload> TYPE = new Type<>(ModId.of("reinhard_sword_kill"));

	public static final StreamCodec<ByteBuf, ReinhardSwordKillS2CPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, ReinhardSwordKillS2CPayload::active,
			ReinhardSwordKillS2CPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
