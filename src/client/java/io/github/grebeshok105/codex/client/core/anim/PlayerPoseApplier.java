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
