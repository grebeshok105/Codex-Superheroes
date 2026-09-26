package com.example.superheroes.hero.pandora;

import com.example.superheroes.ability.MirrorDimensionAbility;
import com.example.superheroes.ability.MirrorModeCycleAbility;
import com.example.superheroes.ability.SpaceCrushAbility;
import com.example.superheroes.ability.SpatialBindAbility;
import com.example.superheroes.ability.VanityStripAbility;
import com.example.superheroes.core.ability.AbilityDenial;
import com.example.superheroes.core.ability.AbilityRules;
import com.example.superheroes.core.module.HeroModule;
import com.example.superheroes.core.module.HeroModuleContext;
import com.example.superheroes.effect.MirrorDimensionController;
import com.example.superheroes.effect.ModEffects;
import com.example.superheroes.effect.PandoraDeathController;
import com.example.superheroes.effect.SpatialBindController;
import com.example.superheroes.hero.Hero;
import com.example.superheroes.hero.PandoraHero;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public final class PandoraModule implements HeroModule {
	private final PandoraHero hero = new PandoraHero();

	@Override
	public Hero hero() {
		return hero;
	}

	@Override
	public void register(HeroModuleContext ctx) {
		ctx.abilities().register(new MirrorDimensionAbility());
		ctx.abilities().register(new MirrorModeCycleAbility());
		ctx.abilities().register(new SpatialBindAbility());
		ctx.abilities().register(new SpaceCrushAbility());
		ctx.abilities().register(new VanityStripAbility());
		MirrorDimensionController.register(ctx);
		PandoraDeathController.register(ctx);
		ctx.ticks().global(PandoraDeathController::serverTick);
		ctx.ticks().global(SpatialBindController::tick);
		// Pandora's own rule: VANITY_STRIPPED (applied only by VanityStripAbility) blocks casting.
		AbilityRules.blocker((player, id) -> player.hasEffect(ModEffects.VANITY_STRIPPED)
				? AbilityDenial.of(Component.translatable("ability.superheroes.vanity_stripped")
						.withStyle(ChatFormatting.DARK_PURPLE)) : null);
	}
}
