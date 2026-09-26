package com.example.superheroes.bootstrap;

import com.example.superheroes.core.module.HeroModule;
import com.example.superheroes.core.module.HeroModuleContext;
import com.example.superheroes.hero.Heroes;

import java.util.List;

/** Composition root: the only place that names hero modules. One line per hero; order = registry order. */
public final class HeroModules {
	public static final List<HeroModule> ALL = List.of(
			new com.example.superheroes.hero.scorpion.ScorpionModule()
	);

	private HeroModules() {
	}

	public static void bootstrap(HeroModuleContext ctx) {
		for (HeroModule module : ALL) {
			Heroes.register(module.hero());
		}
		for (HeroModule module : ALL) {
			module.register(ctx);
		}
	}
}
