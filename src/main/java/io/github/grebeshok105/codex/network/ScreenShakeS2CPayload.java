package io.github.grebeshok105.codex.network;

import io.github.grebeshok105.codex.ModId;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ScreenShakeS2CPayload(float intensity, int durationTicks) implements CustomPacketPayload {
	public static final Type<ScreenShakeS2CPayload> TYPE = new Type<>(ModId.of("screen_shake"));

	public static final StreamCodec<ByteBuf, ScreenShakeS2CPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.FLOAT, ScreenShakeS2CPayload::intensity,
			ByteBufCodecs.VAR_INT, ScreenShakeS2CPayload::durationTicks,
			ScreenShakeS2CPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
