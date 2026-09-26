package io.github.grebeshok105.codex.hero.omniman;

import io.github.grebeshok105.codex.ability.OmnimanThinkMarkAbility;
import io.github.grebeshok105.codex.ability.OmnimanViltrumiteRushAbility;
import io.github.grebeshok105.codex.ability.OmnimanWorldBreakerAbility;
import io.github.grebeshok105.codex.core.module.HeroModule;
import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import io.github.grebeshok105.codex.effect.OmnimanMomentumController;
import io.github.grebeshok105.codex.hero.Hero;
import io.github.grebeshok105.codex.hero.OmnimanHero;

public final class OmnimanModule implements HeroModule {
	private final OmnimanHero hero = new OmnimanHero();

	@Override
	public Hero hero() {
		return hero;
	}

	@Override
	public void register(HeroModuleContext ctx) {
		ctx.abilities().register(new OmnimanViltrumiteRushAbility());
		ctx.abilities().register(new OmnimanThinkMarkAbility());
		ctx.abilities().register(new OmnimanWorldBreakerAbility());
		OmnimanMomentumController.register(ctx);
		ctx.lifecycle().onLeave(OmnimanThinkMarkAbility::clear);
		ctx.lifecycle().onDeath(OmnimanThinkMarkAbility::clear);
		ctx.lifecycle().onHeroClear(OmnimanThinkMarkAbility::clear);
		ctx.lifecycle().onServerStopped(server -> OmnimanThinkMarkAbility.resetAll());
		ctx.ticks().global(OmnimanMomentumController::serverTick);
		ctx.ticks().player((server, p, data) -> OmnimanViltrumiteRushAbility.serverTick(p));
		ctx.ticks().player((server, p, data) -> OmnimanThinkMarkAbility.serverTick(p));
		ctx.ticks().player(OmnimanMomentumController::tickPlayer);
	}
}
