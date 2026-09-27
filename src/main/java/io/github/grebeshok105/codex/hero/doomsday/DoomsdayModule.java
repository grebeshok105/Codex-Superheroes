package io.github.grebeshok105.codex.hero.doomsday;

import io.github.grebeshok105.codex.ability.ChargeTackleAbility;
import io.github.grebeshok105.codex.ability.DoomGripAbility;
import io.github.grebeshok105.codex.ability.DoomsdayBerserkAbility;
import io.github.grebeshok105.codex.ability.DoomsdayBoneSpikeAbility;
import io.github.grebeshok105.codex.ability.DoomsdayRoarAbility;
import io.github.grebeshok105.codex.ability.DoomsdaySmashAbility;
import io.github.grebeshok105.codex.core.module.HeroModule;
import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import io.github.grebeshok105.codex.effect.DoomGripController;
import io.github.grebeshok105.codex.effect.DoomsdayAdaptationController;
import io.github.grebeshok105.codex.effect.DoomsdayFootstepsController;
import io.github.grebeshok105.codex.effect.DoomsdayKryptoniteController;
import io.github.grebeshok105.codex.effect.DoomsdayTierController;
import io.github.grebeshok105.codex.hero.DoomsdayHero;
import io.github.grebeshok105.codex.core.hero.Hero;

public final class DoomsdayModule implements HeroModule {
	private final DoomsdayHero hero = new DoomsdayHero();

	@Override
	public Hero hero() {
		return hero;
	}

	@Override
	public void register(HeroModuleContext ctx) {
		ctx.abilities().register(new DoomsdaySmashAbility());
		ctx.abilities().register(new DoomsdayRoarAbility());
		ctx.abilities().register(new DoomsdayBoneSpikeAbility());
		ctx.abilities().register(new ChargeTackleAbility());
		ctx.abilities().register(new DoomsdayBerserkAbility());
		ctx.abilities().register(new DoomGripAbility());
		DoomsdayAdaptationController.register(ctx);
		DoomsdayTierController.register(ctx);
		DoomsdayKryptoniteController.register(ctx);
		ctx.lifecycle().onLeave(DoomGripController::clear);
		ctx.lifecycle().onDeath(DoomGripController::clear);
		ctx.lifecycle().onHeroClear(DoomGripController::clear);
		ctx.lifecycle().onServerStopped(server -> DoomGripController.resetAll());
		ctx.ticks().global(server -> DoomGripController.serverTick());
		ctx.ticks().global(DoomsdayKryptoniteController::serverTick);
		ctx.ticks().player((server, p, data) -> ChargeTackleAbility.serverTick(p));
		ctx.ticks().player((server, p, data) -> DoomsdayFootstepsController.tickPlayer(p));
	}
}
