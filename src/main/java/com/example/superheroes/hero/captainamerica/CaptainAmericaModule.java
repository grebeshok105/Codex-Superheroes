package com.example.superheroes.hero.captainamerica;

import com.example.superheroes.ability.CapCounterStanceAbility;
import com.example.superheroes.ability.CapShieldDashAbility;
import com.example.superheroes.ability.CapShieldSlamAbility;
import com.example.superheroes.ability.CapShieldThrowAbility;
import com.example.superheroes.core.module.HeroModule;
import com.example.superheroes.core.module.HeroModuleContext;
import com.example.superheroes.hero.CaptainAmericaHero;
import com.example.superheroes.hero.Hero;

public final class CaptainAmericaModule implements HeroModule {
	private final CaptainAmericaHero hero = new CaptainAmericaHero();

	@Override
	public Hero hero() {
		return hero;
	}

	@Override
	public void register(HeroModuleContext ctx) {
		ctx.abilities().register(new CapShieldThrowAbility());
		ctx.abilities().register(new CapShieldSlamAbility());
		ctx.abilities().register(new CapShieldDashAbility());
		ctx.abilities().register(new CapCounterStanceAbility());
	}
}
