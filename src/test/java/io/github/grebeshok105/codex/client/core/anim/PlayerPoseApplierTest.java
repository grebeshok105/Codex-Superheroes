package io.github.grebeshok105.codex.client.core.anim;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link PlayerPoseApplier} frame-pose lifecycle: deltas land on top of the
 * vanilla-computed pose and the returned {@link PlayerPoseApplier.Restoration}
 * reverts the touched parts, so playing the same sample across frames never
 * accumulates on the model (the "model drifts away" regression).
 */
class PlayerPoseApplierTest {
	private static final float DEG = (float) (Math.PI / 180.0);

	@Test
	void applyOffsetsPoseFromVanillaBaseline() {
		HumanoidModel<?> model = humanoid();
		PoseSample sample = new PoseSample(
				Map.of("body", new Vector3f(10, 0, 0)),
				Map.of("body", new Vector3f(0, -2, 0)),
				1f);

		PlayerPoseApplier.apply(model, sample);

		assertEquals(-10f * DEG, model.body.xRot, 1e-5, "bedrock +x pitch flips to -xRot");
		assertEquals(2f, model.body.y, 1e-5, "bedrock -2 px offset flips to +2 y");
	}

	@Test
	void repeatedFramesDoNotAccumulate() {
		HumanoidModel<?> model = humanoid();
		PoseSample sample = new PoseSample(
				Map.of("body", new Vector3f(10, 0, 0), "head", new Vector3f(0, 5, 0)),
				Map.of("body", new Vector3f(0, -2, 0), "head", new Vector3f(1, 0, 0)),
				1f);

		PlayerPoseApplier.Restoration first = PlayerPoseApplier.apply(model, sample);
		float bodyXRot = model.body.xRot;
		float bodyY = model.body.y;
		first.restore();
		PlayerPoseApplier.apply(model, sample);

		assertEquals(bodyXRot, model.body.xRot, 1e-6, "second frame must not stack rotation");
		assertEquals(bodyY, model.body.y, 1e-6, "second frame must not stack pivot offset");
	}

	@Test
	void restoreReturnsPartsToPreApplyState() {
		HumanoidModel<?> model = humanoid();
		model.body.xRot = 0.4f;
		model.body.y = 12f;
		PoseSample sample = new PoseSample(
				Map.of("body", new Vector3f(30, 10, -5)),
				Map.of("body", new Vector3f(3, -1, 2)),
				0.5f);

		PlayerPoseApplier.apply(model, sample).restore();

		assertEquals(0.4f, model.body.xRot, 1e-6, "rotation reverted");
		assertEquals(12f, model.body.y, 1e-6, "pivot reverted");
	}

	@Test
	void emptyWeightAppliesNothing() {
		HumanoidModel<?> model = humanoid();
		PlayerPoseApplier.Restoration restoration = PlayerPoseApplier.apply(model,
				new PoseSample(Map.of("body", new Vector3f(90, 0, 0)),
						Map.of("body", new Vector3f(9, 9, 9)), 0f));

		assertEquals(PlayerPoseApplier.Restoration.NONE, restoration);
		assertEquals(0f, model.body.xRot, 1e-6);
		assertEquals(0f, model.body.y, 1e-6);
	}

	private static HumanoidModel<?> humanoid() {
		return new HumanoidModel<>(new ModelPart(List.of(), Map.of(
				"head", part(), "hat", part(), "body", part(),
				"right_arm", part(), "left_arm", part(),
				"right_leg", part(), "left_leg", part())));
	}

	private static ModelPart part() {
		ModelPart part = new ModelPart(List.of(), Map.of());
		part.setInitialPose(PartPose.ZERO);
		return part;
	}

	// --- renderedRotationDeg convention ---

	@Test
	void renderedRotationNegatesEveryComponent() {
		PoseSample sample = new PoseSample(
				Map.of("head", new Vector3f(-65f, 8f, 4f)), Map.of(), 1f);
		Vector3f rendered = PlayerPoseApplier.renderedRotationDeg(sample, "head");
		assertEquals(65f, rendered.x, 1e-6);
		assertEquals(-8f, rendered.y, 1e-6);
		assertEquals(-4f, rendered.z, 1e-6);
	}

	@Test
	void renderedRotationRescalesByMergedWeight() {
		// Stored rotations are pre-divided by the merged fade weight; the
		// effective delta recovers them via sample.weight().
		PoseSample sample = new PoseSample(
				Map.of("head", new Vector3f(-130f, 0f, 0f)), Map.of(), 0.5f);
		assertEquals(65f, PlayerPoseApplier.renderedRotationDeg(sample, "head").x, 1e-6);
	}

	@Test
	void missingBoneAndZeroWeightGiveZeroDelta() {
		PoseSample empty = new PoseSample(Map.of(), Map.of(), 1f);
		Vector3f none = PlayerPoseApplier.renderedRotationDeg(empty, "head");
		assertTrue(none.lengthSquared() < 1e-12);

		PoseSample faded = new PoseSample(
				Map.of("head", new Vector3f(10f, 0f, 0f)), Map.of(), 0f);
		Vector3f zero = PlayerPoseApplier.renderedRotationDeg(faded, "head");
		assertTrue(zero.lengthSquared() < 1e-12);
	}
}
