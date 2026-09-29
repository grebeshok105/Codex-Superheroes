package io.github.grebeshok105.codex.client.core.flight;

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
}
