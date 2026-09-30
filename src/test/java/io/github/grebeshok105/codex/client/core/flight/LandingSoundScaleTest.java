package io.github.grebeshok105.codex.client.core.flight;

import io.github.grebeshok105.codex.client.core.vfx.params.VfxParams;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link LandingSoundScale} — the volume/pitch curve the landing sound rides:
 * a gentle touchdown stays quiet and bright, a max-speed drop is loud and low.
 */
final class LandingSoundScaleTest {
	private final LandingSoundScale scale = new LandingSoundScale(0.45f, 2.0f, 1.02f, 0.62f);

	@Test
	void zeroImpactStaysQuietAndBright() {
		assertEquals(0.45f, scale.volume(0f), 1e-6);
		assertEquals(1.02f, scale.pitch(0f), 1e-6);
	}

	@Test
	void fullImpactIsLoudAndLow() {
		assertEquals(2.0f, scale.volume(1f), 1e-6);
		assertEquals(0.62f, scale.pitch(1f), 1e-6);
	}

	@Test
	void midImpactInterpolates() {
		assertEquals(1.225f, scale.volume(0.5f), 1e-6);
		assertEquals(0.82f, scale.pitch(0.5f), 1e-6);
	}

	@Test
	void factorClampsOutsideUnitRange() {
		assertEquals(scale.volume(1f), scale.volume(5f), 1e-6);
		assertEquals(scale.volume(0f), scale.volume(-1f), 1e-6);
		assertEquals(scale.pitch(1f), scale.pitch(9f), 1e-6);
	}

	@Test
	void speedFactorNormalizesAgainstReference() {
		assertEquals(0f, LandingSoundScale.speedFactor(0.0, 1.2), 1e-6);
		assertEquals(0.5, LandingSoundScale.speedFactor(0.6, 1.2), 1e-6);
		assertEquals(1.0, LandingSoundScale.speedFactor(9.9, 1.2), 1e-6);
	}

	@Test
	void ofReadsTuningKeys() {
		VfxParams p = new VfxParams(Map.of(
				"landVolumeMin", 0.1f, "landVolumeMax", 3f,
				"landPitchHigh", 1.1f, "landPitchLow", 0.5f), Map.of());

		LandingSoundScale tuned = LandingSoundScale.of(p);
		assertEquals(0.1f, tuned.volume(0f), 1e-6);
		assertEquals(3f, tuned.volume(1f), 1e-6);
		assertEquals(1.1f, tuned.pitch(0f), 1e-6);
		assertEquals(0.5f, tuned.pitch(1f), 1e-6);
	}
}
