package io.github.grebeshok105.codex.client.hero.goku;

import io.github.grebeshok105.codex.client.core.module.HeroClientContext;
import io.github.grebeshok105.codex.client.core.module.HeroClientModule;
import io.github.grebeshok105.codex.hero.GokuHero;
import net.minecraft.resources.ResourceLocation;

public record GokuClientModule() implements HeroClientModule {
	@Override
	public ResourceLocation heroId() {
		return GokuHero.ID;
	}

	@Override
	public void register(HeroClientContext ctx) {
	}
}
