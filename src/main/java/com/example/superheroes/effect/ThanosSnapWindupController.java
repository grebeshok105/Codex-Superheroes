package com.example.superheroes.effect;

import com.example.superheroes.ability.ThanosSnapAbility;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ThanosSnapWindupController {
	private static final Map<UUID, Pending> PENDING = new ConcurrentHashMap<>();

	private ThanosSnapWindupController() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (PENDING.isEmpty()) return;
			Iterator<Map.Entry<UUID, Pending>> it = PENDING.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<UUID, Pending> e = it.next();
				ServerPlayer player = server.getPlayerList().getPlayer(e.getKey());
				// A dead player stays in the player list until respawn — the snap must not fire
				// from a corpse (audit B17).
				if (player == null || player.isDeadOrDying()) {
					it.remove();
					continue;
				}
				Pending p = e.getValue();
				long now = player.serverLevel().getGameTime();
				if (!p.snapped && now >= p.snapAtTick) {
					ThanosSnapAbility.executeSnap(player);
					p.snapped = true;
				}
				if (now >= p.endTick) {
					it.remove();
				}
			}
		});
	}

	public static void schedule(ServerPlayer player, int snapInTicks, int totalTicks) {
		long now = player.serverLevel().getGameTime();
		PENDING.put(player.getUUID(), new Pending(now + snapInTicks, now + totalTicks));
	}

	public static boolean isWindingUp(ServerPlayer player) {
		Pending p = PENDING.get(player.getUUID());
		return p != null && !p.snapped;
	}

	/** Leave/death hook — a pending snap must never fire from a corpse or after relog. */
	public static void cancel(java.util.UUID playerId) {
		PENDING.remove(playerId);
	}

	/** World shutdown — pending snaps die with the world. */
	public static void resetAll() {
		PENDING.clear();
	}

	private static final class Pending {
		final long snapAtTick;
		final long endTick;
		boolean snapped;

		Pending(long snapAtTick, long endTick) {
			this.snapAtTick = snapAtTick;
			this.endTick = endTick;
		}
	}
}
