package io.github.grebeshok105.codex.hero.thanos.runtime;

import io.github.grebeshok105.codex.core.lifecycle.LifecycleRegistrar;
import io.github.grebeshok105.codex.core.lifecycle.OwnedSessionMap;
import io.github.grebeshok105.codex.core.lifecycle.OwnedSessionMap.ClearOn;
import net.minecraft.server.level.ServerPlayer;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.server.MinecraftServer;

public final class ThanosSnapWindupController {
	// Leave/death drop the pending snap — a snap must never fire from a corpse or after relog
	// (audit B17).
	private static final OwnedSessionMap<UUID, Pending> PENDING =
			OwnedSessionMap.create(LifecycleRegistrar.global(), EnumSet.of(ClearOn.LEAVE, ClearOn.DEATH));

	private ThanosSnapWindupController() {
	}


	public static void schedule(ServerPlayer player, int snapInTicks, int totalTicks, Consumer<ServerPlayer> onSnap) {
		long now = player.serverLevel().getGameTime();
		PENDING.put(player.getUUID(), player.getUUID(), new Pending(now + snapInTicks, now + totalTicks, onSnap));
	}

	public static boolean isWindingUp(ServerPlayer player) {
		Pending p = PENDING.get(player.getUUID());
		return p != null && !p.snapped;
	}

	private static final class Pending {
		final long snapAtTick;
		final long endTick;
		final Consumer<ServerPlayer> onSnap;
		boolean snapped;

		Pending(long snapAtTick, long endTick, Consumer<ServerPlayer> onSnap) {
			this.snapAtTick = snapAtTick;
			this.endTick = endTick;
			this.onSnap = onSnap;
		}
	}

	public static void serverTick(MinecraftServer server) {
			if (PENDING.size() == 0) return;
			Iterator<Map.Entry<UUID, Pending>> it = PENDING.iterator();
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
					p.onSnap.accept(player);
					p.snapped = true;
				}
				if (now >= p.endTick) {
					it.remove();
				}
			}
			}

}
