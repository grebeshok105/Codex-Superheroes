package com.example.superheroes.hero.reinhard;

import com.example.superheroes.ability.ReinhardAirSlashAbility;
import com.example.superheroes.ability.ReinhardCounterRiposteAbility;
import com.example.superheroes.ability.ReinhardDivineAuraAbility;
import com.example.superheroes.ability.ReinhardJudgmentMarkAbility;
import com.example.superheroes.ability.ReinhardSpeedJudgmentAbility;
import com.example.superheroes.ability.ReinhardSwordDrawAbility;
import com.example.superheroes.ability.ReinhardSwordWaveAbility;
import com.example.superheroes.ability.ReinhardWishAbility;
import com.example.superheroes.core.module.HeroModule;
import com.example.superheroes.core.module.HeroModuleContext;
import com.example.superheroes.effect.ReinhardController;
import com.example.superheroes.effect.ReinhardSpeedJudgmentController;
import com.example.superheroes.effect.ReinhardSwordDeathMarkController;
import com.example.superheroes.effect.ReinhardSwordDrawCeremonyController;
import com.example.superheroes.effect.ReinhardSwordDrawGateController;
import com.example.superheroes.effect.ReinhardTimeSlowController;
import com.example.superheroes.hero.Hero;
import com.example.superheroes.hero.ReinhardHero;

public final class ReinhardModule implements HeroModule {
	private final ReinhardHero hero = new ReinhardHero();

	@Override
	public Hero hero() {
		return hero;
	}

	@Override
	public void register(HeroModuleContext ctx) {
		ctx.abilities().register(new ReinhardSwordDrawAbility());
		ctx.abilities().register(new ReinhardAirSlashAbility());
		ctx.abilities().register(new ReinhardSwordWaveAbility());
		ctx.abilities().register(new ReinhardCounterRiposteAbility());
		ctx.abilities().register(new ReinhardDivineAuraAbility());
		ctx.abilities().register(new ReinhardSpeedJudgmentAbility());
		ctx.abilities().register(new ReinhardJudgmentMarkAbility());
		ctx.abilities().register(new ReinhardWishAbility());

		ReinhardTimeSlowController.register(ctx);
		ReinhardController.register(ctx);
		ReinhardSwordDeathMarkController.register(ctx);
		ctx.ticks().global(ReinhardTimeSlowController::tick);
		ctx.ticks().global(ReinhardSwordDrawGateController::pruneGonePlayers);
		ctx.ticks().global(ReinhardSpeedJudgmentController::tick);
		ctx.ticks().player(ReinhardController::tickPlayer);
		ctx.ticks().player(ReinhardSwordDrawCeremonyController::tickPlayer);
		ctx.ticks().player(ReinhardSwordDrawGateController::tickPlayer);
	}
}
