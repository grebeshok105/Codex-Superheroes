package io.github.grebeshok105.codex.hero.homelander.ability;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link EyeLaserDamage} owns the laser damage cadence: hits land every
 * {@code DAMAGE_INTERVAL_TICKS} of the channel-active clock (~2/sec), and each
 * hit carries a whole interval of the mana-tiered DPS so continuous fire lands
 * exactly {@code MIN_DPS}..{@code MAX_DPS} — the deliberate rebalance down from
 * the old 56..120 per-tick curve.
 */
class EyeLaserDamageTest {

	@Test
	void damageTicksEveryTenTicks() {
		assertEquals(10, EyeLaserDamage.DAMAGE_INTERVAL_TICKS);
		for (int t = 0; t < 200; t++) {
			assertEquals(t % EyeLaserDamage.DAMAGE_INTERVAL_TICKS == 0,
					EyeLaserDamage.isDamageTick(t), "activeTicks=" + t);
		}
		assertFalse(EyeLaserDamage.isDamageTick(-1));
	}

	@Test
	void perHitDamageReproducesTieredDps() {
		// per-hit × hits-per-second must reproduce the mana-scaled DPS exactly.
		float hitsPerSecond = 20f / EyeLaserDamage.DAMAGE_INTERVAL_TICKS;
		assertEquals(EyeLaserDamage.MIN_DPS,
				EyeLaserDamage.damagePerHit(0f) * hitsPerSecond, 1e-4);
		assertEquals(EyeLaserDamage.MAX_DPS,
				EyeLaserDamage.damagePerHit(1f) * hitsPerSecond, 1e-4);
		assertEquals((EyeLaserDamage.MIN_DPS + EyeLaserDamage.MAX_DPS) / 2f,
				EyeLaserDamage.damagePerHit(0.5f) * hitsPerSecond, 1e-4);
	}

	@Test
	void dpsBandIsRoughlyHalfTheOldCurve() {
		// Old curve: 56..120 DPS applied every tick. New band must sit inside
		// 40..60% of it (spec: ~30-50 DPS at full mana range).
		assertEquals(30f, EyeLaserDamage.MIN_DPS);
		assertEquals(50f, EyeLaserDamage.MAX_DPS);
		assertTrue(EyeLaserDamage.MIN_DPS >= 0.4f * 56f && EyeLaserDamage.MIN_DPS <= 0.6f * 56f);
		assertTrue(EyeLaserDamage.MAX_DPS >= 0.4f * 120f && EyeLaserDamage.MAX_DPS <= 0.6f * 120f);
	}

	@Test
	void manaFractionIsClamped() {
		assertEquals(EyeLaserDamage.damagePerHit(0f), EyeLaserDamage.damagePerHit(-0.5f), 1e-6);
		assertEquals(EyeLaserDamage.damagePerHit(1f), EyeLaserDamage.damagePerHit(4f), 1e-6);
	}
}
