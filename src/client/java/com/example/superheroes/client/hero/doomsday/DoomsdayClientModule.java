package com.example.superheroes.client.hero.doomsday;

import com.example.superheroes.ModId;
import com.example.superheroes.client.core.module.HeroClientContext;
import com.example.superheroes.client.core.module.HeroClientModule;
import com.example.superheroes.client.hud.DoomsdayGlitchHud;
import com.example.superheroes.hero.DoomsdayHero;
import com.example.superheroes.network.DoomsdayProgressS2CPayload;
import net.minecraft.resources.ResourceLocation;

public record DoomsdayClientModule() implements HeroClientModule {
	@Override
	public ResourceLocation heroId() {
		return DoomsdayHero.ID;
	}

	@Override
	public void register(HeroClientContext ctx) {
		ctx.hud(1600, ModId.of("doomsday_glitch"), DoomsdayGlitchHud::render);
		ctx.receive(DoomsdayProgressS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> com.example.superheroes.client.ClientDoomsdayState.update(payload.tier(), payload.adaptations())));
	}
}
