package io.github.grebeshok105.codex.client.core.emf;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Variable-supplier call counter behind the VFX debug HUD flag (plan §7
 * stage 15): while {@code /superheroes vfx hud on} is up, every
 * {@code EmfBridge.registerFloatVariable} supplier routes through
 * {@link #count(String)} and the HUD-driven {@link #endFrame()}
 * snapshots the per-frame totals, so supplier call volume — the §10 perf
 * question "does a variable evaluate once per entity render or per jem
 * channel" — is observable in-game. With the flag off the only cost is one
 * static boolean read per supplier call; the wrap itself is decided once at
 * registration and stays alloc-free (names map to a reused {@code int[1]} —
 * after the first sighting the hot path touches no allocations).
 */
public final class EmfProfiler {
	private static boolean enabled;
	private static final Map<String, int[]> COUNTS = new LinkedHashMap<>();

	private static int total;
	private static int lastTotal;
	private static String topName = "";
	private static int topCount;

	private EmfProfiler() {
	}

	public static boolean enabled() {
		return enabled;
	}

	/**
	 * Driven once per frame by {@code VfxDebugHud.render} with the debug flag;
	 * kept settable so headless tests can profile without a Minecraft client.
	 */
	public static void setEnabled(boolean on) {
		enabled = on;
	}

	/** One supplier invocation. No-op while disabled — a single boolean read. */
	public static void count(String name) {
		if (!enabled) {
			return;
		}
		COUNTS.computeIfAbsent(name, key -> new int[1])[0]++;
		total++;
	}

	/**
	 * Frame boundary: moves this frame's totals into the {@code lastFrame*}
	 * snapshot the HUD displays, then zeroes the counters in place.
	 */
	public static void endFrame() {
		lastTotal = total;
		total = 0;
		topName = "";
		topCount = 0;
		for (Map.Entry<String, int[]> entry : COUNTS.entrySet()) {
			int count = entry.getValue()[0];
			if (count > topCount) {
				topCount = count;
				topName = entry.getKey();
			}
			entry.getValue()[0] = 0;
		}
	}

	/** Total supplier invocations in the last completed frame. */
	public static int lastFrameTotal() {
		return lastTotal;
	}

	/** Most-called variable name in the last completed frame ("" when none). */
	public static String lastFrameTopName() {
		return topName;
	}

	/** Call count of {@link #lastFrameTopName()} in the last completed frame. */
	public static int lastFrameTopCount() {
		return topCount;
	}
}
