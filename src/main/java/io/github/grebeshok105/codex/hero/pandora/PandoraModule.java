package io.github.grebeshok105.codex.hero.pandora;

import io.github.grebeshok105.codex.core.ability.AbilityDenial;
import io.github.grebeshok105.codex.core.ability.AbilityRules;
import io.github.grebeshok105.codex.core.module.HeroModule;
import io.github.grebeshok105.codex.core.module.HeroModuleContext;
import io.github.grebeshok105.codex.damage.DamageTypeSpec;
import io.github.grebeshok105.codex.hero.pandora.ability.MirrorDimensionAbility;
import io.github.grebeshok105.codex.hero.pandora.ability.MirrorModeCycleAbility;
import io.github.grebeshok105.codex.hero.pandora.ability.SpaceCrushAbility;
import io.github.grebeshok105.codex.hero.pandora.ability.SpatialBindAbility;
import io.github.grebeshok105.codex.hero.pandora.ability.VanityStripAbility;
import io.github.grebeshok105.codex.hero.pandora.net.MirrorDimensionS2CPayload;
import io.github.grebeshok105.codex.hero.pandora.net.MirrorDimensionStatusC2SPayload;
import io.github.grebeshok105.codex.hero.pandora.net.PandoraCinematicS2CPayload;
import io.github.grebeshok105.codex.hero.pandora.net.PandoraHouseStateS2CPayload;
import io.github.grebeshok105.codex.hero.pandora.registry.PandoraDamageTypes;
import io.github.grebeshok105.codex.hero.pandora.runtime.MirrorDimensionController;
import io.github.grebeshok105.codex.hero.pandora.runtime.PandoraDeathController;
import io.github.grebeshok105.codex.hero.pandora.runtime.SpatialBindController;
import io.github.grebeshok105.codex.hero.pandora.runtime.VanityStrippedMobEffect;
import io.github.grebeshok105.codex.hero.pandora.sound.PandoraSounds;
import io.github.grebeshok105.codex.core.hero.Hero;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;

import java.util.List;

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
		PandoraAttachments.init();
		PandoraSounds.init();
		PandoraItems.register(ctx.content());
		ctx.payloads().s2c(MirrorDimensionS2CPayload.TYPE, MirrorDimensionS2CPayload.STREAM_CODEC);
		ctx.payloads().s2c(PandoraCinematicS2CPayload.TYPE, PandoraCinematicS2CPayload.STREAM_CODEC);
		ctx.payloads().s2c(PandoraHouseStateS2CPayload.TYPE, PandoraHouseStateS2CPayload.STREAM_CODEC);
		ctx.payloads().c2s(MirrorDimensionStatusC2SPayload.TYPE, MirrorDimensionStatusC2SPayload.STREAM_CODEC,
				(payload, context) -> {
					ServerPlayer player = context.player();
					MirrorDimensionController.handleStatus(player, payload.status());
				});
		MirrorDimensionController.register(ctx);
		PandoraDeathController.register(ctx);
		ctx.ticks().global(PandoraDeathController::serverTick);
		ctx.ticks().global(SpatialBindController::tick);
		// Pandora's own rule: VANITY_STRIPPED (applied only by VanityStripAbility) blocks casting
		// and hides the victim's whole ability list. The holder is read eagerly here — not inside
		// the lambdas — so the MobEffect registers now, before the registry freezes.
		Holder<MobEffect> vanityStripped = VanityStrippedMobEffect.VANITY_STRIPPED;
		AbilityRules.blocker((player, id) -> player.hasEffect(vanityStripped)
				? AbilityDenial.of(Component.translatable("ability.superheroes.vanity_stripped")
						.withStyle(ChatFormatting.DARK_PURPLE)) : null);
		AbilityRules.hideAll(player -> player.hasEffect(vanityStripped));
	}

	@Override
	public List<DamageTypeSpec> damageTypes() {
		return PandoraDamageTypes.SPECS;
	}
}
