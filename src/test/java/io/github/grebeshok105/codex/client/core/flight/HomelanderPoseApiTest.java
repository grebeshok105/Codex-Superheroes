package io.github.grebeshok105.codex.client.core.flight;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link HomelanderPoseApi#centerOffsetBlocks} — the camera pivot shift that
 * keeps the tilted body centered in third person: the tilted chest anchor
 * minus where it stands upright.
 */
final class HomelanderPoseApiTest {
	@Test
	void identityTiltProducesNoShift() {
		Vec3 offset = HomelanderPoseApi.centerOffsetBlocks(FlightBodyTransform.IDENTITY, 0f);

		assertEquals(0, offset.x, 1e-6);
		assertEquals(0, offset.y, 1e-6);
		assertEquals(0, offset.z, 1e-6);
	}

	@Test
	void fullForwardPitchLaysChestAheadOfFeet() {
		// Facing +Z (yaw 0), pitched 90°: the chest point swings from
		// (0, C, 0) to (0, 0, C) — the camera must go down and forward by C.
		Vec3 offset = HomelanderPoseApi.centerOffsetBlocks(
				new FlightBodyTransform(90f, 0f), 0f);
		double c = HomelanderPoseApi.CHEST_Y;

		assertEquals(0, offset.x, 1e-5);
		assertEquals(-c, offset.y, 1e-5);
		assertEquals(c, offset.z, 1e-5);
	}

	@Test
	void fullPitchFollowsBodyYaw() {
		// Facing +X (yaw -90): the chest swings to (C, 0, 0).
		Vec3 offset = HomelanderPoseApi.centerOffsetBlocks(
				new FlightBodyTransform(90f, 0f), -90f);
		double c = HomelanderPoseApi.CHEST_Y;

		assertEquals(c, offset.x, 1e-5);
		assertEquals(-c, offset.y, 1e-5);
		assertEquals(0, offset.z, 1e-5);
	}
}
