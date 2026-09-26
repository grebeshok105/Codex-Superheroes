package com.example.superheroes.client.hero.scaramouche;

import com.example.superheroes.client.core.module.HeroClientContext;
import com.example.superheroes.client.core.module.HeroClientModule;
import com.example.superheroes.hero.ScaramoucheHero;
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
