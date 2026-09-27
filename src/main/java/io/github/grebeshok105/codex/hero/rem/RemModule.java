package io.github.grebeshok105.codex.hero.rem;

import io.github.grebeshok105.codex.ability.RemHealingMagicAbility;
import io.github.grebeshok105.codex.ability.RemHumaIceSpikesAbility;
import io.github.grebeshok105.codex.ability.RemIceBurstAbility;
import io.github.grebeshok105.codex.ability.RemMaceCraterAbility;
import io.github.grebeshok105.codex.ability.RemMorningStarAbility;
import io.github.grebeshok105.codex.ability.RemOniKickAbility;
import io.github.grebeshok105.codex.ability.RemOniRageAbility;
import io.github.grebeshok105.codex.core.module.HeroModule;
import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import io.github.grebeshok105.codex.effect.RamCompanionController;
import io.github.grebeshok105.codex.effect.RemDemonismController;
import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.hero.RemHero;

public final class RemModule implements HeroModule {
	private final RemHero hero = new RemHero();

	@Override
	public Hero hero() {
		return hero;
	}

	@Override
	public void register(HeroModuleContext ctx) {
		ctx.abilities().register(new RemHealingMagicAbility());
		ctx.abilities().register(new RemIceBurstAbility());
		ctx.abilities().register(new RemOniRageAbility());
		ctx.abilities().register(new RemMorningStarAbility());
		ctx.abilities().register(new RemMaceCraterAbility());
		ctx.abilities().register(new RemOniKickAbility());
		ctx.abilities().register(new RemHumaIceSpikesAbility());
		RemDemonismController.register(ctx);
		ctx.ticks().global(RemDemonismController::serverTick);
		ctx.ticks().player(RemDemonismController::tickPlayer);
		ctx.ticks().player(RamCompanionController::tickPlayer);
	}
}
