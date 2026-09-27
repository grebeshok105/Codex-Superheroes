package io.github.grebeshok105.codex.core.net;

import io.github.grebeshok105.codex.ModId;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Unified beam-FX payload: one wire shape for every style of transient beam
 * (Homelander eye lasers, Iron Man repulsor blasts, Thanos' cosmic beam).
 * The style id selects the client-side draw strategy registered in
 * {@code client/core/render/BeamStyles}; audience rules live in {@link BeamFx}.
 */
public record BeamFxS2CPayload(ResourceLocation style, UUID shooter, Vec3 start, Vec3 end)
		implements CustomPacketPayload {
	public static final Type<BeamFxS2CPayload> TYPE = new Type<>(ModId.of("beam_fx"));

	/** Style ids carried on the wire; draw parameters live client-side per style. */
	public static final ResourceLocation STYLE_LASER = ModId.of("laser");
	public static final ResourceLocation STYLE_REPULSOR = ModId.of("repulsor");
	public static final ResourceLocation STYLE_COSMIC_BEAM = ModId.of("cosmic_beam");

	public static final StreamCodec<ByteBuf, BeamFxS2CPayload> STREAM_CODEC = StreamCodec.composite(
			ResourceLocation.STREAM_CODEC, BeamFxS2CPayload::style,
			UUIDUtil.STREAM_CODEC, BeamFxS2CPayload::shooter,
			StreamCodecs.VEC3, BeamFxS2CPayload::start,
			StreamCodecs.VEC3, BeamFxS2CPayload::end,
			BeamFxS2CPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
