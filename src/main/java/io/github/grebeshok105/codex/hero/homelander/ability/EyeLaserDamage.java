package io.github.grebeshok105.codex.hero.homelander.ability;

/**
 * Damage cadence math for {@link EyeLasersAbility}: the beam applies damage
 * every {@link #DAMAGE_INTERVAL_TICKS} ticks (~2 hits/sec) instead of every
 * tick — fewer, heavier hits that read as searing pulses rather than a
 * tickrate blender. The mana-scaled table is preserved, but the per-hit amount
 * accumulates a whole interval of a deliberately lower DPS band
 * ({@link #MIN_DPS}..{@link #MAX_DPS}, roughly half the old 56..120 per-tick
 * curve).
 *
 * <p>Pure math — JUnit drives this without a Minecraft instance.
 */
public final class EyeLaserDamage {
	/** Ticks between damage applications while the beam is firing. */
	public static final int DAMAGE_INTERVAL_TICKS = 10;
	public static final float MIN_DPS = 30.0f;
	public static final float MAX_DPS = 50.0f;

	private EyeLaserDamage() {
	}

	/** Damage lands on ticks 0, 10, 20, … of the channel-active clock. */
	public static boolean isDamageTick(int activeTicks) {
		return activeTicks >= 0 && activeTicks % DAMAGE_INTERVAL_TICKS == 0;
	}

	/**
	 * Per-hit damage for a mana fraction: the tiered DPS accumulated over one
	 * full interval, so firing continuously lands exactly
	 * {@code MIN_DPS..MAX_DPS} damage per second.
	 */
	public static float damagePerHit(float manaFraction) {
		float frac = Math.max(0f, Math.min(1f, manaFraction));
		float dps = MIN_DPS + (MAX_DPS - MIN_DPS) * frac;
		return dps * DAMAGE_INTERVAL_TICKS / 20f;
	}
}
