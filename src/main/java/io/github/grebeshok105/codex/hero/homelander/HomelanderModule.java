package io.github.grebeshok105.codex.hero.homelander;

import io.github.grebeshok105.codex.hero.homelander.ability.EyeLasersAbility;
import io.github.grebeshok105.codex.hero.homelander.ability.HandClapAbility;
import io.github.grebeshok105.codex.hero.homelander.ability.IronFistsAbility;
import io.github.grebeshok105.codex.hero.homelander.ability.StunningRoarAbility;
import io.github.grebeshok105.codex.hero.homelander.ability.XRayAbility;
import io.github.grebeshok105.codex.core.ability.AbilityDenial;
import io.github.grebeshok105.codex.core.ability.AbilityRules;
import io.github.grebeshok105.codex.core.module.HeroModule;
import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import io.github.grebeshok105.codex.damage.DamageTypeSpec;
import io.github.grebeshok105.codex.hero.homelander.registry.HomelanderDamageTypes;
import io.github.grebeshok105.codex.hero.homelander.runtime.HomelanderFlightModifier;
import io.github.grebeshok105.codex.hero.homelander.runtime.HomelanderReactionRule;
import io.github.grebeshok105.codex.hero.homelander.runtime.HomelanderRegenController;
import io.github.grebeshok105.codex.hero.homelander.runtime.IronFistsController;
import io.github.grebeshok105.codex.hero.homelander.runtime.HomelanderMadnessAftermathController;
import io.github.grebeshok105.codex.hero.homelander.runtime.HomelanderMadnessFlightController;
import io.github.grebeshok105.codex.effect.ModEffects;
import io.github.grebeshok105.codex.hero.homelander.runtime.UraniumDefenseController;
import io.github.grebeshok105.codex.hero.homelander.runtime.UraniumOffhandController;
import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.mechanic.flight.FlightProfiles;

import java.util.List;

public final class HomelanderModule implements HeroModule {
	private final HomelanderHero hero = new HomelanderHero();

	@Override
	public Hero hero() {
		return hero;
	}

	@Override
	public List<DamageTypeSpec> damageTypes() {
		return HomelanderDamageTypes.SPECS;
	}

	@Override
	public void register(HeroModuleContext ctx) {
		ctx.abilities().register(new EyeLasersAbility());
		ctx.abilities().register(new XRayAbility());
		ctx.abilities().register(new IronFistsAbility());
		ctx.abilities().register(new HandClapAbility());
		ctx.abilities().register(new StunningRoarAbility());
		HomelanderMadnessFlightController.register(ctx);
		IronFistsController.register(ctx);
		ctx.ticks().global(HomelanderMadnessAftermathController::pruneGonePlayers);
		ctx.ticks().global(UraniumDefenseController::serverTick);
		ctx.ticks().global(UraniumOffhandController::pruneGonePlayers);
		ctx.ticks().player(HomelanderMadnessAftermathController::tickPlayer);
		ctx.ticks().player(HomelanderRegenController::tickPlayer);
		ctx.ticks().player(IronFistsController::tickPlayer);
		ctx.ticks().player(UraniumOffhandController::tickPlayer);
		ctx.lifecycle().onHeroTransformed(HomelanderReactionRule::onTransformed);
		FlightProfiles.registerModifier(HomelanderHero.ID, new HomelanderFlightModifier());
		// Homelander's own ability rules: his MADNESS_AFTERMATH blocks casting silently;
		// while MADNESS (milk) is up his abilities are free. Order preserved.
		AbilityRules.blocker((player, id) -> ModEffects.isAftermath(player) ? AbilityDenial.SILENT : null);
		AbilityRules.freeCost(ModEffects::isMadness);
	}
}
