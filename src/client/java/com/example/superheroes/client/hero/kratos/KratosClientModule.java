package com.example.superheroes.client.hero.kratos;

import com.example.superheroes.ModId;
import com.example.superheroes.client.core.module.HeroClientContext;
import com.example.superheroes.client.core.module.HeroClientModule;
import com.example.superheroes.client.hud.SpartanRageHud;
import com.example.superheroes.hero.KratosHero;
import com.example.superheroes.network.KratosRageS2CPayload;
import net.minecraft.resources.ResourceLocation;

public record KratosClientModule() implements HeroClientModule {
	@Override
	public ResourceLocation heroId() {
		return KratosHero.ID;
	}

	@Override
	public void register(HeroClientContext ctx) {
		ctx.hud(600, ModId.of("spartan_rage"), SpartanRageHud::render);
		ctx.receive(KratosRageS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> com.example.superheroes.client.ClientKratosRageState.update(payload.rage(), payload.active())));
	}
}
