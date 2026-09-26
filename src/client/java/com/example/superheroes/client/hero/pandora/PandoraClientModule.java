package com.example.superheroes.client.hero.pandora;

import com.example.superheroes.ModId;
import com.example.superheroes.client.core.module.HeroClientContext;
import com.example.superheroes.client.core.module.HeroClientModule;
import com.example.superheroes.client.hud.MirrorWarpFlashHud;
import com.example.superheroes.client.hud.PandoraDeathTitleHud;
import com.example.superheroes.hero.PandoraHero;
import com.example.superheroes.network.MirrorDimensionS2CPayload;
import com.example.superheroes.network.PandoraCinematicS2CPayload;
import com.example.superheroes.network.PandoraHouseStateS2CPayload;
import net.minecraft.resources.ResourceLocation;

public record PandoraClientModule() implements HeroClientModule {
	@Override
	public ResourceLocation heroId() {
		return PandoraHero.ID;
	}

	@Override
	public void register(HeroClientContext ctx) {
		ctx.hud(1900, ModId.of("pandora_death_title"), PandoraDeathTitleHud::render);
		ctx.hud(2400, ModId.of("mirror_warp_flash"), MirrorWarpFlashHud::render);
		ctx.receive(MirrorDimensionS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> {
					switch (payload.action()) {
						case com.example.superheroes.network.MirrorDimensionS2CPayload.ACTION_ON ->
								com.example.superheroes.client.ClientMirrorDimensionState.activate(payload.mode(), payload.scale());
						case com.example.superheroes.network.MirrorDimensionS2CPayload.ACTION_OFF ->
								com.example.superheroes.client.ClientMirrorDimensionState.deactivate(true);
						case com.example.superheroes.network.MirrorDimensionS2CPayload.ACTION_KEEPALIVE ->
								com.example.superheroes.client.ClientMirrorDimensionState.keepalive();
						case com.example.superheroes.network.MirrorDimensionS2CPayload.ACTION_SWITCH ->
								com.example.superheroes.client.ClientMirrorDimensionState.switchMode(payload.mode(), payload.scale());
						default -> {
						}
					}
				}));

		ctx.receive(PandoraCinematicS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> {
					if (payload.phase() == com.example.superheroes.network.PandoraCinematicS2CPayload.PHASE_START) {
						com.example.superheroes.client.ClientPandoraDeathState.start(
								payload.pandoraId(), payload.killerId(), payload.px(), payload.py(), payload.pz());
					} else {
						com.example.superheroes.client.ClientPandoraDeathState.end();
					}
				}));

		ctx.receive(PandoraHouseStateS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> com.example.superheroes.client.ClientPandoraHouseState.set(payload.open())));
	}
}
