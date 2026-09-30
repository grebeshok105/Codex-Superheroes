package io.github.grebeshok105.codex.client.hero.homelander.fx;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives {@link LaserLoopVolume} — the pure volume envelope of the eye-laser
 * loop: silent at activation, ramped to full over the charge window, held,
 * then a linear fade to silence once released.
 */
class LaserLoopVolumeTest {
	private static final int CHARGE_TICKS = 6;
	private static final int RELEASE_TICKS = 8;
	private static final LaserLoopVolume VOLUME = new LaserLoopVolume(CHARGE_TICKS, RELEASE_TICKS);

	@Test
	void silentAtStartAndFullByChargeEnd() {
		assertEquals(0f, VOLUME.at(0, -1), 1e-6f);
		float previous = 0f;
		for (int age = 0; age <= CHARGE_TICKS; age++) {
			float value = VOLUME.at(age, -1);
			assertTrue(value >= previous - 1e-6f, "ramp is non-decreasing, age " + age);
			previous = value;
		}
		assertEquals(1f, VOLUME.at(CHARGE_TICKS, -1), 1e-6f);
	}

	@Test
	void holdsAtFullWhileHeld() {
		assertEquals(1f, VOLUME.at(CHARGE_TICKS, -1), 1e-6f);
		assertEquals(1f, VOLUME.at(100, -1), 1e-6f);
		assertEquals(1f, VOLUME.at(1000, -1), 1e-6f);
	}

	@Test
	void fadesLinearlyToSilenceAfterRelease() {
		int releasedAt = 20;
		assertEquals(1f, VOLUME.at(releasedAt, releasedAt), 1e-6f,
				"release begins at the held level");

		float previous = Float.MAX_VALUE;
		for (int age = releasedAt; age <= releasedAt + RELEASE_TICKS; age++) {
			float value = VOLUME.at(age, releasedAt);
			assertTrue(value <= previous + 1e-6f, "volume decreases monotonically after release");
			previous = value;
		}
		assertEquals(0f, VOLUME.at(releasedAt + RELEASE_TICKS, releasedAt), 1e-6f,
				"the loop reaches silence exactly at the fade horizon");
		assertEquals(0f, VOLUME.at(releasedAt + 500, releasedAt), 1e-6f,
				"and stays silent past it");
	}

	@Test
	void releaseMidChargeStartsFadeFromPartialLevel() {
		float releasedAt = 2f;
		float chargeLevelAtRelease = VOLUME.at(releasedAt, -1);
		assertTrue(chargeLevelAtRelease > 0f && chargeLevelAtRelease < 1f,
				"precondition: mid-charge level in (0,1), was " + chargeLevelAtRelease);
		assertEquals(chargeLevelAtRelease, VOLUME.at(releasedAt, releasedAt), 1e-6f,
				"fade starts at the level the ramp had reached — no volume step on release");
		assertEquals(0f, VOLUME.at(releasedAt + RELEASE_TICKS, releasedAt), 1e-6f);
	}

	@Test
	void chargeAgeAtLevelInvertsTheRamp() {
		assertEquals(0f, VOLUME.chargeAgeAtLevel(0f), 1e-6f);
		assertEquals(CHARGE_TICKS, VOLUME.chargeAgeAtLevel(1f), 1e-6f);
		for (float level = 0f; level <= 1f; level += 0.1f) {
			float age = VOLUME.chargeAgeAtLevel(level);
			assertEquals(level, VOLUME.at(age, -1), 1e-4f,
					"at(chargeAgeAtLevel(level)) reproduces level " + level);
		}
	}
}
