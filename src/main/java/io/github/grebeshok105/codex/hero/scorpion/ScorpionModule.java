package io.github.grebeshok105.codex.hero.scorpion;

import io.github.grebeshok105.codex.ability.ScorpionFireTeleportAbility;
import io.github.grebeshok105.codex.ability.ScorpionHellBreathAbility;
import io.github.grebeshok105.codex.ability.ScorpionHellfireAbility;
import io.github.grebeshok105.codex.ability.ScorpionSpearAbility;
import io.github.grebeshok105.codex.core.module.HeroModule;
import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import io.github.grebeshok105.codex.effect.ScorpionController;
import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.hero.ScorpionHero;

public final class ScorpionModule implements HeroModule {
	private final ScorpionHero hero = new ScorpionHero();

	@Override
	public Hero hero() {
		return hero;
	}

	@Override
	public void register(HeroModuleContext ctx) {
		ctx.abilities().register(new ScorpionSpearAbility());
		ctx.abilities().register(new ScorpionHellfireAbility());
		ctx.abilities().register(new ScorpionFireTeleportAbility());
		ctx.abilities().register(new ScorpionHellBreathAbility());
		ctx.ticks().global(ScorpionController::serverTick);
		ctx.ticks().player(ScorpionController::tickPlayer);
	}
}
