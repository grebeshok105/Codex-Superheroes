package com.example.superheroes.client.hero.sungjinwoo;

import com.example.superheroes.client.core.module.HeroClientContext;
import com.example.superheroes.client.core.module.HeroClientModule;
import com.example.superheroes.client.render.ShadowSoldierRenderer;
import com.example.superheroes.entity.ModEntities;
import com.example.superheroes.hero.SungJinwooHero;
import com.example.superheroes.network.SungShadowArmyS2CPayload;
import net.minecraft.resources.ResourceLocation;

public record SungJinwooClientModule() implements HeroClientModule {
	@Override
	public ResourceLocation heroId() {
		return SungJinwooHero.ID;
	}

	@Override
	public void register(HeroClientContext ctx) {
		ctx.entityRenderer(ModEntities.SHADOW_SOLDIER, ShadowSoldierRenderer::new);
		ctx.receive(SungShadowArmyS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> com.example.superheroes.client.ClientShadowArmyState.update(
						payload.playerId(), payload.hasShadows(), payload.count(), payload.phase2())));
	}
}
