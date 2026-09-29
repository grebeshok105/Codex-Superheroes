package io.github.grebeshok105.codex.core.net;

import io.github.grebeshok105.codex.ModId;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * Continuous visual effect on an entity: START opens the channel, UPDATE keeps
 * it alive and retargets it, STOP releases it. Senders refresh at
 * {@link VfxFx#CHANNEL_UPDATE_INTERVAL_TICKS}; the client expires a channel
 * that goes silent.
 */
public record VfxChannelS2CPayload(int entityId, ResourceLocation channel, byte state, Vec3 target)
		implements CustomPacketPayload {
	public static final Type<VfxChannelS2CPayload> TYPE = new Type<>(ModId.of("vfx_channel"));

	public static final byte START = 0;
	public static final byte UPDATE = 1;
	public static final byte STOP = 2;

	private static final StreamCodec<ByteBuf, Byte> STATE_CODEC = StreamCodec.of(
			(buf, state) -> buf.writeByte(state),
			buf -> {
				byte state = buf.readByte();
				if (state != START && state != UPDATE && state != STOP) {
					throw new DecoderException("unknown vfx channel state " + state);
				}
				return state;
			});

	public static final StreamCodec<ByteBuf, VfxChannelS2CPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, VfxChannelS2CPayload::entityId,
			ResourceLocation.STREAM_CODEC, VfxChannelS2CPayload::channel,
			STATE_CODEC, VfxChannelS2CPayload::state,
			StreamCodecs.VEC3, VfxChannelS2CPayload::target,
			VfxChannelS2CPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
