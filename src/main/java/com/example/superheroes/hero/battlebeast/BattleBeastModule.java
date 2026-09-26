package com.example.superheroes.hero.battlebeast;

import com.example.superheroes.ability.BattleBeastAxeCleaveAbility;
import com.example.superheroes.ability.BattleBeastBloodlustAbility;
import com.example.superheroes.ability.BattleBeastPredatorLeapAbility;
import com.example.superheroes.ability.BattleBeastWarRoarAbility;
import com.example.superheroes.core.module.HeroModule;
import com.example.superheroes.core.module.HeroModuleContext;
import com.example.superheroes.effect.BattleBeastCurseController;
import com.example.superheroes.hero.BattleBeastHero;
import com.example.superheroes.hero.Hero;

public final class BattleBeastModule implements HeroModule {
	private final BattleBeastHero hero = new BattleBeastHero();

	@Override
	public Hero hero() {
		return hero;
	}

	@Override
	public void register(HeroModuleContext ctx) {
		ctx.abilities().register(new BattleBeastPredatorLeapAbility());
		ctx.abilities().register(new BattleBeastAxeCleaveAbility());
		ctx.abilities().register(new BattleBeastWarRoarAbility());
		ctx.abilities().register(new BattleBeastBloodlustAbility());
		ctx.lifecycle().onJoin(BattleBeastCurseController::reapplyOnJoin);
		ctx.lifecycle().onServerStopped(server -> BattleBeastCurseController.resetAll());
		ctx.ticks().player(BattleBeastCurseController::tickPlayer);
	}
}
