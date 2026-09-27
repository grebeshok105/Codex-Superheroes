package io.github.grebeshok105.codex.client.bootstrap;

import io.github.grebeshok105.codex.client.core.module.ContentClientModule;
import io.github.grebeshok105.codex.client.core.module.CoreContentClientContext;

import java.util.List;

/** Composition root: the only client class that names content modules; mirrors bootstrap.ContentModules. */
public final class ContentClientModules {
	public static final List<ContentClientModule> ALL = List.of(
			new io.github.grebeshok105.codex.client.content.horde.HordeClientModule(),
			new io.github.grebeshok105.codex.client.content.boss.homelander.HomelanderBossClientModule(),
			new io.github.grebeshok105.codex.client.content.admin.AdminClientModule()
	);

	private ContentClientModules() {
	}

	public static void bootstrap() {
		for (ContentClientModule module : ALL) {
			module.register(new CoreContentClientContext());
		}
	}
}
