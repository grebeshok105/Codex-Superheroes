package io.github.grebeshok105.codex.client.core.module;

/**
 * Client-side wiring of one content slice: payload receivers, HUD layers and entity renderers
 * through narrow registrars. Same contract as {@link HeroClientModule} minus {@code heroId()}
 * — content modules own no hero identity, so no hero-scoped seams (keys, skins, decorations).
 */
public interface ContentClientModule {
	void register(ContentClientContext ctx);
}
