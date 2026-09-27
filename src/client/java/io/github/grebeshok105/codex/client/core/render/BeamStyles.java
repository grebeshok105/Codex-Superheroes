package io.github.grebeshok105.codex.client.core.render;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/** Registry of beam styles, populated by hero client modules at bootstrap. */
public final class BeamStyles {
	private static final Map<ResourceLocation, BeamStyle> STYLES = new HashMap<>();

	private BeamStyles() {
	}

	public static void register(BeamStyle style) {
		STYLES.put(style.id(), style);
	}

	@Nullable
	public static BeamStyle get(ResourceLocation id) {
		return STYLES.get(id);
	}
}
