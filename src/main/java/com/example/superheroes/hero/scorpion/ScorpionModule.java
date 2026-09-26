package com.example.superheroes.hero.scorpion;

import com.example.superheroes.ability.ScorpionFireTeleportAbility;
import com.example.superheroes.ability.ScorpionHellBreathAbility;
import com.example.superheroes.ability.ScorpionHellfireAbility;
import com.example.superheroes.ability.ScorpionSpearAbility;
import com.example.superheroes.core.module.HeroModule;
import com.example.superheroes.core.module.HeroModuleContext;
import com.example.superheroes.effect.ScorpionController;
import com.example.superheroes.hero.Hero;
import com.example.superheroes.hero.ScorpionHero;

public final class ScorpionModule implements HeroModule {
	private final ScorpionHero hero = new ScorpionHero();

	@Override
	public Hero hero() {
		return hero;
	}

	@Override
	public void register(HeroModuleContext ctx) {
		ctx.abilities().register(new ScorpionSpearAbility());
		ctx.abilities().register(new ScorpionHellfireAbility());
		ctx.abilities().register(new ScorpionFireTeleportAbility());
		ctx.abilities().register(new ScorpionHellBreathAbility());
		ctx.ticks().global(ScorpionController::serverTick);
		ctx.ticks().player(ScorpionController::tickPlayer);
	}
}
