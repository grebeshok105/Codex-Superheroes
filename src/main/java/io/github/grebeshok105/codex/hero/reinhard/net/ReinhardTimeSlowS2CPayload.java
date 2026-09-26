package io.github.grebeshok105.codex.hero.reinhard.net;

import io.github.grebeshok105.codex.ModId;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ReinhardTimeSlowS2CPayload(boolean active) implements CustomPacketPayload {
	public static final Type<ReinhardTimeSlowS2CPayload> TYPE = new Type<>(ModId.of("reinhard_time_slow"));

	public static final StreamCodec<ByteBuf, ReinhardTimeSlowS2CPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, ReinhardTimeSlowS2CPayload::active,
			ReinhardTimeSlowS2CPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
