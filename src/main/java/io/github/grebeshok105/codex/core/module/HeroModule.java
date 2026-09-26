package io.github.grebeshok105.codex.core.module;

import io.github.grebeshok105.codex.core.hero.Hero;

/**
 * Bootstrap-time wiring of one hero: registers its hero, abilities, ticks and lifecycle hooks through narrow
 * registrars. Runtime rules live on {@link Hero}; this interface never grows gameplay methods.
 */
public interface HeroModule {
	Hero hero();

	void register(HeroModuleContext ctx);
}
