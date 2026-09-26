package io.github.grebeshok105.codex.client.core.hud;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Radial-menu decorations keyed by ability id. Client modules register via
 * {@code HeroClientContext.abilityDecoration(...)} during bootstrap; {@code RadialMenuHud} calls
 * {@link #render} for each slot so the decorations draw in registration order.
 */
public final class AbilityDecorations {
	private static final Map<ResourceLocation, List<AbilityDecoration>> DECORATIONS = new HashMap<>();

	private AbilityDecorations() {
	}

	public static void register(ResourceLocation abilityId, AbilityDecoration decoration) {
		DECORATIONS.computeIfAbsent(abilityId, id -> new ArrayList<>()).add(decoration);
	}

	/** Renders every decoration registered for {@code abilityId}, in registration order. */
	public static void render(GuiGraphics graphics, ResourceLocation abilityId, int iconCenterX, int iconCenterY, int iconSize) {
		List<AbilityDecoration> decorations = DECORATIONS.get(abilityId);
		if (decorations == null) {
			return;
		}
		for (AbilityDecoration decoration : decorations) {
			decoration.render(graphics, abilityId, iconCenterX, iconCenterY, iconSize);
		}
	}
}
