package io.github.grebeshok105.codex.core.hero;

public record HeroHudConfig(
		String energyName,
		EnergyIconType energyIcon,
		boolean hasUltimate,
		String ultimateName
) {
	public enum EnergyIconType {
		SUN,
		LIGHTNING,
		FLAME,
		SKULL,
		SHADOW,
		REACTOR,
		SHIELD,
		COSMIC,
		SWORD,
		MAGIC,
		LION,
		FIST,
		LEAF,
		SPIRAL,
		ICE,
		BEAST,
		GENERIC
	}

	public static final HeroHudConfig DEFAULT = new HeroHudConfig("hud.superheroes.energy.generic", EnergyIconType.GENERIC, false, null);
}
