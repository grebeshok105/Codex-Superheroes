package io.github.grebeshok105.codex.hero.rem;

import io.github.grebeshok105.codex.core.module.HeroModule;
import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import io.github.grebeshok105.codex.hero.rem.ability.RemHealingMagicAbility;
import io.github.grebeshok105.codex.hero.rem.ability.RemHumaIceSpikesAbility;
import io.github.grebeshok105.codex.hero.rem.ability.RemIceBurstAbility;
import io.github.grebeshok105.codex.hero.rem.ability.RemMaceCraterAbility;
import io.github.grebeshok105.codex.hero.rem.ability.RemMorningStarAbility;
import io.github.grebeshok105.codex.hero.rem.ability.RemOniKickAbility;
import io.github.grebeshok105.codex.hero.rem.ability.RemOniRageAbility;
import io.github.grebeshok105.codex.hero.rem.net.RemDemonismS2CPayload;
import io.github.grebeshok105.codex.hero.rem.runtime.RamCompanionController;
import io.github.grebeshok105.codex.hero.rem.runtime.RemDemonismController;
import io.github.grebeshok105.codex.hero.rem.runtime.RemEntities;
import io.github.grebeshok105.codex.core.hero.Hero;

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
		RemItems.register(ctx.content());
		RemEntities.register();
		ctx.payloads().s2c(RemDemonismS2CPayload.TYPE, RemDemonismS2CPayload.STREAM_CODEC);
		RemDemonismController.register(ctx);
		ctx.ticks().global(RemDemonismController::serverTick);
		ctx.ticks().player(RemDemonismController::tickPlayer);
		ctx.ticks().player(RamCompanionController::tickPlayer);
	}
}
