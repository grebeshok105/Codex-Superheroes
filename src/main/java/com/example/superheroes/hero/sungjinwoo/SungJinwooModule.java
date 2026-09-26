package com.example.superheroes.hero.sungjinwoo;

import com.example.superheroes.ability.AriseAbility;
import com.example.superheroes.ability.MonarchsDomainAbility;
import com.example.superheroes.ability.RulersAuthorityAbility;
import com.example.superheroes.ability.SacrificeAbility;
import com.example.superheroes.ability.ShadowExchangeAbility;
import com.example.superheroes.ability.ShadowExtractionAbility;
import com.example.superheroes.core.module.HeroModule;
import com.example.superheroes.core.module.HeroModuleContext;
import com.example.superheroes.effect.MonarchsDomainController;
import com.example.superheroes.effect.SungJinwooController;
import com.example.superheroes.hero.Hero;
import com.example.superheroes.hero.SungJinwooHero;

public final class SungJinwooModule implements HeroModule {
	private final SungJinwooHero hero = new SungJinwooHero();

	@Override
	public Hero hero() {
		return hero;
	}

	@Override
	public void register(HeroModuleContext ctx) {
		ctx.abilities().register(new AriseAbility());
		ctx.abilities().register(new ShadowExchangeAbility());
		ctx.abilities().register(new SacrificeAbility());
		ctx.abilities().register(new RulersAuthorityAbility());
		ctx.abilities().register(new ShadowExtractionAbility());
		ctx.abilities().register(new MonarchsDomainAbility());
		SungJinwooController.register(ctx);
		ctx.ticks().player(SungJinwooController::tickPlayer);
		ctx.ticks().player(MonarchsDomainController::tickPlayer);
	}
}
