package io.github.grebeshok105.codex.client.core.vfx.backend;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link FlashEnvelope}: a lone feed fades out over its ticks; re-feeding a
 * decaying value (the {@code ScreenFlash} pattern) tracks the caller's curve
 * instead of pinning at the first peak.
 */
class FlashEnvelopeTest {

	@Test
	void singleFeedDecaysToZero() {
		FlashEnvelope envelope = new FlashEnvelope(6f);
		envelope.feed(0.6f, 0xFFAA00);
		assertEquals(0.6f, envelope.shown(), 1e-4);
		envelope.tick(3f);
		assertEquals(0.3f, envelope.shown(), 1e-4);
		envelope.tick(3f);
		assertEquals(0f, envelope.shown(), 1e-4);
		assertFalse(envelope.active());
	}

	@Test
	void reFedDecayFollowsTheFeederCurve() {
		FlashEnvelope envelope = new FlashEnvelope(6f);
		envelope.feed(1f, 0xFFFFFF);
		envelope.tick(1f);
		// A 60-tick caller-side fade re-feeds a lower value each tick: the
		// display must track ~0.9, not stay pinned at the 1.0 peak.
		envelope.feed(0.9f, 0xFFFFFF);
		assertEquals(0.9f, envelope.shown(), 1e-4);
		envelope.tick(1f);
		envelope.feed(0.8f, 0xFFFFFF);
		assertEquals(0.8f, envelope.shown(), 1e-4);
		envelope.tick(1f);
		envelope.feed(0.7f, 0xFFFFFF);
		assertEquals(0.7f, envelope.shown(), 1e-4);
	}

	@Test
	void heldFeedPinsAtItsLevel() {
		FlashEnvelope envelope = new FlashEnvelope(6f);
		for (int i = 0; i < 10; i++) {
			envelope.tick(1f);
			envelope.feed(0.5f, 0xFFFFFF);
			assertEquals(0.5f, envelope.shown(), 1e-4,
					"a constant re-feed holds its level instead of decaying");
		}
	}

	@Test
	void weakerFeedCannotDimOrExtend() {
		FlashEnvelope envelope = new FlashEnvelope(6f);
		envelope.feed(0.9f, 0xFFFFFF);
		envelope.tick(2f);
		float shown = envelope.shown();
		envelope.feed(0.2f, 0x000000);
		assertEquals(shown, envelope.shown(), 1e-4);
		assertEquals(0xFFFFFF, envelope.argb());
	}

	@Test
	void feedingStopsLeavesTailFade() {
		FlashEnvelope envelope = new FlashEnvelope(6f);
		envelope.feed(1f, 0xFFFFFF);
		envelope.tick(1f);
		envelope.feed(0.9f, 0xFFFFFF);
		// No further feeds: the last accepted value decays over fadeTicks.
		envelope.tick(3f);
		assertEquals(0.9f * 0.5f, envelope.shown(), 1e-4);
	}

	@Test
	void zeroAndNegativeFeedsIgnored() {
		FlashEnvelope envelope = new FlashEnvelope(6f);
		envelope.feed(0f, 0xFFFFFF);
		envelope.feed(-0.5f, 0xFFFFFF);
		assertFalse(envelope.active());
		assertTrue(envelope.shown() <= 0f);
	}
}
