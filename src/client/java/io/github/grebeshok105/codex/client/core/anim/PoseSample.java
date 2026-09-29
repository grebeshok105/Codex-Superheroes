package io.github.grebeshok105.codex.client.core.anim;

import org.joml.Vector3f;

import java.util.Map;

/**
 * One frame of layered animation output: per-bone rotation deltas in degrees and
 * position deltas in model pixels, plus the fade {@code weight} the values are scaled
 * by on application ({@link PlayerPoseApplier}). Rotation entries follow the Bedrock
 * bone names ({@code head}, {@code body}, {@code right_arm}, {@code left_arm},
 * {@code right_leg}, {@code left_leg}).
 */
public record PoseSample(Map<String, Vector3f> rotationDeg, Map<String, Vector3f> offsetPx,
		float weight) {
	public static final PoseSample EMPTY = new PoseSample(Map.of(), Map.of(), 0f);

	public PoseSample {
		rotationDeg = Map.copyOf(rotationDeg);
		offsetPx = Map.copyOf(offsetPx);
	}

	public boolean isEmpty() {
		return rotationDeg.isEmpty() && offsetPx.isEmpty();
	}
}
