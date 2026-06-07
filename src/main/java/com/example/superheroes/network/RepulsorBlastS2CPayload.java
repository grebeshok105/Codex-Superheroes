package com.example.superheroes.network;

import com.example.superheroes.ModId;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public record RepulsorBlastS2CPayload(UUID shooter, Vec3 start, Vec3 end) implements CustomPacketPayload {
	public static final Type<RepulsorBlastS2CPayload> TYPE = new Type<>(ModId.of("repulsor_blast"));

	public static final StreamCodec<ByteBuf, RepulsorBlastS2CPayload> STREAM_CODEC = StreamCodec.composite(
			UUIDUtil.STREAM_CODEC, RepulsorBlastS2CPayload::shooter,
			StreamCodecs.VEC3, RepulsorBlastS2CPayload::start,
			StreamCodecs.VEC3, RepulsorBlastS2CPayload::end,
			RepulsorBlastS2CPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
