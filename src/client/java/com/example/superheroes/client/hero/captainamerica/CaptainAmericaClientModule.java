package com.example.superheroes.client.hero.captainamerica;

import com.example.superheroes.client.core.module.HeroClientContext;
import com.example.superheroes.client.core.module.HeroClientModule;
import com.example.superheroes.hero.CaptainAmericaHero;
import net.minecraft.resources.ResourceLocation;

public record CaptainAmericaClientModule() implements HeroClientModule {
	@Override
	public ResourceLocation heroId() {
		return CaptainAmericaHero.ID;
	}

	@Override
	public void register(HeroClientContext ctx) {
	}
}
