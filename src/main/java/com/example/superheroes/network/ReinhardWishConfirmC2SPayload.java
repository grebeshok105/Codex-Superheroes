package com.example.superheroes.network;

import com.example.superheroes.ModId;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ReinhardWishConfirmC2SPayload(String damageTypeId) implements CustomPacketPayload {
	public static final Type<ReinhardWishConfirmC2SPayload> TYPE = new Type<>(ModId.of("reinhard_wish_confirm"));

	public static final StreamCodec<ByteBuf, ReinhardWishConfirmC2SPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, ReinhardWishConfirmC2SPayload::damageTypeId,
			ReinhardWishConfirmC2SPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
