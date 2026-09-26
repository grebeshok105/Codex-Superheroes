package io.github.grebeshok105.codex.client;

import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.core.hero.HeroTheme;
import io.github.grebeshok105.codex.core.hero.Heroes;
import io.github.grebeshok105.codex.client.hud.ScreenFlashHud;
import io.github.grebeshok105.codex.core.transform.HeroData;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public final class ClientHeroState {
	private static volatile HeroData data = HeroData.EMPTY;
	private static volatile List<ResourceLocation> abilities = List.of();

	static {
		ClientSessionState.register(ClientHeroState::reset);
	}

	private ClientHeroState() {
	}

	public static HeroData data() {
		return data;
	}

	public static List<ResourceLocation> abilities() {
		return abilities;
	}

	public static ResourceLocation heroId() {
		return data.hasHero() ? data.heroId() : null;
	}

	public static synchronized void update(HeroData newData) {
		boolean hadHero = data.hasHero();
		data = newData;
		if (newData.hasHero()) {
			Hero hero = Heroes.get(newData.heroId());
			abilities = hero != null ? hero.getAbilities() : List.of();
		} else {
			abilities = List.of();
		}
		if (hadHero != newData.hasHero()) {
			ScreenFlashHud.trigger(newData.hasHero());
		}
	}

	public static synchronized void updateResources(float energy, float mana) {
		data = data.withResources(energy, mana);
	}

	public static float energyMax() {
		if (!data.hasHero()) {
			return 1f;
		}
		Hero hero = Heroes.get(data.heroId());
		return hero != null ? hero.getEnergyMax() : 1f;
	}

	public static float manaMax() {
		if (!data.hasHero()) {
			return 1f;
		}
		Hero hero = Heroes.get(data.heroId());
		return hero != null ? hero.getManaMax() : 1f;
	}

	public static HeroTheme theme() {
		if (!data.hasHero()) {
			return HeroTheme.DEFAULT;
		}
		Hero hero = Heroes.get(data.heroId());
		return hero != null ? hero.getTheme() : HeroTheme.DEFAULT;
	}

	/** Drop all session state (registered with {@code ClientSessionState}). */
	public static void reset() {
		data = HeroData.EMPTY;
		abilities = List.of();
	}
}
