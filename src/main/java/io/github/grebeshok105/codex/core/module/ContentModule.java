package io.github.grebeshok105.codex.core.module;

/**
 * Bootstrap-time wiring of one content slice (a mechanic bundle with no hero): registers its content,
 * ticks, payloads, lifecycle hooks and session state through narrow registrars. Same contract as
 * {@link HeroModule} minus {@code hero()} — content modules own no hero identity.
 */
public interface ContentModule {
	void register(ContentModuleContext ctx);
}
