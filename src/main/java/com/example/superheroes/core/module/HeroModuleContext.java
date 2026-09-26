package com.example.superheroes.core.module;

import com.example.superheroes.lifecycle.LifecycleRegistrar;
import com.example.superheroes.lifecycle.TickRegistrar;

public interface HeroModuleContext {
	AbilitySink abilities();

	TickRegistrar ticks();

	LifecycleRegistrar lifecycle();
}
