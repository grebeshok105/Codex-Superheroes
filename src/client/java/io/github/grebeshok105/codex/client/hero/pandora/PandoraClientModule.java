package io.github.grebeshok105.codex.client.hero.pandora;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.client.core.module.HeroClientContext;
import io.github.grebeshok105.codex.client.core.module.HeroClientModule;
import io.github.grebeshok105.codex.client.hud.MirrorWarpFlashHud;
import io.github.grebeshok105.codex.client.hud.PandoraDeathTitleHud;
import io.github.grebeshok105.codex.hero.PandoraHero;
import io.github.grebeshok105.codex.network.MirrorDimensionS2CPayload;
import io.github.grebeshok105.codex.network.PandoraCinematicS2CPayload;
import io.github.grebeshok105.codex.network.PandoraHouseStateS2CPayload;
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
						case io.github.grebeshok105.codex.network.MirrorDimensionS2CPayload.ACTION_ON ->
								io.github.grebeshok105.codex.client.ClientMirrorDimensionState.activate(payload.mode(), payload.scale());
						case io.github.grebeshok105.codex.network.MirrorDimensionS2CPayload.ACTION_OFF ->
								io.github.grebeshok105.codex.client.ClientMirrorDimensionState.deactivate(true);
						case io.github.grebeshok105.codex.network.MirrorDimensionS2CPayload.ACTION_KEEPALIVE ->
								io.github.grebeshok105.codex.client.ClientMirrorDimensionState.keepalive();
						case io.github.grebeshok105.codex.network.MirrorDimensionS2CPayload.ACTION_SWITCH ->
								io.github.grebeshok105.codex.client.ClientMirrorDimensionState.switchMode(payload.mode(), payload.scale());
						default -> {
						}
					}
				}));

		ctx.receive(PandoraCinematicS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> {
					if (payload.phase() == io.github.grebeshok105.codex.network.PandoraCinematicS2CPayload.PHASE_START) {
						io.github.grebeshok105.codex.client.ClientPandoraDeathState.start(
								payload.pandoraId(), payload.killerId(), payload.px(), payload.py(), payload.pz());
					} else {
						io.github.grebeshok105.codex.client.ClientPandoraDeathState.end();
					}
				}));

		ctx.receive(PandoraHouseStateS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> io.github.grebeshok105.codex.client.ClientPandoraHouseState.set(payload.open())));
	}
}
