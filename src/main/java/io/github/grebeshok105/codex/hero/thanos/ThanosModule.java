package io.github.grebeshok105.codex.hero.thanos;

import io.github.grebeshok105.codex.hero.thanos.ability.ThanosCosmicSlamAbility;
import io.github.grebeshok105.codex.hero.thanos.ability.ThanosMindPulseAbility;
import io.github.grebeshok105.codex.hero.thanos.ability.ThanosRealityTearAbility;
import io.github.grebeshok105.codex.hero.thanos.ability.ThanosSnapAbility;
import io.github.grebeshok105.codex.hero.thanos.ability.ThanosSoulPulseAbility;
import io.github.grebeshok105.codex.hero.thanos.ability.ThanosSpacePortalAbility;
import io.github.grebeshok105.codex.hero.thanos.ability.ThanosTimeRewindAbility;
import io.github.grebeshok105.codex.core.ability.AbilityDenial;
import io.github.grebeshok105.codex.core.ability.AbilityRules;
import io.github.grebeshok105.codex.core.module.HeroModule;
import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import io.github.grebeshok105.codex.damage.DamageTypeSpec;
import io.github.grebeshok105.codex.mechanic.effect.ModEffects;
import io.github.grebeshok105.codex.hero.thanos.net.ThanosStonesS2CPayload;
import io.github.grebeshok105.codex.hero.thanos.registry.ThanosDamageTypes;
import io.github.grebeshok105.codex.hero.thanos.runtime.ThanosGauntletStateController;
import io.github.grebeshok105.codex.hero.thanos.runtime.ThanosSnapWindupController;
import io.github.grebeshok105.codex.hero.thanos.runtime.ThanosStoneRewardController;
import io.github.grebeshok105.codex.hero.thanos.sound.ThanosSounds;
import io.github.grebeshok105.codex.core.hero.Hero;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class ThanosModule implements HeroModule {
	private final ThanosHero hero = new ThanosHero();

	@Override
	public Hero hero() {
		return hero;
	}

	@Override
	public List<DamageTypeSpec> damageTypes() {
		return ThanosDamageTypes.SPECS;
	}

	@Override
	public void register(HeroModuleContext ctx) {
		ThanosItems.register(ctx.content());
		ThanosSounds.register();
		ctx.payloads().s2c(ThanosStonesS2CPayload.TYPE, ThanosStonesS2CPayload.STREAM_CODEC);

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
