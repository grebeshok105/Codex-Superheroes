package io.github.grebeshok105.codex.core.net;

import io.github.grebeshok105.codex.ModId;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * One-shot visual effect trigger. {@code target} is the secondary point an
 * effect aims at (beam end, look target); omnidirectional effects pass
 * {@code origin}. {@code seed} lets the client re-derive deterministic
 * randomness (particle spread) so every observer sees the same effect.
 */
public record VfxEventS2CPayload(ResourceLocation effect, int sourceEntityId, Vec3 origin, Vec3 target,
		float scale, int seed) implements CustomPacketPayload {
	public static final Type<VfxEventS2CPayload> TYPE = new Type<>(ModId.of("vfx_event"));

	/** {@code sourceEntityId} for effects with no source entity (world-fixed explosions, area strikes). */
	public static final int NO_SOURCE = -1;

	public static final StreamCodec<ByteBuf, VfxEventS2CPayload> STREAM_CODEC = StreamCodec.composite(
			ResourceLocation.STREAM_CODEC, VfxEventS2CPayload::effect,
			ByteBufCodecs.VAR_INT, VfxEventS2CPayload::sourceEntityId,
			StreamCodecs.VEC3, VfxEventS2CPayload::origin,
			StreamCodecs.VEC3, VfxEventS2CPayload::target,
			ByteBufCodecs.FLOAT, VfxEventS2CPayload::scale,
			ByteBufCodecs.VAR_INT, VfxEventS2CPayload::seed,
			VfxEventS2CPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
