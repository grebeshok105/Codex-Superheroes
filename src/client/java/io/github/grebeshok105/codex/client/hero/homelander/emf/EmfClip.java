package io.github.grebeshok105.codex.client.hero.homelander.emf;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Parsed {@code emf-keyframe-clip/1}: uniform {@code sample_fps} tracks per
 * bone and channel. Values stay in authored (Blockbench y-up) units —
 * {@code rotation} in degrees, {@code position} a delta in model pixels,
 * {@code scale} a multiplier. The expression builder converts to vanilla
 * space when baking literals.
 */
public record EmfClip(
		ResourceLocation id,
		int frameCount,
		int sampleFps,
		boolean loop,
		boolean referenceOnly,
		Map<String, Map<Channel, float[][]>> tracks) {

	public static final String FORMAT = "emf-keyframe-clip/1";

	public enum Channel {
		ROTATION, POSITION, SCALE;

		public static @Nullable Channel of(String key) {
			return switch (key) {
				case "rotation" -> ROTATION;
				case "position" -> POSITION;
				case "scale" -> SCALE;
				default -> null;
			};
		}
	}

	/** The var-name stem used in {@code var.<name>_w} / {@code var.<name>_t}. */
	public String varName() {
		String path = id.getPath();
		return path.substring(path.lastIndexOf('/') + 1);
	}

	/** Parses a clip file; {@code null} for wrong format or reference-only clips. */
	public static @Nullable EmfClip parse(ResourceLocation id, JsonObject json) {
		if (!FORMAT.equals(json.has("format") ? json.get("format").getAsString() : null)) {
			return null;
		}
		if (json.has("reference_only") && json.get("reference_only").getAsBoolean()) {
			return null;
		}
		int frameCount = json.get("frame_count").getAsInt();
		int sampleFps = json.has("sample_fps") ? json.get("sample_fps").getAsInt() : 60;
		boolean loop = json.has("loop") && json.get("loop").getAsBoolean();
		Map<String, Map<Channel, float[][]>> tracks = new LinkedHashMap<>();
		for (Map.Entry<String, JsonElement> boneEntry : json.getAsJsonObject("bones").entrySet()) {
			Map<Channel, float[][]> channels = new EnumMap<>(Channel.class);
			for (Map.Entry<String, JsonElement> channelEntry : boneEntry.getValue()
					.getAsJsonObject().entrySet()) {
				Channel channel = Channel.of(channelEntry.getKey());
				if (channel == null) {
					continue;
				}
				com.google.gson.JsonArray frames = channelEntry.getValue().getAsJsonArray();
				float[][] values = new float[frames.size()][3];
				for (int f = 0; f < frames.size(); f++) {
					com.google.gson.JsonArray v = frames.get(f).getAsJsonArray();
					values[f][0] = v.get(0).getAsFloat();
					values[f][1] = v.get(1).getAsFloat();
					values[f][2] = v.get(2).getAsFloat();
				}
				channels.put(channel, values);
			}
			if (!channels.isEmpty()) {
				tracks.put(boneEntry.getKey(), channels);
			}
		}
		return new EmfClip(id, frameCount, sampleFps, loop, false, tracks);
	}
}
