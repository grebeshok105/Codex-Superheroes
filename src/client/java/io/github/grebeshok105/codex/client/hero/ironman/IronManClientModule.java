package io.github.grebeshok105.codex.client.hero.ironman;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.client.ClientHeroState;
import io.github.grebeshok105.codex.client.ModKeys;
import io.github.grebeshok105.codex.client.core.module.HeroClientContext;
import io.github.grebeshok105.codex.client.core.module.HeroClientModule;
import io.github.grebeshok105.codex.client.core.render.BeamDraws;
import io.github.grebeshok105.codex.client.core.render.BeamStyle;
import io.github.grebeshok105.codex.client.hero.ironman.hud.IronManPanelSection;
import io.github.grebeshok105.codex.client.hero.ironman.hud.JarvisDetectionHud;
import io.github.grebeshok105.codex.client.hero.ironman.hud.JarvisOverlayHud;
import io.github.grebeshok105.codex.client.hero.ironman.hud.ReactorOverlayHud;
import io.github.grebeshok105.codex.client.hero.ironman.render.IronLegionDroneRenderer;
import io.github.grebeshok105.codex.client.hero.ironman.render.IronManEspRenderer;
import io.github.grebeshok105.codex.client.hero.ironman.render.IronManNanoFormLayer;
import io.github.grebeshok105.codex.client.hero.ironman.render.NanoSuitUpLayer;
import io.github.grebeshok105.codex.client.hero.ironman.render.SmartMissileRenderer;
import io.github.grebeshok105.codex.client.hero.ironman.state.ClientNanoFormState;
import io.github.grebeshok105.codex.client.hero.ironman.state.ClientNanoSuitUpState;
import io.github.grebeshok105.codex.client.hero.ironman.state.ClientNanoWeaponState;
import io.github.grebeshok105.codex.client.hero.ironman.state.ClientReactorState;
import io.github.grebeshok105.codex.client.hero.ironman.state.ClientRepulsorChargeState;
import io.github.grebeshok105.codex.client.hero.ironman.state.ClientSuitVariantState;
import io.github.grebeshok105.codex.core.net.BeamFxS2CPayload;
import io.github.grebeshok105.codex.hero.ironman.entity.IronManEntities;
import io.github.grebeshok105.codex.hero.ironman.IronManHero;
import io.github.grebeshok105.codex.hero.ironman.net.JarvisDetectionS2CPayload;
import io.github.grebeshok105.codex.hero.ironman.net.NanoFormS2CPayload;
import io.github.grebeshok105.codex.hero.ironman.net.ReactorStateS2CPayload;
import io.github.grebeshok105.codex.hero.ironman.net.SuitVariantS2CPayload;
import io.github.grebeshok105.codex.hero.ironman.registry.IronManParticles;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EndRodParticle;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

public record IronManClientModule() implements HeroClientModule {
	@Override
	public ResourceLocation heroId() {
		return IronManHero.ID;
	}

	@Override
	public void register(HeroClientContext ctx) {
		ctx.skin(new IronManSkinProvider());
		ctx.playerLayer(IronManNanoFormLayer::new);
		ctx.playerLayer(NanoSuitUpLayer::new);
		ctx.skinSuppression(ClientNanoSuitUpState::suppressHeroSkin);
		ctx.crosshairSuppression(IronManClientModule::suppressCrosshair);
		ctx.heroPanelSection(new IronManPanelSection());
		ctx.beamStyle(new BeamStyle(BeamFxS2CPayload.STYLE_REPULSOR,
				BeamDraws.REPULSOR_LIFETIME_MS, BeamDraws::repulsorTracer,
				ClientRepulsorChargeState::flash));

		ctx.receive(SuitVariantS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> ClientSuitVariantState.update(payload.playerId(), payload.variant())));
		ctx.receive(ReactorStateS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> ClientReactorState.update(payload.active(), payload.progressTicks(), payload.totalTicks(), payload.hasStock())));
		ctx.receive(JarvisDetectionS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> JarvisDetectionHud.onDetection(
						payload.playerName(), payload.heroId(), payload.distance(),
						payload.threatClass(), payload.jarvisQuote())));
		ctx.receive(NanoFormS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> ClientNanoFormState.update(payload.playerId(), payload.form())));

		ctx.hud(100, ModId.of("jarvis_overlay"), JarvisOverlayHud::render);
		ctx.hud(200, ModId.of("jarvis_detection"), JarvisDetectionHud::render);
		ctx.hud(1000, ModId.of("reactor_overlay"), ReactorOverlayHud::render);

		ctx.particle(IronManParticles.REPULSOR_SPARK, EndRodParticle.Provider::new);
		ctx.particle(IronManParticles.UNIBEAM_SPARK, EndRodParticle.Provider::new);

		ctx.actionKey(new KeyMapping(
				"key.superheroes.nano_weapon",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_N,
				ModKeys.CATEGORY), client -> ClientNanoWeaponState.cycle(1));
		ctx.actionKey(new KeyMapping(
				"key.superheroes.esp_toggle",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_K,
				ModKeys.CATEGORY), client -> IronManEspRenderer.cycleMode());

		IronManEspRenderer.register();
		ctx.entityRenderer(IronManEntities.SMART_MISSILE, SmartMissileRenderer::new);
		ctx.entityRenderer(IronManEntities.IRON_LEGION_DRONE, IronLegionDroneRenderer::new);

		ctx.clientTick(ClientNanoSuitUpState::clientTick);
		ctx.clientTick(JarvisDetectionHud::tick);
		ctx.clientTick(IronManClientModule::tickRepulsorCharge);
	}

	private static boolean suppressCrosshair() {
		var data = ClientHeroState.data();
		return data.hasHero() && IronManHero.ID.equals(data.heroId())
				&& !Minecraft.getInstance().options.hideGui;
	}

	private static void tickRepulsorCharge(Minecraft client) {
		boolean ironMan = client.player != null && ClientHeroState.data().hasHero()
				&& IronManHero.ID.equals(ClientHeroState.data().heroId());
		boolean sneaking = client.player != null && client.player.isShiftKeyDown();
		ClientRepulsorChargeState.clientTick(ironMan, sneaking);
	}
}
