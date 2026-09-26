package com.example.superheroes.hero.doomsday;

import com.example.superheroes.ability.ChargeTackleAbility;
import com.example.superheroes.ability.DoomGripAbility;
import com.example.superheroes.ability.DoomsdayBerserkAbility;
import com.example.superheroes.ability.DoomsdayBoneSpikeAbility;
import com.example.superheroes.ability.DoomsdayRoarAbility;
import com.example.superheroes.ability.DoomsdaySmashAbility;
import com.example.superheroes.core.module.HeroModule;
import com.example.superheroes.core.module.HeroModuleContext;
import com.example.superheroes.effect.DoomGripController;
import com.example.superheroes.effect.DoomsdayAdaptationController;
import com.example.superheroes.effect.DoomsdayFootstepsController;
import com.example.superheroes.effect.DoomsdayKryptoniteController;
import com.example.superheroes.effect.DoomsdayTierController;
import com.example.superheroes.hero.DoomsdayHero;
import com.example.superheroes.hero.Hero;

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
		ctx.ticks().global(server -> DoomGripController.serverTick());
		ctx.ticks().global(DoomsdayKryptoniteController::serverTick);
		ctx.ticks().player((server, p, data) -> ChargeTackleAbility.serverTick(p));
		ctx.ticks().player((server, p, data) -> DoomsdayFootstepsController.tickPlayer(p));
	}
}
