package com.example.superheroes.client.hero.raiden;

import com.example.superheroes.client.core.module.HeroClientContext;
import com.example.superheroes.client.core.module.HeroClientModule;
import com.example.superheroes.hero.RaidenHero;
import net.minecraft.resources.ResourceLocation;

public record RaidenClientModule() implements HeroClientModule {
	@Override
	public ResourceLocation heroId() {
		return RaidenHero.ID;
	}

	@Override
	public void register(HeroClientContext ctx) {
	}
}
