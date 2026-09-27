package io.github.grebeshok105.codex.core.resource;

import io.github.grebeshok105.codex.core.lifecycle.LifecycleRegistrar;
import io.github.grebeshok105.codex.core.lifecycle.OwnedSessionMap;
import io.github.grebeshok105.codex.core.lifecycle.OwnedSessionMap.ClearOn;
import net.minecraft.server.level.ServerPlayer;

import java.util.EnumSet;
import java.util.UUID;

public final class EnergyLocks {
	private static final OwnedSessionMap<UUID, Long> LOCKS =
			OwnedSessionMap.create(LifecycleRegistrar.global(), EnumSet.of(ClearOn.LEAVE));

	private EnergyLocks() {
	}

	public static void lockTicks(ServerPlayer player, int ticks) {
		LOCKS.put(player.getUUID(), player.getUUID(), player.level().getGameTime() + ticks);
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
}
