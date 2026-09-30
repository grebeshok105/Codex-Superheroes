package io.github.grebeshok105.codex.client.hero.homelander.emf;

import io.github.grebeshok105.codex.client.ClientSessionState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link HomelanderPoseState.Entry} — per-player pose clocks/weights behind the
 * EMF variables. Driven directly per entry (the {@code tick(Minecraft)} pump
 * needs a live client; the inner {@code advance} is pure state).
 *
 * <p>Late tracking (§7 stage 15): an entry born while its player is already
 * flying — an observer that starts tracking mid-presentation — must land in
 * HOVER, not replay TAKEOFF; one-shot clips (TAKEOFF, HAND CLAP, MILK DRINK)
 * arm only on their start events, so a missed event simply never plays.
 */
class HomelanderPoseStateTest {

	private static final UUID PLAYER = UUID.randomUUID();
	private static final float ENTER = 0.9f;
	private static final float EXIT = 0.6f;

	@AfterEach
	void tearDown() {
		ClientSessionState.resetAll();
	}

	@Test
	void freshEntryMidFlightStaysInHover() {
		// The free-running clocks would otherwise replay TAKEOFF — weight 1 for
		// the whole 0.65 s hold — for an observer that missed the activation.
		HomelanderPoseState.Entry entry = new HomelanderPoseState.Entry();
		entry.advance(true, 0.0, 0.0, 0.0, 0f, false, false, ENTER, EXIT, false);
		assertEquals(HomelanderPoseMath.TAKEOFF_LENGTH_SECONDS, entry.takeoff.time());
		assertEquals(0f, entry.takeoffWeight(1f));
		assertTrue(entry.weight(1f) > 0f);
		assertTrue(entry.hoverTime(0f) > 0f);
	}

	@Test
	void primedEntryLateSyncOutsideTakeoffWindowStaysInHover() {
		// Observer tracked the entity BEFORE the periodic flight-state sync
		// landed (≤ SYNC_INTERVAL_TICKS later): the entry is primed, flying
		// flips true on the resync — but the synced phase is HOVER/CRUISE/BOOST,
		// not TAKEOFF, so the edge must not replay the clip (§7 stage 15).
		HomelanderPoseState.Entry entry = new HomelanderPoseState.Entry();
		entry.advance(false, 0.0, 0.0, 0.0, 0f, false, false, ENTER, EXIT, true);
		entry.advance(true, 0.0, 0.0, 0.0, 0f, false, false, ENTER, EXIT, false);
		assertEquals(HomelanderPoseMath.TAKEOFF_LENGTH_SECONDS, entry.takeoff.time());
		assertEquals(0f, entry.takeoffWeight(1f));
	}

	@Test
	void primedEntrySyncInsideTakeoffWindowReplaysTakeoff() {
		// Same edge with the synced phase still inside the server's TAKEOFF
		// window — a genuinely witnessed activation must play the clip.
		HomelanderPoseState.Entry entry = new HomelanderPoseState.Entry();
		entry.advance(false, 0.0, 0.0, 0.0, 0f, false, true, ENTER, EXIT, true);
		entry.advance(true, 0.0, 0.0, 0.0, 0f, false, true, ENTER, EXIT, false);
		assertEquals(0.05f, entry.takeoff.time(), 1e-6f);
		assertEquals(1f, entry.takeoffWeight(1f), 1e-6f);
	}

	@Test
	void groundedActivationPlaysTakeoffFromZero() {
		HomelanderPoseState.Entry entry = new HomelanderPoseState.Entry();
		entry.advance(false, 0.0, 0.0, 0.0, 0f, false, true, ENTER, EXIT, true);
		entry.advance(true, 0.0, 0.0, 0.0, 0f, false, true, ENTER, EXIT, false);
		// One tick of clock after the activation reset.
		assertEquals(0.05f, entry.takeoff.time(), 1e-6f);
		assertEquals(1f, entry.takeoffWeight(1f), 1e-6f);
	}

	@Test
	void airborneActivationStartsPastCrouchDip() {
		HomelanderPoseState.Entry entry = new HomelanderPoseState.Entry();
		entry.advance(false, 0.0, 0.0, 0.0, 0f, false, true, ENTER, EXIT, false);
		entry.advance(true, 0.0, 0.0, 0.0, 0f, false, true, ENTER, EXIT, false);
		assertEquals(HomelanderPoseMath.TAKEOFF_AIRBORNE_START_SECONDS + 0.05f,
				entry.takeoff.time(), 1e-6f);
		assertEquals(1f, entry.takeoffWeight(1f), 1e-6f);
	}

	@Test
	void teleportDeltaDoesNotLatchBoost() {
		// Same-dim teleport or dimension change: the position delta is
		// relocation, not motion — it must not feed the smoothed velocity or
		// the boost latch reads a huge one-tick speed (§7 stage 15).
		HomelanderPoseState.Entry teleported = new HomelanderPoseState.Entry();
		teleported.advance(true, 0.0, 0.0, 0.0, 0f, true, false, ENTER, EXIT, false);
		teleported.advance(true, 500.0, 0.0, 500.0, 0f, true, false, ENTER, EXIT, false);
		assertEquals(0f, Math.abs(teleported.forward), 1e-6f);
		assertEquals(0f, teleported.boostWeight(1f));

		// Contrast: real motion (+2 b/t forward while SUPERSONIC) does engage.
		HomelanderPoseState.Entry flying = new HomelanderPoseState.Entry();
		flying.advance(true, 0.0, 0.0, 0.0, 0f, true, false, ENTER, EXIT, false);
		flying.advance(true, 0.0, 0.0, 2.0, 0f, true, false, ENTER, EXIT, false);
		assertTrue(flying.forward > HomelanderPoseMath.SUPERSONIC_BOOST_FORWARD);
		assertTrue(flying.boostWeight(1f) > 0f);
	}

	@Test
	void sessionResetDropsPoseEntries() {
		// startClap creates the entry without ownership — pins the
		// ClientSessionState.register wiring (§7 stage 15).
		HomelanderPoseState.startClap(PLAYER);
		assertNotNull(HomelanderPoseState.smoothedVelocity(PLAYER));
		ClientSessionState.resetAll();
		assertNull(HomelanderPoseState.smoothedVelocity(PLAYER));
		assertEquals(0f, HomelanderPoseState.weight(PLAYER, 0f));
	}
}
