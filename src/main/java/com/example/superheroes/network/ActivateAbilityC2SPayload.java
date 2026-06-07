package com.example.superheroes.network;

import com.example.superheroes.ModId;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ActivateAbilityC2SPayload(ResourceLocation abilityId) implements CustomPacketPayload {
	public static final Type<ActivateAbilityC2SPayload> TYPE = new Type<>(ModId.of("activate_ability"));

	public static final StreamCodec<ByteBuf, ActivateAbilityC2SPayload> STREAM_CODEC = StreamCodec.composite(
			ResourceLocation.STREAM_CODEC, ActivateAbilityC2SPayload::abilityId,
			ActivateAbilityC2SPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
