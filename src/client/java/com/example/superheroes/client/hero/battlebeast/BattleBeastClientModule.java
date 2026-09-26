package com.example.superheroes.client.hero.battlebeast;

import com.example.superheroes.client.core.module.HeroClientContext;
import com.example.superheroes.client.core.module.HeroClientModule;
import com.example.superheroes.hero.BattleBeastHero;
import net.minecraft.resources.ResourceLocation;

public record BattleBeastClientModule() implements HeroClientModule {
	@Override
	public ResourceLocation heroId() {
		return BattleBeastHero.ID;
	}

	@Override
	public void register(HeroClientContext ctx) {
	}
}
