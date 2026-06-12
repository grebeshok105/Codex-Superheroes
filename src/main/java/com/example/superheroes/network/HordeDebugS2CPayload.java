package com.example.superheroes.network;

import com.example.superheroes.ModId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Pre-formatted horde debug overlay text (newline-separated lines, with §
 * colour codes). Empty text hides the overlay.
 */
public record HordeDebugS2CPayload(String text) implements CustomPacketPayload {
	public static final Type<HordeDebugS2CPayload> TYPE = new Type<>(ModId.of("horde_debug"));

	public static final StreamCodec<RegistryFriendlyByteBuf, HordeDebugS2CPayload> STREAM_CODEC =
			StreamCodec.composite(
					ByteBufCodecs.STRING_UTF8, HordeDebugS2CPayload::text,
					HordeDebugS2CPayload::new
			);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
