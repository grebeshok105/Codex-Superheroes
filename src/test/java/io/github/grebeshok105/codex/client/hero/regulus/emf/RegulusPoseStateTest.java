package io.github.grebeshok105.codex.client.hero.regulus.emf;

import io.github.grebeshok105.codex.client.ClientSessionState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link RegulusPoseState.Entry} — per-player clip clocks/weights behind the
 * EMF variables. Driven directly per entry (the {@code tick(Minecraft)} pump
 * needs a live client; the inner {@code tick} is pure state).
 *
 * <p>Clip priority from the design: one-shot casts over the evangelium idle
 * over the combat idle over vanilla — pinned here through the weight readers
 * the {@code superheroes_rg_*} jem variables expose.
 */
class RegulusPoseStateTest {

	private static final UUID PLAYER = UUID.randomUUID();

	@AfterEach
	void tearDown() {
		ClientSessionState.resetAll();
	}

	@Test
	void masterWeightApproachesOneOnceOwned() {
		RegulusPoseState.Entry entry = RegulusPoseState.entryOf(PLAYER, true);
		entry.tick(false);
		float first = RegulusPoseState.weight(PLAYER, 0f);
		assertTrue(first > 0f, "master weight starts rising on the first owned tick");
		entry.tick(false);
		assertTrue(RegulusPoseState.weight(PLAYER, 0f) > first);
	}

	@Test
	void combatIdleSelectedOutsideMadness() {
		RegulusPoseState.Entry entry = RegulusPoseState.entryOf(PLAYER, true);
		for (int i = 0; i < 10; i++) {
			entry.tick(false);
		}
		assertTrue(RegulusPoseState.combatIdleWeight(PLAYER, 0f) > 0f);
		assertEquals(0f, RegulusPoseState.evangeliumIdleWeight(PLAYER, 0f), 1e-6f);
	}

	@Test
	void evangeliumIdleSelectedInsideMadness() {
		RegulusPoseState.Entry entry = RegulusPoseState.entryOf(PLAYER, true);
		for (int i = 0; i < 10; i++) {
			entry.tick(false);
		}
		float combatBefore = RegulusPoseState.combatIdleWeight(PLAYER, 0f);
		entry.tick(true);
		assertTrue(RegulusPoseState.evangeliumIdleWeight(PLAYER, 0f) > 0f);
		assertTrue(RegulusPoseState.combatIdleWeight(PLAYER, 0f) < combatBefore);
	}

	@Test
	void oneShotWeightRisesThenFades() {
		RegulusPoseState.startDebrisKick(PLAYER);
		RegulusPoseState.Entry entry = RegulusPoseState.entryOf(PLAYER, false);
		// DEBRIS_KICK blends in over ~2.2 ticks of its 44-tick duration.
		for (int i = 0; i < 4; i++) {
			entry.tick(false);
		}
		assertEquals(1f, RegulusPoseState.oneShotWeight(PLAYER, RegulusPoseState.Clip.DEBRIS_KICK, 0f), 1e-6f);
		// Past the 2.2 s clip plus its blend-out, the weight is back at rest.
		for (int i = 0; i < 50; i++) {
			entry.tick(false);
		}
		assertEquals(0f, RegulusPoseState.oneShotWeight(PLAYER, RegulusPoseState.Clip.DEBRIS_KICK, 0f), 1e-6f);
	}

	@Test
	void restartResetsTheClipClock() {
		RegulusPoseState.startDebrisKick(PLAYER);
		RegulusPoseState.Entry entry = RegulusPoseState.entryOf(PLAYER, false);
		for (int i = 0; i < 10; i++) {
			entry.tick(false);
		}
		float before = RegulusPoseState.oneShotTime(PLAYER, RegulusPoseState.Clip.DEBRIS_KICK, 0f);
		assertTrue(before > 0.4f);
		RegulusPoseState.startDebrisKick(PLAYER);
		assertEquals(0f, RegulusPoseState.oneShotTime(PLAYER, RegulusPoseState.Clip.DEBRIS_KICK, 0f), 1e-6f);
	}

	@Test
	void sessionResetDropsPoseEntries() {
		// startDebrisKick creates the entry without ownership — pins the
		// ClientSessionState.register wiring.
		RegulusPoseState.startDebrisKick(PLAYER);
		assertTrue(RegulusPoseState.oneShotWeight(PLAYER, RegulusPoseState.Clip.DEBRIS_KICK, 0f) > 0f
				|| RegulusPoseState.entryOf(PLAYER, false) != null);
		ClientSessionState.resetAll();
		assertEquals(0f, RegulusPoseState.weight(PLAYER, 0f));
		assertEquals(0f, RegulusPoseState.oneShotWeight(PLAYER, RegulusPoseState.Clip.DEBRIS_KICK, 0f));
	}
}
