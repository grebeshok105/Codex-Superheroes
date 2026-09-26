package com.example.superheroes.client.hero.regulus;

import com.example.superheroes.ModId;
import com.example.superheroes.client.ClientMadnessState;
import com.example.superheroes.client.core.module.HeroClientContext;
import com.example.superheroes.client.core.module.HeroClientModule;
import com.example.superheroes.client.hud.BloodRainHud;
import com.example.superheroes.client.hud.CracksOverlayHud;
import com.example.superheroes.client.hud.EvangelionZoomHud;
import com.example.superheroes.client.hud.MadnessHudOverlay;
import com.example.superheroes.client.hud.SunWindupHud;
import com.example.superheroes.hero.RegulusHero;
import com.example.superheroes.network.MadnessSyncS2CPayload;
import com.example.superheroes.network.MadnessVisualS2CPayload;
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
