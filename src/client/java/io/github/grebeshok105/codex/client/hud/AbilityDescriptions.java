package io.github.grebeshok105.codex.client.hud;

import io.github.grebeshok105.codex.core.ability.Ability;
import io.github.grebeshok105.codex.core.ability.AbilityRegistry;
import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.core.hero.Heroes;
import net.minecraft.resources.ResourceLocation;

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

	private AbilityDescriptions() {
	}

	public static int passiveCount(ResourceLocation heroId) {
		Hero hero = Heroes.get(heroId);
		return hero == null ? 0 : hero.getPassiveGlyphs().size();
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
