package io.github.grebeshok105.codex.client.hero.pandora;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.client.hero.pandora.iris.IrisShaderBridge;
import io.github.grebeshok105.codex.client.core.input.InputLock;
import io.github.grebeshok105.codex.client.core.module.HeroClientContext;
import io.github.grebeshok105.codex.client.core.module.HeroClientModule;
import io.github.grebeshok105.codex.client.core.text.TextObfuscationLayers;
import io.github.grebeshok105.codex.client.hero.pandora.hud.MirrorWarpFlashHud;
import io.github.grebeshok105.codex.client.hero.pandora.hud.PandoraDeathTitleHud;
import io.github.grebeshok105.codex.client.hero.pandora.hud.VanityCipher;
import io.github.grebeshok105.codex.client.hero.pandora.state.ClientMirrorDimensionState;
import io.github.grebeshok105.codex.client.hero.pandora.state.ClientPandoraDeathState;
import io.github.grebeshok105.codex.client.hero.pandora.state.ClientPandoraHouseState;
import io.github.grebeshok105.codex.hero.pandora.PandoraHero;
import io.github.grebeshok105.codex.hero.pandora.net.MirrorDimensionS2CPayload;
import io.github.grebeshok105.codex.hero.pandora.net.PandoraCinematicS2CPayload;
import io.github.grebeshok105.codex.hero.pandora.net.PandoraHouseStateS2CPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.resources.ResourceLocation;

public record PandoraClientModule() implements HeroClientModule {
	@Override
	public ResourceLocation heroId() {
		return PandoraHero.ID;
	}

	@Override
	public void register(HeroClientContext ctx) {
		// The revival cut-scene locks all input via the shared InputLock mixins (key/button
		// releases still pass through — audit B15).
		InputLock.register(ModId.of("pandora_cinematic"), ClientPandoraDeathState::active);
		// The House-of-Vanity text cipher rides the shared Font obfuscation layers.
		TextObfuscationLayers.register(ModId.of("vanity_cipher"), new TextObfuscationLayers.Layer() {
			@Override
			public boolean active() {
				return VanityCipher.active();
			}

			@Override
			public String apply(String text) {
				return VanityCipher.cipher(text);
			}

			@Override
			public net.minecraft.util.FormattedCharSequence apply(net.minecraft.util.FormattedCharSequence text) {
				return VanityCipher.cipher(text);
			}
		});
		// Wire the flash overlay into the mirror-dimension state — state must not know hud,
		// so the module root bridges the two leaves.
		ClientMirrorDimensionState.setFlashOverlay(new ClientMirrorDimensionState.FlashOverlay() {
			@Override
			public void flashAndRun(Runnable action) {
				MirrorWarpFlashHud.flashAndRun(action);
			}

			@Override
			public boolean isCovering() {
				return MirrorWarpFlashHud.isCovering();
			}

			@Override
			public void fallbackTick() {
				MirrorWarpFlashHud.fallbackTick();
			}
		});
		// Iris crash-restore and the warp/death-state ticks used to live in SuperheroesClient.
		IrisShaderBridge.restoreAfterCrashIfNeeded();
		ClientTickEvents.END_CLIENT_TICK.register(client -> IrisShaderBridge.tickCrashRestore());
		ClientTickEvents.END_CLIENT_TICK.register(ClientMirrorDimensionState::tick);
		ClientTickEvents.END_CLIENT_TICK.register(client -> ClientPandoraDeathState.tick());

		ctx.hud(1900, ModId.of("pandora_death_title"), PandoraDeathTitleHud::render);
		ctx.hud(2400, ModId.of("mirror_warp_flash"), MirrorWarpFlashHud::render);
		ctx.receive(MirrorDimensionS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> {
					switch (payload.action()) {
						case MirrorDimensionS2CPayload.ACTION_ON ->
								ClientMirrorDimensionState.activate(payload.mode(), payload.scale());
						case MirrorDimensionS2CPayload.ACTION_OFF ->
								ClientMirrorDimensionState.deactivate(true);
						case MirrorDimensionS2CPayload.ACTION_KEEPALIVE ->
								ClientMirrorDimensionState.keepalive();
						case MirrorDimensionS2CPayload.ACTION_SWITCH ->
								ClientMirrorDimensionState.switchMode(payload.mode(), payload.scale());
						default -> {
						}
					}
				}));

		ctx.receive(PandoraCinematicS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> {
					if (payload.phase() == PandoraCinematicS2CPayload.PHASE_START) {
						ClientPandoraDeathState.start(
								payload.pandoraId(), payload.killerId(), payload.px(), payload.py(), payload.pz());
					} else {
						ClientPandoraDeathState.end();
					}
				}));

		ctx.receive(PandoraHouseStateS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> ClientPandoraHouseState.set(payload.open())));
	}
}
