package io.github.grebeshok105.codex.client.core.vfx.anchor;

import io.github.grebeshok105.codex.client.core.flight.FlightBodyTransform;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives {@link HumanoidAnchors#eyesFrom} — the pure eye-anchor math.
 * Pinned scenario: beams must leave the rendered eyes under head look and
 * flight tilt, never the feet or camera (plan Review Focus 2).
 */
class HumanoidAnchorsTest {
	private static final Vec3 FEET = new Vec3(0, 0, 0);

	private static Vec3 midpoint(EyePair pair) {
		return pair.left().add(pair.right()).scale(0.5);
	}

	@Test
	void eyesFollowHeadYawAndPitch() {
		// Head yaw 90° faces -X (Vec3.directionFromRotation(0, 90) == (-1,0,0)).
		Vec3 yawed = midpoint(HumanoidAnchors.eyesFrom(FEET, 0f, 90f, 0f,
				FlightBodyTransform.IDENTITY, 1f));
		assertTrue(yawed.x < -0.2, "eyes offset toward -X at yaw 90, got x=" + yawed.x);

		// Head pitch +90° looks straight down: eyes sit below the head pivot.
		Vec3 level = midpoint(HumanoidAnchors.eyesFrom(FEET, 0f, 0f, 0f,
				FlightBodyTransform.IDENTITY, 1f));
		Vec3 pitched = midpoint(HumanoidAnchors.eyesFrom(FEET, 0f, 0f, 90f,
				FlightBodyTransform.IDENTITY, 1f));
		assertTrue(pitched.y < 1.5, "pitch +90 moves eyes below the head pivot, got y=" + pitched.y);
		assertTrue(pitched.y < level.y - 0.2, "pitch +90 moves eyes down vs level, got y=" + pitched.y);
	}

	@Test
	void eyesFollowFlightTilt() {
		EyePair tilted = HumanoidAnchors.eyesFrom(FEET, 0f, 0f, 0f,
				new FlightBodyTransform(80f, 0f), 1f);
		Vec3 mid = midpoint(tilted);
		double horizontal = Math.sqrt(mid.x * mid.x + mid.z * mid.z);
		assertTrue(horizontal >= 1.0,
				"tilt pitch 80 moves eye midpoint >= 1.0 horizontally forward of feet, got " + horizontal);
		// Forward at yaw 0 is +Z: the lean moves eyes toward +Z, not sideways.
		assertTrue(mid.z > 0, "tilt pitch leans eyes along body forward (+Z), got z=" + mid.z);
		assertEquals(0.0, mid.x, 0.35, "no sideways drift for pure pitch tilt");
	}

	@Test
	void eyesSymmetric() {
		float headYaw = 30f;
		EyePair pair = HumanoidAnchors.eyesFrom(FEET, headYaw, headYaw, 10f,
				FlightBodyTransform.IDENTITY, 1f);
		Vec3 headFwd = Vec3.directionFromRotation(10f, headYaw);
		Vec3 headRight = headFwd.cross(new Vec3(0, 1, 0)).normalize();
		Vec3 pivotToMid = midpoint(pair).subtract(new Vec3(0, 1.5, 0));
		assertEquals(0.0, pivotToMid.dot(headRight), 1e-6,
				"eye midpoint has zero lateral offset from the head forward axis");
	}
}
