package com.example.superheroes.hero.scaramouche;

import com.example.superheroes.ability.ScaramoucheElectroSwirlAbility;
import com.example.superheroes.ability.ScaramoucheSkyfallBurstAbility;
import com.example.superheroes.ability.ScaramoucheWindPrisonAbility;
import com.example.superheroes.ability.ScaramoucheWindstepAbility;
import com.example.superheroes.core.module.HeroModule;
import com.example.superheroes.core.module.HeroModuleContext;
import com.example.superheroes.hero.Hero;
import com.example.superheroes.hero.ScaramoucheHero;

public final class ScaramoucheModule implements HeroModule {
	private final ScaramoucheHero hero = new ScaramoucheHero();

	@Override
	public Hero hero() {
		return hero;
	}

	@Override
	public void register(HeroModuleContext ctx) {
		ctx.abilities().register(new ScaramoucheWindstepAbility());
		ctx.abilities().register(new ScaramoucheElectroSwirlAbility());
		ctx.abilities().register(new ScaramoucheWindPrisonAbility());
		ctx.abilities().register(new ScaramoucheSkyfallBurstAbility());
	}
}
