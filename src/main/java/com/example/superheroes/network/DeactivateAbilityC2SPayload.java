package com.example.superheroes.network;

import com.example.superheroes.ModId;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DeactivateAbilityC2SPayload(ResourceLocation abilityId) implements CustomPacketPayload {
	public static final Type<DeactivateAbilityC2SPayload> TYPE = new Type<>(ModId.of("deactivate_ability"));

	public static final StreamCodec<ByteBuf, DeactivateAbilityC2SPayload> STREAM_CODEC = StreamCodec.composite(
			ResourceLocation.STREAM_CODEC, DeactivateAbilityC2SPayload::abilityId,
			DeactivateAbilityC2SPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
