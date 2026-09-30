package io.github.grebeshok105.codex.client.hero.homelander.emf;

import io.github.grebeshok105.codex.client.ClientFlightState;
import io.github.grebeshok105.codex.client.ClientSessionState;
import io.github.grebeshok105.codex.client.core.emf.EmfPresentationOwnership;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player state feeding the {@code superheroes_hl_*} EMF variables: five
 * clip clocks plus the authored-motion weights. Bounded (64 entries, LRU) and
 * zero-allocation on the read path — suppliers look the entity up by uuid and
 * copy floats out, with the last-resolved (uuid → entry) pair cached because
 * EMF evaluates every variable of one entity in a row.
 *
 * <p>State advances once per client tick ({@link #tick}); reads interpolate by
 * the render partial tick, so the sampled values track wall time independent
 * of frame rate (§4.5 of the plan). The hover clock is free-running and never
 * resets — HOVER loops, so blending it in mid-phase is continuous, and flight
 * on/off flips cannot snap the pose. The takeoff clock restarts on every
 * flight-activation edge: from 0 when the player was on the ground, or past
 * the crouch dip ({@link HomelanderPoseMath#TAKEOFF_AIRBORNE_START_SECONDS})
 * when flight activated airborne (§7 stage 3).
 */
final class HomelanderPoseState {

	private static final int MAX_ENTITIES = 64;
	private static final float TICK_SECONDS = 1f / 20f;
	/** Depth below the feet probed for support when {@code onGround} is untrusted. */
	private static final float SUPPORT_PROBE_BLOCKS = 0.05f;

	private static final Map<UUID, Entry> STATES = new LinkedHashMap<>(16, 0.75f, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<UUID, Entry> eldest) {
			if (size() > MAX_ENTITIES) {
				if (eldest.getKey().equals(lastUuid)) {
					lastUuid = null;
					lastEntry = null;
				}
				return true;
			}
			return false;
		}
	};

	private static UUID lastUuid;
	private static Entry lastEntry;

	static {
		ClientSessionState.register(HomelanderPoseState::clearAll);
	}

	private HomelanderPoseState() {
	}

	static final class Entry {
		final HomelanderPoseMath.ClipClock hover = new HomelanderPoseMath.ClipClock();
		final HomelanderPoseMath.ClipClock takeoff = new HomelanderPoseMath.ClipClock();
		final HomelanderPoseMath.ClipClock boost = new HomelanderPoseMath.ClipClock();
		final HomelanderPoseMath.ClipClock clap = new HomelanderPoseMath.ClipClock();
		final HomelanderPoseMath.ClipClock milk = new HomelanderPoseMath.ClipClock();
		boolean flying;
		float active;
		float activePrev;
		float boostWeight;
		float takeoffWeight;
		float takeoffWeightPrev;
		float clapWeight;
		float milkWeight;
		float forward;
		float strafe;
		float pitchDeg;
		float bankDeg;

		/**
		 * One client tick of state. The hover clock is never reset (§7 stage 2);
		 * {@code grounded} is the support probe evaluated by {@link #tick} and is
		 * only consulted on the flight-activation edge (flying on the previous
		 * tick means there is no new takeoff to start).
		 */
		void advance(boolean flying, boolean grounded) {
			boolean activation = flying && !this.flying;
			this.flying = flying;
			activePrev = active;
			active = HomelanderPoseMath.activeWeight(active, flying, 1f);
			takeoffWeightPrev = takeoffWeight;
			if (activation) {
				takeoff.reset(grounded ? 0f : HomelanderPoseMath.TAKEOFF_AIRBORNE_START_SECONDS);
			}
			hover.advance(TICK_SECONDS);
			takeoff.advance(TICK_SECONDS);
			boost.advance(TICK_SECONDS);
			clap.advance(TICK_SECONDS);
			milk.advance(TICK_SECONDS);
			takeoffWeight = HomelanderPoseMath.takeoffWeight(takeoffWeight, flying, takeoff.time(), 1f);
		}

		/** Master weight interpolated to the render partial tick. */
		float weight(float partial) {
			return activePrev + (active - activePrev) * partial;
		}

		/** TAKEOFF weight interpolated to the render partial tick. */
		float takeoffWeight(float partial) {
			return takeoffWeightPrev + (takeoffWeight - takeoffWeightPrev) * partial;
		}

		/** HOVER loop-local time, interpolated forward by the partial tick. */
		float hoverTime(float partial) {
			return HomelanderPoseMath.loopTime(
					hover.time() + partial * TICK_SECONDS, HomelanderPoseMath.HOVER_LENGTH_SECONDS);
		}
	}

	static void tick(Minecraft client) {
		ClientLevel level = client.level;
		if (level == null) {
			if (!STATES.isEmpty()) {
				clearAll();
			}
			return;
		}
		List<AbstractClientPlayer> players = level.players();
		for (int i = 0; i < players.size(); i++) {
			AbstractClientPlayer player = players.get(i);
			if (!EmfPresentationOwnership.isOwned(player)) {
				continue;
			}
			Entry entry = STATES.computeIfAbsent(player.getUUID(), uuid -> new Entry());
			boolean flying = ClientFlightState.get(player.getId()) != null;
			// the support probe only fires on the activation edge — once per
			// takeoff, never per frame
			boolean grounded = flying && !entry.flying && isSupportedNow(player);
			entry.advance(flying, grounded);
		}
	}

	/**
	 * Whether the player reads as standing on ground right now.
	 * {@code ClientFlightState} carries no {@code onGround} bit, and vanilla
	 * never updates {@code Entity.onGround} for remote players on the client
	 * (remote entities lerp their position without running collision
	 * {@code move()}, and the flag is not synced entity data — it stays
	 * {@code false} for them forever). The local player's flag is authoritative;
	 * for remote players we fall back to a collision probe just below the feet.
	 */
	private static boolean isSupportedNow(AbstractClientPlayer player) {
		if (player.onGround()) {
			return true;
		}
		return !player.level().noCollision(
				player, player.getBoundingBox().expandTowards(0.0, -SUPPORT_PROBE_BLOCKS, 0.0));
	}

	private static Entry entryOf(UUID uuid) {
		if (uuid != null && uuid.equals(lastUuid)) {
			return lastEntry;
		}
		Entry entry = STATES.get(uuid);
		if (entry != null) {
			lastUuid = uuid;
			lastEntry = entry;
		}
		return entry;
	}

	/** Master presentation weight (0 = vanilla pose). */
	static float weight(UUID uuid, float partial) {
		Entry entry = entryOf(uuid);
		return entry == null ? 0f : entry.weight(partial);
	}

	/** Clip-local times, interpolated forward by the render partial tick. */
	static float hoverTime(UUID uuid, float partial) {
		Entry entry = entryOf(uuid);
		return entry == null ? 0f : entry.hoverTime(partial);
	}

	static float takeoffTime(UUID uuid, float partial) {
		Entry entry = entryOf(uuid);
		return entry == null ? 0f : entry.takeoff.time() + partial * TICK_SECONDS;
	}

	static float boostTime(UUID uuid, float partial) {
		Entry entry = entryOf(uuid);
		return entry == null ? 0f : entry.boost.time() + partial * TICK_SECONDS;
	}

	static float clapTime(UUID uuid, float partial) {
		Entry entry = entryOf(uuid);
		return entry == null ? 0f : entry.clap.time() + partial * TICK_SECONDS;
	}

	static float milkTime(UUID uuid, float partial) {
		Entry entry = entryOf(uuid);
		return entry == null ? 0f : entry.milk.time() + partial * TICK_SECONDS;
	}

	static float boostWeight(UUID uuid, float partial) {
		Entry entry = entryOf(uuid);
		return entry == null ? 0f : entry.boostWeight;
	}

	static float takeoffWeight(UUID uuid, float partial) {
		Entry entry = entryOf(uuid);
		return entry == null ? 0f : entry.takeoffWeight(partial);
	}

	static float clapWeight(UUID uuid, float partial) {
		Entry entry = entryOf(uuid);
		return entry == null ? 0f : entry.clapWeight;
	}

	static float milkWeight(UUID uuid, float partial) {
		Entry entry = entryOf(uuid);
		return entry == null ? 0f : entry.milkWeight;
	}

	static void clearAll() {
		STATES.clear();
		lastUuid = null;
		lastEntry = null;
	}
}
