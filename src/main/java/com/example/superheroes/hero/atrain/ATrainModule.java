package com.example.superheroes.hero.atrain;

import com.example.superheroes.ability.ATrainAdrenalineRushAbility;
import com.example.superheroes.ability.ATrainHyperspeedAbility;
import com.example.superheroes.ability.ATrainMachDashAbility;
import com.example.superheroes.ability.ATrainSonicBoomAbility;
import com.example.superheroes.core.module.HeroModule;
import com.example.superheroes.core.module.HeroModuleContext;
import com.example.superheroes.hero.ATrainHero;
import com.example.superheroes.hero.Hero;

public final class ATrainModule implements HeroModule {
	private final ATrainHero hero = new ATrainHero();

	@Override
	public Hero hero() {
		return hero;
	}

	@Override
	public void register(HeroModuleContext ctx) {
		ctx.abilities().register(new ATrainMachDashAbility());
		ctx.abilities().register(new ATrainSonicBoomAbility());
		ctx.abilities().register(new ATrainHyperspeedAbility());
		ctx.abilities().register(new ATrainAdrenalineRushAbility());
	}
}
