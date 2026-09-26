package com.example.superheroes.hero.thanos;

import com.example.superheroes.ability.ThanosCosmicSlamAbility;
import com.example.superheroes.ability.ThanosMindPulseAbility;
import com.example.superheroes.ability.ThanosRealityTearAbility;
import com.example.superheroes.ability.ThanosSnapAbility;
import com.example.superheroes.ability.ThanosSoulPulseAbility;
import com.example.superheroes.ability.ThanosSpacePortalAbility;
import com.example.superheroes.ability.ThanosTimeRewindAbility;
import com.example.superheroes.core.ability.AbilityDenial;
import com.example.superheroes.core.ability.AbilityRules;
import com.example.superheroes.core.module.HeroModule;
import com.example.superheroes.core.module.HeroModuleContext;
import com.example.superheroes.effect.ModEffects;
import com.example.superheroes.effect.ThanosGauntletStateController;
import com.example.superheroes.effect.ThanosSnapWindupController;
import com.example.superheroes.effect.ThanosStoneRewardController;
import com.example.superheroes.hero.Hero;
import com.example.superheroes.hero.ThanosHero;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public final class ThanosModule implements HeroModule {
	private final ThanosHero hero = new ThanosHero();

	@Override
	public Hero hero() {
		return hero;
	}

	@Override
	public void register(HeroModuleContext ctx) {
		ctx.abilities().register(new ThanosCosmicSlamAbility());
		ctx.abilities().register(new ThanosRealityTearAbility());
		ctx.abilities().register(new ThanosMindPulseAbility());
		ctx.abilities().register(new ThanosTimeRewindAbility());
		ctx.abilities().register(new ThanosSpacePortalAbility());
		ctx.abilities().register(new ThanosSoulPulseAbility());
		ctx.abilities().register(new ThanosSnapAbility());

		ThanosGauntletStateController.register(ctx);
		ThanosStoneRewardController.register(ctx);
		ctx.ticks().global(ThanosSnapWindupController::serverTick);
		ctx.ticks().player(ThanosGauntletStateController::tickPlayer);
		// Thanos' own rule: DISABLED_ABILITIES (applied by the Snap and Soul Pulse)
		// blocks every ability while it is up — kept between the aftermath and
		// vanity blockers, as in the old table.
		AbilityRules.blocker((player, id) -> player.hasEffect(ModEffects.DISABLED_ABILITIES)
				? AbilityDenial.of(Component.translatable("ability.superheroes.disabled_by_snap")
						.withStyle(ChatFormatting.DARK_PURPLE)) : null);
	}
}
