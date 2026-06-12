package com.example.superheroes.client.fx;

import com.example.superheroes.network.ScorpionFxS2CPayload;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.phys.Vec3;

/**
 * Client dispatcher for Scorpion's hellfire effects. When Veil is installed it spawns
 * glowing Quasar emitters (which also emit dynamic light); otherwise it does nothing
 * extra because the server already broadcasts vanilla particles to every client.
 *
 * Veil classes live in {@link VeilScorpionFx} and are only classloaded when "veil" is
 * present, so the mod runs cleanly with or without Veil installed.
 */
public final class ClientScorpionFx {
	private static final boolean VEIL = FabricLoader.getInstance().isModLoaded("veil");

	private ClientScorpionFx() {
	}

	public static void play(ScorpionFxS2CPayload payload) {
		if (!VEIL) {
			return;
		}
		Vec3 origin = payload.origin();
		Vec3 target = payload.target();
		try {
			switch (payload.kind()) {
				case ScorpionFxS2CPayload.KIND_HARPOON -> VeilScorpionFx.harpoon(origin, target);
				case ScorpionFxS2CPayload.KIND_PILLAR -> VeilScorpionFx.pillar(target);
				case ScorpionFxS2CPayload.KIND_TELEPORT -> VeilScorpionFx.teleport(origin);
				case ScorpionFxS2CPayload.KIND_BREATH -> VeilScorpionFx.breath(origin, target);
				default -> {
				}
			}
		} catch (Throwable ignored) {
			// Never let an FX hiccup break the client.
		}
	}
}
