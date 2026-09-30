package io.github.grebeshok105.codex.client.hero.homelander.emf;

/**
 * Pure scalars behind the Homelander EMF variables: the half-life-smoothed
 * master weight and the free-running clip clocks the jem's
 * {@code keyframe(...)} tables index. No Minecraft state — the pose state
 * ticks these per client tick.
 */
final class HomelanderPoseMath {

	private static final float ACTIVE_HALF_LIFE_TICKS = 3f;
	private static final float TAKEOFF_HALF_LIFE_TICKS = 2f;

	/** Authored HOVER clip length; the emitted jem table loops at exactly this boundary. */
	static final float HOVER_LENGTH_SECONDS = 3.2f;

	/** Authored TAKEOFF clip length: a 0.8 s hold whose last frame equals the HOVER start. */
	static final float TAKEOFF_LENGTH_SECONDS = 0.8f;
	/** TAKEOFF weight holds through this point, then eases out. */
	static final float TAKEOFF_HOLD_SECONDS = 0.65f;
	/** Airborne activation starts past the crouch dip (root ty bottoms at 0.20 s). */
	static final float TAKEOFF_AIRBORNE_START_SECONDS = 0.28f;

	/** Fallback {@code emfBoostEnter} (b/t forward) when the vfx params omit it. */
	static final float DEFAULT_BOOST_ENTER = 0.9f;
	/** Fallback {@code emfBoostExit} (b/t forward) when the vfx params omit it. */
	static final float DEFAULT_BOOST_EXIT = 0.6f;
	/** SUPERSONIC flight boosts at any forward speed above this (plan §7 stage 4). */
	static final float SUPERSONIC_BOOST_FORWARD = 0.3f;
	private static final float BOOST_HALF_LIFE_TICKS = 4f;
	private static final float VELOCITY_HALF_LIFE_TICKS = 3f;

	private HomelanderPoseMath() {
	}

	/**
	 * BOOST latch with hysteresis (§7 stage 4): engages at {@code forward >= enter}
	 * — or at {@code forward > 0.3} while SUPERSONIC — and releases only below
	 * {@code exit}. Between the two thresholds the previous state holds, so
	 * jitter inside the band cannot flap the clip. Backward and sideways
	 * speeds project to {@code forward <= 0} and never engage.
	 */
	static boolean boostEngaged(boolean was, float forward, boolean supersonic,
			float enter, float exit) {
		if (forward >= enter || (supersonic && forward > SUPERSONIC_BOOST_FORWARD)) {
			return true;
		}
		if (forward < exit) {
			return false;
		}
		return was;
	}

	/**
	 * Signed speed along the body-yaw forward axis, in blocks/tick. Minecraft
	 * yaw: 0 faces +Z, 90 faces −X — the forward axis is (−sin θ, cos θ).
	 */
	static float forwardComponent(float vx, float vz, float bodyYawDeg) {
		double rad = Math.toRadians(bodyYawDeg);
		return (float) (-Math.sin(rad) * vx + Math.cos(rad) * vz);
	}

	/**
	 * Signed speed along the body-yaw right axis, in blocks/tick — the
	 * forward axis rotated −90°: (−cos θ, −sin θ).
	 */
	static float strafeComponent(float vx, float vz, float bodyYawDeg) {
		double rad = Math.toRadians(bodyYawDeg);
		return (float) (-Math.cos(rad) * vx - Math.sin(rad) * vz);
	}

	/** BOOST weight approach, half-life 4 ticks (§7 stage 4). */
	static float boostWeight(float current, boolean engaged, float dtTicks) {
		return approach(current, engaged ? 1f : 0f, BOOST_HALF_LIFE_TICKS, dtTicks);
	}

	/** Smoothed render velocity component, half-life 3 ticks (§7 stage 4). */
	static float smoothedVelocity(float current, float raw, float dtTicks) {
		return approach(current, raw, VELOCITY_HALF_LIFE_TICKS, dtTicks);
	}

	/** Exponential approach of {@code current} toward the flight target, half-life 3 ticks. */
	static float activeWeight(float current, boolean flying, float dtTicks) {
		return approach(current, flying ? 1f : 0f, ACTIVE_HALF_LIFE_TICKS, dtTicks);
	}

	/**
	 * TAKEOFF one-shot weight: pinned to 1 while the clip is inside the 0.65 s
	 * hold — the plan's literal "weight = 1 for 0 to 0.65 s" — then eases to 0
	 * with a 2-tick half-life, and to 0 immediately once flight stops. The
	 * vanilla→authored blend is already carried by the master weight's ramp, so
	 * ramping this weight too would only mix HOVER into the crouch dip and mask
	 * it. The clip's last frame is the HOVER start height, so the residual tail
	 * is pinned to 0 at clip end — the crossfade is continuous either way.
	 */
	static float takeoffWeight(float current, boolean flying, float clipTimeSeconds, float dtTicks) {
		if (clipTimeSeconds >= TAKEOFF_LENGTH_SECONDS) {
			return 0f;
		}
		if (flying && clipTimeSeconds < TAKEOFF_HOLD_SECONDS) {
			return 1f;
		}
		return approach(current, 0f, TAKEOFF_HALF_LIFE_TICKS, dtTicks);
	}

	/** Frame-rate-independent exponential approach. */
	static float approach(float current, float target, float halfLifeTicks, float dtTicks) {
		float decay = (float) Math.pow(0.5, dtTicks / halfLifeTicks);
		return target + (current - target) * decay;
	}

	/** Loop-local time in {@code [0, length)}; non-positive lengths return {@code t} (one-shots). */
	static float loopTime(float t, float lengthSeconds) {
		if (lengthSeconds <= 0f) {
			return t;
		}
		float wrapped = t % lengthSeconds;
		return wrapped < 0f ? wrapped + lengthSeconds : wrapped;
	}

	/** Free-running clip clock, advanced in seconds. */
	static final class ClipClock {
		private float t;

		float advance(float dtSeconds) {
			t += dtSeconds;
			return t;
		}

		float time() {
			return t;
		}

		void reset() {
			t = 0f;
		}

		void reset(float seconds) {
			t = seconds;
		}

		boolean finished(float lengthSeconds) {
			return t >= lengthSeconds;
		}
	}
}
