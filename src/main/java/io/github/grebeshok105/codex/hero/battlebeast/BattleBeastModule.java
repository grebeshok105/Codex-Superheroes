package io.github.grebeshok105.codex.hero.battlebeast;

import io.github.grebeshok105.codex.ability.BattleBeastAxeCleaveAbility;
import io.github.grebeshok105.codex.ability.BattleBeastBloodlustAbility;
import io.github.grebeshok105.codex.ability.BattleBeastPredatorLeapAbility;
import io.github.grebeshok105.codex.ability.BattleBeastWarRoarAbility;
import io.github.grebeshok105.codex.core.module.HeroModule;
import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import io.github.grebeshok105.codex.effect.BattleBeastCurseController;
import io.github.grebeshok105.codex.hero.BattleBeastHero;
import io.github.grebeshok105.codex.hero.Hero;

public final class BattleBeastModule implements HeroModule {
	private final BattleBeastHero hero = new BattleBeastHero();

	@Override
	public Hero hero() {
		return hero;
	}

	@Override
	public void register(HeroModuleContext ctx) {
		ctx.abilities().register(new BattleBeastPredatorLeapAbility());
		ctx.abilities().register(new BattleBeastAxeCleaveAbility());
		ctx.abilities().register(new BattleBeastWarRoarAbility());
		ctx.abilities().register(new BattleBeastBloodlustAbility());
		ctx.lifecycle().onJoin(BattleBeastCurseController::reapplyOnJoin);
		ctx.lifecycle().onServerStopped(server -> BattleBeastCurseController.resetAll());
		ctx.ticks().player(BattleBeastCurseController::tickPlayer);
	}
}
