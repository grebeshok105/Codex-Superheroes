package com.example.superheroes.network;

import com.example.superheroes.ModId;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record HeroMeleeChargeC2SPayload(int action, int heldTicks, int targetId) implements CustomPacketPayload {
	public static final int ACTION_START = 0;
	public static final int ACTION_RELEASE = 1;
	public static final int ACTION_CANCEL = 2;

	public static final Type<HeroMeleeChargeC2SPayload> TYPE = new Type<>(ModId.of("hero_melee_charge"));

	public static final StreamCodec<ByteBuf, HeroMeleeChargeC2SPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, HeroMeleeChargeC2SPayload::action,
			ByteBufCodecs.VAR_INT, HeroMeleeChargeC2SPayload::heldTicks,
			ByteBufCodecs.INT, HeroMeleeChargeC2SPayload::targetId,
			HeroMeleeChargeC2SPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
