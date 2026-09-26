package com.example.superheroes.client.bootstrap;

import com.example.superheroes.client.core.module.CoreClientContext;
import com.example.superheroes.client.core.module.HeroClientModule;

import java.util.List;

/** Composition root: the only client class that names hero modules; same order as bootstrap.HeroModules. */
public final class HeroClientModules {
	public static final List<HeroClientModule> ALL = List.of(
			new com.example.superheroes.client.hero.homelander.HomelanderClientModule(),
			new com.example.superheroes.client.hero.ironman.IronManClientModule(),
			new com.example.superheroes.client.hero.regulus.RegulusClientModule(),
			new com.example.superheroes.client.hero.sungjinwoo.SungJinwooClientModule(),
			new com.example.superheroes.client.hero.doomsday.DoomsdayClientModule(),
			new com.example.superheroes.client.hero.goku.GokuClientModule(),
			new com.example.superheroes.client.hero.naruto.NarutoClientModule(),
			new com.example.superheroes.client.hero.captainamerica.CaptainAmericaClientModule(),
			new com.example.superheroes.client.hero.kratos.KratosClientModule(),
			new com.example.superheroes.client.hero.loki.LokiClientModule(),
			new com.example.superheroes.client.hero.thanos.ThanosClientModule(),
			new com.example.superheroes.client.hero.reinhard.ReinhardClientModule(),
			new com.example.superheroes.client.hero.raiden.RaidenClientModule(),
			new com.example.superheroes.client.hero.invincible.InvincibleClientModule(),
			new com.example.superheroes.client.hero.omniman.OmnimanClientModule(),
			new com.example.superheroes.client.hero.kazuha.KazuhaClientModule(),
			new com.example.superheroes.client.hero.scaramouche.ScaramoucheClientModule(),
			new com.example.superheroes.client.hero.battlebeast.BattleBeastClientModule(),
			new com.example.superheroes.client.hero.rem.RemClientModule(),
			new com.example.superheroes.client.hero.atrain.ATrainClientModule(),
			new com.example.superheroes.client.hero.scorpion.ScorpionClientModule(),
			new com.example.superheroes.client.hero.pandora.PandoraClientModule()
	);

	private HeroClientModules() {
	}

	public static void bootstrap() {
		for (HeroClientModule module : ALL) {
			module.register(new CoreClientContext(module.heroId()));
		}
	}
}
