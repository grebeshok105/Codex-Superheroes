package com.example.superheroes.client.hero.ironman;

import com.example.superheroes.client.ClientReactorState;
import com.example.superheroes.client.core.module.HeroClientContext;
import com.example.superheroes.client.core.module.HeroClientModule;
import com.example.superheroes.client.render.RepulsorBeamRenderer;
import com.example.superheroes.hero.IronManHero;
import com.example.superheroes.network.JarvisDetectionS2CPayload;
import com.example.superheroes.network.NanoFormS2CPayload;
import com.example.superheroes.network.ReactorStateS2CPayload;
import com.example.superheroes.network.RepulsorBlastS2CPayload;
import net.minecraft.resources.ResourceLocation;

public record IronManClientModule() implements HeroClientModule {
	@Override
	public ResourceLocation heroId() {
		return IronManHero.ID;
	}

	@Override
	public void register(HeroClientContext ctx) {
		ctx.receive(RepulsorBlastS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> RepulsorBeamRenderer.add(payload.start(), payload.end())));
		ctx.receive(ReactorStateS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> ClientReactorState.update(payload.active(), payload.progressTicks(), payload.totalTicks(), payload.hasStock())));
		ctx.receive(JarvisDetectionS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> com.example.superheroes.client.hud.JarvisDetectionHud.onDetection(
						payload.playerName(), payload.heroId(), payload.distance(),
						payload.threatClass(), payload.jarvisQuote())));
		ctx.receive(NanoFormS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> com.example.superheroes.client.ClientNanoFormState.update(payload.playerId(), payload.form())));
	}
}
