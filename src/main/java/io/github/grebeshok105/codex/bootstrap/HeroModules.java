package io.github.grebeshok105.codex.bootstrap;

import io.github.grebeshok105.codex.core.module.HeroModule;
import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import io.github.grebeshok105.codex.core.hero.Heroes;

import java.util.List;

/** Composition root: the only place that names hero modules. One line per hero; order = registry order. */
public final class HeroModules {
	public static final List<HeroModule> ALL = List.of(
			new io.github.grebeshok105.codex.hero.homelander.HomelanderModule(),
			new io.github.grebeshok105.codex.hero.ironman.IronManModule(),
			new io.github.grebeshok105.codex.hero.regulus.RegulusModule(),
			new io.github.grebeshok105.codex.hero.sungjinwoo.SungJinwooModule(),
			new io.github.grebeshok105.codex.hero.doomsday.DoomsdayModule(),
			new io.github.grebeshok105.codex.hero.goku.GokuModule(),
			new io.github.grebeshok105.codex.hero.naruto.NarutoModule(),
			new io.github.grebeshok105.codex.hero.captainamerica.CaptainAmericaModule(),
			new io.github.grebeshok105.codex.hero.kratos.KratosModule(),
			new io.github.grebeshok105.codex.hero.loki.LokiModule(),
			new io.github.grebeshok105.codex.hero.thanos.ThanosModule(),
			new io.github.grebeshok105.codex.hero.reinhard.ReinhardModule(),
			new io.github.grebeshok105.codex.hero.raiden.RaidenModule(),
			new io.github.grebeshok105.codex.hero.invincible.InvincibleModule(),
			new io.github.grebeshok105.codex.hero.omniman.OmnimanModule(),
			new io.github.grebeshok105.codex.hero.kazuha.KazuhaModule(),
			new io.github.grebeshok105.codex.hero.scaramouche.ScaramoucheModule(),
			new io.github.grebeshok105.codex.hero.battlebeast.BattleBeastModule(),
			new io.github.grebeshok105.codex.hero.rem.RemModule(),
			new io.github.grebeshok105.codex.hero.atrain.ATrainModule(),
			new io.github.grebeshok105.codex.hero.scorpion.ScorpionModule(),
			new io.github.grebeshok105.codex.hero.pandora.PandoraModule()
	);

	private HeroModules() {
	}

	public static void bootstrap(HeroModuleContext ctx) {
		for (HeroModule module : ALL) {
			Heroes.register(module.hero());
		}
		SharedAbilities.register(ctx.abilities());
		SharedMechanics.register(ctx);
		for (HeroModule module : ALL) {
			module.register(ctx);
		}
		SharedMechanics.registerPost(ctx);
	}
}
