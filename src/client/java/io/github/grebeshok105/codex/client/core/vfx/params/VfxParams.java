package io.github.grebeshok105.codex.client.core.vfx.params;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-effect tuning read from {@code assets/<ns>/vfx/<path>.json}: float
 * numbers plus packed {@code #RRGGBB}/{@code #AARRGGBB} colors. Parsing is
 * forgiving — anything malformed is ignored and reads fall back to the
 * caller's default, so a bad tuning file degrades instead of crashing.
 */
public record VfxParams(Map<String, Float> numbers, Map<String, Integer> colors) {
	public static final VfxParams EMPTY = new VfxParams(Map.of(), Map.of());

	public float number(String key, float fallback) {
		return numbers.getOrDefault(key, fallback);
	}

	public int color(String key, int fallback) {
		return colors.getOrDefault(key, fallback);
	}

	/**
	 * A copy of these params with {@code overrides} layered on top — overlay
	 * keys win on conflicts, base-only keys survive. Used to specialise a
	 * shared tuning file (e.g. {@code flight/pose}) for one presentation.
	 */
	public VfxParams withOverrides(@Nullable VfxParams overrides) {
		if (overrides == null || overrides.numbers.isEmpty() && overrides.colors.isEmpty()) {
			return this;
		}
		Map<String, Float> numbers = new HashMap<>(this.numbers);
		numbers.putAll(overrides.numbers);
		Map<String, Integer> colors = new HashMap<>(this.colors);
		colors.putAll(overrides.colors);
		return new VfxParams(numbers, colors);
	}

	public static VfxParams parse(JsonObject json) {
		Map<String, Float> numbers = new HashMap<>();
		Map<String, Integer> colors = new HashMap<>();
		for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
			JsonElement value = entry.getValue();
			if (!value.isJsonPrimitive()) {
				continue;
			}
			JsonPrimitive primitive = value.getAsJsonPrimitive();
			if (primitive.isNumber()) {
				numbers.put(entry.getKey(), primitive.getAsFloat());
			} else if (primitive.isString()) {
				long color = parseColor(primitive.getAsString());
				if (color >= 0) {
					colors.put(entry.getKey(), (int) color);
				}
			}
		}
		return new VfxParams(numbers, colors);
	}

	private static long parseColor(String raw) {
		if (raw == null || raw.length() != 7 && raw.length() != 9 || raw.charAt(0) != '#') {
			return -1;
		}
		try {
			long value = Long.parseLong(raw.substring(1), 16);
			return value > 0xFFFFFFFFL ? -1 : value;
		} catch (NumberFormatException e) {
			return -1;
		}
	}
}
