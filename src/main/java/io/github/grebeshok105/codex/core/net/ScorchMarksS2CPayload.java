package io.github.grebeshok105.codex.core.net;

import io.github.grebeshok105.codex.ModId;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;

/**
 * A batch of laser scorch marks for one dimension. Two send modes share the
 * type: a single freshly-created mark broadcast to nearby observers
 * ({@code reset = false}), and the chunked full-dimension sync a player gets on
 * join, respawn or dimension change ({@code reset = true} on the first chunk so
 * the client drops the previous dimension's marks before appending).
 */
public record ScorchMarksS2CPayload(List<ScorchMark> marks, boolean reset) implements CustomPacketPayload {
	/** Marks per sync chunk — the join/dimension-change stream sends at most this many per packet. */
	public static final int SYNC_CHUNK = 256;
	private static final int MAX_LIST = SYNC_CHUNK;

	public static final Type<ScorchMarksS2CPayload> TYPE = new Type<>(ModId.of("scorch_marks"));

	private static final StreamCodec<ByteBuf, List<ScorchMark>> MARK_LIST = StreamCodec.of(
			(buf, marks) -> {
				ByteBufCodecs.VAR_INT.encode(buf, marks.size());
				for (ScorchMark mark : marks) {
					ScorchMark.STREAM_CODEC.encode(buf, mark);
				}
			},
			buf -> {
				int count = ByteBufCodecs.VAR_INT.decode(buf);
				if (count < 0 || count > MAX_LIST) {
					throw new IllegalArgumentException("Invalid scorch mark count: " + count);
				}
				List<ScorchMark> marks = new ArrayList<>(count);
				for (int i = 0; i < count; i++) {
					marks.add(ScorchMark.STREAM_CODEC.decode(buf));
				}
				return marks;
			});

	public static final StreamCodec<ByteBuf, ScorchMarksS2CPayload> STREAM_CODEC = StreamCodec.composite(
			MARK_LIST, ScorchMarksS2CPayload::marks,
			ByteBufCodecs.BOOL, ScorchMarksS2CPayload::reset,
			ScorchMarksS2CPayload::new);

	public ScorchMarksS2CPayload {
		marks = List.copyOf(marks);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
