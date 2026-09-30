package io.github.grebeshok105.codex.client.hero.homelander.flight;

import io.github.grebeshok105.codex.client.core.flight.FlightBodyTransform;

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

	/** Maps onto the generic anchors tilt type used by {@code HumanoidAnchors}. */
	public FlightBodyTransform tilt() {
		return new FlightBodyTransform((float) pitchDeg, (float) rollDeg);
	}
}
