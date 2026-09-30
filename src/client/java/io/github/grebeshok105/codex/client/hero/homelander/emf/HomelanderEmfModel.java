package io.github.grebeshok105.codex.client.hero.homelander.emf;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import traben.entity_model_features.models.parts.EMFModelPart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The Homelander player model: the {@link EmfModelData} bone tree converted to
 * vanilla space and realised as {@link HomelanderModelPart}s, parented the same
 * way as the authored hierarchy. Built once per resource reload and shared by
 * every rendered Homelander player (EMF writes per-entity state each frame via
 * the animation handler, same as it does for vanilla entity models).
 *
 * <p>{@link #mainTexture()} is the render type's texture; parts whose cubes
 * sample a different texture index (the milk prop palette) get their
 * {@code textureOverride} set so EMF swaps the texture for just those bones.
 */
public final class HomelanderEmfModel {
	private final Map<String, EMFModelPart> parts;
	private final EMFModelPart root;
	private final ResourceLocation mainTexture;

	private HomelanderEmfModel(Map<String, EMFModelPart> parts, EMFModelPart root,
			ResourceLocation mainTexture) {
		this.parts = parts;
		this.root = root;
		this.mainTexture = mainTexture;
	}

	public static HomelanderEmfModel build(EmfModelData data) {
		Map<String, EMFModelPart> parts = new LinkedHashMap<>();
		Map<String, Map<String, ModelPart>> childrenMaps = new LinkedHashMap<>();
		for (String name : data.boneOrder()) {
			EmfModelData.Bone bone = data.bones().get(name);
			List<ModelPart.Cube> cubes = new ArrayList<>();
			float[] pivot = EmfModelConversion.pivot(bone.pivot());
			boolean usesOverrideTexture = false;
			for (EmfModelData.Cube cube : bone.cubes()) {
				float[] min = EmfModelConversion.cubeMin(cube.origin(), cube.size(), pivot);
				Map<String, EmfModelData.Face> faces = new LinkedHashMap<>();
				for (Map.Entry<String, EmfModelData.Face> face : cube.faces().entrySet()) {
					// Texture 0 is the layer's base texture; higher indices ride
					// EMF's per-part textureOverride on this bone.
					if (face.getValue().texture() > 0) {
						usesOverrideTexture = true;
					}
					faces.put(EmfModelConversion.faceName(face.getKey()), face.getValue());
				}
				cubes.add(new UvCube(min[0], min[1], min[2],
						cube.size()[0], cube.size()[1], cube.size()[2],
						faces, data.textureWidth(), data.textureHeight()));
			}
			Map<String, ModelPart> childParts = new LinkedHashMap<>();
			childrenMaps.put(name, childParts);
			HomelanderModelPart part = new HomelanderModelPart(cubes, childParts);
			float[] rel = bone.parent() == null
					? pivot
					: EmfModelConversion.relativePivot(bone.pivot(),
							data.bones().get(bone.parent()).pivot());
			part.setPos(rel[0], rel[1], rel[2]);
			if (usesOverrideTexture && data.textures().containsKey(1)) {
				part.textureOverride = data.textures().get(1);
			}
			parts.put(name, part);
		}
		for (String name : data.boneOrder()) {
			EmfModelData.Bone bone = data.bones().get(name);
			if (bone.parent() != null && childrenMaps.containsKey(bone.parent())) {
				childrenMaps.get(bone.parent()).put(name, parts.get(name));
			}
		}
		EMFModelPart root = parts.get("root");
		if (root == null) {
			throw new IllegalStateException("emf model has no 'root' bone");
		}
		return new HomelanderEmfModel(parts, root,
				data.textures().getOrDefault(0, null));
	}

	/** Bone name → animatable part, for the EMF animation context map. */
	public Map<String, EMFModelPart> parts() {
		return Collections.unmodifiableMap(parts);
	}

	public @Nullable EMFModelPart part(String boneName) {
		return parts.get(boneName);
	}

	/** The part hierarchy's entry point — render this to draw the whole model. */
	public EMFModelPart root() {
		return root;
	}

	public @Nullable ResourceLocation mainTexture() {
		return mainTexture;
	}
}
