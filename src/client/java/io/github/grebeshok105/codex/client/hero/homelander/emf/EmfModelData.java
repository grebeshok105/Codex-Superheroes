package io.github.grebeshok105.codex.client.hero.homelander.emf;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Parsed {@code codex-emf-model/1} model (assets/superheroes/emf/homelander/
 * model.json). Blockbench-authored in y-up model space — the runtime converts
 * to vanilla y-down space in {@link HomelanderEmfModel}; this class keeps the
 * raw authored values.
 */
public record EmfModelData(
		Map<String, Bone> bones,
		List<String> boneOrder,
		Map<Integer, ResourceLocation> textures,
		int textureWidth,
		int textureHeight,
		Set<String> propBones) {

	public static final String FORMAT = "codex-emf-model/1";

	public record Bone(String name, @Nullable String parent, float[] pivot, List<Cube> cubes) {
	}

	/** A per-face-uv cube: {@code uv} is the texture rect [u1,v1,u2,v2]. */
	public record Cube(float[] origin, float[] size, Map<String, Face> faces) {
	}

	public record Face(float u1, float v1, float u2, float v2, int texture) {
	}

	/** Returns the parsed model, or {@code null} when the format tag differs. */
	public static @Nullable EmfModelData parse(JsonObject json) {
		if (!FORMAT.equals(json.has("format") ? json.get("format").getAsString() : null)) {
			return null;
		}
		Map<Integer, ResourceLocation> textures = new LinkedHashMap<>();
		for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("textures").entrySet()) {
			textures.put(Integer.parseInt(entry.getKey()),
					ResourceLocation.parse(entry.getValue().getAsString()));
		}
		com.google.gson.JsonArray size = json.getAsJsonArray("texture_size");
		Set<String> propBones = json.has("prop_bones")
				? json.getAsJsonArray("prop_bones").asList().stream()
						.map(JsonElement::getAsString).collect(java.util.stream.Collectors.toSet())
				: Set.of();
		Map<String, Bone> bones = new LinkedHashMap<>();
		List<String> boneOrder = new ArrayList<>();
		for (JsonElement element : json.getAsJsonArray("bones")) {
			JsonObject b = element.getAsJsonObject();
			String name = b.get("name").getAsString();
			String parent = b.has("parent") && !b.get("parent").isJsonNull()
					? b.get("parent").getAsString() : null;
			List<Cube> cubes = new ArrayList<>();
			for (JsonElement cubeEl : b.getAsJsonArray("cubes")) {
				JsonObject c = cubeEl.getAsJsonObject();
				Map<String, Face> faces = new LinkedHashMap<>();
				for (Map.Entry<String, JsonElement> faceEntry : c.getAsJsonObject("faces").entrySet()) {
					JsonObject f = faceEntry.getValue().getAsJsonObject();
					com.google.gson.JsonArray uv = f.getAsJsonArray("uv");
					faces.put(faceEntry.getKey(), new Face(
							uv.get(0).getAsFloat(), uv.get(1).getAsFloat(),
							uv.get(2).getAsFloat(), uv.get(3).getAsFloat(),
							f.get("texture").getAsInt()));
				}
				cubes.add(new Cube(vec3(c.getAsJsonArray("origin")), vec3(c.getAsJsonArray("size")), faces));
			}
			Bone bone = new Bone(name, parent, vec3(b.getAsJsonArray("pivot")), cubes);
			bones.put(name, bone);
			boneOrder.add(name);
		}
		return new EmfModelData(bones, boneOrder, textures,
				size.get(0).getAsInt(), size.get(1).getAsInt(), propBones);
	}

	private static float[] vec3(com.google.gson.JsonArray array) {
		return new float[]{
				array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat()};
	}
}
