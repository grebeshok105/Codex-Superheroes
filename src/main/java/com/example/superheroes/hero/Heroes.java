package com.example.superheroes.hero;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Heroes {
	private static final Map<ResourceLocation, Hero> REGISTRY = new LinkedHashMap<>();
	public static final HomelanderHero HOMELANDER = new HomelanderHero();
	public static final IronManHero IRON_MAN = new IronManHero();
	public static final RegulusHero REGULUS = new RegulusHero();
	public static final SungJinwooHero SUNG_JINWOO = new SungJinwooHero();
	public static final DoomsdayHero DOOMSDAY = new DoomsdayHero();
	public static final GokuHero GOKU = new GokuHero();
	public static final NarutoHero NARUTO = new NarutoHero();
	public static final CaptainAmericaHero CAPTAIN_AMERICA = new CaptainAmericaHero();
	public static final KratosHero KRATOS = new KratosHero();
	public static final LokiHero LOKI = new LokiHero();
	public static final ThanosHero THANOS = new ThanosHero();
	public static final ReinhardHero REINHARD = new ReinhardHero();
	public static final RaidenHero RAIDEN_SHOGUN = new RaidenHero();
	public static final InvincibleHero INVINCIBLE = new InvincibleHero();
	public static final OmnimanHero OMNIMAN = new OmnimanHero();
	public static final KazuhaHero KAZUHA = new KazuhaHero();
	public static final ScaramoucheHero SCARAMOUCHE = new ScaramoucheHero();
	public static final BattleBeastHero BATTLE_BEAST = new BattleBeastHero();
	public static final RemHero REM = new RemHero();
	public static final ATrainHero A_TRAIN = new ATrainHero();
	public static final ScorpionHero SCORPION = new ScorpionHero();
	public static final DoctorStrangeHero DOCTOR_STRANGE = new DoctorStrangeHero();

	private Heroes() {
	}

	public static void init() {
		register(HOMELANDER);
		register(IRON_MAN);
		register(REGULUS);
		register(SUNG_JINWOO);
		register(DOOMSDAY);
		register(GOKU);
		register(NARUTO);
		register(CAPTAIN_AMERICA);
		register(KRATOS);
		register(LOKI);
		register(THANOS);
		register(REINHARD);
		register(RAIDEN_SHOGUN);
		register(INVINCIBLE);
		register(OMNIMAN);
		register(KAZUHA);
		register(SCARAMOUCHE);
		register(BATTLE_BEAST);
		register(REM);
		register(A_TRAIN);
		register(SCORPION);
		register(DOCTOR_STRANGE);
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
