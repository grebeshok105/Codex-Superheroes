package io.github.grebeshok105.codex.client.hero.homelander;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.client.core.module.HeroClientContext;
import io.github.grebeshok105.codex.client.core.module.HeroClientModule;
import io.github.grebeshok105.codex.client.hud.UraniumThreatHud;
import io.github.grebeshok105.codex.client.render.LaserBeamRenderer;
import io.github.grebeshok105.codex.hero.HomelanderHero;
import io.github.grebeshok105.codex.network.LaserFiredS2CPayload;
import io.github.grebeshok105.codex.network.UraniumPressureS2CPayload;
import io.github.grebeshok105.codex.network.UraniumThreatS2CPayload;
import net.minecraft.resources.ResourceLocation;

public record HomelanderClientModule() implements HeroClientModule {
	@Override
	public ResourceLocation heroId() {
		return HomelanderHero.ID;
	}

	@Override
	public void register(HeroClientContext ctx) {
		ctx.skin(new HomelanderSkinProvider());
		ctx.hud(1400, ModId.of("uranium_threat"), UraniumThreatHud::render);
		ctx.receive(LaserFiredS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> LaserBeamRenderer.add(payload.start(), payload.end())));
		ctx.receive(UraniumPressureS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> io.github.grebeshok105.codex.client.ClientUraniumPressureState.update(payload.pressuredHomelanders())));
		ctx.receive(UraniumThreatS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> io.github.grebeshok105.codex.client.ClientUraniumThreatState.update(payload.self(), payload.sourceCount())));
	}
}
