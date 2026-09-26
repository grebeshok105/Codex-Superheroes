package io.github.grebeshok105.codex.client.hero.naruto;

import io.github.grebeshok105.codex.client.core.module.HeroClientContext;
import io.github.grebeshok105.codex.client.core.module.HeroClientModule;
import io.github.grebeshok105.codex.client.render.KageBunshinRenderer;
import io.github.grebeshok105.codex.entity.ModEntities;
import io.github.grebeshok105.codex.hero.NarutoHero;
import net.minecraft.resources.ResourceLocation;

public record NarutoClientModule() implements HeroClientModule {
	@Override
	public ResourceLocation heroId() {
		return NarutoHero.ID;
	}

	@Override
	public void register(HeroClientContext ctx) {
		ctx.entityRenderer(ModEntities.KAGE_BUNSHIN, KageBunshinRenderer::new);
	}
}
