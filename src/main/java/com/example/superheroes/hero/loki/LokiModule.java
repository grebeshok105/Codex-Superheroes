package com.example.superheroes.hero.loki;

import com.example.superheroes.ability.LokiAstralClonesAbility;
import com.example.superheroes.ability.LokiChaosBoltAbility;
import com.example.superheroes.ability.LokiGlamourAbility;
import com.example.superheroes.ability.LokiMindCharmAbility;
import com.example.superheroes.ability.LokiTesseractBlinkAbility;
import com.example.superheroes.core.module.HeroModule;
import com.example.superheroes.core.module.HeroModuleContext;
import com.example.superheroes.hero.Hero;
import com.example.superheroes.hero.LokiHero;

public final class LokiModule implements HeroModule {
	private final LokiHero hero = new LokiHero();

	@Override
	public Hero hero() {
		return hero;
	}

	@Override
	public void register(HeroModuleContext ctx) {
		ctx.abilities().register(new LokiAstralClonesAbility());
		ctx.abilities().register(new LokiTesseractBlinkAbility());
		ctx.abilities().register(new LokiMindCharmAbility());
		ctx.abilities().register(new LokiGlamourAbility());
		ctx.abilities().register(new LokiChaosBoltAbility());
	}
}
