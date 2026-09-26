package io.github.grebeshok105.codex.hero.reinhard;

import io.github.grebeshok105.codex.ability.ReinhardAirSlashAbility;
import io.github.grebeshok105.codex.ability.ReinhardCounterRiposteAbility;
import io.github.grebeshok105.codex.ability.ReinhardDivineAuraAbility;
import io.github.grebeshok105.codex.ability.ReinhardJudgmentMarkAbility;
import io.github.grebeshok105.codex.ability.ReinhardSpeedJudgmentAbility;
import io.github.grebeshok105.codex.ability.ReinhardSwordDrawAbility;
import io.github.grebeshok105.codex.ability.ReinhardSwordWaveAbility;
import io.github.grebeshok105.codex.ability.ReinhardWishAbility;
import io.github.grebeshok105.codex.core.module.HeroModule;
import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import io.github.grebeshok105.codex.effect.ReinhardController;
import io.github.grebeshok105.codex.effect.ReinhardSpeedJudgmentController;
import io.github.grebeshok105.codex.effect.ReinhardSwordDeathMarkController;
import io.github.grebeshok105.codex.effect.ReinhardSwordDrawCeremonyController;
import io.github.grebeshok105.codex.effect.ReinhardSwordDrawGateController;
import io.github.grebeshok105.codex.effect.ReinhardTimeSlowController;
import io.github.grebeshok105.codex.hero.Hero;
import io.github.grebeshok105.codex.hero.ReinhardHero;

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

		// Ceremony registers first: its cancelCeremony must run before TimeSlow's onPlayerGone
		// in the leave/death hook lists (former registerPlayerLifecycle row order).
		ReinhardSwordDrawCeremonyController.register(ctx);
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
