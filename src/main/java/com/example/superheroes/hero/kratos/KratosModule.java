package com.example.superheroes.hero.kratos;

import com.example.superheroes.ability.KratosBladeStormAbility;
import com.example.superheroes.ability.KratosChainWhirlAbility;
import com.example.superheroes.ability.KratosGodSlayerAbility;
import com.example.superheroes.ability.KratosLeviathanThrowAbility;
import com.example.superheroes.ability.KratosSpartanRageAbility;
import com.example.superheroes.core.module.HeroModule;
import com.example.superheroes.core.module.HeroModuleContext;
import com.example.superheroes.hero.Hero;
import com.example.superheroes.hero.KratosHero;

public final class KratosModule implements HeroModule {
	private final KratosHero hero = new KratosHero();

	@Override
	public Hero hero() {
		return hero;
	}

	@Override
	public void register(HeroModuleContext ctx) {
		ctx.abilities().register(new KratosSpartanRageAbility());
		ctx.abilities().register(new KratosBladeStormAbility());
		ctx.abilities().register(new KratosChainWhirlAbility());
		ctx.abilities().register(new KratosLeviathanThrowAbility());
		ctx.abilities().register(new KratosGodSlayerAbility());
	}
}
