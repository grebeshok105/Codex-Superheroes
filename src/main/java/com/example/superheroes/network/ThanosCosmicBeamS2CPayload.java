package com.example.superheroes.network;

import com.example.superheroes.ModId;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public record ThanosCosmicBeamS2CPayload(UUID shooter, Vec3 start, Vec3 end) implements CustomPacketPayload {
	public static final Type<ThanosCosmicBeamS2CPayload> TYPE = new Type<>(ModId.of("thanos_cosmic_beam"));

	public static final StreamCodec<ByteBuf, ThanosCosmicBeamS2CPayload> STREAM_CODEC = StreamCodec.composite(
			UUIDUtil.STREAM_CODEC, ThanosCosmicBeamS2CPayload::shooter,
			StreamCodecs.VEC3, ThanosCosmicBeamS2CPayload::start,
			StreamCodecs.VEC3, ThanosCosmicBeamS2CPayload::end,
			ThanosCosmicBeamS2CPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
