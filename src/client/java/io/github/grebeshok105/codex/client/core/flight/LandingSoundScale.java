package io.github.grebeshok105.codex.client.core.flight;

import io.github.grebeshok105.codex.client.core.vfx.params.VfxParams;
import net.minecraft.util.Mth;

/**
 * The impact curve the flight-landing sound rides. {@code factor} is the
 * normalized impact (0 = gentle touchdown, 1 = max-speed drop): volume rises
 * {@code volumeMin} → {@code volumeMax} while pitch drops {@code pitchHigh}
 * → {@code pitchLow}, so a hop never sounds catastrophic and a crater dive
 * lands with sub-bass weight. Tuning keys: {@code landVolumeMin},
 * {@code landVolumeMax}, {@code landPitchHigh}, {@code landPitchLow}.
 */
public record LandingSoundScale(float volumeMin, float volumeMax,
		float pitchHigh, float pitchLow) {

	public float volume(float factor) {
		float f = Mth.clamp(factor, 0f, 1f);
		return volumeMin + f * (volumeMax - volumeMin);
	}

	public float pitch(float factor) {
		float f = Mth.clamp(factor, 0f, 1f);
		return pitchHigh + f * (pitchLow - pitchHigh);
	}

	/** Speed normalized to 0..1 against {@code refSpeed} blocks/tick. */
	public static double speedFactor(double speed, double refSpeed) {
		return Mth.clamp(speed / Math.max(0.01, refSpeed), 0.0, 1.0);
	}

	public static LandingSoundScale of(VfxParams params) {
		return new LandingSoundScale(
				params.number("landVolumeMin", 0.45f),
				params.number("landVolumeMax", 2.0f),
				params.number("landPitchHigh", 1.02f),
				params.number("landPitchLow", 0.62f));
	}
}
