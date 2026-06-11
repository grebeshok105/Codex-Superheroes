package com.example.superheroes.hero;

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

	public static final HeroHudConfig HOMELANDER = new HeroHudConfig("hud.superheroes.energy.laser_power", EnergyIconType.LIGHTNING, true, "STUNNING ROAR");
	public static final HeroHudConfig IRON_MAN = new HeroHudConfig("hud.superheroes.energy.arc_reactor", EnergyIconType.REACTOR, true, "IRON LEGION");
	public static final HeroHudConfig GOKU = new HeroHudConfig("hud.superheroes.energy.ki", EnergyIconType.SUN, true, "SPIRIT BOMB");
	public static final HeroHudConfig NARUTO = new HeroHudConfig("hud.superheroes.energy.chakra", EnergyIconType.SPIRAL, true, "BIJUUDAMA");
	public static final HeroHudConfig KRATOS = new HeroHudConfig("hud.superheroes.energy.spartan_rage", EnergyIconType.FLAME, true, "GOD SLAYER");
	public static final HeroHudConfig SUNG_JINWOO = new HeroHudConfig("hud.superheroes.energy.shadow_power", EnergyIconType.SHADOW, true, "MONARCH'S DOMAIN");
	public static final HeroHudConfig DOOMSDAY = new HeroHudConfig("hud.superheroes.energy.rage", EnergyIconType.SKULL, true, "DOOM GRIP");
	public static final HeroHudConfig THANOS = new HeroHudConfig("hud.superheroes.energy.cosmic_power", EnergyIconType.COSMIC, true, "SNAP");
	public static final HeroHudConfig REINHARD = new HeroHudConfig("hud.superheroes.energy.divine_blessing", EnergyIconType.SWORD, false, null);
	public static final HeroHudConfig RAIDEN = new HeroHudConfig("hud.superheroes.energy.electro", EnergyIconType.LIGHTNING, true, "TRANSCENDENCE");
	public static final HeroHudConfig INVINCIBLE = new HeroHudConfig("hud.superheroes.energy.viltrumite_power", EnergyIconType.FIST, true, "GUARDIAN'S BREAKER");
	public static final HeroHudConfig OMNIMAN = new HeroHudConfig("hud.superheroes.energy.viltrumite_power", EnergyIconType.FIST, true, "WORLD BREAKER");
	public static final HeroHudConfig CAPTAIN_AMERICA = new HeroHudConfig("hud.superheroes.energy.super_serum", EnergyIconType.SHIELD, true, "COUNTER STANCE");
	public static final HeroHudConfig LOKI = new HeroHudConfig("hud.superheroes.energy.magic", EnergyIconType.MAGIC, true, "CHAOS BOLT");
	public static final HeroHudConfig REGULUS = new HeroHudConfig("hud.superheroes.energy.lion_heart", EnergyIconType.LION, false, null);
	public static final HeroHudConfig KAZUHA = new HeroHudConfig("hud.superheroes.energy.anemo", EnergyIconType.LEAF, true, "MAPLE STORM");
	public static final HeroHudConfig SCARAMOUCHE = new HeroHudConfig("hud.superheroes.energy.storm_power", EnergyIconType.LIGHTNING, true, "SKYFALL BURST");
	public static final HeroHudConfig BATTLE_BEAST = new HeroHudConfig("hud.superheroes.energy.blood_curse", EnergyIconType.BEAST, true, "BLOODLUST");
	public static final HeroHudConfig REM = new HeroHudConfig("hud.superheroes.energy.oni_power", EnergyIconType.ICE, true, "ONI RAGE");
	public static final HeroHudConfig A_TRAIN = new HeroHudConfig("hud.superheroes.energy.compound_v", EnergyIconType.LIGHTNING, true, "HYPERSPEED");

	public static final HeroHudConfig DEFAULT = new HeroHudConfig("hud.superheroes.energy.generic", EnergyIconType.GENERIC, false, null);
}
