package io.github.grebeshok105.codex.hero.raiden;

import io.github.grebeshok105.codex.hero.raiden.ability.RaidenEyeOfJudgmentAbility;
import io.github.grebeshok105.codex.hero.raiden.ability.RaidenMusouIsshinAbility;
import io.github.grebeshok105.codex.hero.raiden.ability.RaidenMusouShinsetsuAbility;
import io.github.grebeshok105.codex.hero.raiden.ability.RaidenPlungingStrikeAbility;
import io.github.grebeshok105.codex.hero.raiden.ability.RaidenSwordDrawAbility;
import io.github.grebeshok105.codex.hero.raiden.ability.RaidenTranscendenceAbility;
import io.github.grebeshok105.codex.core.module.HeroModule;
import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import io.github.grebeshok105.codex.hero.raiden.runtime.HeavensStrikeController;
import io.github.grebeshok105.codex.hero.raiden.runtime.RaidenAuraController;
import io.github.grebeshok105.codex.hero.raiden.runtime.RaidenBurstController;
import io.github.grebeshok105.codex.hero.raiden.runtime.RaidenLifecycleController;
import io.github.grebeshok105.codex.hero.raiden.runtime.RaidenMusouIsshinController;
import io.github.grebeshok105.codex.hero.raiden.runtime.RaidenPlungingLandingController;
import io.github.grebeshok105.codex.core.hero.Hero;

public final class RaidenModule implements HeroModule {
	private final RaidenHero hero = new RaidenHero();

	@Override
	public Hero hero() {
		return hero;
	}

	@Override
	public void register(HeroModuleContext ctx) {
		// Attachment + items register eagerly inside module bootstrap, matching the timing
		// they had under ModAttachments.init()/ModItems class-init.
		RaidenAttachments.init();
		RaidenItems.register(ctx.content());

		ctx.abilities().register(new RaidenSwordDrawAbility());
		ctx.abilities().register(new RaidenEyeOfJudgmentAbility());
		ctx.abilities().register(new RaidenMusouShinsetsuAbility());
		ctx.abilities().register(new RaidenMusouIsshinAbility());
		ctx.abilities().register(new RaidenPlungingStrikeAbility());
		ctx.abilities().register(new RaidenTranscendenceAbility());

		RaidenLifecycleController.register(ctx);
		ctx.ticks().global(HeavensStrikeController::serverTick);
		ctx.ticks().global(RaidenMusouIsshinController::serverTick);
		ctx.ticks().player(RaidenBurstController::tickPlayer);
		ctx.ticks().player(RaidenAuraController::tickPlayer);
		ctx.ticks().player(RaidenPlungingLandingController::tickPlayer);
	}
}
