package com.example.superheroes.network;

import com.example.superheroes.ModId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record MadnessVisualS2CPayload(int event) implements CustomPacketPayload {
	public static final int EVENT_ENTER = 1;
	public static final int EVENT_EXIT = 2;

	public static final Type<MadnessVisualS2CPayload> TYPE = new Type<>(ModId.of("madness_visual"));

	public static final StreamCodec<RegistryFriendlyByteBuf, MadnessVisualS2CPayload> STREAM_CODEC =
			StreamCodec.composite(
					ByteBufCodecs.VAR_INT, MadnessVisualS2CPayload::event,
					MadnessVisualS2CPayload::new
			);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
