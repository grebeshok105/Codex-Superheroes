package com.example.superheroes.client.hero.regulus;

import com.example.superheroes.client.ClientMadnessState;
import com.example.superheroes.client.core.module.HeroClientContext;
import com.example.superheroes.client.core.module.HeroClientModule;
import com.example.superheroes.client.hud.BloodRainHud;
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
