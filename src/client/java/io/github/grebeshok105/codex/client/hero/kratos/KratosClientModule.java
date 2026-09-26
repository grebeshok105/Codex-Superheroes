package io.github.grebeshok105.codex.client.hero.kratos;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.client.core.module.HeroClientContext;
import io.github.grebeshok105.codex.client.core.module.HeroClientModule;
import io.github.grebeshok105.codex.client.hud.SpartanRageHud;
import io.github.grebeshok105.codex.hero.KratosHero;
import io.github.grebeshok105.codex.network.KratosRageS2CPayload;
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
				context.client().execute(() -> io.github.grebeshok105.codex.client.ClientKratosRageState.update(payload.rage(), payload.active())));
	}
}
