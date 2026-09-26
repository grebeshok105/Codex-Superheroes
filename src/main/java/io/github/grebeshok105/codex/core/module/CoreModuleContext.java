package io.github.grebeshok105.codex.core.module;

import io.github.grebeshok105.codex.ability.AbilityRegistry;
import io.github.grebeshok105.codex.lifecycle.HeroTickDispatcher;
import io.github.grebeshok105.codex.lifecycle.LifecycleRegistrar;
import io.github.grebeshok105.codex.lifecycle.TickRegistrar;

public final class CoreModuleContext implements HeroModuleContext {
	public static final CoreModuleContext INSTANCE = new CoreModuleContext();

	private CoreModuleContext() {
	}

	@Override
	public AbilitySink abilities() {
		return AbilityRegistry::register;
	}

	@Override
	public TickRegistrar ticks() {
		return HeroTickDispatcher.registrar();
	}

	@Override
	public LifecycleRegistrar lifecycle() {
		return LifecycleRegistrar.global();
	}
}
