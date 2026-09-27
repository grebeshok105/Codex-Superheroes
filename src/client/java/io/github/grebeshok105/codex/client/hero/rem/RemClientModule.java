package io.github.grebeshok105.codex.client.hero.rem;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.client.ClientRemDemonismState;
import io.github.grebeshok105.codex.client.core.module.HeroClientContext;
import io.github.grebeshok105.codex.client.core.module.HeroClientModule;
import io.github.grebeshok105.codex.client.hud.RemDemonismHud;
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
		// Rem's demonism bar was split out of the legacy shared rage-bar layer; own
		// registration under the rem_demonism layer id.
		ctx.hud(600, ModId.of("rem_demonism"), RemDemonismHud::render);
		ctx.playerLayer(renderer -> new RemOniHornFeatureRenderer(renderer));
		ctx.entityRenderer(ModEntities.RAM, RamRenderer::new);
		ctx.receive(RemDemonismS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> ClientRemDemonismState.update(
						payload.playerId(), payload.charge(), payload.active(), payload.permanent())));
	}
}
