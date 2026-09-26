package io.github.grebeshok105.codex.client.hero.regulus;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.client.ClientMadnessState;
import io.github.grebeshok105.codex.client.core.module.HeroClientContext;
import io.github.grebeshok105.codex.client.core.module.HeroClientModule;
import io.github.grebeshok105.codex.client.hud.BloodRainHud;
import io.github.grebeshok105.codex.client.hud.CracksOverlayHud;
import io.github.grebeshok105.codex.client.hud.EvangelionZoomHud;
import io.github.grebeshok105.codex.client.hud.MadnessHudOverlay;
import io.github.grebeshok105.codex.client.hud.SunWindupHud;
import io.github.grebeshok105.codex.hero.RegulusHero;
import io.github.grebeshok105.codex.network.MadnessSyncS2CPayload;
import io.github.grebeshok105.codex.network.MadnessVisualS2CPayload;
import net.minecraft.resources.ResourceLocation;

public record RegulusClientModule() implements HeroClientModule {
	@Override
	public ResourceLocation heroId() {
		return RegulusHero.ID;
	}

	@Override
	public void register(HeroClientContext ctx) {
		ctx.hud(900, ModId.of("sun_windup"), SunWindupHud::render);
		ctx.hud(1100, ModId.of("madness_overlay"), MadnessHudOverlay::render);
		ctx.hud(1200, ModId.of("blood_rain"), BloodRainHud::render);
		ctx.hud(1300, ModId.of("evangelion_zoom"), EvangelionZoomHud::render);
		ctx.hud(1500, ModId.of("cracks_overlay"), CracksOverlayHud::render);
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
