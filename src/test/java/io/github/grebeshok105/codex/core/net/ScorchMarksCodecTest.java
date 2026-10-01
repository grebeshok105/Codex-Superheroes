package io.github.grebeshok105.codex.core.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import org.junit.jupiter.api.Test;

import java.util.List;

class ScorchMarksCodecTest {
	@Test
	void markRoundTrips() {
		ScorchMark mark = new ScorchMark(new BlockPos(-45, 70, 128), (byte) 1,
				0.37f, 0.92f, 0.31f, 217);
		FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
		ScorchMark.STREAM_CODEC.encode(buf, mark);
		assertEquals(mark, ScorchMark.STREAM_CODEC.decode(buf));
	}

	@Test
	void markIsExactlySixteenBytesOnTheWire() {
		ScorchMark mark = new ScorchMark(new BlockPos(1, 2, 3), (byte) 5, 0.5f, 0.5f, 0.4f, 90);
		ByteBuf buf = Unpooled.buffer();
		ScorchMark.STREAM_CODEC.encode(buf, mark);
		assertEquals(ScorchMark.WIRE_BYTES, buf.readableBytes());
	}

	@Test
	void markNormalizesOutOfRangeFields() {
		ScorchMark mark = new ScorchMark(new BlockPos(0, 0, 0), (byte) 2, 1.7f, -0.4f, 3f, -10);
		assertTrue(mark.u() >= 0f && mark.u() <= 1f, "u clamped");
		assertTrue(mark.v() >= 0f && mark.v() <= 1f, "v clamped");
		assertEquals(350, mark.rot(), "rot wraps into 0..359");
	}

	@Test
	void payloadRoundTrips() {
		List<ScorchMark> marks = List.of(
				new ScorchMark(new BlockPos(0, 64, 0), (byte) 1, 0.5f, 0.5f, 0.3f, 0),
				new ScorchMark(new BlockPos(7, 63, -9), (byte) 4, 0.1f, 0.9f, 0.25f, 180));
		ScorchMarksS2CPayload payload = new ScorchMarksS2CPayload(marks, true);
		FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
		ScorchMarksS2CPayload.STREAM_CODEC.encode(buf, payload);
		assertEquals(payload, ScorchMarksS2CPayload.STREAM_CODEC.decode(buf));
	}

	@Test
	void oversizedMarkListRejected() {
		ByteBuf buf = Unpooled.buffer();
		ByteBufCodecs.VAR_INT.encode(buf, ScorchMarksS2CPayload.SYNC_CHUNK + 1);
		buf.writeBoolean(false);
		assertThrows(IllegalArgumentException.class, () -> ScorchMarksS2CPayload.STREAM_CODEC.decode(buf));
	}
}
