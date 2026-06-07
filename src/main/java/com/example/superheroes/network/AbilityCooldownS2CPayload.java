package com.example.superheroes.network;

import com.example.superheroes.ModId;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AbilityCooldownS2CPayload(ResourceLocation abilityId, int remainingTicks) implements CustomPacketPayload {
	public static final Type<AbilityCooldownS2CPayload> TYPE = new Type<>(ModId.of("ability_cooldown"));

	public static final StreamCodec<ByteBuf, AbilityCooldownS2CPayload> STREAM_CODEC = StreamCodec.composite(
			ResourceLocation.STREAM_CODEC, AbilityCooldownS2CPayload::abilityId,
			ByteBufCodecs.VAR_INT, AbilityCooldownS2CPayload::remainingTicks,
			AbilityCooldownS2CPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
