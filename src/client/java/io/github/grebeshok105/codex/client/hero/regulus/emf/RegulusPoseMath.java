package io.github.grebeshok105.codex.client.hero.regulus.emf;

/**
 * Pure clip math for Regulus's EMF driver — kept static and side-effect
 * free so JUnit can drive it without a Minecraft instance. Mirrors the
 * Homelander driver shape (hero-to-hero imports are forbidden).
 */
final class RegulusPoseMath {
	static final float TICK_SECONDS = 1f / 20f;

	/** Master authored-presentation weight — exponential half-life, like Homelander. */
	static final float ACTIVE_HALF_LIFE_TICKS = 3f;

	private RegulusPoseMath() {
	}

	/** Frame-rate-independent exponential approach. */
	static float approach(float current, float target, float halfLifeTicks, float dtTicks) {
		float decay = (float) Math.pow(0.5, dtTicks / halfLifeTicks);
		return target + (current - target) * decay;
	}

	/**
	 * Linear blend weight: rises at {@code 1/blendInTicks} while the target
	 * is 1, falls at {@code 1/blendOutTicks} toward 0 — the authored
	 * {@code blend_in}/{@code blend_out} seconds of each clip in ticks.
	 */
	static float blend(float current, float target, float blendInTicks, float blendOutTicks) {
		if (target > 0f) {
			return Math.min(1f, current + 1f / Math.max(0.5f, blendInTicks));
		}
		return Math.max(0f, current - 1f / Math.max(0.5f, blendOutTicks));
	}

	/** Wraps a raw clock into a loop clip's timeline. */
	static float loopTime(float t, float lengthSeconds) {
		float r = t % lengthSeconds;
		return r < 0f ? r + lengthSeconds : r;
	}

	/**
	 * A per-clip wall clock in seconds. A started one-shot advances to its
	 * duration and then holds on the final frame while the blend-out weight
	 * fades; a loop clip's clock advances forever and wraps at read time
	 * via {@link #loopTime}.
	 */
	static final class ClipClock {
		private float time;
		private boolean playing;

		/** A loop clip clock — always advancing. */
		static ClipClock running() {
			ClipClock clock = new ClipClock();
			clock.playing = true;
			return clock;
		}

		/** (Re)starts a one-shot from frame 0. */
		void start() {
			time = 0f;
			playing = true;
		}

		void advance(float dtSeconds, float durationSeconds) {
			if (!playing) {
				return;
			}
			time += dtSeconds;
			if (time >= durationSeconds) {
				time = durationSeconds;
				playing = false;
			}
		}

		boolean playing() {
			return playing;
		}

		boolean finished() {
			return !playing && time > 0f;
		}

		float time() {
			return time;
		}
	}
}
