package com.example.superheroes.effect;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class GokuKiStackController {
	public static final int MAX_STACKS = 3;

	private static final Map<UUID, Integer> STACKS = new HashMap<>();

	private GokuKiStackController() {
	}

	public static void init() {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (entity instanceof ServerPlayer player && amount > 0.5f) {
				STACKS.remove(player.getUUID());
			}
			return true;
		});
	}

	public static int getStacks(ServerPlayer player) {
		return STACKS.getOrDefault(player.getUUID(), 0);
	}

	public static int addStack(ServerPlayer player) {
		int next = Math.min(MAX_STACKS, getStacks(player) + 1);
		STACKS.put(player.getUUID(), next);
		return next;
	}

	public static void clear(ServerPlayer player) {
		STACKS.remove(player.getUUID());
	}

	public static int consume(ServerPlayer player) {
		Integer s = STACKS.remove(player.getUUID());
		return s == null ? 0 : s;
	}
}
