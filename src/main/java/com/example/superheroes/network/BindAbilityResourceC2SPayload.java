package com.example.superheroes.network;

import com.example.superheroes.ModId;
import com.example.superheroes.resource.ResourceKind;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record BindAbilityResourceC2SPayload(ResourceLocation abilityId, ResourceKind kind) implements CustomPacketPayload {
	public static final Type<BindAbilityResourceC2SPayload> TYPE = new Type<>(ModId.of("bind_ability_resource"));

	public static final StreamCodec<ByteBuf, BindAbilityResourceC2SPayload> STREAM_CODEC = StreamCodec.composite(
			ResourceLocation.STREAM_CODEC, BindAbilityResourceC2SPayload::abilityId,
			ResourceKind.STREAM_CODEC, BindAbilityResourceC2SPayload::kind,
			BindAbilityResourceC2SPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
