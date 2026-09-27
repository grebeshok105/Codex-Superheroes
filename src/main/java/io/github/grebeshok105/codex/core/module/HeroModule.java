package io.github.grebeshok105.codex.core.module;

import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.damage.DamageTypeSpec;

import java.util.List;

/**
 * Bootstrap-time wiring of one hero: registers its hero, abilities, ticks and lifecycle hooks through narrow
 * registrars. Runtime rules live on {@link Hero}; this interface never grows gameplay methods.
 */
public interface HeroModule {
	Hero hero();

	void register(HeroModuleContext ctx);

	/**
	 * Damage types this hero owns. Datagen bootstraps them and tag providers read their tag
	 * membership — shared registries never name hero classes. Read-only metadata, safe to call
	 * before {@link #register}.
	 */
	default List<DamageTypeSpec> damageTypes() {
		return List.of();
	}
}
