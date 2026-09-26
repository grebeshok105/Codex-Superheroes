package io.github.grebeshok105.codex.hero.naruto;

import io.github.grebeshok105.codex.ability.AbilityIds;
import io.github.grebeshok105.codex.ability.NarutoBijuudamaAbility;
import io.github.grebeshok105.codex.ability.NarutoOodamaRasenganAbility;
import io.github.grebeshok105.codex.ability.NarutoRasenganAbility;
import io.github.grebeshok105.codex.ability.NarutoRasenshurikenAbility;
import io.github.grebeshok105.codex.ability.NarutoSageModeAbility;
import io.github.grebeshok105.codex.ability.NarutoShadowClonesAbility;
import io.github.grebeshok105.codex.core.module.HeroModule;
import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import io.github.grebeshok105.codex.effect.KawarimiController;
import io.github.grebeshok105.codex.effect.NarutoWallRunController;
import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.hero.NarutoHero;

public final class NarutoModule implements HeroModule {
	private final NarutoHero hero = new NarutoHero();

	@Override
	public Hero hero() {
		return hero;
	}

	@Override
	public void register(HeroModuleContext ctx) {
		ctx.abilities().register(new NarutoRasenganAbility());
		ctx.abilities().register(new NarutoOodamaRasenganAbility());
		ctx.abilities().register(new NarutoRasenshurikenAbility());
		ctx.abilities().register(new NarutoSageModeAbility());
		ctx.abilities().register(new NarutoBijuudamaAbility());
		ctx.abilities().register(new NarutoShadowClonesAbility());
		KawarimiController.register(ctx);
		ctx.ticks().player((server, p, data) -> NarutoRasenganAbility.serverTick(p));
		ctx.ticks().player((server, p, data) -> NarutoOodamaRasenganAbility.serverTick(p));
		ctx.ticks().player((server, p, data) -> NarutoRasenshurikenAbility.serverTick(p));
		ctx.ticks().player(NarutoWallRunController::tickPlayer);
		ctx.ticks().activeAbility(AbilityIds.NARUTO_SAGE_MODE,
				(server, p, data) -> NarutoSageModeAbility.serverTick(p));
	}
}
