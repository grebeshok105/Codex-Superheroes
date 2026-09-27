package io.github.grebeshok105.codex.client.core.text;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Client-side text obfuscation layers — the generic mechanism behind Pandora's House-of-Vanity
 * cipher. A hero client module registers a layer; the {@code Font} mixin funnels every drawn
 * {@code String}/{@code FormattedCharSequence} through each active layer in registration order.
 * With no active layer the text passes through untouched.
 */
public final class TextObfuscationLayers {
	/** One obfuscation rule: an activity gate plus the text transform it applies. */
	public interface Layer {
		boolean active();

		String apply(String text);

		FormattedCharSequence apply(FormattedCharSequence text);
	}

	private static final Map<ResourceLocation, Layer> LAYERS = new LinkedHashMap<>();

	private TextObfuscationLayers() {
	}

	public static void register(ResourceLocation id, Layer layer) {
		LAYERS.put(id, layer);
	}

	public static String apply(String text) {
		String result = text;
		for (Layer layer : LAYERS.values()) {
			if (layer.active()) {
				result = layer.apply(result);
			}
		}
		return result;
	}

	public static FormattedCharSequence apply(FormattedCharSequence text) {
		FormattedCharSequence result = text;
		for (Layer layer : LAYERS.values()) {
			if (layer.active()) {
				result = layer.apply(result);
			}
		}
		return result;
	}
}
