package io.github.grebeshok105.codex.client.hero.homelander;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.client.core.module.HeroClientContext;
import io.github.grebeshok105.codex.client.core.module.HeroClientModule;
import io.github.grebeshok105.codex.client.hero.homelander.hud.SunWindupHud;
import io.github.grebeshok105.codex.client.hero.homelander.hud.UraniumThreatHud;
import io.github.grebeshok105.codex.client.hero.homelander.render.LocalLaserOverlay;
import io.github.grebeshok105.codex.client.hero.homelander.state.ClientUraniumPressureState;
import io.github.grebeshok105.codex.client.hero.homelander.state.ClientUraniumThreatState;
import io.github.grebeshok105.codex.client.render.LaserBeamRenderer;
import io.github.grebeshok105.codex.hero.homelander.HomelanderHero;
import io.github.grebeshok105.codex.network.LaserFiredS2CPayload;
import io.github.grebeshok105.codex.hero.homelander.net.UraniumPressureS2CPayload;
import io.github.grebeshok105.codex.hero.homelander.net.UraniumThreatS2CPayload;
import net.minecraft.resources.ResourceLocation;

public record HomelanderClientModule() implements HeroClientModule {
	@Override
	public ResourceLocation heroId() {
		return HomelanderHero.ID;
	}

	@Override
	public void register(HeroClientContext ctx) {
		ctx.skin(new HomelanderSkinProvider());
		ctx.hud(900, ModId.of("sun_windup"), SunWindupHud::render);
		ctx.hud(1400, ModId.of("uranium_threat"), UraniumThreatHud::render);
		LocalLaserOverlay.register();
		ctx.receive(LaserFiredS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> LaserBeamRenderer.add(payload.start(), payload.end())));
		ctx.receive(UraniumPressureS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> ClientUraniumPressureState.update(payload.pressuredHomelanders())));
		ctx.receive(UraniumThreatS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> ClientUraniumThreatState.update(payload.self(), payload.sourceCount())));
	}
}
