package com.example.superheroes.network;

import com.example.superheroes.ModId;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Pandora — one-time death cinematic (#7). Sent to the killer (and Pandora) to start
 * the client-side cinematic: time-freeze feel, blood-red repaint, the killer's camera
 * smoothly turning toward Pandora at {@code (x, y, z)}, then a fade. The client times
 * and ends the cinematic itself.
 */
public record PandoraDeathS2CPayload(double x, double y, double z) implements CustomPacketPayload {
	public static final Type<PandoraDeathS2CPayload> TYPE = new Type<>(ModId.of("pandora_death"));

	public static final StreamCodec<ByteBuf, PandoraDeathS2CPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.DOUBLE, PandoraDeathS2CPayload::x,
			ByteBufCodecs.DOUBLE, PandoraDeathS2CPayload::y,
			ByteBufCodecs.DOUBLE, PandoraDeathS2CPayload::z,
			PandoraDeathS2CPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
