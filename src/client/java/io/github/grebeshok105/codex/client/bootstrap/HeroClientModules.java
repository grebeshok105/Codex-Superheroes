package io.github.grebeshok105.codex.client.bootstrap;

import io.github.grebeshok105.codex.client.core.module.CoreClientContext;
import io.github.grebeshok105.codex.client.core.module.HeroClientModule;

import java.util.List;

/** Composition root: the only client class that names hero modules; same order as bootstrap.HeroModules. */
public final class HeroClientModules {
	public static final List<HeroClientModule> ALL = List.of(
			new io.github.grebeshok105.codex.client.hero.homelander.HomelanderClientModule(),
			new io.github.grebeshok105.codex.client.hero.ironman.IronManClientModule(),
			new io.github.grebeshok105.codex.client.hero.regulus.RegulusClientModule(),
			new io.github.grebeshok105.codex.client.hero.sungjinwoo.SungJinwooClientModule(),
			new io.github.grebeshok105.codex.client.hero.doomsday.DoomsdayClientModule(),
			new io.github.grebeshok105.codex.client.hero.goku.GokuClientModule(),
			new io.github.grebeshok105.codex.client.hero.naruto.NarutoClientModule(),
			new io.github.grebeshok105.codex.client.hero.captainamerica.CaptainAmericaClientModule(),
			new io.github.grebeshok105.codex.client.hero.kratos.KratosClientModule(),
			new io.github.grebeshok105.codex.client.hero.loki.LokiClientModule(),
			new io.github.grebeshok105.codex.client.hero.thanos.ThanosClientModule(),
			new io.github.grebeshok105.codex.client.hero.reinhard.ReinhardClientModule(),
			new io.github.grebeshok105.codex.client.hero.raiden.RaidenClientModule(),
			new io.github.grebeshok105.codex.client.hero.invincible.InvincibleClientModule(),
			new io.github.grebeshok105.codex.client.hero.omniman.OmnimanClientModule(),
			new io.github.grebeshok105.codex.client.hero.kazuha.KazuhaClientModule(),
			new io.github.grebeshok105.codex.client.hero.scaramouche.ScaramoucheClientModule(),
			new io.github.grebeshok105.codex.client.hero.battlebeast.BattleBeastClientModule(),
			new io.github.grebeshok105.codex.client.hero.rem.RemClientModule(),
			new io.github.grebeshok105.codex.client.hero.atrain.ATrainClientModule(),
			new io.github.grebeshok105.codex.client.hero.scorpion.ScorpionClientModule(),
			new io.github.grebeshok105.codex.client.hero.pandora.PandoraClientModule()
	);

	private HeroClientModules() {
	}

	public static void bootstrap() {
		for (HeroClientModule module : ALL) {
			module.register(new CoreClientContext(module.heroId()));
		}
	}
}
