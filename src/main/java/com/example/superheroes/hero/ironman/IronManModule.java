package com.example.superheroes.hero.ironman;

import com.example.superheroes.ability.IronManFlightAbility;
import com.example.superheroes.ability.RepulsorAbility;
import com.example.superheroes.ability.SmartMissileAbility;
import com.example.superheroes.ability.SupersonicAbility;
import com.example.superheroes.ability.UnibeamAbility;
import com.example.superheroes.ability.ironman.IronManLegionAbility;
import com.example.superheroes.ability.ironman.IronManNanoFormAbility;
import com.example.superheroes.ability.ironman.IronManSuitSwitchAbility;
import com.example.superheroes.core.module.HeroModule;
import com.example.superheroes.core.module.HeroModuleContext;
import com.example.superheroes.hero.Hero;
import com.example.superheroes.hero.IronManHero;

public final class IronManModule implements HeroModule {
	private final IronManHero hero = new IronManHero();

	@Override
	public Hero hero() {
		return hero;
	}

	@Override
	public void register(HeroModuleContext ctx) {
		ctx.abilities().register(new IronManFlightAbility());
		ctx.abilities().register(new SupersonicAbility());
		ctx.abilities().register(new RepulsorAbility());
		ctx.abilities().register(new UnibeamAbility());
		ctx.abilities().register(new SmartMissileAbility());
		ctx.abilities().register(new IronManNanoFormAbility());
		ctx.abilities().register(new IronManSuitSwitchAbility());
		ctx.abilities().register(new IronManLegionAbility());
	}
}
