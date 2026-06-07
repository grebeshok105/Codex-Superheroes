package com.example.superheroes.effect;

import com.example.superheroes.SuperheroesMod;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.player.Player;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Soft-dep bridge that revokes hero state on snapped victims for known third-party
 * superhero mods. Runtime-only reflection so we don't depend on those mods at compile time.
 *
 * Currently bridged: falbiks_heroes (CCA-based, com.falbiks.heroes.core.component.ModComponents#HERO,
 * HeroComponent#clearHero()).
 */
public final class ThanosCrossModSnapHook {
	private static final boolean FALBIKS_AVAILABLE;
	private static final Object FALBIKS_HERO_KEY;
	private static final MethodHandle FALBIKS_KEY_GET;
	private static final MethodHandle FALBIKS_CLEAR_HERO;

	static {
		boolean ok = false;
		Object key = null;
		MethodHandle get = null;
		MethodHandle clear = null;
		if (FabricLoader.getInstance().isModLoaded("falbiks_heroes")) {
			try {
				Class<?> components = Class.forName("com.falbiks.heroes.core.component.ModComponents");
				Field heroField = components.getDeclaredField("HERO");
				heroField.setAccessible(true);
				key = heroField.get(null);
				if (key != null) {
					Method getMethod = key.getClass().getMethod("get", Object.class);
					Class<?> heroComponent = Class.forName("com.falbiks.heroes.core.component.HeroComponent");
					Method clearMethod = heroComponent.getDeclaredMethod("clearHero");
					MethodHandles.Lookup lookup = MethodHandles.lookup();
					get = lookup.unreflect(getMethod);
					clear = lookup.unreflect(clearMethod);
					ok = true;
				}
			} catch (Throwable t) {
				SuperheroesMod.LOGGER.warn("falbiks_heroes detected but cross-mod snap hook init failed: {}", t.toString());
			}
		}
		FALBIKS_AVAILABLE = ok;
		FALBIKS_HERO_KEY = key;
		FALBIKS_KEY_GET = get;
		FALBIKS_CLEAR_HERO = clear;
	}

	private ThanosCrossModSnapHook() {
	}

	public static void revokeHero(Player victim) {
		if (FALBIKS_AVAILABLE) {
			try {
				Object component = FALBIKS_KEY_GET.invoke(FALBIKS_HERO_KEY, victim);
				if (component != null) {
					FALBIKS_CLEAR_HERO.invoke(component);
				}
			} catch (Throwable ignored) {
			}
		}
	}
}
