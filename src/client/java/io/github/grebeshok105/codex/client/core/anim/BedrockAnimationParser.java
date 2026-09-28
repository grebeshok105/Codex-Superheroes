package io.github.grebeshok105.codex.client.core.anim;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;

import net.minecraft.resources.ResourceLocation;

/**
 * Parses Blockbench Bedrock {@code .animation.json} files into {@link AnimationClip}s.
 * Tolerates the value shapes the OMP pipeline ships — {@code {"vector": [x,y,z]}} with an
 * optional {@code easing}, bare {@code [x,y,z]} arrays, and {@code {pre, post, lerp_mode}}
 * objects — plus channel-level single-keyframe shorthand. Any Molang (string) value marks
 * the clip unplayable: it is skipped with one warning, never thrown. Bones outside the six
 * vanilla player parts are ignored with a warning; the clip survives.
 */
public final class BedrockAnimationParser {
	private static final Set<String> BONES =
			Set.of("head", "body", "right_arm", "left_arm", "right_leg", "left_leg");
	private static final List<String> EVENT_TIMELINES =
			List.of("sound_effects", "particle_effects", "events");
	private static final String ANIMATION_PREFIX = "animation.";
	private static final String HOLD_ON_LAST_FRAME = "hold_on_last_frame";

	/** Marker returned by value parsing when a Molang expression is found. */
	private static final Keyframes MOLANG = null;

	private BedrockAnimationParser() {
	}

	public static List<AnimationClip> parse(JsonObject root, Consumer<String> warn) {
		List<AnimationClip> clips = new ArrayList<>();
		JsonElement animations = root.get("animations");
		if (animations == null || !animations.isJsonObject()) {
			warn.accept("no 'animations' object");
			return clips;
		}
		for (Map.Entry<String, JsonElement> entry : animations.getAsJsonObject().entrySet()) {
			ResourceLocation id = clipId(entry.getKey());
			if (id == null) {
				warn.accept("animation key '" + entry.getKey()
						+ "' is not an animation.<ns>.<hero>.<clip> name — skipped");
				continue;
			}
			if (!entry.getValue().isJsonObject()) {
				warn.accept(id + ": clip body is not an object — skipped");
				continue;
			}
			AnimationClip clip = parseClip(id, entry.getValue().getAsJsonObject(), warn);
			if (clip != null) {
				clips.add(clip);
			}
		}
		return clips;
	}

	/** {@code animation.superheroes.homelander.clap} → {@code superheroes:homelander/clap}. */
	private static ResourceLocation clipId(String name) {
		if (!name.startsWith(ANIMATION_PREFIX)) {
			return null;
		}
		String rest = name.substring(ANIMATION_PREFIX.length());
		int nsEnd = rest.indexOf('.');
		if (nsEnd <= 0 || nsEnd >= rest.length() - 1) {
			return null;
		}
		String namespace = rest.substring(0, nsEnd);
		String path = rest.substring(nsEnd + 1).replaceFirst("\\.", "/");
		return ResourceLocation.tryParse(namespace + ":" + path);
	}

	private static AnimationClip parseClip(ResourceLocation id, JsonObject anim, Consumer<String> warn) {
		AnimationClip.Loop loop = parseLoop(id, anim.get("loop"), warn);

		Map<String, BoneTrack> bones = new HashMap<>();
		float maxKeyTime = 0f;
		JsonElement bonesEl = anim.get("bones");
		if (bonesEl != null && bonesEl.isJsonObject()) {
			for (Map.Entry<String, JsonElement> bone : bonesEl.getAsJsonObject().entrySet()) {
				String name = bone.getKey();
				if (!BONES.contains(name)) {
					warn.accept(id + ": unknown bone '" + name + "' ignored");
					continue;
				}
				if (!bone.getValue().isJsonObject()) {
					warn.accept(id + ": bone '" + name + "' is not an object — ignored");
					continue;
				}
				JsonObject channels = bone.getValue().getAsJsonObject();
				Keyframes rotation = parseChannel(id, name, channels.get("rotation"), warn);
				if (rotation == MOLANG) {
					warn.accept(id + ": molang value in bone '" + name + "' — clip skipped");
					return null;
				}
				Keyframes position = parseChannel(id, name, channels.get("position"), warn);
				if (position == MOLANG) {
					warn.accept(id + ": molang value in bone '" + name + "' — clip skipped");
					return null;
				}
				if (channels.has("scale")) {
					warn.accept(id + ": bone '" + name + "' scale channel ignored");
				}
				maxKeyTime = Math.max(maxKeyTime,
						Math.max(rotation.endSeconds(), position.endSeconds()));
				bones.put(name, new BoneTrack(rotation, position));
			}
		}

		float length = anim.has("animation_length") && anim.get("animation_length").isJsonPrimitive()
				&& anim.get("animation_length").getAsJsonPrimitive().isNumber()
				? anim.get("animation_length").getAsFloat()
				: maxKeyTime;
		return new AnimationClip(id, length, loop, bones, eventTimes(anim));
	}

