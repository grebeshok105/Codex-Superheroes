package com.example.superheroes.client.hero.loki;

import com.example.superheroes.client.core.module.HeroClientContext;
import com.example.superheroes.client.core.module.HeroClientModule;
import com.example.superheroes.hero.LokiHero;
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
