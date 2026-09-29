package io.github.grebeshok105.codex.client.core.vfx.backend;

/**
 * Shared fade-out curve for {@link VfxBackend#flash}: a feed stores the
 * intensity the caller asked for, and {@link #shown} decays it to zero over
 * {@code fadeTicks}. A feed replaces the stored level only when it is at
 * least what the decay is currently showing — so a caller re-feeding a
 * decaying value (see {@code ScreenFlash}) tracks its own fade curve while a
 * weaker stray call can neither dim nor extend a stronger flash.
 *
 * <p>Pure math — JUnit drives this without a Minecraft instance.
 */
public final class FlashEnvelope {
	private final float fadeTicks;
	private float level;
	private int argb;
	private float ticksLeft;

	public FlashEnvelope(float fadeTicks) {
		this.fadeTicks = Math.max(1f, fadeTicks);
	}

	/** Feeds a flash; ignored when weaker than the level the decay still shows. */
	public void feed(float intensity, int rgb) {
		if (intensity <= 0f) {
			return;
		}
		if (intensity >= shown()) {
			level = Math.min(intensity, 1f);
			argb = rgb & 0xFFFFFF;
			ticksLeft = fadeTicks;
		}
	}

	/** Advances the fade by {@code ticks}. */
	public void tick(float ticks) {
		if (ticksLeft > 0f) {
			ticksLeft = Math.max(0f, ticksLeft - ticks);
		}
	}

	/** The intensity to display right now: {@code level} decayed by elapsed time. */
	public float shown() {
		return ticksLeft > 0f ? level * (ticksLeft / fadeTicks) : 0f;
	}

	/** The color of the last accepted feed (RGB, no alpha). */
	public int argb() {
		return argb;
	}

	public boolean active() {
		return ticksLeft > 0f && level > 0f;
	}

	public void reset() {
		level = 0f;
		ticksLeft = 0f;
	}
}
