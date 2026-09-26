package io.github.grebeshok105.codex.client.hero.reinhard;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.ability.AbilityIds;
import io.github.grebeshok105.codex.client.core.module.HeroClientContext;
import io.github.grebeshok105.codex.client.core.module.HeroClientModule;
import io.github.grebeshok105.codex.client.hud.HudUtil;
import io.github.grebeshok105.codex.client.hud.ReinhardCeremonyOverlay;
import io.github.grebeshok105.codex.client.hud.ReinhardDarknessOverlay;
import io.github.grebeshok105.codex.client.hud.ReinhardSwordDeathOverlay;
import io.github.grebeshok105.codex.client.render.ReinhardScabbardLayer;
import io.github.grebeshok105.codex.hero.ReinhardHero;
import io.github.grebeshok105.codex.network.ReinhardCeremonyS2CPayload;
import io.github.grebeshok105.codex.network.ReinhardDarknessS2CPayload;
import io.github.grebeshok105.codex.network.ReinhardSwordGateS2CPayload;
import io.github.grebeshok105.codex.network.ReinhardSwordKillS2CPayload;
import io.github.grebeshok105.codex.network.ReinhardTimeSlowS2CPayload;
import io.github.grebeshok105.codex.network.ReinhardWishOptionsS2CPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;

public record ReinhardClientModule() implements HeroClientModule {
	@Override
	public ResourceLocation heroId() {
		return ReinhardHero.ID;
	}

	@Override
	public void register(HeroClientContext ctx) {
		ctx.playerLayer(renderer -> new ReinhardScabbardLayer(renderer));
		ctx.hud(1700, ModId.of("reinhard_ceremony"), ReinhardCeremonyOverlay::render);
		ctx.hud(2200, ModId.of("reinhard_sword_death"), ReinhardSwordDeathOverlay::render);
		ctx.hud(2300, ModId.of("reinhard_darkness"), ReinhardDarknessOverlay::render);
		ctx.receive(ReinhardWishOptionsS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> {
					Minecraft mc = Minecraft.getInstance();
					mc.setScreen(io.github.grebeshok105.codex.client.screen.ReinhardWishScreen.of(
							payload.damageTypeIds(), payload.adaptedDamageTypeIds(),
							payload.wishesUsed(), payload.wishesMax()));
				}));

		ctx.receive(ReinhardCeremonyS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> io.github.grebeshok105.codex.client.ClientReinhardCeremonyState.update(
						payload.active(), payload.progress())));

		ctx.receive(ReinhardSwordGateS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> io.github.grebeshok105.codex.client.ClientReinhardSwordGateState.update(
						payload.ready(), payload.progress())));

		ctx.receive(ReinhardSwordKillS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> io.github.grebeshok105.codex.client.ClientReinhardSwordKillState.update(payload.active())));

		ctx.receive(ReinhardDarknessS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> io.github.grebeshok105.codex.client.ClientReinhardDarknessState.activate(payload.durationTicks())));

		ctx.receive(ReinhardTimeSlowS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> io.github.grebeshok105.codex.client.ClientReinhardTimeSlowState.update(payload.active())));

		ctx.soundFilter(ReinhardClientModule::muteWorldDuringTimeSlow);
		ctx.abilityDecoration(AbilityIds.REINHARD_SWORD_DRAW, ReinhardClientModule::swordDrawReadyHalo);
	}

	/** Time slow mutes every sound except Reinhard's own — same condition the old client mixin had. */
	private static boolean muteWorldDuringTimeSlow(SoundInstance instance) {
		if (!io.github.grebeshok105.codex.client.ClientReinhardTimeSlowState.active()) return false;
		SoundSource source = instance.getSource();
		if (source == SoundSource.MUSIC || source == SoundSource.MASTER || source == SoundSource.VOICE) return false;
		String soundPath = instance.getLocation().toString();
		return !soundPath.contains("superheroes:reinhard");
	}

	/** Gold pulsing border around the sword-draw icon while the gate is ready and the ability isn't running. */
	private static void swordDrawReadyHalo(GuiGraphics graphics, ResourceLocation abilityId, int iconCenterX, int iconCenterY, int iconSize) {
		if (io.github.grebeshok105.codex.client.ClientHeroState.data().isActive(AbilityIds.REINHARD_SWORD_DRAW)
				|| !io.github.grebeshok105.codex.client.ClientReinhardSwordGateState.ready()) {
			return;
		}
		int half = iconSize / 2;
		float pulse = 0.55f + 0.45f * (float) Math.sin(System.currentTimeMillis() / 220.0);
		int a = Math.max(70, Math.min(255, (int) (200 * pulse)));
		HudUtil.roundedRectBorder(graphics, iconCenterX - half - 2, iconCenterY - half - 6, iconSize + 4, iconSize + 4,
				(a << 24) | 0x00FFD24A);
	}
}
