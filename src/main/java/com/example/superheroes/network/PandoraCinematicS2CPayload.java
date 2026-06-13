package com.example.superheroes.network;

import com.example.superheroes.ModId;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Pandora — scripted death cinematic (#6–#10).
 *
 * <p>Sent to EVERY player in Pandora's level so the whole scene freezes together.
 * {@code phase}: {@code 0 = START}, {@code 1 = END}.
 *
 * <ul>
 *   <li>{@code pandoraId} / {@code killerId} — entity ids of the two principals
 *       (killer may be {@code -1} if there is no player killer). Used by the client to
 *       know whether the local player is Pandora (POV is swapped to the killer server-side)
 *       and to drive the black-lightning render around Pandora.</li>
 *   <li>{@code px,py,pz} — Pandora's world position at the moment of the freeze, used as
 *       the anchor for the black silhouette lightning.</li>
 * </ul>
 *
 * <p>The cinematic look (full-black silhouettes + red→white gradient sky) is produced by a
 * dedicated Iris shaderpack the mod swaps to for the duration — never via Veil.
 */
public record PandoraCinematicS2CPayload(int phase, int pandoraId, int killerId,
		double px, double py, double pz) implements CustomPacketPayload {

	public static final int PHASE_START = 0;
	public static final int PHASE_END = 1;

	public static final Type<PandoraCinematicS2CPayload> TYPE = new Type<>(ModId.of("pandora_cinematic"));

	public static final StreamCodec<ByteBuf, PandoraCinematicS2CPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, PandoraCinematicS2CPayload::phase,
			ByteBufCodecs.VAR_INT, PandoraCinematicS2CPayload::pandoraId,
			ByteBufCodecs.VAR_INT, PandoraCinematicS2CPayload::killerId,
			ByteBufCodecs.DOUBLE, PandoraCinematicS2CPayload::px,
			ByteBufCodecs.DOUBLE, PandoraCinematicS2CPayload::py,
			ByteBufCodecs.DOUBLE, PandoraCinematicS2CPayload::pz,
			PandoraCinematicS2CPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
