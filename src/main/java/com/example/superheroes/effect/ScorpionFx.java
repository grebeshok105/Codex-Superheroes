package com.example.superheroes.effect;

import com.example.superheroes.network.ScorpionFxS2CPayload;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Server-side broadcaster for Scorpion's hellfire visual effects.
 * Sends a {@link ScorpionFxS2CPayload} to every player tracking the action so the
 * client can spawn Veil/Quasar emitters (or a vanilla fallback) where it happened.
 */
public final class ScorpionFx {
	private static final double BROADCAST_RADIUS = 64.0;

	private ScorpionFx() {
	}

	public static void broadcast(ServerLevel level, int kind, Vec3 origin, Vec3 target) {
		ScorpionFxS2CPayload payload = new ScorpionFxS2CPayload(kind, origin, target);
		Vec3 center = origin != null ? origin : target;
		if (center == null) {
			return;
		}
		for (ServerPlayer near : PlayerLookup.around(level, center, BROADCAST_RADIUS)) {
			ServerPlayNetworking.send(near, payload);
		}
	}

	public static void harpoon(ServerLevel level, Vec3 from, Vec3 to) {
		broadcast(level, ScorpionFxS2CPayload.KIND_HARPOON, from, to);
	}

	public static void pillar(ServerLevel level, Vec3 at) {
		broadcast(level, ScorpionFxS2CPayload.KIND_PILLAR, at, at);
	}

	public static void teleport(ServerLevel level, Vec3 at) {
		broadcast(level, ScorpionFxS2CPayload.KIND_TELEPORT, at, at);
	}

	public static void breath(ServerLevel level, Vec3 mouth, Vec3 dir) {
		broadcast(level, ScorpionFxS2CPayload.KIND_BREATH, mouth, dir);
	}
}
