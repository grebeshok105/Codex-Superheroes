package io.github.grebeshok105.codex.hero.captainamerica;

import io.github.grebeshok105.codex.ability.CapCounterStanceAbility;
import io.github.grebeshok105.codex.ability.CapShieldDashAbility;
import io.github.grebeshok105.codex.ability.CapShieldSlamAbility;
import io.github.grebeshok105.codex.ability.CapShieldThrowAbility;
import io.github.grebeshok105.codex.core.module.HeroModule;
import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import io.github.grebeshok105.codex.hero.CaptainAmericaHero;
import io.github.grebeshok105.codex.core.hero.Hero;

public final class CaptainAmericaModule implements HeroModule {
	private final CaptainAmericaHero hero = new CaptainAmericaHero();

	@Override
	public Hero hero() {
		return hero;
	}

	@Override
	public void register(HeroModuleContext ctx) {
		ctx.abilities().register(new CapShieldThrowAbility());
		ctx.abilities().register(new CapShieldSlamAbility());
		ctx.abilities().register(new CapShieldDashAbility());
		ctx.abilities().register(new CapCounterStanceAbility());

		ctx.ticks().player((server, p, data) -> CapShieldSlamAbility.serverTick(p));
	}
}
