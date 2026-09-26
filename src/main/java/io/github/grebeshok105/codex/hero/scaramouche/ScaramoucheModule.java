package io.github.grebeshok105.codex.hero.scaramouche;

import io.github.grebeshok105.codex.ability.ScaramoucheElectroSwirlAbility;
import io.github.grebeshok105.codex.ability.ScaramoucheSkyfallBurstAbility;
import io.github.grebeshok105.codex.ability.ScaramoucheWindPrisonAbility;
import io.github.grebeshok105.codex.ability.ScaramoucheWindstepAbility;
import io.github.grebeshok105.codex.core.module.HeroModule;
import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import io.github.grebeshok105.codex.hero.Hero;
import io.github.grebeshok105.codex.hero.ScaramoucheHero;

public final class ScaramoucheModule implements HeroModule {
	private final ScaramoucheHero hero = new ScaramoucheHero();

	@Override
	public Hero hero() {
		return hero;
	}

	@Override
	public void register(HeroModuleContext ctx) {
		ctx.abilities().register(new ScaramoucheWindstepAbility());
		ctx.abilities().register(new ScaramoucheElectroSwirlAbility());
		ctx.abilities().register(new ScaramoucheWindPrisonAbility());
		ctx.abilities().register(new ScaramoucheSkyfallBurstAbility());
	}
}
