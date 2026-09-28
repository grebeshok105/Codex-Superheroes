package io.github.grebeshok105.codex.client;

import io.github.grebeshok105.codex.client.bootstrap.ContentClientModules;
import io.github.grebeshok105.codex.client.bootstrap.HeroClientModules;
import io.github.grebeshok105.codex.client.config.SuperheroesClientConfig;
import io.github.grebeshok105.codex.client.core.anim.AnimationLibrary;
import io.github.grebeshok105.codex.client.core.hud.HudLayers;
import io.github.grebeshok105.codex.client.core.input.InputSenders;
import io.github.grebeshok105.codex.client.core.input.MeleeChargeSender;
import io.github.grebeshok105.codex.client.core.input.ModKeys;
import io.github.grebeshok105.codex.client.core.net.CoreClientReceivers;
import io.github.grebeshok105.codex.client.core.vfx.VfxRuntime;
import io.github.grebeshok105.codex.client.fx.CoreFx;
import io.github.grebeshok105.codex.client.hud.CoreHuds;
import io.github.grebeshok105.codex.client.render.CoreRenderers;
import io.github.grebeshok105.codex.client.screen.ModScreens;
import net.fabricmc.api.ClientModInitializer;

/** Composition root: client core bootstrap + module lists. Every block lives at its owner. */
public class SuperheroesClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientSessionState.init();
		ModKeys.init();
		CoreClientReceivers.init();
		VfxRuntime.init();
		AnimationLibrary.init();
		HeroClientModules.bootstrap();
		ContentClientModules.bootstrap();
		ClientHeroDimsWatcher.init();
		CoreRenderers.init();
		CoreFx.init();
		SuperheroesClientConfig.load();
		HudLayers.init();
		CoreHuds.init();
		ModScreens.init();
		InputSenders.init();
		MeleeChargeSender.init();
	}
}
