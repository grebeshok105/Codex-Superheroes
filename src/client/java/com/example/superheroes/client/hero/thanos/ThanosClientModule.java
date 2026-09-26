package com.example.superheroes.client.hero.thanos;

import com.example.superheroes.client.core.module.HeroClientContext;
import com.example.superheroes.client.core.module.HeroClientModule;
import com.example.superheroes.client.render.CosmicBeamRenderer;
import com.example.superheroes.hero.ThanosHero;
import com.example.superheroes.network.ThanosCosmicBeamS2CPayload;
import com.example.superheroes.network.ThanosStonesS2CPayload;
import net.minecraft.resources.ResourceLocation;

public record ThanosClientModule() implements HeroClientModule {
	@Override
	public ResourceLocation heroId() {
		return ThanosHero.ID;
	}

	@Override
	public void register(HeroClientContext ctx) {
		ctx.skin(new ThanosSkinProvider());
		ctx.receive(ThanosCosmicBeamS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> CosmicBeamRenderer.add(payload.start(), payload.end())));
		ctx.receive(ThanosStonesS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> com.example.superheroes.client.ClientThanosState.update(payload.playerId(), payload.bitmask())));
	}
}
