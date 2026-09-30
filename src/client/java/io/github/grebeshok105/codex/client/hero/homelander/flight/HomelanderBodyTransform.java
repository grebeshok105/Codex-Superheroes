package io.github.grebeshok105.codex.client.hero.homelander.flight;

import io.github.grebeshok105.codex.client.core.flight.FlightBodyTransform;
import net.minecraft.world.phys.Vec3;

/**
 * The current whole-body transform the EMF model presents with:
 * {@code pitchDeg}/{@code rollDeg} are the lean vars in degrees (positive
 * pitch = head-first toward travel, positive roll = bank right) and
 * {@code yOffsetPx} is the model-pixel vertical offset applied to the root
 * bone. This is the contract Agent D's F5 camera work consumes via
 * {@link HomelanderPoseApi#currentBodyTransform}.
 */
public record HomelanderBodyTransform(double pitchDeg, double rollDeg, double yOffsetPx) {
	public static final HomelanderBodyTransform IDENTITY = new HomelanderBodyTransform(0, 0, 0);

	/** Chest height in blocks the camera centers on (mid torso). */
	static final double CHEST_Y = 0.9;

	private static final Vec3 UP = new Vec3(0, 1, 0);

	/** Maps onto the generic anchors tilt type used by {@code HumanoidAnchors}. */
	public FlightBodyTransform tilt() {
		return new FlightBodyTransform((float) pitchDeg, (float) rollDeg);
	}

	/**
	 * Where the feet-pivot tilt moved the chest anchor relative to upright,
	 * in blocks — zero at identity, forward+down at full pitch. Includes the
	 * {@code yOffsetPx} root offset (px → blocks). Pure math for tests.
	 */
	public Vec3 cameraCenterOffsetBlocks(float bodyYawDeg) {
		// Yaw-only forward is always horizontal, so forward × up never degenerates.
		Vec3 bodyForward = Vec3.directionFromRotation(0f, bodyYawDeg);
		Vec3 bodyRight = bodyForward.cross(UP).normalize();
		Vec3 displaced = tilt().applyTo(new Vec3(0, CHEST_Y, 0), bodyRight, bodyForward);
		return displaced.subtract(0, CHEST_Y, 0).add(0, yOffsetPx / 16.0, 0);
	}
}
