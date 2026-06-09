package com.example.superheroes.network;

import com.example.superheroes.ModId;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

public record WallImpactDebrisS2CPayload(Vec3 position, Vec3 direction, float intensity, int[] blockStateIds) implements CustomPacketPayload {
	private static final int MAX_BLOCK_STATE_IDS = 256;

	public static final Type<WallImpactDebrisS2CPayload> TYPE = new Type<>(ModId.of("wall_impact_debris"));

	private static final StreamCodec<ByteBuf, int[]> INT_ARRAY = StreamCodec.of(
			(buf, values) -> {
				ByteBufCodecs.VAR_INT.encode(buf, values.length);
				for (int value : values) {
					ByteBufCodecs.VAR_INT.encode(buf, value);
				}
			},
			buf -> {
				int length = ByteBufCodecs.VAR_INT.decode(buf);
				if (length < 0 || length > MAX_BLOCK_STATE_IDS) {
					throw new IllegalArgumentException("Invalid block state id count: " + length);
				}
				int[] values = new int[length];
				for (int i = 0; i < length; i++) {
					values[i] = ByteBufCodecs.VAR_INT.decode(buf);
				}
				return values;
			}
	);

	public static final StreamCodec<ByteBuf, WallImpactDebrisS2CPayload> STREAM_CODEC = StreamCodec.composite(
			StreamCodecs.VEC3, WallImpactDebrisS2CPayload::position,
			StreamCodecs.VEC3, WallImpactDebrisS2CPayload::direction,
			ByteBufCodecs.FLOAT, WallImpactDebrisS2CPayload::intensity,
			INT_ARRAY, WallImpactDebrisS2CPayload::blockStateIds,
			WallImpactDebrisS2CPayload::new
	);

	public WallImpactDebrisS2CPayload {
		blockStateIds = blockStateIds == null ? new int[0] : blockStateIds.clone();
	}

	@Override
	public int[] blockStateIds() {
		return blockStateIds.clone();
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
