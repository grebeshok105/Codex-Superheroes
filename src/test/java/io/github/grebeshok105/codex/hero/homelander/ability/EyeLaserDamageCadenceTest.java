package io.github.grebeshok105.codex.hero.homelander.ability;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Stage 10 laser damage cadence: {@link EyeLaserPhases#shouldDamage(int)} gates
 * the {@code hurt} call onto an explicit grid of one hit every
 * {@code DAMAGE_INTERVAL_TICKS} firing ticks (2 hits/s), replacing the old
 * every-tick call that landed unconditionally because {@code eye_laser}
 * bypasses the hurt cooldown.
 *
 * <p>The argument is a *firing-tick* index: {@code EyeLasersAbility} keeps it on
 * its own clock that only advances while the beam fires, so the uranium-pulse
 * {@code fire=false} pauses cannot shift the hit grid the way they would a
 * counter riding {@code ACTIVE_TICK} (which counts straight through pauses).
 */
class EyeLaserDamageCadenceTest {

	@Test
	void intervalIsTenTicks() {
		assertEquals(10, EyeLaserPhases.DAMAGE_INTERVAL_TICKS);
	}

	@Test
	void hitsLandOnEveryTenthFiringTick() {
		for (int ticks = 0; ticks < 200; ticks++) {
			assertEquals(ticks % EyeLaserPhases.DAMAGE_INTERVAL_TICKS == 0,
					EyeLaserPhases.shouldDamage(ticks), "ticks=" + ticks);
		}
		assertTrue(EyeLaserPhases.shouldDamage(0), "the activation firing tick lands immediately");
		assertFalse(EyeLaserPhases.shouldDamage(9));
		assertTrue(EyeLaserPhases.shouldDamage(10));
		assertFalse(EyeLaserPhases.shouldDamage(11));
	}

	@Test
	void twoHitsPerSecondOverFortyFiringTicks() {
		int hits = 0;
		for (int ticks = 0; ticks < 40; ticks++) {
			if (EyeLaserPhases.shouldDamage(ticks)) {
				hits++;
			}
		}
		assertEquals(4, hits, "40 firing ticks land exactly 4 hits = 2 hits/s");
	}
}
