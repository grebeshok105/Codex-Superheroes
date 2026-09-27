package io.github.grebeshok105.codex.core.module;

import io.github.grebeshok105.codex.damage.DamageTypeSpec;

import java.util.List;

/**
 * Bootstrap-time wiring of one content slice (a mechanic bundle with no hero): registers its content,
 * ticks, payloads, lifecycle hooks and session state through narrow registrars. Same contract as
 * {@link HeroModule} minus {@code hero()} — content modules own no hero identity.
 */
public interface ContentModule {
	void register(ContentModuleContext ctx);

	/**
	 * Damage types this content slice owns. Datagen bootstraps them and tag providers read their tag
	 * membership — shared registries never name module classes. Read-only metadata, safe to call
	 * before {@link #register}.
	 */
	default List<DamageTypeSpec> damageTypes() {
		return List.of();
	}
}
