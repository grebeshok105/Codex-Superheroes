package io.github.grebeshok105.codex.client.core.flight;

import io.github.grebeshok105.codex.client.core.vfx.params.VfxParams;
import io.github.grebeshok105.codex.mechanic.flight.FlightPhase;
import net.minecraft.world.phys.Vec3;

/**
 * Pure flight-pose math: maps the synced {@link FlightPhase} + per-tick
 * velocity to a target {@link FlightBodyTransform} and approaches it
 * exponentially so phase flips never snap the rendered body.
 *
 * <p>Tuning keys (from {@code vfx/flight/pose.json}):
 * {@code hoverMaxPitch}/{@code hoverRefSpeed},
 * {@code cruiseMinPitch}/{@code cruiseMaxPitch}/{@code cruiseRefSpeed},
 * {@code boostPitch}, {@code rollFactor},
 * {@code rollMax}, {@code halfLifeTicks}. A presentation that carries a
 * {@code poseParams} overlay (e.g. EMF-owned Homelander) may additionally
 * supply {@code emfBoostRootPitch} — the authored BOOST root rotation —
 * which then replaces {@code boostPitch} as the BOOST pitch target, plus
 * the {@link #directional} keys {@code hoverForwardPitch},
 * {@code hoverBackwardPitch}, {@code verticalRef}/{@code verticalPitch}
 * and {@code strafeRef}/{@code strafeRoll}.
 */
public final class FlightPoseMath {
	/** Default {@link #step} half-life in ticks (also the pose.json default). */
	public static final float DEFAULT_HALF_LIFE_TICKS = 3f;

	/**
	 * Upright cone half-angle for {@link #directional}: 90° − 4° margin.
	 * Past this the body reads belly-up — the inversion PR #138 removed.
	 */
	private static final float UPRIGHT_PITCH_LIMIT = 86f;

	private FlightPoseMath() {
	}

	/**
	 * Pose target for the phase: HOVER leans 0–{@code hoverMaxPitch} by
	 * horizontal speed, CRUISE up to {@code cruiseMaxPitch}, BOOST a fixed
	 * {@code boostPitch}, IDLE/TAKEOFF/LANDING zero. Pitch stays positive in
	 * every phase — a head-first lean must never flip the body belly-up while
	 * descending; roll banks {@code -yawRate × rollFactor}, clamped to
	 * ±{@code rollMax}.
	 */
	public static FlightBodyTransform target(FlightPhase phase, Vec3 velocity,
			float yawRateDegPerTick, VfxParams p) {
		float horizontalSpeed = (float) Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
		float pitch = switch (phase) {
			case HOVER -> p.number("hoverMaxPitch", 10f)
					* clamp01(horizontalSpeed / Math.max(1e-3f, p.number("hoverRefSpeed", 0.3f)));
			case CRUISE -> {
				float lo = p.number("cruiseMinPitch", 15f);
				float hi = p.number("cruiseMaxPitch", 55f);
				yield lo + (hi - lo)
						* clamp01(horizontalSpeed / Math.max(1e-3f, p.number("cruiseRefSpeed", 0.8f)));
			}
			case BOOST -> p.number("emfBoostRootPitch", p.number("boostPitch", 80f));
			default -> 0f;
		};
		float rollMax = p.number("rollMax", 25f);
		float roll = clamp(-yawRateDegPerTick * p.number("rollFactor", 2.5f), -rollMax, rollMax);
		return new FlightBodyTransform(pitch, roll);
	}

	/**
	 * Directional pose target for EMF-owned players (plan §7 stage 5) —
	 * continuous in signed speed, no phase switch:
	 *
	 * <ul><li>pitch follows {@code forward} — up to {@code hoverForwardPitch}
	 * ahead and down to −{@code hoverBackwardPitch} behind — then lerps to
	 * the authored {@code emfBoostRootPitch} as {@code boostWeight} → 1.</li>
	 * <li>while {@code boostWeight < 0.5} a bounded vertical-speed term leans
	 * the nose into climbs/descents; the sum is clamped to the upright cone
	 * so a dive can never flip the body belly-up (the PR #138 rule).</li>
	 * <li>roll banks from strafe plus yaw rate, clamped to ±{@code rollMax}.</li></ul>
	 *
	 * <p>Smoothing stays the caller's job via {@link #step}; inputs come from
	 * the presentation's already-smoothed render velocity through
	 * {@link DirectionalPoseSource}, never a second smoothing path.
	 */
	public static FlightBodyTransform directional(float forward, float strafe,
			float vertical, float yawRateDegPerTick, float boostWeight, VfxParams p) {
		float hoverRef = Math.max(1e-3f, p.number("hoverRefSpeed", 0.3f));
		float hoverPitch = clamp(forward / hoverRef, -1f, 1f)
				* (forward >= 0f
						? p.number("hoverForwardPitch", 12f)
						: p.number("hoverBackwardPitch", 8f));
		float w = clamp01(boostWeight);
		float pitch = hoverPitch
				+ (p.number("emfBoostRootPitch", p.number("boostPitch", 80f)) - hoverPitch) * w;
		if (w < 0.5f) {
			float verticalRef = Math.max(1e-3f, p.number("verticalRef", 0.6f));
			pitch = clamp(pitch + clamp(-vertical / verticalRef, -1f, 1f)
					* p.number("verticalPitch", 6f),
					-UPRIGHT_PITCH_LIMIT, UPRIGHT_PITCH_LIMIT);
		}
		float rollMax = p.number("rollMax", 25f);
		float strafeRef = Math.max(1e-3f, p.number("strafeRef", 0.5f));
		float roll = clamp(
				-strafe / strafeRef * p.number("strafeRoll", 14f)
						- yawRateDegPerTick * p.number("rollFactor", 2.5f),
				-rollMax, rollMax);
		return new FlightBodyTransform(pitch, roll);
	}

	/**
	 * Exponential approach: {@code current + (target − current)·(1 − 2^(−dt/halfLife))}.
	 * Always stays between current and target — continuous for any dt, never
	 * overshoots; {@code halfLifeTicks} ≤ 0 snaps to the target.
	 */
	public static FlightBodyTransform step(FlightBodyTransform current, FlightBodyTransform target,
			float dtTicks, float halfLifeTicks) {
		float f;
		if (halfLifeTicks <= 0f) {
			f = 1f;
		} else {
			f = 1f - (float) Math.pow(2.0, -Math.max(0f, dtTicks) / halfLifeTicks);
		}
		return new FlightBodyTransform(
				current.pitchDeg() + (target.pitchDeg() - current.pitchDeg()) * f,
				current.rollDeg() + (target.rollDeg() - current.rollDeg()) * f);
	}

	private static float clamp(float v, float lo, float hi) {
		return v < lo ? lo : Math.min(v, hi);
	}

	private static float clamp01(float v) {
		return clamp(v, 0f, 1f);
	}
}
