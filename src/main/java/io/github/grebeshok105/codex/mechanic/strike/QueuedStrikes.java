package io.github.grebeshok105.codex.mechanic.strike;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player queue of delayed strikes: a module queues a {@link StrikeSession} with a
 * game-time deadline and callbacks, and {@link #serverTick} drains it on the server tick,
 * resolving the owner via {@code server.getPlayerList()}. Entries drop when the owner is
 * offline, when {@link Handlers#stillValid} rejects them (hero/item checks), or once the
 * strike impacts. No lifecycle wiring needed — the drain is self-cleaning.
 *
 * <p>Extracted from the Heavens Strike / Musou Isshin pending loops: both kept the same
 * {@code Map<UUID, Pending>} + iterator drain shape, differing only in the per-tick body.
 */
public final class QueuedStrikes<S extends StrikeSession> {
	public interface Handlers<S extends StrikeSession> {
		/** Extra drop conditions beyond the owner being online (hero check, held item). */
		default boolean stillValid(ServerPlayer owner, S session) {
			return true;
		}

		/** Runs every drained tick, before the impact check (caster lock, freeze refresh). */
		default void tick(ServerPlayer owner, S session, long now) {
		}

		/** Runs only while the strike is still pending ({@code now < impactTick}). */
		default void windup(ServerPlayer owner, S session, long now) {
		}

		/** Runs once when {@code now >= impactTick}; the entry is removed right after. */
		void impact(ServerPlayer owner, S session);
	}

	private final Map<UUID, S> pending = new ConcurrentHashMap<>();
	private final Handlers<S> handlers;

	public QueuedStrikes(Handlers<S> handlers) {
		this.handlers = handlers;
	}

	public boolean isCharging(ServerPlayer player) {
		return pending.containsKey(player.getUUID());
	}

	/** Queues a strike for the player; returns false when one is already pending. */
	public boolean queue(ServerPlayer player, S session) {
		if (pending.containsKey(player.getUUID())) {
			return false;
		}
		pending.put(player.getUUID(), session);
		return true;
	}

	public void cancel(UUID playerId) {
		pending.remove(playerId);
	}

	public void serverTick(MinecraftServer server) {
		if (pending.isEmpty()) return;
		Iterator<Map.Entry<UUID, S>> it = pending.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, S> e = it.next();
			ServerPlayer owner = server.getPlayerList().getPlayer(e.getKey());
			S session = e.getValue();
			if (owner == null || !handlers.stillValid(owner, session)) {
				it.remove();
				continue;
			}
			long now = owner.serverLevel().getGameTime();
			handlers.tick(owner, session, now);
			if (now >= session.impactTick()) {
				handlers.impact(owner, session);
				it.remove();
			} else {
				handlers.windup(owner, session, now);
			}
		}
	}
}
