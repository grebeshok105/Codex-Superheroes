package io.github.grebeshok105.codex.client.core.vfx.debug;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Task 13: the probe's rolling window must report a sane average and isolate
 * the worst 1 % of frames — 600 frames at 10 ms plus 6 spikes at 50 ms reads
 * as ≈ 97 fps average with a 1 %-low of ≈ 20 fps.
 */
final class VfxPerfProbeTest {

	@Test
	void averageAndOnePercentLow() {
		VfxPerfProbe.reset();
		for (int i = 0; i < 600; i++) {
			VfxPerfProbe.record(10_000_000L);
		}
		for (int i = 0; i < 6; i++) {
			VfxPerfProbe.record(50_000_000L);
		}
		assertEquals(97.0, VfxPerfProbe.averageFps(), 1.0);
		assertEquals(20.0, VfxPerfProbe.onePercentLowFps(), 0.5);
	}

	@Test
	void windowDropsOldestBeyondCapacity() {
		VfxPerfProbe.reset();
		for (int i = 0; i < 600; i++) {
			VfxPerfProbe.record(100_000_000L); // 10 fps
		}
		for (int i = 0; i < 100; i++) {
			VfxPerfProbe.record(10_000_000L); // 100 fps
		}
		// Window keeps only the last 600: 500×100ms + 100×10ms → 600000/51000 ≈ 11.8 fps.
		assertEquals(11.8, VfxPerfProbe.averageFps(), 0.5);
	}
}
