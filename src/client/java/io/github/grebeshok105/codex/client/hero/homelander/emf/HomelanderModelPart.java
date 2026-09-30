package io.github.grebeshok105.codex.client.hero.homelander.emf;

import net.minecraft.client.model.geom.ModelPart;
import traben.entity_model_features.models.parts.EMFModelPart;

import java.util.List;
import java.util.Map;

/**
 * The model's animatable part: an {@link EMFModelPart} so EMF's
 * {@code bone.rx/ry/rz/tx/ty/tz/sx/sy/sz/visible} appliers drive it through
 * the generated expressions. Passing {@code null} for {@code EMFModelPartRoot}
 * is deliberate — we are not an EMF-managed entity model, and EMF's own render
 * path never dereferences the root when {@link #textureOverride} is set for
 * prop bones or while {@code render()} dispatches to the vanilla path.
 */
public final class HomelanderModelPart extends EMFModelPart {
	HomelanderModelPart(List<Cube> cubes, Map<String, ModelPart> children) {
		super(cubes, children, null);
	}

	@Override
	protected float[] debugBoxColor() {
		return new float[]{0.35f, 0.6f, 1f, 1f};
	}
}
