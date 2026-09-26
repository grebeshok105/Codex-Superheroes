package io.github.grebeshok105.codex.hero.loki;

import io.github.grebeshok105.codex.ability.LokiAstralClonesAbility;
import io.github.grebeshok105.codex.ability.LokiChaosBoltAbility;
import io.github.grebeshok105.codex.ability.LokiGlamourAbility;
import io.github.grebeshok105.codex.ability.LokiMindCharmAbility;
import io.github.grebeshok105.codex.ability.LokiTesseractBlinkAbility;
import io.github.grebeshok105.codex.core.module.HeroModule;
import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import io.github.grebeshok105.codex.hero.Hero;
import io.github.grebeshok105.codex.hero.LokiHero;

public final class LokiModule implements HeroModule {
	private final LokiHero hero = new LokiHero();

	@Override
	public Hero hero() {
		return hero;
	}

	@Override
	public void register(HeroModuleContext ctx) {
		ctx.abilities().register(new LokiAstralClonesAbility());
		ctx.abilities().register(new LokiTesseractBlinkAbility());
		ctx.abilities().register(new LokiMindCharmAbility());
		ctx.abilities().register(new LokiGlamourAbility());
		ctx.abilities().register(new LokiChaosBoltAbility());
	}
}
