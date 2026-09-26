package com.example.superheroes.client.hero.invincible;

import com.example.superheroes.client.core.module.HeroClientContext;
import com.example.superheroes.client.core.module.HeroClientModule;
import com.example.superheroes.hero.InvincibleHero;
import net.minecraft.resources.ResourceLocation;

public record InvincibleClientModule() implements HeroClientModule {
	@Override
	public ResourceLocation heroId() {
		return InvincibleHero.ID;
	}

	@Override
	public void register(HeroClientContext ctx) {
	}
}
