package io.github.grebeshok105.codex.client.core.hud;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/** Hero panel sections registered by client modules, keyed by hero id. */
public final class HeroPanelSections {
	private static final Map<ResourceLocation, HeroPanelSection> BY_HERO = new HashMap<>();

	private HeroPanelSections() {
	}

	public static void register(ResourceLocation heroId, HeroPanelSection section) {
		BY_HERO.put(heroId, section);
	}

	@Nullable
	public static HeroPanelSection forHero(ResourceLocation heroId) {
		return BY_HERO.get(heroId);
	}
}
