package io.github.grebeshok105.codex.hero.homelander.ability;

import io.github.grebeshok105.codex.core.net.VfxFx;

/**
 * Charge/hold/release timing for the eye-laser VFX channel, plus the UPDATE
 * heartbeat rule. The heartbeat runs on the *active* clock — deliberately not
 * gated on the uranium-pulse fire flag: the beam keeps rendering through the
 * 10-tick {@code fire=false} pauses, so UPDATEs must keep flowing or the
 * client's channel timeout would drop the channel mid-pause.
 */
public final class EyeLaserPhases {
	public static final int CHARGE_TICKS = 6;
	public static final int RELEASE_TICKS = 8;

	/**
	 * One damage hit per this many firing ticks (2 hits/s). The argument of
	 * {@link #shouldDamage(int)} is a firing-tick index, not the channel's
	 * {@code ACTIVE_TICK}: the firing clock only advances while the beam fires,
	 * so uranium-pulse pauses cannot shift the hit grid.
	 */
	public static final int DAMAGE_INTERVAL_TICKS = 10;

	private EyeLaserPhases() {
	}

	public static boolean shouldSendUpdate(int activeTicks) {
		return activeTicks >= 0 && activeTicks % VfxFx.CHANNEL_UPDATE_INTERVAL_TICKS == 0;
	}

	public static boolean shouldDamage(int damageTicks) {
		return damageTicks >= 0 && damageTicks % DAMAGE_INTERVAL_TICKS == 0;
	}
}
