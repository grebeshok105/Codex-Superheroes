package com.example.superheroes.hero.pandora;

import com.example.superheroes.ability.MirrorDimensionAbility;
import com.example.superheroes.ability.MirrorModeCycleAbility;
import com.example.superheroes.ability.SpaceCrushAbility;
import com.example.superheroes.ability.SpatialBindAbility;
import com.example.superheroes.ability.VanityStripAbility;
import com.example.superheroes.core.module.HeroModule;
import com.example.superheroes.core.module.HeroModuleContext;
import com.example.superheroes.effect.MirrorDimensionController;
import com.example.superheroes.effect.PandoraDeathController;
import com.example.superheroes.effect.SpatialBindController;
import com.example.superheroes.hero.Hero;
import com.example.superheroes.hero.PandoraHero;

public final class PandoraModule implements HeroModule {
	private final PandoraHero hero = new PandoraHero();

	@Override
	public Hero hero() {
		return hero;
	}

	@Override
	public void register(HeroModuleContext ctx) {
		ctx.abilities().register(new MirrorDimensionAbility());
		ctx.abilities().register(new MirrorModeCycleAbility());
		ctx.abilities().register(new SpatialBindAbility());
		ctx.abilities().register(new SpaceCrushAbility());
		ctx.abilities().register(new VanityStripAbility());
		MirrorDimensionController.register(ctx);
		// damage/death listeners move with lifecycle in D2b-2; ticks only here
		ctx.ticks().global(PandoraDeathController::serverTick);
		ctx.ticks().global(SpatialBindController::tick);
	}
}
