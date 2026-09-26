package io.github.grebeshok105.codex.client.hero.kazuha;

import io.github.grebeshok105.codex.client.core.module.HeroClientContext;
import io.github.grebeshok105.codex.client.core.module.HeroClientModule;
import io.github.grebeshok105.codex.hero.KazuhaHero;
import net.minecraft.resources.ResourceLocation;

public record KazuhaClientModule() implements HeroClientModule {
	@Override
	public ResourceLocation heroId() {
		return KazuhaHero.ID;
	}

	@Override
	public void register(HeroClientContext ctx) {
	}
}
