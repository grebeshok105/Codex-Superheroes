package io.github.grebeshok105.codex.hero.omniman;

import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.core.module.HeroModule;
import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import io.github.grebeshok105.codex.core.net.C2SGuards;
import io.github.grebeshok105.codex.hero.omniman.ability.OmnimanThinkMarkAbility;
import io.github.grebeshok105.codex.hero.omniman.ability.OmnimanViltrumiteRushAbility;
import io.github.grebeshok105.codex.hero.omniman.ability.OmnimanWorldBreakerAbility;
import io.github.grebeshok105.codex.hero.omniman.net.ThinkMarkDashC2SPayload;
import io.github.grebeshok105.codex.hero.omniman.net.ThinkMarkS2CPayload;
import io.github.grebeshok105.codex.hero.omniman.runtime.OmnimanMomentumController;
import io.github.grebeshok105.codex.hero.omniman.runtime.OmnimanReactionRule;

public final class OmnimanModule implements HeroModule {
	private final OmnimanHero hero = new OmnimanHero();

	@Override
	public Hero hero() {
		return hero;
	}

	@Override
	public void register(HeroModuleContext ctx) {
		OmnimanItems.register(ctx.content());
		ctx.abilities().register(new OmnimanViltrumiteRushAbility());
		ctx.abilities().register(new OmnimanThinkMarkAbility());
		ctx.abilities().register(new OmnimanWorldBreakerAbility());
		ctx.payloads().s2c(ThinkMarkS2CPayload.TYPE, ThinkMarkS2CPayload.STREAM_CODEC);
		ctx.payloads().c2s(ThinkMarkDashC2SPayload.TYPE, ThinkMarkDashC2SPayload.STREAM_CODEC,
				(payload, context) -> {
					if (C2SGuards.requireHero(context.player(), OmnimanHero.ID)) {
						OmnimanThinkMarkAbility.triggerDash(context.player());
					}
				});
		OmnimanMomentumController.register(ctx);
		ctx.lifecycle().onLeave(OmnimanThinkMarkAbility::clear);
		ctx.lifecycle().onDeath(OmnimanThinkMarkAbility::clear);
		ctx.lifecycle().onHeroClear(OmnimanThinkMarkAbility::clear);
		ctx.lifecycle().onHeroTransformed(OmnimanReactionRule::onTransformed);
		ctx.ticks().global(OmnimanMomentumController::serverTick);
		ctx.ticks().player((server, p, data) -> OmnimanViltrumiteRushAbility.serverTick(p));
		ctx.ticks().player((server, p, data) -> OmnimanThinkMarkAbility.serverTick(p));
		ctx.ticks().player(OmnimanMomentumController::tickPlayer);
	}
}
