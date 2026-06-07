package com.example.superheroes.resource;

import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EnergyLocks {
	private static final Map<UUID, Long> LOCKS = new ConcurrentHashMap<>();

	private EnergyLocks() {
	}

	public static void lockTicks(ServerPlayer player, int ticks) {
		LOCKS.put(player.getUUID(), player.level().getGameTime() + ticks);
	}

	public static boolean isLocked(ServerPlayer player) {
		Long deadline = LOCKS.get(player.getUUID());
		if (deadline == null) return false;
		if (player.level().getGameTime() >= deadline) {
			LOCKS.remove(player.getUUID());
			return false;
		}
		return true;
	}

	public static int remainingTicks(ServerPlayer player) {
		Long deadline = LOCKS.get(player.getUUID());
		if (deadline == null) return 0;
		long left = deadline - player.level().getGameTime();
		return left > 0 ? (int) left : 0;
	}

	public static void clear(UUID id) {
		LOCKS.remove(id);
	}
}
