package com.example.superheroes.hero.raiden;

import com.example.superheroes.ability.RaidenEyeOfJudgmentAbility;
import com.example.superheroes.ability.RaidenMusouIsshinAbility;
import com.example.superheroes.ability.RaidenMusouShinsetsuAbility;
import com.example.superheroes.ability.RaidenPlungingStrikeAbility;
import com.example.superheroes.ability.RaidenSwordDrawAbility;
import com.example.superheroes.ability.RaidenTranscendenceAbility;
import com.example.superheroes.core.module.HeroModule;
import com.example.superheroes.core.module.HeroModuleContext;
import com.example.superheroes.hero.Hero;
import com.example.superheroes.hero.RaidenHero;

public final class RaidenModule implements HeroModule {
	private final RaidenHero hero = new RaidenHero();

	@Override
	public Hero hero() {
		return hero;
	}

	@Override
	public void register(HeroModuleContext ctx) {
		ctx.abilities().register(new RaidenSwordDrawAbility());
		ctx.abilities().register(new RaidenEyeOfJudgmentAbility());
		ctx.abilities().register(new RaidenMusouShinsetsuAbility());
		ctx.abilities().register(new RaidenMusouIsshinAbility());
		ctx.abilities().register(new RaidenPlungingStrikeAbility());
		ctx.abilities().register(new RaidenTranscendenceAbility());
	}
}
