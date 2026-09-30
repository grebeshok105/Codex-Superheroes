package io.github.grebeshok105.codex.client.hero.homelander.flight;

import io.github.grebeshok105.codex.client.hero.homelander.emf.EmfPlaybackState;

/**
 * Procedural lean layered on top of the animated flight clips: forward speed
 * pitches the body head-first, strafe banks it, vertical speed adds a small
 * pitch correction, and backward flight (never BOOST) applies a slight
 * backward lean. Pure math — inputs are speeds already resolved into the
 * player's look frame by the driver.
 *
 * <p>All outputs feed the EMF lean vars:
 * {@code var.lean_pitch}/{@code var.lean_roll} (radians, on {@code root.rx}/
 * {@code root.rz}) and {@code var.lean_y} (model pixels, on {@code root.ty}).
 * The boost clip already poses the superman body, so lean magnitudes damp
 * toward zero as boost weight rises (see {@link Params#boostDamping}).
 */
public final class HomelanderFlightLean {
	public record Params(
			double fwdPitchMaxDeg, double fwdRefSpeedBpt,
			double strafeRollMaxDeg, double strafeRefSpeedBpt,
			double verticalPitchMaxDeg, double verticalRefSpeedBpt,
			double backwardPitchMaxDeg,
			double yOffsetMaxPx, double yOffsetRefSpeedBpt,
			double halfLifeTicks, double boostDamping) {

		public static Params defaults() {
			return new Params(
					35.0, 0.6,   // forward pitch: up to 35° at 0.6 b/t
					20.0, 0.5,   // strafe bank: up to 20°
					8.0, 0.4,    // vertical: ±8°
					10.0,        // backward lean: 10° nose-up at backward ref
					2.0, 0.5,    // vertical sink/rise: ±2px
					3.0, 0.7);   // half-life ticks, boost damping
		}
	}

	public record Targets(double pitchRad, double rollRad, double yPx) {
		public static final Targets ZERO = new Targets(0, 0, 0);
	}

	private final Params params;

	public HomelanderFlightLean(Params params) {
		this.params = params;
	}

	/**
	 * @param forwardSpeed signed speed along look direction (b/t, + forward)
	 * @param strafeSpeed  signed speed toward the player's right (b/t)
	 * @param verticalSpeed signed vertical speed (b/t, + up)
	 * @param boostWeight current boost clip weight ∈ [0,1]
	 */
	public Targets target(double forwardSpeed, double strafeSpeed,
			double verticalSpeed, double boostWeight) {
		double damp = 1.0 - params.boostDamping() * clamp01(boostWeight);
		double pitchDeg;
		if (forwardSpeed >= 0) {
			pitchDeg = params.fwdPitchMaxDeg() * Math.min(1.0, forwardSpeed / params.fwdRefSpeedBpt());
		} else {
			// BACKWARD RULE lean: slight backward pitch, capped small.
			pitchDeg = -params.backwardPitchMaxDeg()
					* Math.min(1.0, -forwardSpeed / params.fwdRefSpeedBpt());
		}
		double rollDeg = params.strafeRollMaxDeg()
				* clampSigned(strafeSpeed / params.strafeRefSpeedBpt());
		double verticalDeg = -params.verticalPitchMaxDeg()
				* clampSigned(verticalSpeed / params.verticalRefSpeedBpt());
		double yPx = params.yOffsetMaxPx()
				* clampSigned(verticalSpeed / params.yOffsetRefSpeedBpt());
		return new Targets(
				Math.toRadians((pitchDeg + verticalDeg) * damp),
				Math.toRadians(rollDeg * damp),
				yPx * damp);
	}

	/** Exponential approach identical to {@code EmfPlaybackState}'s smoothing. */
	public double approach(double current, double target, double ticks) {
		return EmfPlaybackState.smoothApproach(current, target, ticks, params.halfLifeTicks());
	}

	private static double clampSigned(double v) {
		return Math.max(-1.0, Math.min(1.0, v));
	}

	private static double clamp01(double v) {
		return v < 0 ? 0 : Math.min(v, 1.0);
	}
}
