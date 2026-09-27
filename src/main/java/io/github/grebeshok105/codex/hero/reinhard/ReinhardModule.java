package io.github.grebeshok105.codex.hero.reinhard;

import io.github.grebeshok105.codex.hero.reinhard.ability.ReinhardAirSlashAbility;
import io.github.grebeshok105.codex.hero.reinhard.ability.ReinhardCounterRiposteAbility;
import io.github.grebeshok105.codex.hero.reinhard.ability.ReinhardDivineAuraAbility;
import io.github.grebeshok105.codex.hero.reinhard.ability.ReinhardJudgmentMarkAbility;
import io.github.grebeshok105.codex.hero.reinhard.ability.ReinhardSpeedJudgmentAbility;
import io.github.grebeshok105.codex.hero.reinhard.ability.ReinhardSwordDrawAbility;
import io.github.grebeshok105.codex.hero.reinhard.ability.ReinhardSwordWaveAbility;
import io.github.grebeshok105.codex.hero.reinhard.ability.ReinhardWishAbility;
import io.github.grebeshok105.codex.hero.reinhard.net.ReinhardCeremonyS2CPayload;
import io.github.grebeshok105.codex.hero.reinhard.net.ReinhardDarknessS2CPayload;
import io.github.grebeshok105.codex.hero.reinhard.net.ReinhardSwordGateS2CPayload;
import io.github.grebeshok105.codex.hero.reinhard.net.ReinhardSwordKillS2CPayload;
import io.github.grebeshok105.codex.hero.reinhard.net.ReinhardTimeSlowS2CPayload;
import io.github.grebeshok105.codex.hero.reinhard.net.ReinhardWishConfirmC2SPayload;
import io.github.grebeshok105.codex.hero.reinhard.net.ReinhardWishOptionsS2CPayload;
import io.github.grebeshok105.codex.hero.reinhard.runtime.ReinhardController;
import io.github.grebeshok105.codex.hero.reinhard.runtime.ReinhardSpeedJudgmentController;
import io.github.grebeshok105.codex.hero.reinhard.runtime.ReinhardSwordDeathMarkController;
import io.github.grebeshok105.codex.hero.reinhard.runtime.ReinhardSwordDrawCeremonyController;
import io.github.grebeshok105.codex.hero.reinhard.runtime.ReinhardSwordDrawGateController;
import io.github.grebeshok105.codex.hero.reinhard.runtime.ReinhardTimeSlowController;
import io.github.grebeshok105.codex.hero.reinhard.sound.ReinhardSounds;
import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.core.module.HeroModule;
import io.github.grebeshok105.codex.core.module.HeroModuleContext;

public final class ReinhardModule implements HeroModule {
	private final ReinhardHero hero = new ReinhardHero();

	@Override
	public Hero hero() {
		return hero;
	}

	@Override
	public void register(HeroModuleContext ctx) {
		// Attachment + sounds register eagerly inside module bootstrap, matching the timing
		// they had under ModAttachments.init()/ModSounds.init().
		ReinhardAttachments.init();
		ReinhardSounds.init();
		ReinhardItems.register(ctx.content());

		ctx.abilities().register(new ReinhardSwordDrawAbility());
		ctx.abilities().register(new ReinhardAirSlashAbility());
		ctx.abilities().register(new ReinhardSwordWaveAbility());
		ctx.abilities().register(new ReinhardCounterRiposteAbility());
		ctx.abilities().register(new ReinhardDivineAuraAbility());
		ctx.abilities().register(new ReinhardSpeedJudgmentAbility());
		ctx.abilities().register(new ReinhardJudgmentMarkAbility());
		ctx.abilities().register(new ReinhardWishAbility());

		ctx.payloads().s2c(ReinhardWishOptionsS2CPayload.TYPE, ReinhardWishOptionsS2CPayload.STREAM_CODEC);
		ctx.payloads().s2c(ReinhardCeremonyS2CPayload.TYPE, ReinhardCeremonyS2CPayload.STREAM_CODEC);
		ctx.payloads().s2c(ReinhardSwordGateS2CPayload.TYPE, ReinhardSwordGateS2CPayload.STREAM_CODEC);
		ctx.payloads().s2c(ReinhardSwordKillS2CPayload.TYPE, ReinhardSwordKillS2CPayload.STREAM_CODEC);
		ctx.payloads().s2c(ReinhardDarknessS2CPayload.TYPE, ReinhardDarknessS2CPayload.STREAM_CODEC);
		ctx.payloads().s2c(ReinhardTimeSlowS2CPayload.TYPE, ReinhardTimeSlowS2CPayload.STREAM_CODEC);
		ctx.payloads().c2s(ReinhardWishConfirmC2SPayload.TYPE, ReinhardWishConfirmC2SPayload.STREAM_CODEC,
				(payload, context) -> ReinhardWishAbility.handleWishConfirm(context.player(), payload.damageTypeId()));

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
