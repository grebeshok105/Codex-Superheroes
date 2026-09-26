package io.github.grebeshok105.codex.hero.homelander;

import io.github.grebeshok105.codex.ability.EyeLasersAbility;
import io.github.grebeshok105.codex.ability.HandClapAbility;
import io.github.grebeshok105.codex.ability.IronFistsAbility;
import io.github.grebeshok105.codex.ability.StunningRoarAbility;
import io.github.grebeshok105.codex.ability.XRayAbility;
import io.github.grebeshok105.codex.core.ability.AbilityDenial;
import io.github.grebeshok105.codex.core.ability.AbilityRules;
import io.github.grebeshok105.codex.core.module.HeroModule;
import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import io.github.grebeshok105.codex.effect.HomelanderRegenController;
import io.github.grebeshok105.codex.effect.IronFistsController;
import io.github.grebeshok105.codex.effect.MadnessAftermathController;
import io.github.grebeshok105.codex.effect.MadnessFlightController;
import io.github.grebeshok105.codex.effect.ModEffects;
import io.github.grebeshok105.codex.effect.UraniumDefenseController;
import io.github.grebeshok105.codex.effect.UraniumOffhandController;
import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.hero.HomelanderHero;

public final class HomelanderModule implements HeroModule {
	private final HomelanderHero hero = new HomelanderHero();

	@Override
	public Hero hero() {
		return hero;
	}

	@Override
	public void register(HeroModuleContext ctx) {
		ctx.abilities().register(new EyeLasersAbility());
		ctx.abilities().register(new XRayAbility());
		ctx.abilities().register(new IronFistsAbility());
		ctx.abilities().register(new HandClapAbility());
		ctx.abilities().register(new StunningRoarAbility());
		MadnessFlightController.register(ctx);
		IronFistsController.register(ctx);
		ctx.ticks().global(MadnessAftermathController::pruneGonePlayers);
		ctx.ticks().global(UraniumDefenseController::serverTick);
		ctx.ticks().global(UraniumOffhandController::pruneGonePlayers);
		ctx.ticks().player(MadnessAftermathController::tickPlayer);
		ctx.ticks().player(HomelanderRegenController::tickPlayer);
		ctx.ticks().player(IronFistsController::tickPlayer);
		ctx.ticks().player(UraniumOffhandController::tickPlayer);
		// Homelander's own ability rules: his MADNESS_AFTERMATH blocks casting silently;
		// while MADNESS (milk) is up his abilities are free. Order preserved.
		AbilityRules.blocker((player, id) -> ModEffects.isAftermath(player) ? AbilityDenial.SILENT : null);
		AbilityRules.freeCost(ModEffects::isMadness);
	}
}
