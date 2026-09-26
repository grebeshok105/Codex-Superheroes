package com.example.superheroes.hero.homelander;

import com.example.superheroes.ability.EyeLasersAbility;
import com.example.superheroes.ability.HandClapAbility;
import com.example.superheroes.ability.IronFistsAbility;
import com.example.superheroes.ability.StunningRoarAbility;
import com.example.superheroes.ability.XRayAbility;
import com.example.superheroes.core.ability.AbilityDenial;
import com.example.superheroes.core.ability.AbilityRules;
import com.example.superheroes.core.module.HeroModule;
import com.example.superheroes.core.module.HeroModuleContext;
import com.example.superheroes.effect.HomelanderRegenController;
import com.example.superheroes.effect.IronFistsController;
import com.example.superheroes.effect.MadnessAftermathController;
import com.example.superheroes.effect.MadnessFlightController;
import com.example.superheroes.effect.ModEffects;
import com.example.superheroes.effect.UraniumDefenseController;
import com.example.superheroes.effect.UraniumOffhandController;
import com.example.superheroes.hero.Hero;
import com.example.superheroes.hero.HomelanderHero;

public final class HomelanderModule implements HeroModule {
	private final HomelanderHero hero = new HomelanderHero();

	@Override
	public Hero hero() {
		return hero;
	}

	@Override
	public void register(HeroModuleContext ctx) {
		ctx.abilities().register(new EyeLasersAbility());
		ctx.abilities().register(new XRayAbility());
		ctx.abilities().register(new IronFistsAbility());
		ctx.abilities().register(new HandClapAbility());
		ctx.abilities().register(new StunningRoarAbility());
		MadnessFlightController.register(ctx);
		IronFistsController.register(ctx);
		ctx.ticks().global(MadnessAftermathController::pruneGonePlayers);
		ctx.ticks().global(UraniumDefenseController::serverTick);
		ctx.ticks().global(UraniumOffhandController::pruneGonePlayers);
		ctx.ticks().player(MadnessAftermathController::tickPlayer);
		ctx.ticks().player(HomelanderRegenController::tickPlayer);
		ctx.ticks().player(IronFistsController::tickPlayer);
		ctx.ticks().player(UraniumOffhandController::tickPlayer);
		// Homelander's own ability rules: his MADNESS_AFTERMATH blocks casting silently;
		// while MADNESS (milk) is up his abilities are free. Order preserved.
		AbilityRules.blocker((player, id) -> ModEffects.isAftermath(player) ? AbilityDenial.SILENT : null);
		AbilityRules.freeCost(ModEffects::isMadness);
	}
}
