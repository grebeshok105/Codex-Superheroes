package io.github.grebeshok105.codex.hero.homelander.ability;

import io.github.grebeshok105.codex.core.net.VfxFx;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link EyeLaserPhases} owns the laser channel timing: the server heartbeats
 * UPDATEs every {@code CHANNEL_UPDATE_INTERVAL_TICKS} on the *active* clock —
 * never gated on the uranium-pulse fire flag — so the 10-tick pauses can't
 * starve the client's {@code CHANNEL_TIMEOUT_TICKS} expiry and the beam stays
 * lit straight through them.
 */
class EyeLaserPhasesTest {

	@Test
	void chargeTicksIsSix() {
		assertEquals(6, EyeLaserPhases.CHARGE_TICKS);
		assertEquals(8, EyeLaserPhases.RELEASE_TICKS);
	}

	@Test
	void updatesEveryTwoTicksIncludingPulsePauses() {
		// The whole 80-tick pulse cycle (fire 0-19, pause 20-29, fire 30-69,
		// pause 70-79) must update on the same 2-tick grid — pauses included.
		for (int activeTicks = 0; activeTicks < 160; activeTicks++) {
			assertEquals(activeTicks % VfxFx.CHANNEL_UPDATE_INTERVAL_TICKS == 0,
					EyeLaserPhases.shouldSendUpdate(activeTicks), "activeTicks=" + activeTicks);
		}
		// Explicitly inside the fire=false windows: updates still flow.
		assertTrue(EyeLaserPhases.shouldSendUpdate(20), "pause-1 start still heartbeats");
		assertTrue(EyeLaserPhases.shouldSendUpdate(28), "inside pause-1 still heartbeats");
		assertTrue(EyeLaserPhases.shouldSendUpdate(70), "pause-2 start still heartbeats");
		assertTrue(EyeLaserPhases.shouldSendUpdate(78), "inside pause-2 still heartbeats");
		assertFalse(EyeLaserPhases.shouldSendUpdate(21));
		assertFalse(EyeLaserPhases.shouldSendUpdate(79));
	}
}
