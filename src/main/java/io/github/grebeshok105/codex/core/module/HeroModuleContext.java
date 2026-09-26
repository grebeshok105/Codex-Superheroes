package io.github.grebeshok105.codex.core.module;

import io.github.grebeshok105.codex.lifecycle.LifecycleRegistrar;
import io.github.grebeshok105.codex.lifecycle.TickRegistrar;

public interface HeroModuleContext {
	AbilitySink abilities();

	TickRegistrar ticks();

	LifecycleRegistrar lifecycle();
}
