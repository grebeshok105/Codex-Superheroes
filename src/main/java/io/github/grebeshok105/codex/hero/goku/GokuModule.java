package io.github.grebeshok105.codex.hero.goku;

import io.github.grebeshok105.codex.ability.AbilityIds;
import io.github.grebeshok105.codex.ability.GokuInstantTransmissionAbility;
import io.github.grebeshok105.codex.ability.GokuKamehamehaAbility;
import io.github.grebeshok105.codex.ability.GokuKiChargeAbility;
import io.github.grebeshok105.codex.ability.GokuSolarFlareAbility;
import io.github.grebeshok105.codex.ability.GokuSpiritBombAbility;
import io.github.grebeshok105.codex.ability.GokuSuperSaiyanAuraAbility;
import io.github.grebeshok105.codex.core.module.HeroModule;
import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import io.github.grebeshok105.codex.effect.GokuKiResilienceController;
import io.github.grebeshok105.codex.effect.GokuKiStackController;
import io.github.grebeshok105.codex.hero.GokuHero;
import io.github.grebeshok105.codex.hero.Hero;

public final class GokuModule implements HeroModule {
	private final GokuHero hero = new GokuHero();

	@Override
	public Hero hero() {
		return hero;
	}

	@Override
	public void register(HeroModuleContext ctx) {
		ctx.abilities().register(new GokuKamehamehaAbility());
		ctx.abilities().register(new GokuInstantTransmissionAbility());
		ctx.abilities().register(new GokuKiChargeAbility());
		ctx.abilities().register(new GokuSolarFlareAbility());
		ctx.abilities().register(new GokuSpiritBombAbility());
		ctx.abilities().register(new GokuSuperSaiyanAuraAbility());
		GokuKiStackController.register(ctx);
		ctx.ticks().player((server, p, data) -> GokuKamehamehaAbility.serverTick(p));
		ctx.ticks().player((server, p, data) -> GokuSpiritBombAbility.serverTick(p));
		ctx.ticks().player(GokuKiResilienceController::tickPlayer);
		ctx.ticks().activeAbility(AbilityIds.GOKU_SUPER_SAIYAN_AURA,
				(server, p, data) -> GokuSuperSaiyanAuraAbility.serverTick(p));
	}
}
