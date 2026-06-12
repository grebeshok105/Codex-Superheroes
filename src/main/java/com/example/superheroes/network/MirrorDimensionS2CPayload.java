package com.example.superheroes.network;

import com.example.superheroes.ModId;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Doctor Strange "Mirror Dimension": drives the victim's client-side Acid
 * shaderpack (Iris). The world-warp itself is rendered by the player's own
 * Acid Shaders pack; this payload only switches it on/off and supplies the
 * deformation options (MODE/J shader options).
 *
 * action: 0 = ON, 1 = OFF, 2 = KEEPALIVE (deadman switch refresh).
 */
public record MirrorDimensionS2CPayload(int action, int mode, int scale) implements CustomPacketPayload {
	public static final int ACTION_ON = 0;
	public static final int ACTION_OFF = 1;
	public static final int ACTION_KEEPALIVE = 2;

	public static final Type<MirrorDimensionS2CPayload> TYPE = new Type<>(ModId.of("mirror_dimension"));

	public static final StreamCodec<ByteBuf, MirrorDimensionS2CPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, MirrorDimensionS2CPayload::action,
			ByteBufCodecs.VAR_INT, MirrorDimensionS2CPayload::mode,
			ByteBufCodecs.VAR_INT, MirrorDimensionS2CPayload::scale,
			MirrorDimensionS2CPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
