package com.example.superheroes.hero.goku;

import com.example.superheroes.ability.GokuInstantTransmissionAbility;
import com.example.superheroes.ability.GokuKamehamehaAbility;
import com.example.superheroes.ability.GokuKiChargeAbility;
import com.example.superheroes.ability.GokuSolarFlareAbility;
import com.example.superheroes.ability.GokuSpiritBombAbility;
import com.example.superheroes.ability.GokuSuperSaiyanAuraAbility;
import com.example.superheroes.core.module.HeroModule;
import com.example.superheroes.core.module.HeroModuleContext;
import com.example.superheroes.hero.GokuHero;
import com.example.superheroes.hero.Hero;

public final class GokuModule implements HeroModule {
	private final GokuHero hero = new GokuHero();

	@Override
	public Hero hero() {
		return hero;
	}

	@Override
	public void register(HeroModuleContext ctx) {
		ctx.abilities().register(new GokuKamehamehaAbility());
		ctx.abilities().register(new GokuInstantTransmissionAbility());
		ctx.abilities().register(new GokuKiChargeAbility());
		ctx.abilities().register(new GokuSolarFlareAbility());
		ctx.abilities().register(new GokuSpiritBombAbility());
		ctx.abilities().register(new GokuSuperSaiyanAuraAbility());
	}
}
