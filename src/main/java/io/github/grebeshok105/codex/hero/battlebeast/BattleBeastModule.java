package io.github.grebeshok105.codex.hero.battlebeast;

import io.github.grebeshok105.codex.core.module.HeroModule;
import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import io.github.grebeshok105.codex.hero.battlebeast.ability.BattleBeastAxeCleaveAbility;
import io.github.grebeshok105.codex.hero.battlebeast.ability.BattleBeastBloodlustAbility;
import io.github.grebeshok105.codex.hero.battlebeast.ability.BattleBeastPredatorLeapAbility;
import io.github.grebeshok105.codex.hero.battlebeast.ability.BattleBeastWarRoarAbility;
import io.github.grebeshok105.codex.hero.battlebeast.runtime.BattleBeastCurseController;
import io.github.grebeshok105.codex.core.hero.Hero;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

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
		ctx.ticks().player(BattleBeastCurseController::tickPlayer);
		BattleBeastItems.register(ctx.content());
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) ->
				BattleBeastCommands.register(dispatcher));
	}
}
