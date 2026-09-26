package io.github.grebeshok105.codex.client.hero.loki;

import io.github.grebeshok105.codex.client.core.module.HeroClientContext;
import io.github.grebeshok105.codex.client.core.module.HeroClientModule;
import io.github.grebeshok105.codex.hero.LokiHero;
import net.minecraft.resources.ResourceLocation;

public record LokiClientModule() implements HeroClientModule {
	@Override
	public ResourceLocation heroId() {
		return LokiHero.ID;
	}

	@Override
	public void register(HeroClientContext ctx) {
	}
}
