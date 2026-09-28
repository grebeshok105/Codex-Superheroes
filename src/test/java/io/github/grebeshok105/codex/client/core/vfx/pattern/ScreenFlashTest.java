package io.github.grebeshok105.codex.client.core.vfx.pattern;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Drives {@link ScreenFlash#attenuation} — pure distance/LOS falloff math. */
class ScreenFlashTest {
	@Test
	void attenuationIsOneAtZeroDistance() {
		assertEquals(1f, ScreenFlash.attenuation(0, 64, true), 1e-6f);
	}

	@Test
	void attenuationReachesZeroAtRadius() {
		assertEquals(0f, ScreenFlash.attenuation(64, 64, true), 1e-6f);
		assertEquals(0f, ScreenFlash.attenuation(200, 64, true), 1e-6f);
	}

	@Test
	void attenuationReducedWithoutLineOfSight() {
		assertEquals(0.175f, ScreenFlash.attenuation(32, 64, false), 1e-4f);
	}
}
