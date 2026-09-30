package io.github.grebeshok105.codex.hero.homelander.runtime;

import io.github.grebeshok105.codex.core.lifecycle.LifecycleRegistrar;
import io.github.grebeshok105.codex.core.lifecycle.OwnedSessionMap;
import io.github.grebeshok105.codex.core.lifecycle.OwnedSessionMap.ClearOn;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-side windup for {@code HandClapAbility}: activation schedules the
 * cone impact at the EMF {@code hand_clap} clip's contact frame instead of
 * dealing damage at t=0. Same rule as {@link ThanosSnapWindupController} —
 * leave, death or hero-clear drops the pending clap, so a cancelled swing
 * never deals damage from a corpse or after relog.
 */
public final class HandClapWindupController {
	private static final OwnedSessionMap<UUID, Pending> PENDING =
			OwnedSessionMap.create(LifecycleRegistrar.global(), EnumSet.of(
					ClearOn.LEAVE, ClearOn.DEATH, ClearOn.HERO_CLEAR));

	private HandClapWindupController() {
	}

	public static void schedule(ServerPlayer player, int contactInTicks, Consumer<ServerPlayer> onContact) {
		long contactAtTick = player.serverLevel().getGameTime() + contactInTicks;
		PENDING.put(player.getUUID(), player.getUUID(), new Pending(contactAtTick, onContact));
	}

	public static boolean isWindingUp(ServerPlayer player) {
		return PENDING.containsKey(player.getUUID());
	}

	private record Pending(long contactAtTick, Consumer<ServerPlayer> onContact) {
	}

	public static void serverTick(MinecraftServer server) {
		if (PENDING.size() == 0) {
			return;
		}
		Iterator<Map.Entry<UUID, Pending>> it = PENDING.iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, Pending> e = it.next();
			ServerPlayer player = server.getPlayerList().getPlayer(e.getKey());
			if (player == null || player.isDeadOrDying()) {
				it.remove();
				continue;
			}
			if (player.serverLevel().getGameTime() >= e.getValue().contactAtTick()) {
				it.remove();
				e.getValue().onContact().accept(player);
			}
		}
	}
}
