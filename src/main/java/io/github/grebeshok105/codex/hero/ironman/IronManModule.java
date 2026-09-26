package io.github.grebeshok105.codex.hero.ironman;

import io.github.grebeshok105.codex.ability.IronManFlightAbility;
import io.github.grebeshok105.codex.ability.RepulsorAbility;
import io.github.grebeshok105.codex.ability.RepulsorChargeController;
import io.github.grebeshok105.codex.ability.SmartMissileAbility;
import io.github.grebeshok105.codex.ability.SupersonicAbility;
import io.github.grebeshok105.codex.ability.UnibeamAbility;
import io.github.grebeshok105.codex.ability.ironman.IronManLegionAbility;
import io.github.grebeshok105.codex.ability.ironman.IronManNanoFormAbility;
import io.github.grebeshok105.codex.ability.ironman.IronManNanoFormController;
import io.github.grebeshok105.codex.ability.ironman.IronManSuitSwitchAbility;
import io.github.grebeshok105.codex.ability.ironman.IronManSuitSyncController;
import io.github.grebeshok105.codex.core.module.HeroModule;
import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import io.github.grebeshok105.codex.effect.IronManAutoEjectController;
import io.github.grebeshok105.codex.effect.IronManJarvisController;
import io.github.grebeshok105.codex.effect.IronManReactorTracker;
import io.github.grebeshok105.codex.effect.UnibeamController;
import io.github.grebeshok105.codex.hero.Hero;
import io.github.grebeshok105.codex.hero.IronManHero;

public final class IronManModule implements HeroModule {
	private final IronManHero hero = new IronManHero();

	@Override
	public Hero hero() {
		return hero;
	}

	@Override
	public void register(HeroModuleContext ctx) {
		ctx.abilities().register(new IronManFlightAbility());
		ctx.abilities().register(new SupersonicAbility());
		ctx.abilities().register(new RepulsorAbility());
		ctx.abilities().register(new UnibeamAbility());
		ctx.abilities().register(new SmartMissileAbility());
		ctx.abilities().register(new IronManNanoFormAbility());
		ctx.abilities().register(new IronManSuitSwitchAbility());
		ctx.abilities().register(new IronManLegionAbility());
		IronManJarvisController.register(ctx);
		IronManNanoFormController.register(ctx);
		IronManSuitSyncController.register(ctx);
		ctx.lifecycle().onLeave(p -> UnibeamController.clearState(p.getUUID()));
		ctx.ticks().global(UnibeamController::pruneGonePlayers);
		ctx.ticks().global(IronManJarvisController::serverTick);
		ctx.ticks().player((server, p, data) -> IronManNanoFormController.serverTick(p));
		ctx.ticks().player((server, p, data) -> RepulsorChargeController.serverTick(p));
		ctx.ticks().player(UnibeamController::tickPlayer);
		ctx.ticks().player((server, p, data) -> IronManReactorTracker.tick(p));
		ctx.ticks().player((server, p, data) -> IronManAutoEjectController.tick(p, server.getTickCount()));
	}
}
