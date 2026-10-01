package io.github.grebeshok105.codex.core.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;

/**
 * One laser scorch decal on a block face: the block the mark is painted on,
 * which face it is on, where on the face it sits ({@code u}/{@code v} in 0..1
 * from the block corner along the face's in-plane axes), its extent in blocks,
 * and its in-plane rotation in degrees.
 *
 * <p>Wire layout is exactly 16 bytes per mark: {@code pos} as a packed long (8),
 * {@code face} (1), {@code u}/{@code v}/{@code size} quantized to bytes (3),
 * {@code rot} as an int (4). The record normalizes {@code u}/{@code v} and
 * {@code size} onto the byte grid at construction so server state, NBT and the
 * wire always agree bit-for-bit.
 */
public record ScorchMark(BlockPos pos, byte face, float u, float v, float size, int rot) {
	public static final int WIRE_BYTES = 16;

	public static final StreamCodec<ByteBuf, ScorchMark> STREAM_CODEC = StreamCodec.of(
			(buf, m) -> {
				buf.writeLong(m.pos.asLong());
				buf.writeByte(m.face);
				buf.writeByte(quantize(m.u));
				buf.writeByte(quantize(m.v));
				buf.writeByte(quantize(m.size));
				buf.writeInt(m.rot);
			},
			buf -> new ScorchMark(BlockPos.of(buf.readLong()), buf.readByte(),
					unquantize(buf.readByte()), unquantize(buf.readByte()),
					unquantize(buf.readByte()), buf.readInt()));

	public ScorchMark {
		u = Mth.clamp(unquantize(quantize(u)), 0f, 1f);
		v = Mth.clamp(unquantize(quantize(v)), 0f, 1f);
		size = unquantize(quantize(size));
		rot = Math.floorMod(rot, 360);
	}

	/** The face the decal is painted on. */
	public Direction faceDirection() {
		return Direction.from3DDataValue(face);
	}

	public static int quantize(float value) {
		return Mth.clamp(Math.round(value * 255f), 0, 255);
	}

	public static float unquantize(int quantized) {
		return (quantized & 0xFF) / 255f;
	}
}
