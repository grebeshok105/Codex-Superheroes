package com.example.superheroes.network;

import com.example.superheroes.ModId;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public record LaserFiredS2CPayload(UUID shooter, Vec3 start, Vec3 end) implements CustomPacketPayload {
	public static final Type<LaserFiredS2CPayload> TYPE = new Type<>(ModId.of("laser_fired"));

	public static final StreamCodec<ByteBuf, LaserFiredS2CPayload> STREAM_CODEC = StreamCodec.composite(
			UUIDUtil.STREAM_CODEC, LaserFiredS2CPayload::shooter,
			StreamCodecs.VEC3, LaserFiredS2CPayload::start,
			StreamCodecs.VEC3, LaserFiredS2CPayload::end,
			LaserFiredS2CPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
