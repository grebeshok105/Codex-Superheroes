package io.github.grebeshok105.codex.client.hero.scaramouche;

import io.github.grebeshok105.codex.client.core.module.HeroClientContext;
import io.github.grebeshok105.codex.client.core.module.HeroClientModule;
import io.github.grebeshok105.codex.hero.ScaramoucheHero;
import net.minecraft.resources.ResourceLocation;

public record ScaramoucheClientModule() implements HeroClientModule {
	@Override
	public ResourceLocation heroId() {
		return ScaramoucheHero.ID;
	}

	@Override
	public void register(HeroClientContext ctx) {
	}
}
