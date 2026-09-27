package io.github.grebeshok105.codex.mechanic.charge;

import io.github.grebeshok105.codex.core.lifecycle.LifecycleRegistrar;
import io.github.grebeshok105.codex.core.lifecycle.OwnedSessionMap;
import io.github.grebeshok105.codex.core.lifecycle.OwnedSessionMap.ClearOn;
import net.minecraft.server.level.ServerPlayer;

import java.util.EnumSet;
import java.util.UUID;

/**
 * «Charge → release» sessions for charge-type abilities (Kamehameha, Spirit Bomb, the
 * Rasengan line, ...). Each ability owns a {@link Track} configured with its own
 * charge/release timings; the track keeps per-player state in an {@link OwnedSessionMap}
 * (entries drop on leave/death and on server stop) and advances it once per tick from
 * the ability's {@code ctx.ticks()} task via {@link Track#tick}.
 */
public final class ChargeSession {
	private ChargeSession() {
	}

	/** Per-ability session track: {@code chargeTicks} of charge, then {@code releaseTicks} of release. */
	public static <S> Track<S> track(int chargeTicks, int releaseTicks) {
		return new Track<>(chargeTicks, releaseTicks);
	}

	@FunctionalInterface
	public interface Body<S> {
		/**
		 * Runs once per tick while the session lives. Return {@code true} to keep the
		 * session open, {@code false} to drop it immediately (a hit, a detach, ...).
		 */
		boolean onTick(ServerPlayer player, Progress<S> progress);
	}

	/** Per-player progress view handed to the tick body — phase math lives here. */
	public static final class Progress<S> {
		private final S payload;
		private final int chargeTicks;
		private final int totalTicks;
		private int elapsed;

		private Progress(S payload, int chargeTicks, int totalTicks) {
			this.payload = payload;
			this.chargeTicks = chargeTicks;
			this.totalTicks = totalTicks;
		}

		/** Ability-owned session payload. */
		public S payload() {
			return payload;
		}

		/** Ticks elapsed since activation (0 on the first tick). */
		public int phase() {
			return elapsed;
		}

		/** Whether the session is still in its charge phase. */
		public boolean charging() {
			return elapsed < chargeTicks;
		}

		/** Ticks into the release phase (0 on the first release tick). */
		public int releasePhase() {
			return elapsed - chargeTicks;
		}

		/** Whether this tick is the session's last (it is dropped afterwards). */
		public boolean lastTick() {
			return elapsed >= totalTicks - 1;
		}
	}

	/** One ability's charge-session driver; timings are fixed per track. */
	public static final class Track<S> {
		private final OwnedSessionMap<UUID, Progress<S>> sessions =
				OwnedSessionMap.create(LifecycleRegistrar.global(), EnumSet.of(ClearOn.LEAVE, ClearOn.DEATH));
		private final int chargeTicks;
		private final int totalTicks;

		private Track(int chargeTicks, int releaseTicks) {
			this.chargeTicks = chargeTicks;
			this.totalTicks = chargeTicks + releaseTicks;
		}

		/** Opens a session for the player carrying {@code payload}. */
		public void begin(ServerPlayer player, S payload) {
			sessions.put(player.getUUID(), player.getUUID(),
					new Progress<>(payload, chargeTicks, totalTicks));
		}

		/** Whether the player has a live session — the {@code canActivate} gate. */
		public boolean active(ServerPlayer player) {
			return sessions.containsKey(player.getUUID());
		}

		/** Drops a live session without releasing (manual cancel; leave/death are automatic). */
		public void cancel(ServerPlayer player) {
			sessions.remove(player.getUUID());
		}

		/**
		 * Advances the player's session by one tick: runs {@code body}, then either drops
		 * the session (body said {@code false} or the last tick elapsed) or keeps it.
		 */
		public void tick(ServerPlayer player, Body<S> body) {
			Progress<S> progress = sessions.get(player.getUUID());
			if (progress == null) {
				return;
			}
			if (!body.onTick(player, progress)) {
				sessions.remove(player.getUUID());
				return;
			}
			progress.elapsed++;
			if (progress.elapsed >= totalTicks) {
				sessions.remove(player.getUUID());
			}
		}
	}
}
