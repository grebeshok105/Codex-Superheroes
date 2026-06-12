package com.example.superheroes.network;

import com.example.superheroes.ModId;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client → server: the Omni-Man grab is active and the player pressed "use"
 * (RMB) to launch the dash/slam.
 */
public record ThinkMarkDashC2SPayload() implements CustomPacketPayload {
	public static final Type<ThinkMarkDashC2SPayload> TYPE = new Type<>(ModId.of("think_mark_dash"));

	public static final StreamCodec<ByteBuf, ThinkMarkDashC2SPayload> STREAM_CODEC =
			StreamCodec.unit(new ThinkMarkDashC2SPayload());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
