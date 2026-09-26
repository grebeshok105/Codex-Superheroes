package io.github.grebeshok105.codex.hero.raiden;

import io.github.grebeshok105.codex.ability.RaidenEyeOfJudgmentAbility;
import io.github.grebeshok105.codex.ability.RaidenMusouIsshinAbility;
import io.github.grebeshok105.codex.ability.RaidenMusouShinsetsuAbility;
import io.github.grebeshok105.codex.ability.RaidenPlungingStrikeAbility;
import io.github.grebeshok105.codex.ability.RaidenSwordDrawAbility;
import io.github.grebeshok105.codex.ability.RaidenTranscendenceAbility;
import io.github.grebeshok105.codex.core.module.HeroModule;
import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import io.github.grebeshok105.codex.effect.RaidenAuraController;
import io.github.grebeshok105.codex.effect.RaidenBurstController;
import io.github.grebeshok105.codex.effect.RaidenLifecycleController;
import io.github.grebeshok105.codex.effect.RaidenMusouIsshinController;
import io.github.grebeshok105.codex.effect.RaidenPlungingLandingController;
import io.github.grebeshok105.codex.hero.Hero;
import io.github.grebeshok105.codex.hero.RaidenHero;

public final class RaidenModule implements HeroModule {
	private final RaidenHero hero = new RaidenHero();

	@Override
	public Hero hero() {
		return hero;
	}

	@Override
	public void register(HeroModuleContext ctx) {
		ctx.abilities().register(new RaidenSwordDrawAbility());
		ctx.abilities().register(new RaidenEyeOfJudgmentAbility());
		ctx.abilities().register(new RaidenMusouShinsetsuAbility());
		ctx.abilities().register(new RaidenMusouIsshinAbility());
		ctx.abilities().register(new RaidenPlungingStrikeAbility());
		ctx.abilities().register(new RaidenTranscendenceAbility());

		RaidenLifecycleController.register(ctx);
		RaidenPlungingLandingController.register(ctx);
		ctx.ticks().global(RaidenMusouIsshinController::serverTick);
		ctx.ticks().player(RaidenBurstController::tickPlayer);
		ctx.ticks().player(RaidenAuraController::tickPlayer);
		ctx.ticks().player(RaidenPlungingLandingController::tickPlayer);
	}
}
