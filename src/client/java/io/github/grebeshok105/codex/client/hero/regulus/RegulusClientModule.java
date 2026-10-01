package io.github.grebeshok105.codex.client.hero.regulus;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.client.core.hud.AbilityDecoration;
import io.github.grebeshok105.codex.client.core.module.HeroClientContext;
import io.github.grebeshok105.codex.client.core.module.HeroClientModule;
import io.github.grebeshok105.codex.client.hero.regulus.fx.RegulusFx;
import io.github.grebeshok105.codex.client.hero.regulus.hud.BloodRainHud;
import io.github.grebeshok105.codex.client.hero.regulus.hud.ClientHudGlitch;
import io.github.grebeshok105.codex.client.hero.regulus.hud.CracksOverlayHud;
import io.github.grebeshok105.codex.client.hero.regulus.hud.EvangelionZoomHud;
import io.github.grebeshok105.codex.client.hero.regulus.hud.MadnessHudOverlay;
import io.github.grebeshok105.codex.client.hero.regulus.render.RegulusHeartsRenderer;
import io.github.grebeshok105.codex.client.hero.regulus.state.ClientHeartsState;
import io.github.grebeshok105.codex.client.hero.regulus.state.ClientMadnessState;
import io.github.grebeshok105.codex.hero.regulus.RegulusAbilities;
import io.github.grebeshok105.codex.hero.regulus.RegulusHero;
import io.github.grebeshok105.codex.hero.regulus.net.HeartsSyncS2CPayload;
import io.github.grebeshok105.codex.hero.regulus.net.MadnessSyncS2CPayload;
import io.github.grebeshok105.codex.hero.regulus.net.MadnessVisualS2CPayload;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.ResourceLocation;

public record RegulusClientModule() implements HeroClientModule {
	@Override
	public ResourceLocation heroId() {
		return RegulusHero.ID;
	}

	@Override
	public void register(HeroClientContext ctx) {
		RegulusFx.register(ctx);
		ctx.hud(1100, ModId.of("madness_overlay"), MadnessHudOverlay::render);
		ctx.hud(1200, ModId.of("blood_rain"), BloodRainHud::render);
		ctx.hud(1300, ModId.of("evangelion_zoom"), EvangelionZoomHud::render);
		ctx.hud(1500, ModId.of("cracks_overlay"), CracksOverlayHud::render);
		ctx.hudGlitchSource(ClientHudGlitch.SOURCE);
		// The evangelion reading zoom: linearly shrink fov over the 10s channel.
		ctx.fovModifier((camera, partial, fov) -> {
			if (!ClientMadnessState.isReading()) {
				return fov;
			}
			long until = ClientMadnessState.readingUntilMs();
			long now = System.currentTimeMillis();
			long total = 10000L;
			long remaining = until - now;
			if (remaining <= 0L || remaining > total) {
				return fov;
			}
			float progress = 1f - (remaining / (float) total);
			double zoomFactor = 1.0 - progress * 0.7;
			return fov * zoomFactor;
		});
		// While the book is open the inventory screen must not cover the zoom.
		ctx.clientTick(client -> {
			if (ClientMadnessState.isReading() && client.screen instanceof InventoryScreen) {
				client.setScreen(null);
			}
		});
		// Counter-Strike stays a mystery in the panel until madness reveals it.
		ctx.abilityDecoration(RegulusAbilities.COUNTER_STRIKE, new AbilityDecoration() {
			@Override
			public void render(net.minecraft.client.gui.GuiGraphics graphics, ResourceLocation abilityId,
					int iconCenterX, int iconCenterY, int iconSize) {
			}

			@Override
			public boolean masksIdentity() {
				return !ClientMadnessState.isMadness();
			}
		});
		// Owner-only little-king highlight pass (golden corners on bearer entities).
		RegulusHeartsRenderer.register();
		ctx.receive(HeartsSyncS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> ClientHeartsState.update(
						payload.heartEntityIds(), payload.lionHeartActive(), payload.overheatTicks())));
		ctx.receive(MadnessSyncS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> ClientMadnessState.update(
						payload.madness(), payload.bonusLifeAvailable(),
						payload.readingRemainingMs(), payload.manaLockRemainingMs())));
		ctx.receive(MadnessVisualS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> {
					if (payload.event() == MadnessVisualS2CPayload.EVENT_ENTER) {
						BloodRainHud.trigger();
					} else if (payload.event() == MadnessVisualS2CPayload.EVENT_EXIT) {
						BloodRainHud.clear();
					}
				}));
	}
}
