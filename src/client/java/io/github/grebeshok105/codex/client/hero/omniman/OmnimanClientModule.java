package io.github.grebeshok105.codex.client.hero.omniman;

import io.github.grebeshok105.codex.client.core.module.HeroClientContext;
import io.github.grebeshok105.codex.client.core.module.HeroClientModule;
import io.github.grebeshok105.codex.hero.OmnimanHero;
import io.github.grebeshok105.codex.network.ThinkMarkS2CPayload;
import net.minecraft.resources.ResourceLocation;

public record OmnimanClientModule() implements HeroClientModule {
	@Override
	public ResourceLocation heroId() {
		return OmnimanHero.ID;
	}

	@Override
	public void register(HeroClientContext ctx) {
		ctx.receive(ThinkMarkS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> io.github.grebeshok105.codex.client.ClientThinkMarkState.update(payload.playerId(), payload.active())));
	}
}
