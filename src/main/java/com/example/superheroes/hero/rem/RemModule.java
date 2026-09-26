package com.example.superheroes.hero.rem;

import com.example.superheroes.ability.RemHealingMagicAbility;
import com.example.superheroes.ability.RemHumaIceSpikesAbility;
import com.example.superheroes.ability.RemIceBurstAbility;
import com.example.superheroes.ability.RemMaceCraterAbility;
import com.example.superheroes.ability.RemMorningStarAbility;
import com.example.superheroes.ability.RemOniKickAbility;
import com.example.superheroes.ability.RemOniRageAbility;
import com.example.superheroes.core.module.HeroModule;
import com.example.superheroes.core.module.HeroModuleContext;
import com.example.superheroes.effect.RamCompanionController;
import com.example.superheroes.effect.RemDemonismController;
import com.example.superheroes.hero.Hero;
import com.example.superheroes.hero.RemHero;

public final class RemModule implements HeroModule {
	private final RemHero hero = new RemHero();

	@Override
	public Hero hero() {
		return hero;
	}

	@Override
	public void register(HeroModuleContext ctx) {
		ctx.abilities().register(new RemHealingMagicAbility());
		ctx.abilities().register(new RemIceBurstAbility());
		ctx.abilities().register(new RemOniRageAbility());
		ctx.abilities().register(new RemMorningStarAbility());
		ctx.abilities().register(new RemMaceCraterAbility());
		ctx.abilities().register(new RemOniKickAbility());
		ctx.abilities().register(new RemHumaIceSpikesAbility());
		RemDemonismController.register(ctx);
		ctx.ticks().global(RemDemonismController::serverTick);
		ctx.ticks().player(RemDemonismController::tickPlayer);
		ctx.ticks().player(RamCompanionController::tickPlayer);
	}
}
