package io.github.grebeshok105.codex.client.hero.rem;

import io.github.grebeshok105.codex.client.ClientRemDemonismState;
import io.github.grebeshok105.codex.client.core.module.HeroClientContext;
import io.github.grebeshok105.codex.client.core.module.HeroClientModule;
import io.github.grebeshok105.codex.client.render.RamRenderer;
import io.github.grebeshok105.codex.client.render.RemOniHornFeatureRenderer;
import io.github.grebeshok105.codex.entity.ModEntities;
import io.github.grebeshok105.codex.hero.RemHero;
import io.github.grebeshok105.codex.network.RemDemonismS2CPayload;
import net.minecraft.resources.ResourceLocation;

public record RemClientModule() implements HeroClientModule {
	@Override
	public ResourceLocation heroId() {
		return RemHero.ID;
	}

	@Override
	public void register(HeroClientContext ctx) {
		ctx.playerLayer(renderer -> new RemOniHornFeatureRenderer(renderer));
		ctx.entityRenderer(ModEntities.RAM, RamRenderer::new);
		ctx.receive(RemDemonismS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> ClientRemDemonismState.update(
						payload.playerId(), payload.charge(), payload.active(), payload.permanent())));
	}
}
