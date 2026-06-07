package com.example.superheroes.network;

import com.example.superheroes.ModId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record DoomsdayProgressS2CPayload(int tier, int adaptations) implements CustomPacketPayload {
	public static final Type<DoomsdayProgressS2CPayload> TYPE = new Type<>(ModId.of("doomsday_progress"));

	public static final StreamCodec<RegistryFriendlyByteBuf, DoomsdayProgressS2CPayload> STREAM_CODEC =
			StreamCodec.composite(
					ByteBufCodecs.VAR_INT, DoomsdayProgressS2CPayload::tier,
					ByteBufCodecs.VAR_INT, DoomsdayProgressS2CPayload::adaptations,
					DoomsdayProgressS2CPayload::new
			);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
