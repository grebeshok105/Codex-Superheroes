package io.github.grebeshok105.codex.client.hero.homelander.fx;

/**
 * Volume envelope of the eye-laser loop sound, as a 0..1 multiplier over the
 * loop's base volume: an ease-out ramp from silence over {@code chargeTicks},
 * 1 while held, then a linear fade to silence over {@code releaseTicks}
 * counted from {@code releasedAt} (the age at which the fade began — a
 * negative value means "still held"). Releasing mid-ramp fades from the level
 * the ramp had reached, not from 1 — the same continuity rule
 * {@link io.github.grebeshok105.codex.client.core.vfx.pattern.PhaseTimeline}
 * applies to the beam's visual intensity.
 *
 * <p>Pure math — JUnit drives this without a Minecraft instance.
 */
public record LaserLoopVolume(int chargeTicks, int releaseTicks) {

	/** Envelope value at loop age {@code age}; {@code releasedAt} < 0 means held. */
	public float at(float age, float releasedAt) {
		if (releasedAt >= 0f && age >= releasedAt) {
			float start = chargeLevel(releasedAt);
			float decay = Math.min(1f, (age - releasedAt) / Math.max(1, releaseTicks));
			return start * Math.max(0f, 1f - decay);
		}
		return chargeLevel(age);
	}

	/**
	 * Charge-ramp age whose level equals {@code level} — the inverse of the
	 * ramp, used to resume a partly faded loop without a volume step.
	 */
	public float chargeAgeAtLevel(float level) {
		if (level <= 0f) {
			return 0f;
		}
		if (level >= 1f) {
			return chargeTicks;
		}
		return (1f - (float) Math.sqrt(1f - level)) * chargeTicks;
	}

	private float chargeLevel(float t) {
		if (t <= 0f) {
			return 0f;
		}
		if (t >= chargeTicks) {
			return 1f;
		}
		float x = t / Math.max(1, chargeTicks);
		return 1f - (1f - x) * (1f - x);
	}
}
