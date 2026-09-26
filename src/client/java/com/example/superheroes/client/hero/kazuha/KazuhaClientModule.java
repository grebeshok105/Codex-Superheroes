package com.example.superheroes.client.hero.kazuha;

import com.example.superheroes.client.core.module.HeroClientContext;
import com.example.superheroes.client.core.module.HeroClientModule;
import com.example.superheroes.hero.KazuhaHero;
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
