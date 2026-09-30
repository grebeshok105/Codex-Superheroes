package io.github.grebeshok105.codex.client.core.flight;

import net.minecraft.world.phys.Vec3;

/**
 * Whole-body render tilt applied to a flying player: {@code pitchDeg} leans
 * the body forward around its lateral axis (positive = head-first toward
 * travel), {@code rollDeg} banks around the forward axis. Filled by the
 * flight pose tracker; consumed by the player renderer mixin and the
 * {@code client/core/vfx/anchor} eye anchors so beams and trails leave the
 * rendered pose rather than the untilted one.
 */
public record FlightBodyTransform(float pitchDeg, float rollDeg) {
	public static final FlightBodyTransform IDENTITY = new FlightBodyTransform(0f, 0f);

	/**
	 * Rotates a feet-relative body {@code offset} by this tilt — pitch about
	 * {@code bodyRight} first, then roll about {@code bodyForward} — matching
	 * the transform the player renderer applies. Positive pitch leans
	 * head-first toward {@code bodyForward}; positive roll banks toward the
	 * player's right.
	 */
	public Vec3 applyTo(Vec3 offset, Vec3 bodyRight, Vec3 bodyForward) {
		if (pitchDeg != 0f) {
			offset = rotate(offset, bodyRight, -pitchDeg);
		}
		if (rollDeg != 0f) {
			offset = rotate(offset, bodyForward, rollDeg);
		}
		return offset;
	}

	/** Rodrigues rotation of {@code v} around a unit {@code axis} by {@code deg} degrees. */
	public static Vec3 rotate(Vec3 v, Vec3 axis, float deg) {
		double rad = Math.toRadians(deg);
		double cos = Math.cos(rad);
		double sin = Math.sin(rad);
		double along = axis.dot(v) * (1.0 - cos);
		return v.scale(cos).add(axis.cross(v).scale(sin)).add(axis.scale(along));
	}
}
