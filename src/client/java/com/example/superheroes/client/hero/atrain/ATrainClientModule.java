package com.example.superheroes.client.hero.atrain;

import com.example.superheroes.client.core.module.HeroClientContext;
import com.example.superheroes.client.core.module.HeroClientModule;
import com.example.superheroes.hero.ATrainHero;
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
