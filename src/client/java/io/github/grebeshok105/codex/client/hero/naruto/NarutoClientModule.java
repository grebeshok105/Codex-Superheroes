package io.github.grebeshok105.codex.client.hero.naruto;

import io.github.grebeshok105.codex.client.core.module.HeroClientContext;
import io.github.grebeshok105.codex.client.core.module.HeroClientModule;
import io.github.grebeshok105.codex.client.hero.naruto.render.KageBunshinRenderer;
import io.github.grebeshok105.codex.hero.naruto.entity.NarutoEntities;
import io.github.grebeshok105.codex.hero.naruto.NarutoHero;
import net.minecraft.resources.ResourceLocation;

public record NarutoClientModule() implements HeroClientModule {
	@Override
	public ResourceLocation heroId() {
		return NarutoHero.ID;
	}

	@Override
	public void register(HeroClientContext ctx) {
		ctx.entityRenderer(NarutoEntities.KAGE_BUNSHIN, KageBunshinRenderer::new);
	}
}
