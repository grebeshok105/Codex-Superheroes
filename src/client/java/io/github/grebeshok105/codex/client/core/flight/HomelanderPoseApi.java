package io.github.grebeshok105.codex.client.core.flight;

import io.github.grebeshok105.codex.client.ClientFlightState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Presentation seam for flight-body transforms: exposes the whole-body tilt
 * {@link FlightPoseTracker} drives plus the camera pivot offset that keeps a
 * tilted player centered in third person. {@code null} while no flight
 * presentation owns the player, so readers need no hero knowledge.
 */
public final class HomelanderPoseApi {
	private static final Vec3 UP = new Vec3(0, 1, 0);
	/** Chest height in blocks the camera centers on (mid torso). */
	static final double CHEST_Y = 0.9;

	private HomelanderPoseApi() {
	}

	/** Whole-body rotation plus the body-center offset, in render px (1/16 block). */
	public record BodyTransform(float pitchDeg, float rollDeg, Vec3 centerOffsetPx) {
	}

	/**
	 * Transform for {@code playerId} at {@code partialTick}, or {@code null}
	 * when no flight presentation is active — including first-person-only
	 * states — so callers apply nothing.
	 */
	public static @Nullable BodyTransform currentBodyTransform(int playerId, float partialTick) {
		if (!ClientFlightState.isPresentationOwned(playerId)) {
			return null;
		}
		Minecraft client = Minecraft.getInstance();
		if (client.level == null) {
			return null;
		}
		Entity entity = null;
		for (Entity candidate : client.level.entitiesForRendering()) {
			if (candidate.getId() == playerId) {
				entity = candidate;
				break;
			}
		}
		if (!(entity instanceof AbstractClientPlayer player)) {
			return null;
		}
		FlightBodyTransform tilt = FlightPoseTracker.transform(playerId, partialTick);
		float bodyYaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
		return new BodyTransform(tilt.pitchDeg(), tilt.rollDeg(),
				centerOffsetBlocks(tilt, bodyYaw).scale(16.0));
	}

	/**
	 * Where the feet-pivot tilt moved the chest anchor relative to upright —
	 * zero at identity, forward+down at full pitch. Pure math for tests.
	 */
	static Vec3 centerOffsetBlocks(FlightBodyTransform tilt, float bodyYawDeg) {
		// Yaw-only forward is always horizontal, so forward × up never degenerates.
		Vec3 bodyForward = Vec3.directionFromRotation(0f, bodyYawDeg);
		Vec3 bodyRight = bodyForward.cross(UP).normalize();
		return tilt.applyTo(new Vec3(0, CHEST_Y, 0), bodyRight, bodyForward)
				.subtract(0, CHEST_Y, 0);
	}
}
