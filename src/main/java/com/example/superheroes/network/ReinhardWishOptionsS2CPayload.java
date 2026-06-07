package com.example.superheroes.network;

import com.example.superheroes.ModId;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

public record ReinhardWishOptionsS2CPayload(
		List<String> damageTypeIds,
		List<String> adaptedDamageTypeIds,
		int wishesUsed,
		int wishesMax
) implements CustomPacketPayload {
	public static final Type<ReinhardWishOptionsS2CPayload> TYPE = new Type<>(ModId.of("reinhard_wish_options"));

	public static final StreamCodec<ByteBuf, ReinhardWishOptionsS2CPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), ReinhardWishOptionsS2CPayload::damageTypeIds,
			ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), ReinhardWishOptionsS2CPayload::adaptedDamageTypeIds,
			ByteBufCodecs.VAR_INT, ReinhardWishOptionsS2CPayload::wishesUsed,
			ByteBufCodecs.VAR_INT, ReinhardWishOptionsS2CPayload::wishesMax,
			ReinhardWishOptionsS2CPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
