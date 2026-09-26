package io.github.grebeshok105.codex.hero;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Heroes {
	private static final Map<ResourceLocation, Hero> REGISTRY = new LinkedHashMap<>();

	private Heroes() {
	}

	public static void register(Hero hero) {
		REGISTRY.put(hero.getId(), hero);
	}

	@Nullable
	public static Hero get(@Nullable ResourceLocation id) {
		return id == null ? null : REGISTRY.get(id);
	}

	public static Map<ResourceLocation, Hero> all() {
		return Collections.unmodifiableMap(REGISTRY);
	}
}
