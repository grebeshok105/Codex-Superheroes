package com.example.superheroes.client.hero.naruto;

import com.example.superheroes.client.core.module.HeroClientContext;
import com.example.superheroes.client.core.module.HeroClientModule;
import com.example.superheroes.client.render.KageBunshinRenderer;
import com.example.superheroes.entity.ModEntities;
import com.example.superheroes.hero.NarutoHero;
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
