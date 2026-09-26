package io.github.grebeshok105.codex.client.hero.atrain;

import io.github.grebeshok105.codex.client.core.module.HeroClientContext;
import io.github.grebeshok105.codex.client.core.module.HeroClientModule;
import io.github.grebeshok105.codex.hero.ATrainHero;
import net.minecraft.resources.ResourceLocation;

public record ATrainClientModule() implements HeroClientModule {
	@Override
	public ResourceLocation heroId() {
		return ATrainHero.ID;
	}

	@Override
	public void register(HeroClientContext ctx) {
	}
}
