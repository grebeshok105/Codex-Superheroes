package io.github.grebeshok105.codex.client.hero.homelander.emf;

/**
 * Pure scalars behind the Homelander EMF variables: the half-life-smoothed
 * master weight and the free-running clip clocks the jem's
 * {@code keyframe(...)} tables index. No Minecraft state — the pose state
 * ticks these per client tick.
 */
final class HomelanderPoseMath {

	private static final float ACTIVE_HALF_LIFE_TICKS = 3f;

	private HomelanderPoseMath() {
	}

	/** Exponential approach of {@code current} toward the authored-presentation target, half-life 3 ticks. */
	static float activeWeight(float current, boolean presenting, float dtTicks) {
		return approach(current, presenting ? 1f : 0f, ACTIVE_HALF_LIFE_TICKS, dtTicks);
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

		boolean finished(float lengthSeconds) {
			return t >= lengthSeconds;
		}
	}
}
