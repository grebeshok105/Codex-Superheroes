package com.example.superheroes.client.hero.ironman;

import com.example.superheroes.ModId;
import com.example.superheroes.client.ClientHeroState;
import com.example.superheroes.client.ClientNanoFormState;
import com.example.superheroes.client.ClientNanoSuitUpState;
import com.example.superheroes.client.ClientNanoWeaponState;
import com.example.superheroes.client.ClientReactorState;
import com.example.superheroes.client.ClientRepulsorChargeState;
import com.example.superheroes.client.ModKeys;
import com.example.superheroes.client.core.module.HeroClientContext;
import com.example.superheroes.client.core.module.HeroClientModule;
import com.example.superheroes.client.hud.JarvisDetectionHud;
import com.example.superheroes.client.hud.JarvisOverlayHud;
import com.example.superheroes.client.hud.ReactorOverlayHud;
import com.example.superheroes.client.render.IronLegionDroneRenderer;
import com.example.superheroes.client.render.IronManEspRenderer;
import com.example.superheroes.client.render.RepulsorBeamRenderer;
import com.example.superheroes.client.render.SmartMissileRenderer;
import com.example.superheroes.entity.ModEntities;
import com.example.superheroes.hero.IronManHero;
import com.example.superheroes.network.JarvisDetectionS2CPayload;
import com.example.superheroes.network.NanoFormS2CPayload;
import com.example.superheroes.network.ReactorStateS2CPayload;
import com.example.superheroes.network.RepulsorBlastS2CPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

public record IronManClientModule() implements HeroClientModule {
	@Override
	public ResourceLocation heroId() {
		return IronManHero.ID;
	}

	@Override
	public void register(HeroClientContext ctx) {
		ctx.receive(RepulsorBlastS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> RepulsorBeamRenderer.add(payload.start(), payload.end())));
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

		RepulsorBeamRenderer.register();
		IronManEspRenderer.register();
		EntityRendererRegistry.register(ModEntities.SMART_MISSILE, SmartMissileRenderer::new);
		EntityRendererRegistry.register(ModEntities.IRON_LEGION_DRONE, IronLegionDroneRenderer::new);

		ClientTickEvents.END_CLIENT_TICK.register(ClientNanoSuitUpState::clientTick);
		ClientTickEvents.END_CLIENT_TICK.register(JarvisDetectionHud::tick);
		ClientTickEvents.END_CLIENT_TICK.register(IronManClientModule::tickRepulsorCharge);
	}

	private static void tickRepulsorCharge(Minecraft client) {
		boolean ironMan = client.player != null && ClientHeroState.data().hasHero()
				&& IronManHero.ID.equals(ClientHeroState.data().heroId());
		boolean sneaking = client.player != null && client.player.isShiftKeyDown();
		ClientRepulsorChargeState.clientTick(ironMan, sneaking);
	}
}
