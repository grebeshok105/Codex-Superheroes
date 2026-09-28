package io.github.grebeshok105.codex.client.core.anim;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;

import java.util.Map;

/**
 * Applies a {@link PoseSample} on top of the pose {@code setupAnim} produced, using the
 * GeckoLib Bedrock→Java sign convention: {@code xRot += -rad(x)}, {@code yRot += -rad(y)},
 * {@code zRot += rad(z)}; offsets {@code x += -px.x}, {@code y += -px.y},
 * {@code z += px.z}; every delta scaled by the sample weight.
 */
public final class PlayerPoseApplier {
	private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

	private PlayerPoseApplier() {
	}

	public static void apply(HumanoidModel<?> model, PoseSample sample) {
		float weight = sample.weight();
		if (weight <= 0f) {
			return;
		}
		for (Map.Entry<String, Vector3f> entry : sample.rotationDeg().entrySet()) {
			ModelPart part = part(model, entry.getKey());
			if (part == null) {
				continue;
			}
			Vector3f v = entry.getValue();
			part.xRot += -v.x * DEG_TO_RAD * weight;
			part.yRot += -v.y * DEG_TO_RAD * weight;
			part.zRot += v.z * DEG_TO_RAD * weight;
		}
		for (Map.Entry<String, Vector3f> entry : sample.offsetPx().entrySet()) {
			ModelPart part = part(model, entry.getKey());
			if (part == null) {
				continue;
			}
			Vector3f p = entry.getValue();
			part.x += -p.x * weight;
			part.y += -p.y * weight;
			part.z += p.z * weight;
		}
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
