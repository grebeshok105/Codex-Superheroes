package io.github.grebeshok105.codex.hero.kratos;

import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.core.module.HeroModule;
import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import io.github.grebeshok105.codex.damage.DamageTypeSpec;
import io.github.grebeshok105.codex.hero.kratos.ability.KratosBladeStormAbility;
import io.github.grebeshok105.codex.hero.kratos.ability.KratosChainWhirlAbility;
import io.github.grebeshok105.codex.hero.kratos.ability.KratosGodSlayerAbility;
import io.github.grebeshok105.codex.hero.kratos.ability.KratosLeviathanThrowAbility;
import io.github.grebeshok105.codex.hero.kratos.ability.KratosSpartanRageAbility;
import io.github.grebeshok105.codex.hero.kratos.net.KratosRageS2CPayload;
import io.github.grebeshok105.codex.hero.kratos.registry.KratosDamageTypes;
import io.github.grebeshok105.codex.hero.kratos.registry.KratosParticles;
import io.github.grebeshok105.codex.hero.kratos.runtime.KratosHandStrikeFxController;
import io.github.grebeshok105.codex.hero.kratos.runtime.KratosRageController;

import java.util.List;

public final class KratosModule implements HeroModule {
	private final KratosHero hero = new KratosHero();

	@Override
	public Hero hero() {
		return hero;
	}

	@Override
	public List<DamageTypeSpec> damageTypes() {
		return KratosDamageTypes.SPECS;
	}

	@Override
	public void register(HeroModuleContext ctx) {
		ctx.abilities().register(new KratosSpartanRageAbility());
		ctx.abilities().register(new KratosBladeStormAbility());
		ctx.abilities().register(new KratosChainWhirlAbility());
		ctx.abilities().register(new KratosLeviathanThrowAbility());
		ctx.abilities().register(new KratosGodSlayerAbility());
		KratosItems.register(ctx.content());
		KratosParticles.register();
		ctx.payloads().s2c(KratosRageS2CPayload.TYPE, KratosRageS2CPayload.STREAM_CODEC);
		KratosRageController.register(ctx);
		KratosHandStrikeFxController.register(ctx);
		ctx.ticks().global(server -> KratosRageController.serverTick(server, KratosAbilities.SPARTAN_RAGE));
	}
}
