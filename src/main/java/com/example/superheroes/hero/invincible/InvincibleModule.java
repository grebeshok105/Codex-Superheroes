package com.example.superheroes.hero.invincible;

import com.example.superheroes.ability.GuardiansBreakerAbility;
import com.example.superheroes.ability.ViltrumiteChargeAbility;
import com.example.superheroes.core.module.HeroModule;
import com.example.superheroes.core.module.HeroModuleContext;
import com.example.superheroes.hero.Hero;
import com.example.superheroes.hero.InvincibleHero;

public final class InvincibleModule implements HeroModule {
	private final InvincibleHero hero = new InvincibleHero();

	@Override
	public Hero hero() {
		return hero;
	}

	@Override
	public void register(HeroModuleContext ctx) {
		ctx.abilities().register(new ViltrumiteChargeAbility());
		ctx.abilities().register(new GuardiansBreakerAbility());
	}
}
