package io.github.grebeshok105.codex.client.core.vfx.pattern;

/**
 * Charge → hold → release timing shared by beam/channel effects.
 * {@code chargeTicks} is the wind-up length; {@code releaseTicks} the
 * fade-out length once the effect is released. While the effect is held,
 * callers pass {@code releasedAtAge = -1}; once released they pin the age at
 * which the release started.
 *
 * <p>Pure math — JUnit drives this without a Minecraft instance.
 */
public record PhaseTimeline(int chargeTicks, int releaseTicks) {
	public enum Phase {
		CHARGE, HOLD, RELEASE, DONE
	}

	public Phase phaseAt(int age, int releasedAtAge) {
		if (releasedAtAge >= 0 && age >= releasedAtAge) {
			return age - releasedAtAge >= releaseTicks ? Phase.DONE : Phase.RELEASE;
		}
		return age < chargeTicks ? Phase.CHARGE : Phase.HOLD;
	}

	/**
	 * 0→1 ease-out over the charge, 1 while held, then a linear fade to 0
	 * over the release starting at whatever intensity the charge had reached
	 * (a release mid-charge fades from that partial value, not from 1).
	 */
	public float intensity(int age, int releasedAtAge, float partial) {
		float t = age + partial;
		if (releasedAtAge >= 0 && t >= releasedAtAge) {
			float start = chargeLevel(releasedAtAge);
			float decay = Math.min(1f, (t - releasedAtAge) / Math.max(1, releaseTicks));
			return start * Math.max(0f, 1f - decay);
		}
		return chargeLevel(t);
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
