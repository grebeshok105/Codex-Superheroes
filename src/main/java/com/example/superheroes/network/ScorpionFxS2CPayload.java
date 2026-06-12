package com.example.superheroes.network;

import com.example.superheroes.ModId;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

/**
 * Tells the client to play one of Scorpion's hellfire effects at a position.
 * Veil's Quasar particle system is client-only, so ability use is broadcast
 * here and rendered client-side (Veil emitters when installed, vanilla fallback otherwise).
 *
 * kind: 1 = harpoon chain, 2 = hellfire pillar, 3 = hellport burst, 4 = hell breath cone.
 */
public record ScorpionFxS2CPayload(int kind, Vec3 origin, Vec3 target) implements CustomPacketPayload {
	public static final int KIND_HARPOON = 1;
	public static final int KIND_PILLAR = 2;
	public static final int KIND_TELEPORT = 3;
	public static final int KIND_BREATH = 4;

	public static final Type<ScorpionFxS2CPayload> TYPE = new Type<>(ModId.of("scorpion_fx"));

	public static final StreamCodec<ByteBuf, ScorpionFxS2CPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, ScorpionFxS2CPayload::kind,
			StreamCodecs.VEC3, ScorpionFxS2CPayload::origin,
			StreamCodecs.VEC3, ScorpionFxS2CPayload::target,
			ScorpionFxS2CPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
