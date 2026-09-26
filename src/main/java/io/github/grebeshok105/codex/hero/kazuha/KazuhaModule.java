package io.github.grebeshok105.codex.hero.kazuha;

import io.github.grebeshok105.codex.ability.KazuhaAutumnWhirlwindAbility;
import io.github.grebeshok105.codex.ability.KazuhaChihayaburuAbility;
import io.github.grebeshok105.codex.ability.KazuhaMapleStormAbility;
import io.github.grebeshok105.codex.ability.KazuhaMidareRanzanAbility;
import io.github.grebeshok105.codex.core.module.HeroModule;
import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.hero.KazuhaHero;

public final class KazuhaModule implements HeroModule {
	private final KazuhaHero hero = new KazuhaHero();

	@Override
	public Hero hero() {
		return hero;
	}

	@Override
	public void register(HeroModuleContext ctx) {
		ctx.abilities().register(new KazuhaChihayaburuAbility());
		ctx.abilities().register(new KazuhaMidareRanzanAbility());
		ctx.abilities().register(new KazuhaAutumnWhirlwindAbility());
		ctx.abilities().register(new KazuhaMapleStormAbility());
	}
}
