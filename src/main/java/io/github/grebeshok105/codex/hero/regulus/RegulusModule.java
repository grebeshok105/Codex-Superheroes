package io.github.grebeshok105.codex.hero.regulus;

import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.core.module.HeroModule;
import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import io.github.grebeshok105.codex.damage.DamageTypeSpec;
import io.github.grebeshok105.codex.hero.regulus.ability.CounterStrikeAbility;
import io.github.grebeshok105.codex.hero.regulus.ability.GreedsEmbraceAbility;
import io.github.grebeshok105.codex.hero.regulus.ability.LionHeartAbility;
import io.github.grebeshok105.codex.hero.regulus.ability.LionRoarAbility;
import io.github.grebeshok105.codex.hero.regulus.ability.ManiaOfGreedAbility;
import io.github.grebeshok105.codex.hero.regulus.net.HeartsSyncS2CPayload;
import io.github.grebeshok105.codex.hero.regulus.net.MadnessSyncS2CPayload;
import io.github.grebeshok105.codex.hero.regulus.net.MadnessVisualS2CPayload;
import io.github.grebeshok105.codex.hero.regulus.registry.RegulusDamageTypes;
import io.github.grebeshok105.codex.hero.regulus.runtime.RegulusCastState;
import io.github.grebeshok105.codex.hero.regulus.runtime.RegulusGreedController;
import io.github.grebeshok105.codex.hero.regulus.runtime.RegulusHearts;
import io.github.grebeshok105.codex.hero.regulus.runtime.RegulusMadnessController;
import io.github.grebeshok105.codex.hero.regulus.sound.RegulusSounds;
import io.github.grebeshok105.codex.hero.regulus.runtime.RegulusTotemController;

import java.util.List;

public final class RegulusModule implements HeroModule {
	private final RegulusHero hero = new RegulusHero();

	@Override
	public Hero hero() {
		return hero;
	}

	@Override
	public List<DamageTypeSpec> damageTypes() {
		return RegulusDamageTypes.SPECS;
	}

	@Override
	public void register(HeroModuleContext ctx) {
		RegulusAttachments.init();
		RegulusSounds.init();
		RegulusItems.register(ctx.content());
		ctx.payloads().s2c(MadnessSyncS2CPayload.TYPE, MadnessSyncS2CPayload.STREAM_CODEC);
		ctx.payloads().s2c(HeartsSyncS2CPayload.TYPE, HeartsSyncS2CPayload.STREAM_CODEC);
		ctx.payloads().s2c(MadnessVisualS2CPayload.TYPE, MadnessVisualS2CPayload.STREAM_CODEC);
		// Regulus→TIME stone reward row lives in InfinityStones' static table (hero.thanos
		// owns the stone registry — heroes never import sibling heroes).
		ctx.abilities().register(new LionHeartAbility());
		ctx.abilities().register(new ManiaOfGreedAbility());
		ctx.abilities().register(new GreedsEmbraceAbility());
		ctx.abilities().register(new LionRoarAbility());
		ctx.abilities().register(new CounterStrikeAbility());
		RegulusTotemController.register(ctx);
		RegulusCastState.register(ctx);
		RegulusHearts.register(ctx);
		RegulusGreedController.register(ctx);
		RegulusMadnessController.register(ctx);
		ctx.ticks().global(RegulusGreedController::tickFreezes);
		ctx.ticks().global(RegulusMadnessController::tickCounters);
		ctx.ticks().player(RegulusGreedController::tickPlayer);
		ctx.ticks().player(RegulusMadnessController::tickPlayer);
	}
}
