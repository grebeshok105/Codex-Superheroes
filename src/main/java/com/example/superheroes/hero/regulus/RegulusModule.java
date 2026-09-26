package com.example.superheroes.hero.regulus;

import com.example.superheroes.ability.CounterStrikeAbility;
import com.example.superheroes.ability.GreedsEmbraceAbility;
import com.example.superheroes.ability.LionHeartAbility;
import com.example.superheroes.ability.LionRoarAbility;
import com.example.superheroes.ability.ManiaOfGreedAbility;
import com.example.superheroes.core.module.HeroModule;
import com.example.superheroes.core.module.HeroModuleContext;
import com.example.superheroes.hero.Hero;
import com.example.superheroes.hero.RegulusHero;

public final class RegulusModule implements HeroModule {
	private final RegulusHero hero = new RegulusHero();

	@Override
	public Hero hero() {
		return hero;
	}

	@Override
	public void register(HeroModuleContext ctx) {
		ctx.abilities().register(new LionHeartAbility());
		ctx.abilities().register(new ManiaOfGreedAbility());
		ctx.abilities().register(new GreedsEmbraceAbility());
		ctx.abilities().register(new LionRoarAbility());
		ctx.abilities().register(new CounterStrikeAbility());
	}
}
