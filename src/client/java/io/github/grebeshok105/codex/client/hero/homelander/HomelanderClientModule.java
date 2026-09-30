package io.github.grebeshok105.codex.client.hero.homelander;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.client.core.module.HeroClientContext;
import io.github.grebeshok105.codex.client.core.module.HeroClientModule;
import io.github.grebeshok105.codex.client.hero.homelander.emf.EmfAssets;
import io.github.grebeshok105.codex.client.hero.homelander.flight.HomelanderEmfLayer;
import io.github.grebeshok105.codex.client.hero.homelander.flight.HomelanderFlightDriver;
import io.github.grebeshok105.codex.client.hero.homelander.flight.HomelanderPoseApi;
import io.github.grebeshok105.codex.client.hero.homelander.fx.HomelanderFx;
import io.github.grebeshok105.codex.client.hero.homelander.hud.SunWindupHud;
import io.github.grebeshok105.codex.client.hero.homelander.hud.UraniumThreatHud;

import io.github.grebeshok105.codex.client.hero.homelander.state.ClientUraniumPressureState;
import io.github.grebeshok105.codex.client.hero.homelander.state.ClientUraniumThreatState;
import io.github.grebeshok105.codex.client.core.render.BeamDraws;
import io.github.grebeshok105.codex.client.core.render.BeamStyle;
import io.github.grebeshok105.codex.hero.homelander.HomelanderBlocks;
import io.github.grebeshok105.codex.hero.homelander.HomelanderHero;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.RenderType;
import io.github.grebeshok105.codex.core.net.BeamFxS2CPayload;
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
		// The boss entity still sends BeamFx.laser (HomelanderEyeLaserGoal);
		// STYLE_LASER stays registered for that path. Player eye lasers now
		// render through the homelander/laser VFX channel instead.
		ctx.beamStyle(new BeamStyle(BeamFxS2CPayload.STYLE_LASER,
				BeamDraws.LASER_LIFETIME_MS, BeamDraws::laserPair));
		// EMF presentation: segmented model + clip runtime + flight driver.
		// The suppression hides the vanilla PlayerModel while the runtime has
		// any weight on; the layer renders the EMF bone tree in its place.
		EmfAssets.init();
		ctx.playerLayer(HomelanderEmfLayer::new);
		ctx.playerModelSuppression(HomelanderPoseApi::suppressesVanillaModel);
		ctx.flightCameraFocus(HomelanderPoseApi::cameraFocusOffset);
		ctx.clientTick(HomelanderFlightDriver::tick);
		HomelanderFx.register(ctx);
		ctx.receive(UraniumPressureS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> ClientUraniumPressureState.update(payload.pressuredHomelanders())));
		ctx.receive(UraniumThreatS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> ClientUraniumThreatState.update(payload.self(), payload.sourceCount())));
		// The scorch decal carries alpha in its texture; without cutout the
		// transparent corners render as opaque black.
		BlockRenderLayerMap.INSTANCE.putBlock(HomelanderBlocks.LASER_SCORCH, RenderType.cutout());
	}
}
