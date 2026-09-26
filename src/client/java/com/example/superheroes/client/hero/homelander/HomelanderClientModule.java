package com.example.superheroes.client.hero.homelander;

import com.example.superheroes.ModId;
import com.example.superheroes.client.core.module.HeroClientContext;
import com.example.superheroes.client.core.module.HeroClientModule;
import com.example.superheroes.client.hud.UraniumThreatHud;
import com.example.superheroes.client.render.LaserBeamRenderer;
import com.example.superheroes.hero.HomelanderHero;
import com.example.superheroes.network.LaserFiredS2CPayload;
import com.example.superheroes.network.UraniumPressureS2CPayload;
import com.example.superheroes.network.UraniumThreatS2CPayload;
import net.minecraft.resources.ResourceLocation;

public record HomelanderClientModule() implements HeroClientModule {
	@Override
	public ResourceLocation heroId() {
		return HomelanderHero.ID;
	}

	@Override
	public void register(HeroClientContext ctx) {
		ctx.skin(new HomelanderSkinProvider());
		ctx.hud(1400, ModId.of("uranium_threat"), UraniumThreatHud::render);
		ctx.receive(LaserFiredS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> LaserBeamRenderer.add(payload.start(), payload.end())));
		ctx.receive(UraniumPressureS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> com.example.superheroes.client.ClientUraniumPressureState.update(payload.pressuredHomelanders())));
		ctx.receive(UraniumThreatS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> com.example.superheroes.client.ClientUraniumThreatState.update(payload.self(), payload.sourceCount())));
	}
}
