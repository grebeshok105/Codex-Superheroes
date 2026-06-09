package com.example.superheroes.client.hud;

import com.example.superheroes.ability.Ability;
import com.example.superheroes.ability.AbilityRegistry;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public final class AbilityDescriptions {
	public enum Kind {
		PASSIVE("P"),
		TOGGLE("T"),
		ACTIVE("A");

		private final String badge;

		Kind(String badge) {
			this.badge = badge;
		}

		public String badge() {
			return badge;
		}
	}

	private static final Map<String, Integer> HERO_PASSIVE_COUNT = new HashMap<>();

	static {
		HERO_PASSIVE_COUNT.put("homelander", 3);
		HERO_PASSIVE_COUNT.put("iron_man", 3);
		HERO_PASSIVE_COUNT.put("regulus", 4);
		HERO_PASSIVE_COUNT.put("sung_jinwoo", 4);
		HERO_PASSIVE_COUNT.put("doomsday", 5);
		HERO_PASSIVE_COUNT.put("goku", 3);
		HERO_PASSIVE_COUNT.put("naruto", 3);
		HERO_PASSIVE_COUNT.put("captain_america", 3);
		HERO_PASSIVE_COUNT.put("kratos", 4);
		HERO_PASSIVE_COUNT.put("loki", 3);
		HERO_PASSIVE_COUNT.put("thanos", 4);
		HERO_PASSIVE_COUNT.put("reinhard", 6);
		HERO_PASSIVE_COUNT.put("raiden_shogun", 0);
		HERO_PASSIVE_COUNT.put("invincible", 4);
		HERO_PASSIVE_COUNT.put("omniman", 4);
		HERO_PASSIVE_COUNT.put("kazuha", 3);
		HERO_PASSIVE_COUNT.put("scaramouche", 3);
		HERO_PASSIVE_COUNT.put("battle_beast", 3);
		HERO_PASSIVE_COUNT.put("rem", 3);
		HERO_PASSIVE_COUNT.put("a_train", 3);
	}

	private AbilityDescriptions() {
	}

	public static int passiveCount(ResourceLocation heroId) {
		return HERO_PASSIVE_COUNT.getOrDefault(heroId.getPath(), 0);
	}

	public static String passiveKey(ResourceLocation heroId, int index) {
		return "hero." + heroId.getNamespace() + "." + heroId.getPath() + ".passive." + index;
	}

	public static String nameKey(ResourceLocation abilityId) {
		return "ability." + abilityId.getNamespace() + "." + abilityId.getPath();
	}

	public static String descKey(ResourceLocation abilityId) {
		return "ability." + abilityId.getNamespace() + "." + abilityId.getPath() + ".desc";
	}

	public static Kind kindOf(ResourceLocation abilityId) {
		Ability ability = AbilityRegistry.get(abilityId);
		if (ability == null) {
			return Kind.ACTIVE;
		}
		if (ability.isToggle()) {
			return Kind.TOGGLE;
		}
		return Kind.ACTIVE;
	}

	public static String costLabel(ResourceLocation abilityId) {
		Ability ability = AbilityRegistry.get(abilityId);
		if (ability == null) {
			return "";
		}
		if (ability.isToggle()) {
			float perSec = ability.costPerTick() * 20f;
			if (perSec <= 0f) {
				return "";
			}
			return formatFloat(perSec) + "/s";
		}
		float cost = ability.costOnActivate();
		if (cost <= 0f) {
			return "";
		}
		return formatFloat(cost);
	}

	private static String formatFloat(float v) {
		if (v == Math.floor(v)) {
			return Integer.toString((int) v);
		}
		return String.format(java.util.Locale.ROOT, "%.1f", v);
	}
}
