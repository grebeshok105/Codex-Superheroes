package io.github.grebeshok105.codex.core.net;

import io.github.grebeshok105.codex.ModId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * One-shot Photon FX trigger. The client resolves {@code fx} through
 * {@code FXHelper.getFX} ({@code assets/<ns>/fx/<id>.fx}) and spawns an executor:
 * anchored to {@code anchorEntityId} when {@link #NO_ANCHOR} is not set (follows the
 * entity's eye position), otherwise world-anchored at {@code pos}. {@code rotation}
 * indexes {@link RotationMode} (entity anchors only; world anchors ignore it).
 */
public record PhotonFxS2CPayload(ResourceLocation fx, int anchorEntityId, Vec3 pos, Vec3 offset,
		int rotation, float scale, int delay, boolean allowMulti) implements CustomPacketPayload {
	public static final Type<PhotonFxS2CPayload> TYPE = new Type<>(ModId.of("photon_fx"));

	public static final int NO_ANCHOR = -1;

	public enum RotationMode {
		NONE, FORWARD, LOOK, XROT
	}

	public static final StreamCodec<RegistryFriendlyByteBuf, PhotonFxS2CPayload> STREAM_CODEC = StreamCodec.of(
			(buf, p) -> {
				ResourceLocation.STREAM_CODEC.encode(buf, p.fx);
				buf.writeVarInt(p.anchorEntityId);
				StreamCodecs.VEC3.encode(buf, p.pos);
				StreamCodecs.VEC3.encode(buf, p.offset);
				buf.writeVarInt(p.rotation);
				buf.writeFloat(p.scale);
				buf.writeVarInt(p.delay);
				buf.writeBoolean(p.allowMulti);
			},
			buf -> new PhotonFxS2CPayload(ResourceLocation.STREAM_CODEC.decode(buf), buf.readVarInt(),
					StreamCodecs.VEC3.decode(buf), StreamCodecs.VEC3.decode(buf),
					buf.readVarInt(), buf.readFloat(), buf.readVarInt(), buf.readBoolean())
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
