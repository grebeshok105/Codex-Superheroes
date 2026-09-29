package io.github.grebeshok105.codex.client.core.vfx.pattern;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Drives {@link PhaseTimeline} phase boundaries and intensity curves. */
class PhaseTimelineTest {
	private static final PhaseTimeline TIMELINE = new PhaseTimeline(6, 8);

	@Test
	void phaseBoundaries() {
		assertEquals(PhaseTimeline.Phase.CHARGE, TIMELINE.phaseAt(0, -1));
		assertEquals(PhaseTimeline.Phase.CHARGE, TIMELINE.phaseAt(5, -1));
		assertEquals(PhaseTimeline.Phase.HOLD, TIMELINE.phaseAt(6, -1));
		assertEquals(PhaseTimeline.Phase.HOLD, TIMELINE.phaseAt(100, -1));
		assertEquals(PhaseTimeline.Phase.RELEASE, TIMELINE.phaseAt(20, 15));
		assertEquals(PhaseTimeline.Phase.DONE, TIMELINE.phaseAt(23, 15));
	}

	@Test
	void intensityEasesInOverChargeAndHoldsAtOne() {
		float midCharge = TIMELINE.intensity(3, -1, 0f);
		assertTrue(midCharge > 0f && midCharge < 1f, "mid-charge intensity in (0,1), was " + midCharge);
		assertEquals(1f, TIMELINE.intensity(6, -1, 0f), 1e-6f);
		assertEquals(1f, TIMELINE.intensity(40, -1, 0.5f), 1e-6f);
	}

	@Test
	void releaseBeforeChargeEndsStartsReleaseFromCurrentIntensity() {
		int releasedAt = 2;
		float chargeValueAtRelease = TIMELINE.intensity(releasedAt, -1, 0f);
		assertEquals(chargeValueAtRelease, TIMELINE.intensity(releasedAt, releasedAt, 0f), 1e-6f,
				"release begins at the charge intensity reached so far");

		float previous = Float.MAX_VALUE;
		for (int age = releasedAt; age <= releasedAt + 8; age++) {
			float value = TIMELINE.intensity(age, releasedAt, 0f);
			assertTrue(value <= previous + 1e-6f, "intensity decreases monotonically after release");
			previous = value;
		}
		assertEquals(0f, TIMELINE.intensity(releasedAt + 8, releasedAt, 0f), 1e-6f);
	}

	@Test
	void doneIntensityIsZero() {
		assertEquals(0f, TIMELINE.intensity(23, 15, 0.5f), 1e-6f);
		assertEquals(0f, TIMELINE.intensity(500, 15, 0f), 1e-6f);
	}
}
