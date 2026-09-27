package io.github.grebeshok105.codex.client.hero.ironman;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.client.ClientHeroState;
import io.github.grebeshok105.codex.client.ClientNanoFormState;
import io.github.grebeshok105.codex.client.ClientNanoSuitUpState;
import io.github.grebeshok105.codex.client.ClientNanoWeaponState;
import io.github.grebeshok105.codex.client.ClientReactorState;
import io.github.grebeshok105.codex.client.ClientRepulsorChargeState;
import io.github.grebeshok105.codex.client.ModKeys;
import io.github.grebeshok105.codex.client.core.module.HeroClientContext;
import io.github.grebeshok105.codex.client.core.module.HeroClientModule;
import io.github.grebeshok105.codex.client.hud.JarvisDetectionHud;
import io.github.grebeshok105.codex.client.hud.JarvisOverlayHud;
import io.github.grebeshok105.codex.client.hud.ReactorOverlayHud;
import io.github.grebeshok105.codex.client.render.IronLegionDroneRenderer;
import io.github.grebeshok105.codex.client.render.IronManEspRenderer;
import io.github.grebeshok105.codex.client.render.IronManNanoFormLayer;
import io.github.grebeshok105.codex.client.render.NanoSuitUpLayer;
import io.github.grebeshok105.codex.client.render.RepulsorBeamRenderer;
import io.github.grebeshok105.codex.client.render.SmartMissileRenderer;
import io.github.grebeshok105.codex.entity.ModEntities;
import io.github.grebeshok105.codex.hero.IronManHero;
import io.github.grebeshok105.codex.network.JarvisDetectionS2CPayload;
import io.github.grebeshok105.codex.network.NanoFormS2CPayload;
import io.github.grebeshok105.codex.network.ReactorStateS2CPayload;
import io.github.grebeshok105.codex.network.RepulsorBlastS2CPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
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
		ctx.skin(new IronManSkinProvider());
		ctx.playerLayer(renderer -> new IronManNanoFormLayer(renderer));
		ctx.playerLayer(renderer -> new NanoSuitUpLayer(renderer));
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
		ctx.entityRenderer(ModEntities.SMART_MISSILE, SmartMissileRenderer::new);
		ctx.entityRenderer(ModEntities.IRON_LEGION_DRONE, IronLegionDroneRenderer::new);

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
