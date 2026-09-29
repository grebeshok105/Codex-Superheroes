package io.github.grebeshok105.codex.core.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.grebeshok105.codex.ModId;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class VfxPayloadCodecTest {
	@Test
	void eventRoundTrips() {
		VfxEventS2CPayload payload = new VfxEventS2CPayload(ModId.of("laser"), 42,
				new Vec3(1.5, 64.0, -3.25), new Vec3(8.0, 65.5, -2.0), 1.25f, 777);
		FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
		VfxEventS2CPayload.STREAM_CODEC.encode(buf, payload);
		assertEquals(payload, VfxEventS2CPayload.STREAM_CODEC.decode(buf));
	}

	@Test
	void channelRoundTrips() {
		VfxChannelS2CPayload payload = new VfxChannelS2CPayload(7, ModId.of("laser_channel"),
				VfxChannelS2CPayload.START, new Vec3(0.0, 1.0, 2.0));
		FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
		VfxChannelS2CPayload.STREAM_CODEC.encode(buf, payload);
		assertEquals(payload, VfxChannelS2CPayload.STREAM_CODEC.decode(buf));
	}

	@Test
	void channelStateOutOfRangeRejected() {
		FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
		VfxChannelS2CPayload.STREAM_CODEC.encode(buf,
				new VfxChannelS2CPayload(7, ModId.of("laser_channel"), (byte) 7, Vec3.ZERO));
		assertThrows(DecoderException.class, () -> VfxChannelS2CPayload.STREAM_CODEC.decode(buf));
	}
}
