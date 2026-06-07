package com.example.superheroes.network;

import com.example.superheroes.ModId;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;
import java.util.UUID;

public record UraniumPressureS2CPayload(List<UUID> pressuredHomelanders) implements CustomPacketPayload {
	public static final Type<UraniumPressureS2CPayload> TYPE = new Type<>(ModId.of("uranium_pressure"));

	public static final StreamCodec<ByteBuf, UraniumPressureS2CPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.<ByteBuf, UUID>list().apply(UUIDUtil.STREAM_CODEC),
			UraniumPressureS2CPayload::pressuredHomelanders,
			UraniumPressureS2CPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
