package io.github.grebeshok105.codex.network;

import io.github.grebeshok105.codex.ModId;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SuperJumpC2SPayload() implements CustomPacketPayload {
	public static final SuperJumpC2SPayload INSTANCE = new SuperJumpC2SPayload();
	public static final Type<SuperJumpC2SPayload> TYPE = new Type<>(ModId.of("super_jump"));

	public static final StreamCodec<ByteBuf, SuperJumpC2SPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
