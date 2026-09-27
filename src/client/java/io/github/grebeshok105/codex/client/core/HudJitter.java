package io.github.grebeshok105.codex.client.core;

import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The shared read side of {@link HudGlitchSource}: vanilla-UI and HUD code call these
 * statics instead of a concrete hero's glitch state. The strongest source by
 * {@link HudGlitchSource#ramp()} answers; with none active every method is the identity
 * (zero offsets, unchanged colours and text).
 */
public final class HudJitter {
	private static final float IDLE = 0.001f;
	private static final List<HudGlitchSource> SOURCES = new CopyOnWriteArrayList<>();

	private HudJitter() {
	}

	public static void register(HudGlitchSource source) {
		SOURCES.add(source);
	}

	private static HudGlitchSource active() {
		HudGlitchSource best = null;
		float bestRamp = IDLE;
		for (HudGlitchSource source : SOURCES) {
			float ramp = source.ramp();
			if (ramp > bestRamp) {
				best = source;
				bestRamp = ramp;
			}
		}
		return best;
	}

	public static float ramp() {
		HudGlitchSource source = active();
		return source == null ? 0f : source.ramp();
	}

	public static int jitterX() {
		HudGlitchSource source = active();
		return source == null ? 0 : source.jitterX();
	}

	public static int jitterY() {
		HudGlitchSource source = active();
		return source == null ? 0 : source.jitterY();
	}

	public static int badgeJitterX() {
		HudGlitchSource source = active();
		return source == null ? 0 : source.badgeJitterX();
	}

	public static int badgeJitterY() {
		HudGlitchSource source = active();
		return source == null ? 0 : source.badgeJitterY();
	}

	public static boolean ghostDouble() {
		HudGlitchSource source = active();
		return source != null && source.ghostDouble();
	}

	public static int ghostOffsetX() {
		HudGlitchSource source = active();
		return source == null ? 0 : source.ghostOffsetX();
	}

	public static int tintColor(int baseArgb) {
		HudGlitchSource source = active();
		return source == null ? baseArgb : source.tintColor(baseArgb);
	}

	public static Component maybeObfuscate(Component component) {
		HudGlitchSource source = active();
		return source == null ? component : source.maybeObfuscate(component);
	}
}
