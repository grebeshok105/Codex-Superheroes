package io.github.grebeshok105.codex.client.core.flight;

import io.github.grebeshok105.codex.client.core.vfx.params.VfxParams;
import net.minecraft.world.phys.Vec3;

/**
 * Decides whether a flight speed shockwave ring may fire this tick. A ring is
 * an accel event, not a state: it fires when the per-tick velocity change
 * clears {@code ringAccelDelta} (blocks/tick²) while the player is already
 * moving faster than {@code ringMinSpeed} (blocks/tick, keeps landing spikes
 * and hover twitches quiet), at most once per {@code ringCooldownTicks}.
 */
final class SpeedRingGate {
	private SpeedRingGate() {
	}

	static boolean shouldFire(Vec3 prevVelocity, Vec3 velocity, VfxParams pose,
			long lastFiredTick, long nowTick) {
		double accel = velocity.subtract(prevVelocity).length();
		if (accel < pose.number("ringAccelDelta", 0.08f)) {
			return false;
		}
		if (velocity.length() < pose.number("ringMinSpeed", 0.45f)) {
			return false;
		}
		long cooldown = (long) pose.number("ringCooldownTicks", 20f);
		return lastFiredTick < 0 || nowTick - lastFiredTick >= cooldown;
	}
}
