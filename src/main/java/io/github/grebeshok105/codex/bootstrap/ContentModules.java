package io.github.grebeshok105.codex.bootstrap;

import io.github.grebeshok105.codex.core.module.ContentModule;
import io.github.grebeshok105.codex.core.module.ContentModuleContext;

import java.util.List;

/** Composition root: the only place that names content modules. One line per content slice. */
public final class ContentModules {
	public static final List<ContentModule> ALL = List.of(
			new io.github.grebeshok105.codex.content.horde.HordeModule()
	);

	private ContentModules() {
	}

	public static void bootstrap(ContentModuleContext ctx) {
		for (ContentModule module : ALL) {
			module.register(ctx);
		}
	}
}
