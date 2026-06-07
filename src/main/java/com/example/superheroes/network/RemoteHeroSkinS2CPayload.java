package com.example.superheroes.network;

import com.example.superheroes.ModId;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.UUIDUtil;

import java.util.Optional;
import java.util.UUID;

public record RemoteHeroSkinS2CPayload(UUID playerId, Optional<ResourceLocation> heroId) implements CustomPacketPayload {
	public static final Type<RemoteHeroSkinS2CPayload> TYPE = new Type<>(ModId.of("remote_hero_skin"));

	public static final StreamCodec<ByteBuf, RemoteHeroSkinS2CPayload> STREAM_CODEC = StreamCodec.composite(
			UUIDUtil.STREAM_CODEC, RemoteHeroSkinS2CPayload::playerId,
			ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), RemoteHeroSkinS2CPayload::heroId,
			RemoteHeroSkinS2CPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
