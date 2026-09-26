package com.example.superheroes.client.hero.scorpion;

import com.example.superheroes.client.core.module.HeroClientContext;
import com.example.superheroes.client.core.module.HeroClientModule;
import com.example.superheroes.client.fx.ClientScorpionFx;
import com.example.superheroes.hero.ScorpionHero;
import com.example.superheroes.network.ScorpionFxS2CPayload;
import net.minecraft.resources.ResourceLocation;

public record ScorpionClientModule() implements HeroClientModule {
	@Override
	public ResourceLocation heroId() {
		return ScorpionHero.ID;
	}

	@Override
	public void register(HeroClientContext ctx) {
		ctx.receive(ScorpionFxS2CPayload.TYPE,
				(payload, context) -> context.client().execute(() -> ClientScorpionFx.play(payload)));
	}
}
