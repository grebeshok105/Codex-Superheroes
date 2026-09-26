package io.github.grebeshok105.codex.hero.invincible;

import io.github.grebeshok105.codex.ability.GuardiansBreakerAbility;
import io.github.grebeshok105.codex.ability.ViltrumiteChargeAbility;
import io.github.grebeshok105.codex.core.module.HeroModule;
import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import io.github.grebeshok105.codex.effect.InvincibleCombatController;
import io.github.grebeshok105.codex.hero.Hero;
import io.github.grebeshok105.codex.hero.InvincibleHero;

public final class InvincibleModule implements HeroModule {
	private final InvincibleHero hero = new InvincibleHero();

	@Override
	public Hero hero() {
		return hero;
	}

	@Override
	public void register(HeroModuleContext ctx) {
		ctx.abilities().register(new ViltrumiteChargeAbility());
		ctx.abilities().register(new GuardiansBreakerAbility());
		InvincibleCombatController.register(ctx);
		ctx.ticks().global(InvincibleCombatController::serverTick);
		ctx.ticks().player((server, p, data) -> ViltrumiteChargeAbility.serverTick(p));
	}
}
