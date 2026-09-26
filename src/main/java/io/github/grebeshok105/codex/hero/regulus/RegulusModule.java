package io.github.grebeshok105.codex.hero.regulus;

import io.github.grebeshok105.codex.ability.CounterStrikeAbility;
import io.github.grebeshok105.codex.ability.GreedsEmbraceAbility;
import io.github.grebeshok105.codex.ability.LionHeartAbility;
import io.github.grebeshok105.codex.ability.LionRoarAbility;
import io.github.grebeshok105.codex.ability.ManiaOfGreedAbility;
import io.github.grebeshok105.codex.core.module.HeroModule;
import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import io.github.grebeshok105.codex.effect.GreedCageController;
import io.github.grebeshok105.codex.effect.RegulusGreedController;
import io.github.grebeshok105.codex.effect.RegulusMadnessController;
import io.github.grebeshok105.codex.effect.RegulusTotemController;
import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.hero.RegulusHero;

public final class RegulusModule implements HeroModule {
	private final RegulusHero hero = new RegulusHero();

	@Override
	public Hero hero() {
		return hero;
	}

	@Override
	public void register(HeroModuleContext ctx) {
		ctx.abilities().register(new LionHeartAbility());
		ctx.abilities().register(new ManiaOfGreedAbility());
		ctx.abilities().register(new GreedsEmbraceAbility());
		ctx.abilities().register(new LionRoarAbility());
		ctx.abilities().register(new CounterStrikeAbility());
		RegulusTotemController.register(ctx);
		RegulusGreedController.register(ctx);
		RegulusMadnessController.register(ctx);
		ctx.ticks().global(RegulusGreedController::tickFreezes);
		ctx.ticks().global(GreedCageController::tick);
		ctx.ticks().global(RegulusMadnessController::tickCounters);
		ctx.ticks().player(RegulusGreedController::tickPlayer);
		ctx.ticks().player(RegulusMadnessController::tickPlayer);
	}
}