	private static AnimationClip.Loop parseLoop(ResourceLocation id, JsonElement loop,
			Consumer<String> warn) {
		if (loop == null || loop.isJsonNull()) {
			return AnimationClip.Loop.OFF;
		}
		if (loop.isJsonPrimitive()) {
			if (loop.getAsJsonPrimitive().isBoolean()) {
				return loop.getAsBoolean() ? AnimationClip.Loop.WRAP : AnimationClip.Loop.OFF;
			}
			if (loop.getAsJsonPrimitive().isString()
					&& loop.getAsString().equals(HOLD_ON_LAST_FRAME)) {
				return AnimationClip.Loop.HOLD;
			}
		}
		warn.accept(id + ": unsupported loop value " + loop + " — treated as non-loop");
		return AnimationClip.Loop.OFF;
	}

	/**
	 * A channel is either a timeline {@code {"<seconds>": <value>, ...}} or a single-keyframe
	 * shorthand ({@code [x,y,z]} or {@code {pre/post/vector/...}} at {@code t=0}).
	 * Returns {@link #MOLANG} when a string (Molang) value is found.
	 */
	private static Keyframes parseChannel(ResourceLocation id, String bone, JsonElement channel,
			Consumer<String> warn) {
		if (channel == null) {
			return Keyframes.EMPTY;
		}
		if (channel.isJsonPrimitive() && channel.getAsJsonPrimitive().isString()) {
			return MOLANG;
		}
		if (channel.isJsonArray() || isKeyframeObject(channel)) {
			Keyframes.Key key = parseKey(id, bone, 0f, channel, warn);
			if (key == null) {
				return hasStringValue(channel) ? MOLANG : Keyframes.EMPTY;
			}
			return new Keyframes(List.of(key));
		}
		if (!channel.isJsonObject()) {
			warn.accept(id + ": bone '" + bone + "' channel " + channel + " ignored");
			return Keyframes.EMPTY;
		}
		List<Keyframes.Key> keys = new ArrayList<>();
		for (Map.Entry<Float, JsonElement> at : sortedTimeline(channel.getAsJsonObject())) {
			Keyframes.Key key = parseKey(id, bone, at.getKey(), at.getValue(), warn);
			if (key == null) {
				if (hasStringValue(at.getValue())) {
					return MOLANG;
				}
				warn.accept(id + ": malformed keyframe in bone '" + bone + "' at "
						+ at.getKey() + " s skipped");
				continue;
			}
			keys.add(key);
		}
		return new Keyframes(keys);
	}

	/** True when the object IS a keyframe ({@code vector}/{@code pre}/{@code post}) rather than a timeline. */
	private static boolean isKeyframeObject(JsonElement el) {
		return el.isJsonObject() && (el.getAsJsonObject().has("vector")
				|| el.getAsJsonObject().has("pre") || el.getAsJsonObject().has("post"));
	}

	private static List<Map.Entry<Float, JsonElement>> sortedTimeline(JsonObject timeline) {
		TreeMap<Float, JsonElement> sorted = new TreeMap<>();
		for (Map.Entry<String, JsonElement> entry : timeline.entrySet()) {
			try {
				sorted.put(Float.parseFloat(entry.getKey()), entry.getValue());
			} catch (NumberFormatException ignored) {
				// a non-numeric timeline key cannot be a time — dropped with the entry
			}
		}
		return List.copyOf(sorted.entrySet());
	}

	/** One keyframe value; {@code null} marks a Molang (string) component. */
	private static Keyframes.Key parseKey(ResourceLocation id, String bone, float t,
			JsonElement value, Consumer<String> warn) {
		Vector3f vector = null;
		Keyframes.Easing easing = Keyframes.Easing.LINEAR;
		Keyframes.Lerp lerp = Keyframes.Lerp.LINEAR;
		if (value.isJsonObject()) {
			JsonObject obj = value.getAsJsonObject();
			JsonElement vectorEl = obj.has("post") ? obj.get("post")
					: obj.has("pre") ? obj.get("pre") : obj.get("vector");
			vector = vec3(vectorEl);
			if (obj.has("easing") && obj.get("easing").isJsonPrimitive()) {
				easing = Keyframes.Easing.byName(obj.get("easing").getAsString());
				if (easing == null) {
					warn.accept(id + ": bone '" + bone + "' easing '"
							+ obj.get("easing").getAsString() + "' unknown — linear");
					easing = Keyframes.Easing.LINEAR;
				}
			}
			if (obj.has("lerp_mode")) {
				JsonElement mode = obj.get("lerp_mode");
				if (mode.isJsonPrimitive() && mode.getAsJsonPrimitive().isString()) {
					lerp = Keyframes.Lerp.byName(mode.getAsString());
					if (lerp == null) {
						warn.accept(id + ": bone '" + bone + "' lerp_mode '"
								+ mode.getAsString() + "' unknown — linear");
						lerp = Keyframes.Lerp.LINEAR;
					}
				} else if (mode.isJsonObject()) {
					// Bedrock per-axis lerp objects ({ "catmullrom": [...] }) — the only
					// curved mode we ship is Catmull-Rom.
					lerp = Keyframes.Lerp.CATMULLROM;
				}
			}
		} else {
			vector = vec3(value);
		}
		if (vector == null) {
			return null;
		}
		return new Keyframes.Key(t, vector, easing, lerp);
	}

