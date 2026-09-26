package com.example.superheroes.client.bootstrap;

import com.example.superheroes.client.core.module.CoreClientContext;
import com.example.superheroes.client.core.module.HeroClientModule;

import java.util.List;

/** Composition root: the only client class that names hero modules; same order as bootstrap.HeroModules. */
public final class HeroClientModules {
	public static final List<HeroClientModule> ALL = List.of(
			new com.example.superheroes.client.hero.scorpion.ScorpionClientModule()
	);

	private HeroClientModules() {
	}

	public static void bootstrap() {
		for (HeroClientModule module : ALL) {
			module.register(new CoreClientContext(module.heroId()));
		}
	}
}
