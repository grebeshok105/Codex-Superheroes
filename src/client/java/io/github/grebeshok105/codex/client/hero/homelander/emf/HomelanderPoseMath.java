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

	private HomelanderPoseMath() {
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
