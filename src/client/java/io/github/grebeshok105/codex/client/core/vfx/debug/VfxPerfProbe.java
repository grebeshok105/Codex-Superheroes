package io.github.grebeshok105.codex.client.core.vfx.debug;

import java.util.Arrays;

/**
 * Rolling frame-time probe feeding {@link VfxDebugHud}: keeps the last
 * {@value #CAPACITY} recorded frame durations and derives average FPS,
 * 1%-low FPS, and mean frame time in milliseconds. Recording happens once per
 * client tick when the HUD is visible.
 */
public final class VfxPerfProbe {
	static final int CAPACITY = 600;

	private static final long[] frames = new long[CAPACITY];
	private static int cursor;
	private static int size;
	private static long total;

	private VfxPerfProbe() {
	}

	public static void record(long frameNanos) {
		if (frameNanos <= 0L) {
			return;
		}
		if (size == CAPACITY) {
			total -= frames[cursor];
		} else {
			size++;
		}
		frames[cursor] = frameNanos;
		total += frameNanos;
		cursor = (cursor + 1) % CAPACITY;
	}

	/** Clears the rolling window. */
	public static void reset() {
		Arrays.fill(frames, 0L);
		cursor = 0;
		size = 0;
		total = 0L;
	}

	/** Mean frame duration over the window, in milliseconds. */
	public static double averageFrameMillis() {
		if (size == 0) {
			return 0.0;
		}
		return (total / 1_000_000.0) / size;
	}

	/** FPS implied by the mean frame duration. */
	public static double averageFps() {
		if (size == 0) {
			return 0.0;
		}
		return 1_000_000_000.0 / (total / (double) size);
	}

	/** FPS implied by the mean of the worst ~1% of frames in the window. */
	public static double onePercentLowFps() {
		if (size == 0) {
			return 0.0;
		}
		int lowCount = Math.max(1, (int) Math.ceil(size * 0.01));
		long[] sorted = Arrays.copyOf(frames, size);
		Arrays.sort(sorted);
		double worst = 0.0;
		for (int i = size - lowCount; i < size; i++) {
			worst += sorted[i];
		}
		return 1_000_000_000.0 / (worst / lowCount);
	}
}
