package io.github.grebeshok105.codex.client.core.anim;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Applies a {@link PoseSample} on top of the pose {@code setupAnim} produced, using the
 * GeckoLib Bedrock→Java sign convention: {@code xRot += -rad(x)}, {@code yRot += -rad(y)},
 * {@code zRot += rad(z)}; offsets {@code x += -px.x}, {@code y += -px.y},
 * {@code z += px.z}; every delta scaled by the sample weight.
 *
 * <p>{@code ModelPart} state survives the frame: {@code setupAnim} rewrites only the
 * fields vanilla animates, so a pivot offset or a rotation axis it skips would keep
 * accumulating the delta forever. {@link #apply} therefore snapshots every part it
 * touches and returns a {@link Restoration}; the caller must restore it before the
 * model is posed again (e.g. at the head of the next {@code setupAnim}), which leaves
 * each frame's delta applied exactly once on top of a vanilla-computed pose.
 */
public final class PlayerPoseApplier {
	private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);
	/** Bones that keep their vanilla pose under {@link #resetDrivenLimbs}: the head still tracks the view. */
	private static final String HEAD_BONE = "head";

	private PlayerPoseApplier() {
	}

	/** Pre-apply snapshot of every part {@link #apply} mutated; {@link #restore} reverts it. */
	public record Restoration(List<Map.Entry<ModelPart, PartPose>> saved) {
		public static final Restoration NONE = new Restoration(List.of());

		public void restore() {
			for (Map.Entry<ModelPart, PartPose> entry : saved) {
				entry.getKey().loadPose(entry.getValue());
			}
		}
	}

	/**
	 * The rotation delta {@link #apply} writes into a bone, expressed in entity
	 * terms (x = pitch, y = yaw, z = roll about the forward axis) rather than raw
	 * Bedrock sample space. The renderer maps the full pose through
	 * {@code Ry(180-yaw)·scale(-1,-1,1)} — a 180° rotation about X — so the
	 * model's +z axis points backward and a positive {@code zRot} rolls about
	 * the back axis, i.e. a negative roll about the forward axis. Every
	 * component is therefore the negation of the sampled degrees, scaled by the
	 * sample weight. Stored values are pre-divided by the merged fade weight;
	 * multiplying back by {@code sample.weight()} restores the effective
	 * per-bone degrees.
	 */
	public static Vector3f renderedRotationDeg(PoseSample sample, String bone) {
		Vector3f v = sample.rotationDeg().get(bone);
		if (v == null || sample.weight() <= 0f) {
			return new Vector3f();
		}
		return new Vector3f(-v.x, -v.y, -v.z).mul(sample.weight());
	}

	/**
	 * Clears the vanilla rotation of every limb the sample drives so the
	 * clip's authored pose shows through instead of overlaying residual
	 * swing (e.g. dangling legs under a flight clip). The head is skipped —
	 * it keeps tracking the view while the clip adds its authored offset.
	 * Run before {@link #apply}; the returned Restoration still reverts only
	 * the apply deltas, and vanilla rewrites the pose next frame anyway.
	 */
	public static void resetDrivenLimbs(HumanoidModel<?> model, PoseSample sample) {
		for (String bone : sample.rotationDeg().keySet()) {
			resetPart(model, bone);
		}
		for (String bone : sample.offsetPx().keySet()) {
			resetPart(model, bone);
		}
	}

	private static void resetPart(HumanoidModel<?> model, String bone) {
		if (HEAD_BONE.equals(bone)) {
			return;
		}
		ModelPart part = part(model, bone);
		if (part != null) {
			part.xRot = 0f;
			part.yRot = 0f;
			part.zRot = 0f;
		}
	}

	public static Restoration apply(HumanoidModel<?> model, PoseSample sample) {
		float weight = sample.weight();
		if (weight <= 0f) {
			return Restoration.NONE;
		}
		Map<String, ModelPart> touched = new LinkedHashMap<>();
		for (String bone : sample.rotationDeg().keySet()) {
			touched.put(bone, part(model, bone));
		}
		for (String bone : sample.offsetPx().keySet()) {
			touched.put(bone, part(model, bone));
		}
		List<Map.Entry<ModelPart, PartPose>> saved = new ArrayList<>();
		for (ModelPart part : touched.values()) {
			if (part != null) {
				saved.add(Map.entry(part, part.storePose()));
			}
		}
		for (Map.Entry<String, Vector3f> entry : sample.rotationDeg().entrySet()) {
			ModelPart part = touched.get(entry.getKey());
			if (part == null) {
				continue;
			}
			Vector3f v = entry.getValue();
			part.xRot += -v.x * DEG_TO_RAD * weight;
			part.yRot += -v.y * DEG_TO_RAD * weight;
			part.zRot += v.z * DEG_TO_RAD * weight;
		}
		for (Map.Entry<String, Vector3f> entry : sample.offsetPx().entrySet()) {
			ModelPart part = touched.get(entry.getKey());
			if (part == null) {
				continue;
			}
			Vector3f p = entry.getValue();
			part.x += -p.x * weight;
			part.y += -p.y * weight;
			part.z += p.z * weight;
		}
		return new Restoration(saved);
	}

	private static ModelPart part(HumanoidModel<?> model, String bone) {
		return switch (bone) {
			case "head" -> model.head;
			case "body" -> model.body;
			case "right_arm" -> model.rightArm;
			case "left_arm" -> model.leftArm;
			case "right_leg" -> model.rightLeg;
			case "left_leg" -> model.leftLeg;
			default -> null;
		};
	}
}
