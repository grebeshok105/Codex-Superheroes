package com.example.superheroes.hero.naruto;

import com.example.superheroes.ability.NarutoBijuudamaAbility;
import com.example.superheroes.ability.NarutoOodamaRasenganAbility;
import com.example.superheroes.ability.NarutoRasenganAbility;
import com.example.superheroes.ability.NarutoRasenshurikenAbility;
import com.example.superheroes.ability.NarutoSageModeAbility;
import com.example.superheroes.ability.NarutoShadowClonesAbility;
import com.example.superheroes.core.module.HeroModule;
import com.example.superheroes.core.module.HeroModuleContext;
import com.example.superheroes.hero.Hero;
import com.example.superheroes.hero.NarutoHero;

public final class NarutoModule implements HeroModule {
	private final NarutoHero hero = new NarutoHero();

	@Override
	public Hero hero() {
		return hero;
	}

	@Override
	public void register(HeroModuleContext ctx) {
		ctx.abilities().register(new NarutoRasenganAbility());
		ctx.abilities().register(new NarutoOodamaRasenganAbility());
		ctx.abilities().register(new NarutoRasenshurikenAbility());
		ctx.abilities().register(new NarutoSageModeAbility());
		ctx.abilities().register(new NarutoBijuudamaAbility());
		ctx.abilities().register(new NarutoShadowClonesAbility());
	}
}
