package com.example.superheroes.core.module;

import com.example.superheroes.ability.Ability;

@FunctionalInterface
public interface AbilitySink {
	void register(Ability ability);
}
