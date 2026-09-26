package com.example.superheroes.hero.kazuha;

import com.example.superheroes.ability.KazuhaAutumnWhirlwindAbility;
import com.example.superheroes.ability.KazuhaChihayaburuAbility;
import com.example.superheroes.ability.KazuhaMapleStormAbility;
import com.example.superheroes.ability.KazuhaMidareRanzanAbility;
import com.example.superheroes.core.module.HeroModule;
import com.example.superheroes.core.module.HeroModuleContext;
import com.example.superheroes.hero.Hero;
import com.example.superheroes.hero.KazuhaHero;

public final class KazuhaModule implements HeroModule {
	private final KazuhaHero hero = new KazuhaHero();

	@Override
	public Hero hero() {
		return hero;
	}

	@Override
	public void register(HeroModuleContext ctx) {
		ctx.abilities().register(new KazuhaChihayaburuAbility());
		ctx.abilities().register(new KazuhaMidareRanzanAbility());
		ctx.abilities().register(new KazuhaAutumnWhirlwindAbility());
		ctx.abilities().register(new KazuhaMapleStormAbility());
	}
}
