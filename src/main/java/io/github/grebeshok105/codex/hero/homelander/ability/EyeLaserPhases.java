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

	private EyeLaserPhases() {
	}

	public static boolean shouldSendUpdate(int activeTicks) {
		return activeTicks >= 0 && activeTicks % VfxFx.CHANNEL_UPDATE_INTERVAL_TICKS == 0;
	}
}
