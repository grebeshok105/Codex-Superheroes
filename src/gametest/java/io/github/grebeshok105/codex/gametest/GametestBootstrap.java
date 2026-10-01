package io.github.grebeshok105.codex.gametest;

import io.github.grebeshok105.codex.SuperheroesMod;
import net.fabricmc.api.ModInitializer;

/**
 * The mod declares {@code environment: "client"} (EMF is a hard dep), so Fabric
 * env-filters it out of this dedicated-server run: its {@code main} entrypoint
 * and mixin configs never fire. The GameTest harness runs all of
 * {@link SuperheroesMod#onInitialize()} itself — registrations, attachments,
 * payloads and hero modules all live behind that one call.
 */
public final class GametestBootstrap implements ModInitializer {
	@Override
	public void onInitialize() {
		new SuperheroesMod().onInitialize();
	}
}
