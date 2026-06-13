package com.example.superheroes.network;

import com.example.superheroes.ModId;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Victim's client reports what happened when it tried to apply/restore the
 * Acid shaderpack, so the caster gets honest feedback instead of a silent fail.
 */
public record MirrorDimensionStatusC2SPayload(int status) implements CustomPacketPayload {
	public static final int OK_APPLIED = 0;
	public static final int NO_IRIS = 1;
	public static final int NO_PACK = 2;
	public static final int IRIS_API_FAIL = 3;
	public static final int OK_RESTORED = 4;

	public static final Type<MirrorDimensionStatusC2SPayload> TYPE = new Type<>(ModId.of("mirror_dimension_status"));

	public static final StreamCodec<ByteBuf, MirrorDimensionStatusC2SPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, MirrorDimensionStatusC2SPayload::status,
			MirrorDimensionStatusC2SPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
