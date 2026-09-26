package com.example.superheroes.client.hero.omniman;

import com.example.superheroes.client.core.module.HeroClientContext;
import com.example.superheroes.client.core.module.HeroClientModule;
import com.example.superheroes.hero.OmnimanHero;
import com.example.superheroes.network.ThinkMarkS2CPayload;
import net.minecraft.resources.ResourceLocation;

public record OmnimanClientModule() implements HeroClientModule {
	@Override
	public ResourceLocation heroId() {
		return OmnimanHero.ID;
	}

	@Override
	public void register(HeroClientContext ctx) {
		ctx.receive(ThinkMarkS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> com.example.superheroes.client.ClientThinkMarkState.update(payload.playerId(), payload.active())));
	}
}
