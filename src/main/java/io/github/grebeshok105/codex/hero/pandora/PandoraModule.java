package io.github.grebeshok105.codex.hero.pandora;

import io.github.grebeshok105.codex.ability.MirrorDimensionAbility;
import io.github.grebeshok105.codex.ability.MirrorModeCycleAbility;
import io.github.grebeshok105.codex.ability.SpaceCrushAbility;
import io.github.grebeshok105.codex.ability.SpatialBindAbility;
import io.github.grebeshok105.codex.ability.VanityStripAbility;
import io.github.grebeshok105.codex.core.ability.AbilityDenial;
import io.github.grebeshok105.codex.core.ability.AbilityRules;
import io.github.grebeshok105.codex.core.module.HeroModule;
import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import io.github.grebeshok105.codex.effect.MirrorDimensionController;
import io.github.grebeshok105.codex.effect.ModEffects;
import io.github.grebeshok105.codex.effect.PandoraDeathController;
import io.github.grebeshok105.codex.effect.SpatialBindController;
import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.hero.PandoraHero;
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
