package io.github.grebeshok105.codex.client.hero.homelander.emf;

/**
 * Blockbench (y-up, head at y≥24) → vanilla model-space (y-down, feet at
 * y=24) conversion shared by the model builder and the expression builder.
 * The transform is the affine map {@code v = (x, 24 − y, z)}: local offsets
 * map as {@code (dx, −dy, dz)}, rotations conjugate into all-negated Euler
 * angles under {@code ModelPart}'s ZYX order, scale is unchanged.
 */
public final class EmfModelConversion {
	/** Vanilla model origin: feet height in pixels (1.5 blocks). */
	public static final float MODEL_HEIGHT = 24f;

	private EmfModelConversion() {
	}

	/** bb-space bone pivot → vanilla-space pivot. */
	public static float[] pivot(float[] bb) {
		return new float[]{bb[0], MODEL_HEIGHT - bb[1], bb[2]};
	}

	/** bb-space parent-relative pivot offset → vanilla-space offset. */
	public static float[] relativePivot(float[] bb, float[] bbParent) {
		return new float[]{
				bb[0] - bbParent[0], bbParent[1] - bb[1], bb[2] - bbParent[2]};
	}

	/**
	 * bb-space cube origin (min corner) → vanilla local-space min corner,
	 * given the vanilla pivot of the owning bone.
	 */
	public static float[] cubeMin(float[] bbOrigin, float[] bbSize, float[] bbPivot) {
		return new float[]{
				bbOrigin[0] - bbPivot[0],
				bbPivot[1] - bbOrigin[1] - bbSize[1],
				bbOrigin[2] - bbPivot[2]};
	}

	/**
	 * bb face name → vanilla face name: the y-flip swaps up/down,
	 * horizontal directions are unchanged.
	 */
	public static String faceName(String bb) {
		return switch (bb) {
			case "up" -> "down";
			case "down" -> "up";
			default -> bb;
		};
	}

	/** bb rotation degrees → vanilla rotation radians (all axes negate). */
	public static float[] rotationRad(float xDeg, float yDeg, float zDeg) {
		return new float[]{
				(float) Math.toRadians(-xDeg),
				(float) Math.toRadians(-yDeg),
				(float) Math.toRadians(-zDeg)};
	}

	/** bb-space position delta → vanilla-space position delta. */
	public static float[] positionDelta(float x, float y, float z) {
		return new float[]{x, -y, z};
	}
}
