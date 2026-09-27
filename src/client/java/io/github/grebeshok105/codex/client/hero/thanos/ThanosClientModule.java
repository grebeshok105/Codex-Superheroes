package io.github.grebeshok105.codex.client.hero.thanos;

import io.github.grebeshok105.codex.client.core.module.HeroClientContext;
import io.github.grebeshok105.codex.client.core.module.HeroClientModule;
import io.github.grebeshok105.codex.client.render.CosmicBeamRenderer;
import io.github.grebeshok105.codex.hero.ThanosHero;
import io.github.grebeshok105.codex.network.ThanosCosmicBeamS2CPayload;
import io.github.grebeshok105.codex.network.ThanosStonesS2CPayload;
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
				context.client().execute(() -> io.github.grebeshok105.codex.client.ClientThanosState.update(payload.playerId(), payload.bitmask())));
	}
}
