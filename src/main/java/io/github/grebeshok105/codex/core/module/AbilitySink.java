package io.github.grebeshok105.codex.core.module;

import io.github.grebeshok105.codex.core.ability.Ability;

@FunctionalInterface
public interface AbilitySink {
	void register(Ability ability);
}
