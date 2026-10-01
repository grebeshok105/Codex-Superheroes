package io.github.grebeshok105.codex.datagen;

import io.github.grebeshok105.codex.SuperheroesMod;
import net.fabricmc.api.ModInitializer;

/**
 * The mod declares {@code environment: "client"} (EMF is a hard dep), so Fabric
 * env-filters it out of this dedicated-server datagen run. This harness mod
 * re-runs {@link SuperheroesMod#onInitialize()} so registry content the data
 * providers read (damage types, tags, items) exists, then the
 * {@code fabric-datagen} entrypoint in {@code superheroes-datagen} feeds the
 * providers.
 */
public final class DatagenBootstrap implements ModInitializer {
	@Override
	public void onInitialize() {
		new SuperheroesMod().onInitialize();
	}
}
