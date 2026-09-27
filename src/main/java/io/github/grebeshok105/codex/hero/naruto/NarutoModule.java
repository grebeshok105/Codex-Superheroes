package io.github.grebeshok105.codex.hero.naruto;

import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.core.module.HeroModule;
import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import io.github.grebeshok105.codex.damage.DamageTypeSpec;
import io.github.grebeshok105.codex.hero.naruto.ability.NarutoBijuudamaAbility;
import io.github.grebeshok105.codex.hero.naruto.ability.NarutoOodamaRasenganAbility;
import io.github.grebeshok105.codex.hero.naruto.ability.NarutoRasenganAbility;
import io.github.grebeshok105.codex.hero.naruto.ability.NarutoRasenshurikenAbility;
import io.github.grebeshok105.codex.hero.naruto.ability.NarutoSageModeAbility;
import io.github.grebeshok105.codex.hero.naruto.ability.NarutoShadowClonesAbility;
import io.github.grebeshok105.codex.hero.naruto.entity.NarutoEntities;
import io.github.grebeshok105.codex.hero.naruto.registry.NarutoDamageTypes;
import io.github.grebeshok105.codex.hero.naruto.registry.NarutoParticles;
import io.github.grebeshok105.codex.hero.naruto.runtime.KawarimiController;
import io.github.grebeshok105.codex.hero.naruto.runtime.NarutoWallRunController;

import java.util.List;

public final class NarutoModule implements HeroModule {
	private final NarutoHero hero = new NarutoHero();

	@Override
	public Hero hero() {
		return hero;
	}

	@Override
	public List<DamageTypeSpec> damageTypes() {
		return NarutoDamageTypes.SPECS;
	}

	@Override
	public void register(HeroModuleContext ctx) {
		ctx.abilities().register(new NarutoRasenganAbility());
		ctx.abilities().register(new NarutoOodamaRasenganAbility());
		ctx.abilities().register(new NarutoRasenshurikenAbility());
		ctx.abilities().register(new NarutoSageModeAbility());
		ctx.abilities().register(new NarutoBijuudamaAbility());
		ctx.abilities().register(new NarutoShadowClonesAbility());
		NarutoItems.register(ctx.content());
		NarutoParticles.register();
		NarutoEntities.register();
		KawarimiController.register(ctx);
		ctx.ticks().player((server, p, data) -> NarutoRasenganAbility.serverTick(p));
		ctx.ticks().player((server, p, data) -> NarutoOodamaRasenganAbility.serverTick(p));
		ctx.ticks().player((server, p, data) -> NarutoRasenshurikenAbility.serverTick(p));
		ctx.ticks().player(NarutoWallRunController::tickPlayer);
		ctx.ticks().hero(NarutoHero.ID, (server, p, data) -> {
			if (data.isActive(NarutoAbilities.NARUTO_SAGE_MODE)) {
				NarutoSageModeAbility.serverTick(p);
			}
		});
	}
}
