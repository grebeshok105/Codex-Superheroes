package io.github.grebeshok105.codex.client.core.flight;

import io.github.grebeshok105.codex.client.core.vfx.params.VfxParams;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link SpeedRingGate} — the acceleration/cooldown/min-speed gate that
 * decides whether a speed shockwave ring may fire this tick.
 */
final class SpeedRingGateTest {
	private static final Vec3 SLOW = new Vec3(0.3, 0, 0);
	private static final Vec3 FAST = new Vec3(0.8, 0, 0);

	private static VfxParams params(Map<String, Float> numbers) {
		return new VfxParams(numbers, Map.of());
	}

	@Test
	void firesOnHardAccelAboveMinSpeed() {
		VfxParams p = params(Map.of(
				"ringAccelDelta", 0.08f, "ringMinSpeed", 0.45f, "ringCooldownTicks", 20f));

		assertTrue(SpeedRingGate.shouldFire(SLOW, FAST, p, 1000, 1040),
				"hard acceleration while fast fires the ring");
	}

	@Test
	void steadySpeedNeverFires() {
		VfxParams p = params(Map.of(
				"ringAccelDelta", 0.08f, "ringMinSpeed", 0.45f, "ringCooldownTicks", 20f));

		assertFalse(SpeedRingGate.shouldFire(FAST, FAST, p, 1000, 1040),
				"constant boost speed is not an accel event");
		assertFalse(SpeedRingGate.shouldFire(FAST, FAST, p, -1, 0),
				"even with no prior fire, steady speed does not fire");
	}

	@Test
	void accelBelowThresholdNeverFires() {
		VfxParams p = params(Map.of(
				"ringAccelDelta", 0.08f, "ringMinSpeed", 0.45f, "ringCooldownTicks", 20f));
		Vec3 slightAccel = SLOW.add(0.05, 0, 0);

		assertFalse(SpeedRingGate.shouldFire(SLOW, slightAccel, p, 1000, 1040));
	}

	@Test
	void slowPlayerNeverFires() {
		VfxParams p = params(Map.of(
				"ringAccelDelta", 0.08f, "ringMinSpeed", 0.45f, "ringCooldownTicks", 20f));
		Vec3 slower = new Vec3(0.1, 0, 0);

		assertFalse(SpeedRingGate.shouldFire(Vec3.ZERO, slower, p, 1000, 1040),
				"accel from near standstill is below ringMinSpeed");
	}

	@Test
	void cooldownBlocksRepeatFire() {
		VfxParams p = params(Map.of(
				"ringAccelDelta", 0.08f, "ringMinSpeed", 0.45f, "ringCooldownTicks", 20f));

		assertFalse(SpeedRingGate.shouldFire(SLOW, FAST, p, 1030, 1040),
				"10 ticks after the last fire is inside the 20-tick cooldown");
		assertTrue(SpeedRingGate.shouldFire(SLOW, FAST, p, 1020, 1040),
				"exactly at the cooldown boundary the ring fires again");
	}
}
