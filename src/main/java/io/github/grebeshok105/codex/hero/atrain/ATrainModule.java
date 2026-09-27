package io.github.grebeshok105.codex.hero.atrain;

import io.github.grebeshok105.codex.core.module.HeroModule;
import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.hero.atrain.ability.ATrainAdrenalineRushAbility;
import io.github.grebeshok105.codex.hero.atrain.ability.ATrainHyperspeedAbility;
import io.github.grebeshok105.codex.hero.atrain.ability.ATrainMachDashAbility;
import io.github.grebeshok105.codex.hero.atrain.ability.ATrainSonicBoomAbility;

public final class ATrainModule implements HeroModule {
	private final ATrainHero hero = new ATrainHero();

	@Override
	public Hero hero() {
		return hero;
	}

	@Override
	public void register(HeroModuleContext ctx) {
		ctx.abilities().register(new ATrainMachDashAbility());
		ctx.abilities().register(new ATrainSonicBoomAbility());
		ctx.abilities().register(new ATrainHyperspeedAbility());
		ctx.abilities().register(new ATrainAdrenalineRushAbility());
		ATrainItems.register(ctx.content());
	}
}
