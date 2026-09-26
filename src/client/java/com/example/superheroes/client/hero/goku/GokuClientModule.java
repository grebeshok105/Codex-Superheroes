package com.example.superheroes.client.hero.goku;

import com.example.superheroes.client.core.module.HeroClientContext;
import com.example.superheroes.client.core.module.HeroClientModule;
import com.example.superheroes.hero.GokuHero;
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
