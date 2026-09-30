package io.github.grebeshok105.codex.client.hero.homelander.flight;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link HomelanderBodyTransform#cameraCenterOffsetBlocks} — the camera pivot
 * shift that keeps the tilted body centered in third person: the tilted
 * chest anchor minus where it stands upright.
 */
final class HomelanderBodyTransformTest {
	@Test
	void identityTiltProducesNoShift() {
		Vec3 offset = HomelanderBodyTransform.IDENTITY.cameraCenterOffsetBlocks(0f);

		assertEquals(0, offset.x, 1e-6);
		assertEquals(0, offset.y, 1e-6);
		assertEquals(0, offset.z, 1e-6);
	}

	@Test
	void fullForwardPitchLaysChestAheadOfFeet() {
		// Facing +Z (yaw 0), pitched 90°: the chest point swings from
		// (0, C, 0) to (0, 0, C) — the camera must go down and forward by C.
		Vec3 offset = new HomelanderBodyTransform(90, 0, 0).cameraCenterOffsetBlocks(0f);
		double c = HomelanderBodyTransform.CHEST_Y;

		assertEquals(0, offset.x, 1e-5);
		assertEquals(-c, offset.y, 1e-5);
		assertEquals(c, offset.z, 1e-5);
	}

	@Test
	void fullPitchFollowsBodyYaw() {
		// Facing +X (yaw -90): the chest swings to (C, 0, 0).
		Vec3 offset = new HomelanderBodyTransform(90, 0, 0).cameraCenterOffsetBlocks(-90f);
		double c = HomelanderBodyTransform.CHEST_Y;

		assertEquals(c, offset.x, 1e-5);
		assertEquals(-c, offset.y, 1e-5);
		assertEquals(0, offset.z, 1e-5);
	}

	@Test
	void yOffsetSlidesCameraVertically() {
		// A 16px root lift should raise the camera a full block.
		Vec3 offset = new HomelanderBodyTransform(0, 0, 16).cameraCenterOffsetBlocks(0f);

		assertEquals(0, offset.x, 1e-6);
		assertEquals(1.0, offset.y, 1e-6);
		assertEquals(0, offset.z, 1e-6);
	}
}
