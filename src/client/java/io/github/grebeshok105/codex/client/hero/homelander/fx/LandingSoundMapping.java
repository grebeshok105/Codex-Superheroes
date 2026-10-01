package io.github.grebeshok105.codex.client.hero.homelander.fx;

import net.minecraft.util.Mth;

/**
 * Inverse of the server-side {@code HomelanderHero.onLanded} wire mapping
 * ({@code scale = 0.30 + intensity * 1.20}): {@link #fromScale} recovers the
 * normalized impact strength {@code s} from the {@code LANDING} event scale
 * and derives the landing sound's volume/pitch, so a light touchdown thuds
 * softly and a heavy drop booms.
 */
public final class LandingSoundMapping {
	private LandingSoundMapping() {
	}

	public static Mapped fromScale(float scale) {
		float s = Mth.clamp((scale - 0.30f) / 1.20f, 0f, 1f);
		return new Mapped(s, 0.4f + 0.8f * s, 1.15f - 0.3f * s);
	}

	public record Mapped(float intensity, float volume, float pitch) {
	}
}