	/** True when a keyframe value carries a string (Molang) component in its vector payload. */
	private static boolean hasStringValue(JsonElement el) {
		if (el == null) {
			return false;
		}
		if (el.isJsonPrimitive()) {
			return el.getAsJsonPrimitive().isString();
		}
		if (el.isJsonArray()) {
			for (JsonElement component : el.getAsJsonArray()) {
				if (hasStringValue(component)) {
					return true;
				}
			}
			return false;
		}
		if (el.isJsonObject()) {
			JsonObject obj = el.getAsJsonObject();
			for (String field : List.of("vector", "pre", "post")) {
				if (hasStringValue(obj.get(field))) {
					return true;
				}
			}
		}
		return false;
	}

	/** {@code [x,y,z]} or {@code {"vector": [x,y,z]}} → vector; {@code null} on a string component. */
	private static Vector3f vec3(JsonElement el) {
		if (el == null) {
			return null;
		}
		if (el.isJsonObject()) {
			return vec3(el.getAsJsonObject().get("vector"));
		}
		if (!el.isJsonArray() || el.getAsJsonArray().size() < 3) {
			return null;
		}
		float[] out = new float[3];
		for (int i = 0; i < 3; i++) {
			JsonElement component = el.getAsJsonArray().get(i);
			if (!component.isJsonPrimitive() || !component.getAsJsonPrimitive().isNumber()) {
				return null;
			}
			out[i] = component.getAsFloat();
		}
		return new Vector3f(out[0], out[1], out[2]);
	}

	/**
	 * Bedrock timeline maps — {@code {"<seconds>": {"effect"|"name": "<event>"}}} — plus the
	 * name-keyed shorthand {@code {"<event>": <seconds>}} / {@code {"<event>": {"time": s}}}
	 * the spec allows on {@code events}. Absent timelines are not errors.
	 */
	private static Map<String, Float> eventTimes(JsonObject anim) {
		Map<String, Float> times = new HashMap<>();
		for (String key : EVENT_TIMELINES) {
			JsonElement el = anim.get(key);
			if (el == null || !el.isJsonObject()) {
				continue;
			}
			for (Map.Entry<String, JsonElement> entry : el.getAsJsonObject().entrySet()) {
				Float seconds = tryFloat(entry.getKey());
				if (seconds != null) {
					for (String name : eventNames(entry.getValue())) {
						times.put(name, seconds);
					}
				} else {
					Float time = eventTimeValue(entry.getValue());
					if (time != null) {
						times.put(entry.getKey(), time);
					}
				}
			}
		}
		return times;
	}

	private static List<String> eventNames(JsonElement payload) {
		List<String> names = new ArrayList<>();
		List<JsonElement> entries = new ArrayList<>();
		if (payload.isJsonArray()) {
			payload.getAsJsonArray().forEach(entries::add);
		} else {
			entries.add(payload);
		}
		for (JsonElement entry : entries) {
			if (entry.isJsonPrimitive() && entry.getAsJsonPrimitive().isString()) {
				names.add(entry.getAsString());
			} else if (entry.isJsonObject()) {
				JsonObject obj = entry.getAsJsonObject();
				JsonElement name = obj.has("effect") ? obj.get("effect") : obj.get("name");
				if (name != null && name.isJsonPrimitive() && name.getAsJsonPrimitive().isString()) {
					names.add(name.getAsString());
				}
			}
		}
		return names;
	}

	private static Float eventTimeValue(JsonElement payload) {
		if (payload.isJsonPrimitive() && payload.getAsJsonPrimitive().isNumber()) {
			return payload.getAsFloat();
		}
		if (payload.isJsonPrimitive() && payload.getAsJsonPrimitive().isString()) {
			return tryFloat(payload.getAsString());
		}
		if (payload.isJsonObject()) {
			JsonElement time = payload.getAsJsonObject().get("time");
			if (time != null && time.isJsonPrimitive() && time.getAsJsonPrimitive().isNumber()) {
				return time.getAsFloat();
			}
		}
		return null;
	}

	private static Float tryFloat(String text) {
		try {
			return Float.parseFloat(text);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
