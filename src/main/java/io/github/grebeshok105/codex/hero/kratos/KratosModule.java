package io.github.grebeshok105.codex.hero.kratos;

import io.github.grebeshok105.codex.ability.KratosBladeStormAbility;
import io.github.grebeshok105.codex.ability.KratosChainWhirlAbility;
import io.github.grebeshok105.codex.ability.KratosGodSlayerAbility;
import io.github.grebeshok105.codex.ability.KratosLeviathanThrowAbility;
import io.github.grebeshok105.codex.ability.KratosSpartanRageAbility;
import io.github.grebeshok105.codex.core.module.HeroModule;
import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import io.github.grebeshok105.codex.effect.KratosHandStrikeFxController;
import io.github.grebeshok105.codex.effect.KratosRageController;
import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.hero.KratosHero;

public final class KratosModule implements HeroModule {
	private final KratosHero hero = new KratosHero();

	@Override
	public Hero hero() {
		return hero;
	}

	@Override
	public void register(HeroModuleContext ctx) {
		ctx.abilities().register(new KratosSpartanRageAbility());
		ctx.abilities().register(new KratosBladeStormAbility());
		ctx.abilities().register(new KratosChainWhirlAbility());
		ctx.abilities().register(new KratosLeviathanThrowAbility());
		ctx.abilities().register(new KratosGodSlayerAbility());

		KratosRageController.register(ctx);
		KratosHandStrikeFxController.register(ctx);
		ctx.ticks().global(KratosRageController::serverTick);
	}
}
