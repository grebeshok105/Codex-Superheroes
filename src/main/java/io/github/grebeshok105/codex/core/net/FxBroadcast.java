package io.github.grebeshok105.codex.core.net;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Who receives a visual-only payload. Pick the variant the call site used before; do not widen audiences silently. */
public final class FxBroadcast {
	private FxBroadcast() {
	}

	public static void tracking(Entity source, CustomPacketPayload payload) {
		for (ServerPlayer observer : PlayerLookup.tracking(source)) {
			ServerPlayNetworking.send(observer, payload);
		}
	}

	public static void trackingAndSelf(ServerPlayer source, CustomPacketPayload payload) {
		tracking(source, payload);
		ServerPlayNetworking.send(source, payload);
	}

	public static void around(ServerLevel level, Vec3 center, double radius, CustomPacketPayload payload) {
		for (ServerPlayer near : PlayerLookup.around(level, center, radius)) {
			ServerPlayNetworking.send(near, payload);
		}
	}
}
