package io.github.grebeshok105.codex.core.net;

import io.github.grebeshok105.codex.core.net.PhotonFxS2CPayload.RotationMode;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Senders for the Photon FX payload. All playback happens client-side via Photon's
 * executor cache; the server only chooses the anchor (entity-follow vs world pos) and
 * the audience. Mirrors {@link VfxFx}: a player anchor notifies tracking + self, any
 * other entity anchor notifies its trackers; world-anchored effects go to the radial
 * audience around the spawn point.
 */
public final class PhotonFx {
	/** Default radial audience for world-anchored effects (blocks). */
	public static final double DEFAULT_BLOCK_AUDIENCE = 96.0;

	private PhotonFx() {
	}

	/** FX that follows {@code anchor}'s eye position, rotated with its look when {@code rotation} says so. */
	public static void follow(Entity anchor, ResourceLocation fx, Vec3 offset, RotationMode rotation,
			float scale, int delay, boolean allowMulti) {
		PhotonFxS2CPayload payload = new PhotonFxS2CPayload(fx, anchor.getId(), Vec3.ZERO, offset,
				rotation.ordinal(), scale, delay, allowMulti);
		if (anchor instanceof ServerPlayer player) {
			FxBroadcast.trackingAndSelf(player, payload);
		} else {
			FxBroadcast.tracking(anchor, payload);
		}
	}

	public static void follow(Entity anchor, ResourceLocation fx) {
		follow(anchor, fx, Vec3.ZERO, RotationMode.NONE, 1f, 0, true);
	}

	/** FX pinned to a world position; everyone near enough to see it gets the trigger. */
	public static void at(ServerLevel level, Vec3 pos, ResourceLocation fx, Vec3 offset,
			float scale, int delay, boolean allowMulti) {
		FxBroadcast.around(level, pos, DEFAULT_BLOCK_AUDIENCE, new PhotonFxS2CPayload(fx,
				PhotonFxS2CPayload.NO_ANCHOR, pos, offset, RotationMode.NONE.ordinal(), scale, delay, allowMulti));
	}

	public static void at(ServerLevel level, Vec3 pos, ResourceLocation fx) {
		at(level, pos, fx, Vec3.ZERO, 1f, 0, true);
	}
}
