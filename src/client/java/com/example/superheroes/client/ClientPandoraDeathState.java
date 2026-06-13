package com.example.superheroes.client;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Client-side timeline of Pandora's one-time death cinematic (#7), seen by her killer
 * (and Pandora). Phases over ~6.5s:
 * <pre>
 *   0.00–0.20  freeze-in: blood-red repaint floods, the world darkens to silhouettes
 *   0.20–0.60  hold: camera smoothly turns toward Pandora (if not already facing her)
 *   0.60–0.85  rise: red deepens, blood vignette peaks
 *   0.85–1.00  fade out back to normal
 * </pre>
 * Everything self-times on the client; the server only fires the start packet.
 */
public final class ClientPandoraDeathState {
	private static final long DURATION_MS = 6500L;

	private static volatile boolean active;
	private static long startMs;
	private static Vec3 target = Vec3.ZERO;

	private ClientPandoraDeathState() {
	}

	public static void start(double x, double y, double z) {
		target = new Vec3(x, y, z);
		startMs = System.currentTimeMillis();
		active = true;
	}

	public static boolean active() {
		return active;
	}

	public static Vec3 target() {
		return target;
	}

	/** 0..1 progress; auto-clears when finished. */
	public static float progress() {
		if (!active) {
			return 0f;
		}
		float p = (System.currentTimeMillis() - startMs) / (float) DURATION_MS;
		if (p >= 1f) {
			active = false;
			return 0f;
		}
		return Mth.clamp(p, 0f, 1f);
	}

	/** How strongly the camera should be steered toward Pandora right now (0..1). */
	public static float cameraSteer() {
		float p = progress();
		if (!active) {
			return 0f;
		}
		// Ease in during the "hold" window, ease back out near the end.
		if (p < 0.20f) {
			return p / 0.20f * 0.6f;
		}
		if (p < 0.85f) {
			return Mth.lerp((p - 0.20f) / 0.65f, 0.6f, 1f);
		}
		return Mth.lerp((p - 0.85f) / 0.15f, 1f, 0f);
	}

	public static void onDisconnect() {
		active = false;
	}
}
