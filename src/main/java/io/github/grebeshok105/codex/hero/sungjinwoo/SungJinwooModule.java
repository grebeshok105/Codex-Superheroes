package io.github.grebeshok105.codex.hero.sungjinwoo;

import io.github.grebeshok105.codex.ability.AriseAbility;
import io.github.grebeshok105.codex.ability.MonarchsDomainAbility;
import io.github.grebeshok105.codex.ability.RulersAuthorityAbility;
import io.github.grebeshok105.codex.ability.SacrificeAbility;
import io.github.grebeshok105.codex.ability.ShadowExchangeAbility;
import io.github.grebeshok105.codex.ability.ShadowExtractionAbility;
import io.github.grebeshok105.codex.core.module.HeroModule;
import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import io.github.grebeshok105.codex.effect.MonarchsDomainController;
import io.github.grebeshok105.codex.effect.SungJinwooController;
import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.hero.SungJinwooHero;

public final class SungJinwooModule implements HeroModule {
	private final SungJinwooHero hero = new SungJinwooHero();

	@Override
	public Hero hero() {
		return hero;
	}

	@Override
	public void register(HeroModuleContext ctx) {
		ctx.abilities().register(new AriseAbility());
		ctx.abilities().register(new ShadowExchangeAbility());
		ctx.abilities().register(new SacrificeAbility());
		ctx.abilities().register(new RulersAuthorityAbility());
		ctx.abilities().register(new ShadowExtractionAbility());
		ctx.abilities().register(new MonarchsDomainAbility());
		SungJinwooController.register(ctx);
		ctx.ticks().player(SungJinwooController::tickPlayer);
		ctx.ticks().player(MonarchsDomainController::tickPlayer);
	}
}
