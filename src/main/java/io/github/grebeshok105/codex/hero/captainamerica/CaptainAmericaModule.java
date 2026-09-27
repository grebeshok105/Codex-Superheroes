package io.github.grebeshok105.codex.hero.captainamerica;

import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.core.module.HeroModule;
import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import io.github.grebeshok105.codex.damage.DamageTypeSpec;
import io.github.grebeshok105.codex.effect.ThanosStoneRewardController;
import io.github.grebeshok105.codex.hero.captainamerica.ability.CapCounterStanceAbility;
import io.github.grebeshok105.codex.hero.captainamerica.ability.CapShieldDashAbility;
import io.github.grebeshok105.codex.hero.captainamerica.ability.CapShieldSlamAbility;
import io.github.grebeshok105.codex.hero.captainamerica.ability.CapShieldThrowAbility;
import io.github.grebeshok105.codex.hero.captainamerica.entity.CaptainAmericaEntities;
import io.github.grebeshok105.codex.hero.captainamerica.registry.CaptainAmericaDamageTypes;
import io.github.grebeshok105.codex.hero.captainamerica.registry.CaptainAmericaParticles;
import io.github.grebeshok105.codex.item.infinity.InfinityStoneType;

import java.util.List;

public final class CaptainAmericaModule implements HeroModule {
	private final CaptainAmericaHero hero = new CaptainAmericaHero();

	@Override
	public Hero hero() {
		return hero;
	}

	@Override
	public List<DamageTypeSpec> damageTypes() {
		return CaptainAmericaDamageTypes.SPECS;
	}

	@Override
	public void register(HeroModuleContext ctx) {
		ctx.abilities().register(new CapShieldThrowAbility());
		ctx.abilities().register(new CapShieldSlamAbility());
		ctx.abilities().register(new CapShieldDashAbility());
		ctx.abilities().register(new CapCounterStanceAbility());
		CaptainAmericaItems.register(ctx.content());
		CaptainAmericaParticles.register();
		CaptainAmericaEntities.register();
		ThanosStoneRewardController.registerHeroStone(CaptainAmericaHero.ID, InfinityStoneType.SOUL);

		ctx.ticks().player((server, p, data) -> CapShieldSlamAbility.serverTick(p));
	}
}
