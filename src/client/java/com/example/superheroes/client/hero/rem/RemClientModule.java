package com.example.superheroes.client.hero.rem;

import com.example.superheroes.client.ClientRemDemonismState;
import com.example.superheroes.client.core.module.HeroClientContext;
import com.example.superheroes.client.core.module.HeroClientModule;
import com.example.superheroes.client.render.RamRenderer;
import com.example.superheroes.entity.ModEntities;
import com.example.superheroes.hero.RemHero;
import com.example.superheroes.network.RemDemonismS2CPayload;
import net.minecraft.resources.ResourceLocation;

public record RemClientModule() implements HeroClientModule {
	@Override
	public ResourceLocation heroId() {
		return RemHero.ID;
	}

	@Override
	public void register(HeroClientContext ctx) {
		ctx.entityRenderer(ModEntities.RAM, RamRenderer::new);
		ctx.receive(RemDemonismS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> ClientRemDemonismState.update(
						payload.playerId(), payload.charge(), payload.active(), payload.permanent())));
	}
}
