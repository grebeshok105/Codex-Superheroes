package io.github.grebeshok105.codex.core.module;

import io.github.grebeshok105.codex.core.content.ContentRegistrar;
import io.github.grebeshok105.codex.core.lifecycle.LifecycleRegistrar;
import io.github.grebeshok105.codex.core.lifecycle.TickRegistrar;
import io.github.grebeshok105.codex.core.net.PayloadRegistrar;

public interface HeroModuleContext {
	AbilitySink abilities();

	TickRegistrar ticks();

	LifecycleRegistrar lifecycle();

	ContentRegistrar content();

	PayloadRegistrar payloads();
}
