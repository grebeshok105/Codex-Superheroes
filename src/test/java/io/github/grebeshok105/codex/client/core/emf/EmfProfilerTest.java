package io.github.grebeshok105.codex.client.core.emf;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * {@link EmfProfiler} — per-frame supplier-call counters behind the debug flag
 * (§7 stage 15): gated counting, frame-boundary snapshots, and the
 * in-place counter reset.
 */
class EmfProfilerTest {

	@AfterEach
	void tearDown() {
		EmfProfiler.setEnabled(false);
		EmfProfiler.endFrame();
	}

	@Test
	void disabledCountsNothing() {
		EmfProfiler.setEnabled(false);
		assertFalse(EmfProfiler.enabled());
		EmfProfiler.count("superheroes_hl_w");
		EmfProfiler.endFrame();
		assertEquals(0, EmfProfiler.lastFrameTotal());
		assertEquals("", EmfProfiler.lastFrameTopName());
	}

	@Test
	void countsSuppliersPerFrame() {
		EmfProfiler.setEnabled(true);
		EmfProfiler.count("superheroes_hl_w");
		EmfProfiler.count("superheroes_hl_w");
		EmfProfiler.count("superheroes_hl_w");
		EmfProfiler.count("superheroes_hl_hover_t");
		EmfProfiler.endFrame();
		assertEquals(4, EmfProfiler.lastFrameTotal());
		assertEquals("superheroes_hl_w", EmfProfiler.lastFrameTopName());
		assertEquals(3, EmfProfiler.lastFrameTopCount());
	}

	@Test
	void endFrameResetsCountersForNextFrame() {
		EmfProfiler.setEnabled(true);
		EmfProfiler.count("superheroes_hl_w");
		EmfProfiler.endFrame();
		assertEquals(1, EmfProfiler.lastFrameTotal());
		// No calls this frame — the next boundary reports a clean zero.
		EmfProfiler.endFrame();
		assertEquals(0, EmfProfiler.lastFrameTotal());
		assertEquals("", EmfProfiler.lastFrameTopName());
		assertEquals(0, EmfProfiler.lastFrameTopCount());
	}

	@Test
	void toggleMidProfileKeepsOnlyCountedCalls() {
		EmfProfiler.setEnabled(true);
		EmfProfiler.count("superheroes_hl_w");
		EmfProfiler.setEnabled(false);
		EmfProfiler.count("superheroes_hl_w");
		EmfProfiler.count("superheroes_hl_boost_t");
		EmfProfiler.endFrame();
		assertEquals(1, EmfProfiler.lastFrameTotal());
		assertEquals("superheroes_hl_w", EmfProfiler.lastFrameTopName());
	}
}
