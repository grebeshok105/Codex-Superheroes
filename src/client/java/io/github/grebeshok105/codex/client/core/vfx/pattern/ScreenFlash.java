package io.github.grebeshok105.codex.client.core.vfx.pattern;

import io.github.grebeshok105.codex.client.ClientSessionState;
import io.github.grebeshok105.codex.client.core.vfx.backend.VfxBackends;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/**
 * Full-screen flash helper: callers attenuate a base intensity by distance
 * and line of sight, then {@link #trigger}. The flash keeps decaying state
 * and re-feeds the backend every client tick over {@code fadeTicks} (both
 * backends decay a single shot in ~6 ticks, so re-feeding is how a longer
 * fade is produced). Tick/reset self-register on class load — the class only
 * loads once a flash is first requested, so no bootstrap wiring is needed.
 */
public final class ScreenFlash {
	/** Attenuation multiplier applied when the source has no line of sight to the camera. */
	private static final float NO_LOS_FACTOR = 0.35f;

	private static float intensity;
	private static int argb;
	private static int ticksLeft;
	private static int fadeTicks = 1;

	static {
		ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
		ClientSessionState.register(ScreenFlash::reset);
	}

	private ScreenFlash() {
	}

	/**
	 * Linear falloff: 1 at zero distance, 0 at {@code radius} and beyond,
	 * multiplied by {@link #NO_LOS_FACTOR} without line of sight.
	 */
	public static float attenuation(double distance, double radius, boolean lineOfSight) {
		if (radius <= 0.0) {
			return 0f;
		}
		float base = (float) Math.max(0.0, 1.0 - distance / radius);
		return lineOfSight ? base : base * NO_LOS_FACTOR;
	}

	/** Fires a flash; a stronger or equal trigger while one is live replaces it. */
	public static void trigger(float newIntensity, int newArgb, int newFadeTicks) {
		if (newIntensity <= 0f || newFadeTicks <= 0) {
			return;
		}
		if (newIntensity >= intensity || ticksLeft <= 0) {
			intensity = Math.min(newIntensity, 1f);
			argb = newArgb;
			fadeTicks = Math.max(1, newFadeTicks);
			ticksLeft = fadeTicks;
		} else {
			ticksLeft = Math.max(ticksLeft, newFadeTicks);
		}
	}

	public static void tick() {
		if (ticksLeft <= 0) {
			return;
		}
		VfxBackends.current().flash(intensity * ((float) ticksLeft / fadeTicks), argb & 0xFFFFFF);
		ticksLeft--;
		if (ticksLeft <= 0) {
			intensity = 0f;
		}
	}

	/** Session reset: drops any flash in flight. */
	public static void reset() {
		intensity = 0f;
		ticksLeft = 0;
	}
}
