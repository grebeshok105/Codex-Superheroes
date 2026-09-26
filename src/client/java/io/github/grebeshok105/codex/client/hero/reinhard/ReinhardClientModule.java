package io.github.grebeshok105.codex.client.hero.reinhard;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.client.core.module.HeroClientContext;
import io.github.grebeshok105.codex.client.core.module.HeroClientModule;
import io.github.grebeshok105.codex.client.hud.ReinhardCeremonyOverlay;
import io.github.grebeshok105.codex.client.hud.ReinhardDarknessOverlay;
import io.github.grebeshok105.codex.client.hud.ReinhardSwordDeathOverlay;
import io.github.grebeshok105.codex.client.render.ReinhardScabbardLayer;
import io.github.grebeshok105.codex.hero.reinhard.ReinhardHero;
import io.github.grebeshok105.codex.hero.reinhard.net.ReinhardCeremonyS2CPayload;
import io.github.grebeshok105.codex.hero.reinhard.net.ReinhardDarknessS2CPayload;
import io.github.grebeshok105.codex.hero.reinhard.net.ReinhardSwordGateS2CPayload;
import io.github.grebeshok105.codex.hero.reinhard.net.ReinhardSwordKillS2CPayload;
import io.github.grebeshok105.codex.hero.reinhard.net.ReinhardTimeSlowS2CPayload;
import io.github.grebeshok105.codex.hero.reinhard.net.ReinhardWishConfirmC2SPayload;
import io.github.grebeshok105.codex.hero.reinhard.net.ReinhardWishOptionsS2CPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

public record ReinhardClientModule() implements HeroClientModule {
	@Override
	public ResourceLocation heroId() {
		return ReinhardHero.ID;
	}

	/** The screen lives outside this package (G1) — it calls back instead of knowing the payload. */
	public static void sendWishConfirm(String damageTypeId) {
		ClientPlayNetworking.send(new ReinhardWishConfirmC2SPayload(damageTypeId));
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
							payload.wishesUsed(), payload.wishesMax(),
							ReinhardClientModule::sendWishConfirm));
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
	}
}
