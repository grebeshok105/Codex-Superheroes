package io.github.grebeshok105.codex.client.hero.homelander.fx;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pins the inverse of the server-side landing scale mapping
 * ({@code scale = 0.30 + intensity * 1.20} in {@code HomelanderHero.onLanded}):
 * the wire scale recovers normalized impact {@code s}, and the landing sound
 * volume/pitch follow {@code 0.4 + 0.8 s} / {@code 1.15 - 0.3 s}.
 */
class LandingSoundMappingTest {
	private static final float EPS = 1e-4f;

	@Test
	void weakestLandingIsQuietAndBright() {
		LandingSoundMapping.Mapped m = LandingSoundMapping.fromScale(0.30f);
		assertEquals(0f, m.intensity(), EPS);
		assertEquals(0.4f, m.volume(), EPS);
		assertEquals(1.15f, m.pitch(), EPS);
	}

	@Test
	void strongestLandingIsLoudAndDeep() {
		LandingSoundMapping.Mapped m = LandingSoundMapping.fromScale(1.50f);
		assertEquals(1f, m.intensity(), EPS);
		assertEquals(1.2f, m.volume(), EPS);
		assertEquals(0.85f, m.pitch(), EPS);
	}

	@Test
	void midScaleInvertsServerMapping() {
		// intensity 0.5 -> scale 0.90 -> back to s = 0.5
		LandingSoundMapping.Mapped m = LandingSoundMapping.fromScale(0.90f);
		assertEquals(0.5f, m.intensity(), EPS);
		assertEquals(0.8f, m.volume(), EPS);
		assertEquals(1.0f, m.pitch(), EPS);
	}

	@Test
	void outOfRangeScaleClamps() {
		assertEquals(0f, LandingSoundMapping.fromScale(0f).intensity(), EPS);
		assertEquals(0.4f, LandingSoundMapping.fromScale(0f).volume(), EPS);
		assertEquals(1f, LandingSoundMapping.fromScale(9.9f).intensity(), EPS);
		assertEquals(1.2f, LandingSoundMapping.fromScale(9.9f).volume(), EPS);
	}
}
