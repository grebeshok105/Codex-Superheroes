package io.github.grebeshok105.codex.client.core.emf;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import org.joml.Vector3f;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link RenderedPoseCache}: fixed 64-slot LRU slab that carries the EMF-animated
 * part fields out of {@code PlayerRenderer.render} for the anchor consumers.
 * Captured values must round-trip, evict least-recently-used first, and drop
 * cleanly on session reset — without any per-call allocation.
 */
class RenderedPoseCacheTest {

	@AfterEach
	void reset() {
		RenderedPoseCache.clearAll();
	}

	@Test
	void missBeforeAnyCapture() {
		float[] out = new float[6];
		assertFalse(RenderedPoseCache.headPose(7, out));
		assertFalse(RenderedPoseCache.headOffsetPx(7, new Vector3f()));
		assertFalse(RenderedPoseCache.headRotationRad(7, new Vector3f()));
	}

	@Test
	void capturedHeadFieldsRoundTrip() {
		PlayerModel<?> model = model();
		model.head.x = 1.5f;
		model.head.y = -2.25f;
		model.head.z = 0.5f;
		model.head.xRot = 0.11f;
		model.head.yRot = -0.22f;
		model.head.zRot = 0.33f;
		RenderedPoseCache.capture(11, model);

		float[] out = new float[6];
		assertTrue(RenderedPoseCache.headPose(11, out));
		assertEquals(1.5f, out[0], 1e-6);
		assertEquals(-2.25f, out[1], 1e-6);
		assertEquals(0.5f, out[2], 1e-6);
		assertEquals(0.11f, out[3], 1e-6);
		assertEquals(-0.22f, out[4], 1e-6);
		assertEquals(0.33f, out[5], 1e-6);

		Vector3f offset = new Vector3f();
		assertTrue(RenderedPoseCache.headOffsetPx(11, offset));
		assertEquals(1.5f, offset.x, 1e-6);
		Vector3f rotation = new Vector3f();
		assertTrue(RenderedPoseCache.headRotationRad(11, rotation));
		assertEquals(-0.22f, rotation.y, 1e-6);
	}

	@Test
	void secondCaptureOverwrites() {
		PlayerModel<?> model = model();
		RenderedPoseCache.capture(3, model);
		model.head.xRot = 0.9f;
		RenderedPoseCache.capture(3, model);
		float[] out = new float[6];
		assertTrue(RenderedPoseCache.headPose(3, out));
		assertEquals(0.9f, out[3], 1e-6);
	}

	@Test
	void clearDropsOnlyThatEntry() {
		PlayerModel<?> model = model();
		RenderedPoseCache.capture(1, model);
		RenderedPoseCache.capture(2, model);
		RenderedPoseCache.clear(1);
		assertFalse(RenderedPoseCache.headPose(1, new float[6]));
		assertTrue(RenderedPoseCache.headPose(2, new float[6]));
	}

	@Test
	void clearAllDropsEverything() {
		PlayerModel<?> model = model();
		RenderedPoseCache.capture(1, model);
		RenderedPoseCache.capture(2, model);
		RenderedPoseCache.clearAll();
		assertFalse(RenderedPoseCache.headPose(1, new float[6]));
		assertFalse(RenderedPoseCache.headPose(2, new float[6]));
	}

	@Test
	void evictsLeastRecentlyUsedBeyondCapacity() {
		PlayerModel<?> model = model();
		for (int id = 0; id < RenderedPoseCache.CAPACITY; id++) {
			RenderedPoseCache.capture(id, model);
		}
		// touch id 0 so it is no longer the oldest
		assertTrue(RenderedPoseCache.headPose(0, new float[6]));
		RenderedPoseCache.capture(RenderedPoseCache.CAPACITY, model);
		assertTrue(RenderedPoseCache.headPose(0, new float[6]),
				"recently read entry must survive eviction");
		assertFalse(RenderedPoseCache.headPose(1, new float[6]),
				"oldest entry must be evicted");
		assertTrue(RenderedPoseCache.headPose(RenderedPoseCache.CAPACITY, new float[6]));
	}

	private static PlayerModel<?> model() {
		return new PlayerModel<>(root(), false);
	}

	private static ModelPart root() {
		return new ModelPart(List.of(), Map.ofEntries(
				Map.entry("head", part()), Map.entry("hat", part()), Map.entry("body", part()),
				Map.entry("right_arm", part()), Map.entry("left_arm", part()),
				Map.entry("right_leg", part()), Map.entry("left_leg", part()),
				Map.entry("ear", part()), Map.entry("cloak", part()),
				Map.entry("left_sleeve", part()), Map.entry("right_sleeve", part()),
				Map.entry("left_pants", part()), Map.entry("right_pants", part()),
				Map.entry("jacket", part())));
	}

	private static ModelPart part() {
		ModelPart part = new ModelPart(List.of(), Map.of());
		part.setInitialPose(PartPose.ZERO);
		return part;
	}
}
