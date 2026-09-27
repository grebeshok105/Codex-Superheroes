package io.github.grebeshok105.codex.hero.invincible;

import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.core.module.HeroModule;
import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import io.github.grebeshok105.codex.hero.invincible.ability.GuardiansBreakerAbility;
import io.github.grebeshok105.codex.hero.invincible.runtime.InvincibleCombatController;
import io.github.grebeshok105.codex.mechanic.ability.ViltrumiteChargeAbility;

public final class InvincibleModule implements HeroModule {
	private final InvincibleHero hero = new InvincibleHero();

	@Override
	public Hero hero() {
		return hero;
	}

	@Override
	public void register(HeroModuleContext ctx) {
		InvincibleItems.register(ctx.content());
		ctx.abilities().register(new GuardiansBreakerAbility());
		InvincibleCombatController.register(ctx);
		ctx.ticks().global(InvincibleCombatController::serverTick);
		ctx.ticks().player((server, p, data) -> ViltrumiteChargeAbility.serverTick(p));
	}
}
