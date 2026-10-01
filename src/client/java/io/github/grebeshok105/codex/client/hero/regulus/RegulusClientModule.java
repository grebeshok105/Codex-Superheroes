package io.github.grebeshok105.codex.client.hero.regulus;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.client.core.hud.AbilityDecoration;
import io.github.grebeshok105.codex.client.core.module.HeroClientContext;
import io.github.grebeshok105.codex.client.core.module.HeroClientModule;
import io.github.grebeshok105.codex.client.hero.regulus.emf.RegulusEmf;
import io.github.grebeshok105.codex.client.hero.regulus.fx.RegulusFx;
import io.github.grebeshok105.codex.client.hero.regulus.hud.BloodRainHud;
import io.github.grebeshok105.codex.client.hero.regulus.hud.ClientHudGlitch;
import io.github.grebeshok105.codex.client.hero.regulus.hud.CracksOverlayHud;
import io.github.grebeshok105.codex.client.hero.regulus.hud.EvangelionZoomHud;
import io.github.grebeshok105.codex.client.hero.regulus.hud.MadnessHudOverlay;
import io.github.grebeshok105.codex.client.hero.regulus.render.EvangelionBookLayer;
import io.github.grebeshok105.codex.client.hero.regulus.render.RegulusHeartsRenderer;
import io.github.grebeshok105.codex.client.hero.regulus.state.ClientHeartsState;
import io.github.grebeshok105.codex.client.hero.regulus.state.ClientMadnessState;
import io.github.grebeshok105.codex.hero.regulus.RegulusAbilities;
import io.github.grebeshok105.codex.hero.regulus.RegulusHero;
import io.github.grebeshok105.codex.hero.regulus.net.HeartsSyncS2CPayload;
import io.github.grebeshok105.codex.hero.regulus.runtime.RegulusMadnessController;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.ResourceLocation;

public record RegulusClientModule() implements HeroClientModule {
	@Override
	public ResourceLocation heroId() {
		return RegulusHero.ID;
	}

	@Override
	public void register(HeroClientContext ctx) {
		ctx.hud(1100, ModId.of("madness_overlay"), MadnessHudOverlay::render);
		ctx.hud(1200, ModId.of("blood_rain"), BloodRainHud::render);
		ctx.hud(1300, ModId.of("evangelion_zoom"), EvangelionZoomHud::render);
		ctx.hud(1500, ModId.of("cracks_overlay"), CracksOverlayHud::render);
		ctx.hudGlitchSource(ClientHudGlitch.SOURCE);
		ctx.playerLayer(EvangelionBookLayer::new);
		// EMF presentation (clip clocks + merged-jem variables) and every
		// RegulusVfxIds one-shot/channel factory. Both are no-ops when EMF is
		// absent (RegulusEmf gates on EmfBridge.isAvailable()).
		RegulusEmf.register(ctx);
		RegulusFx.register(ctx);
		// The evangelion reading zoom: shrink fov over the 60-tick channel, computed
		// from the synced game-tick deadline (never wall-clock — a client that
		// observes the ritual late still gets the right remaining time).
		ctx.fovModifier((camera, partial, fov) -> {
			Minecraft mc = Minecraft.getInstance();
			if (mc.level == null || !ClientMadnessState.isReading()) {
				return fov;
			}
			long remaining = ClientMadnessState.ritualUntilTick() - mc.level.getGameTime();
			if (remaining <= 0L || remaining > RegulusMadnessController.RITUAL_TICKS) {
				return fov;
			}
			// 1200 = 60 ticks × 20 sub-tick units — the progress unit the plan fixes.
			float progress = 1f - (remaining * 20f) / 1200f;
			double zoomFactor = 1.0 - progress * 0.7;
			return fov * zoomFactor;
		});
		ctx.clientTick(client -> {
			// While the book is open the inventory screen must not cover the zoom.
			if (ClientMadnessState.isReading() && client.screen instanceof InventoryScreen) {
				client.setScreen(null);
			}
			// Blood rain rides the synced madness flag's edges — the old
			// madness_visual payload is gone.
			int edge = ClientMadnessState.madnessEdge();
			if (edge > 0) {
				BloodRainHud.trigger();
			} else if (edge < 0) {
				BloodRainHud.clear();
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
	}
}
