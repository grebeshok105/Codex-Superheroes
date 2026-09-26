package com.example.superheroes.bootstrap;

import com.example.superheroes.core.module.HeroModule;
import com.example.superheroes.core.module.HeroModuleContext;
import com.example.superheroes.hero.Heroes;

import java.util.List;

/** Composition root: the only place that names hero modules. One line per hero; order = registry order. */
public final class HeroModules {
	public static final List<HeroModule> ALL = List.of(
			new com.example.superheroes.hero.homelander.HomelanderModule(),
			new com.example.superheroes.hero.ironman.IronManModule(),
			new com.example.superheroes.hero.regulus.RegulusModule(),
			new com.example.superheroes.hero.sungjinwoo.SungJinwooModule(),
			new com.example.superheroes.hero.doomsday.DoomsdayModule(),
			new com.example.superheroes.hero.goku.GokuModule(),
			new com.example.superheroes.hero.naruto.NarutoModule(),
			new com.example.superheroes.hero.captainamerica.CaptainAmericaModule(),
			new com.example.superheroes.hero.kratos.KratosModule(),
			new com.example.superheroes.hero.loki.LokiModule(),
			new com.example.superheroes.hero.thanos.ThanosModule(),
			new com.example.superheroes.hero.reinhard.ReinhardModule(),
			new com.example.superheroes.hero.raiden.RaidenModule(),
			new com.example.superheroes.hero.invincible.InvincibleModule(),
			new com.example.superheroes.hero.omniman.OmnimanModule(),
			new com.example.superheroes.hero.kazuha.KazuhaModule(),
			new com.example.superheroes.hero.scaramouche.ScaramoucheModule(),
			new com.example.superheroes.hero.battlebeast.BattleBeastModule(),
			new com.example.superheroes.hero.rem.RemModule(),
			new com.example.superheroes.hero.atrain.ATrainModule(),
			new com.example.superheroes.hero.scorpion.ScorpionModule(),
			new com.example.superheroes.hero.pandora.PandoraModule()
	);

	private HeroModules() {
	}

	public static void bootstrap(HeroModuleContext ctx) {
		for (HeroModule module : ALL) {
			Heroes.register(module.hero());
		}
		SharedAbilities.register(ctx.abilities());
		for (HeroModule module : ALL) {
			module.register(ctx);
		}
	}
}
