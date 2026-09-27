package io.github.grebeshok105.codex.hero.goku;

import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.core.module.HeroModule;
import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import io.github.grebeshok105.codex.damage.DamageTypeSpec;
import io.github.grebeshok105.codex.hero.goku.ability.GokuInstantTransmissionAbility;
import io.github.grebeshok105.codex.hero.goku.ability.GokuKamehamehaAbility;
import io.github.grebeshok105.codex.hero.goku.ability.GokuKiChargeAbility;
import io.github.grebeshok105.codex.hero.goku.ability.GokuSolarFlareAbility;
import io.github.grebeshok105.codex.hero.goku.ability.GokuSpiritBombAbility;
import io.github.grebeshok105.codex.hero.goku.ability.GokuSuperSaiyanAuraAbility;
import io.github.grebeshok105.codex.hero.goku.registry.GokuDamageTypes;
import io.github.grebeshok105.codex.hero.goku.registry.GokuParticles;
import io.github.grebeshok105.codex.hero.goku.runtime.GokuKiResilienceController;
import io.github.grebeshok105.codex.hero.goku.runtime.GokuKiStackController;

import java.util.List;

public final class GokuModule implements HeroModule {
	private final GokuHero hero = new GokuHero();

	@Override
	public Hero hero() {
		return hero;
	}

	@Override
	public List<DamageTypeSpec> damageTypes() {
		return GokuDamageTypes.SPECS;
	}

	@Override
	public void register(HeroModuleContext ctx) {
		ctx.abilities().register(new GokuKamehamehaAbility());
		ctx.abilities().register(new GokuInstantTransmissionAbility());
		ctx.abilities().register(new GokuKiChargeAbility());
		ctx.abilities().register(new GokuSolarFlareAbility());
		ctx.abilities().register(new GokuSpiritBombAbility());
		ctx.abilities().register(new GokuSuperSaiyanAuraAbility());
		GokuItems.register(ctx.content());
		GokuParticles.register();
		GokuKiStackController.register(ctx);
		ctx.ticks().player((server, p, data) -> GokuKamehamehaAbility.serverTick(p));
		ctx.ticks().player((server, p, data) -> GokuSpiritBombAbility.serverTick(p));
		ctx.ticks().player(GokuKiResilienceController::tickPlayer);
		ctx.ticks().hero(GokuHero.ID, (server, p, data) -> {
			if (data.isActive(GokuAbilities.GOKU_SUPER_SAIYAN_AURA)) {
				GokuSuperSaiyanAuraAbility.serverTick(p);
			}
		});
	}
}
