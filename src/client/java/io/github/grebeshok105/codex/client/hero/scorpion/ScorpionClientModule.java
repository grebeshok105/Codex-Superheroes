package io.github.grebeshok105.codex.client.hero.scorpion;

import io.github.grebeshok105.codex.client.core.module.HeroClientContext;
import io.github.grebeshok105.codex.client.core.module.HeroClientModule;
import io.github.grebeshok105.codex.client.fx.ClientScorpionFx;
import io.github.grebeshok105.codex.hero.ScorpionHero;
import io.github.grebeshok105.codex.network.ScorpionFxS2CPayload;
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
