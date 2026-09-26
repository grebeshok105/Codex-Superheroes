package com.example.superheroes.hero.omniman;

import com.example.superheroes.ability.OmnimanThinkMarkAbility;
import com.example.superheroes.ability.OmnimanViltrumiteRushAbility;
import com.example.superheroes.ability.OmnimanWorldBreakerAbility;
import com.example.superheroes.core.module.HeroModule;
import com.example.superheroes.core.module.HeroModuleContext;
import com.example.superheroes.hero.Hero;
import com.example.superheroes.hero.OmnimanHero;

public final class OmnimanModule implements HeroModule {
	private final OmnimanHero hero = new OmnimanHero();

	@Override
	public Hero hero() {
		return hero;
	}

	@Override
	public void register(HeroModuleContext ctx) {
		ctx.abilities().register(new OmnimanViltrumiteRushAbility());
		ctx.abilities().register(new OmnimanThinkMarkAbility());
		ctx.abilities().register(new OmnimanWorldBreakerAbility());
	}
}
