package io.github.grebeshok105.codex.client.hero.homelander.emf;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.client.ClientFlightState;
import io.github.grebeshok105.codex.client.ClientSessionState;
import io.github.grebeshok105.codex.client.core.camera.ThirdPersonFraming;
import io.github.grebeshok105.codex.client.core.emf.EmfPresentationOwnership;
import io.github.grebeshok105.codex.client.core.flight.DirectionalPoseSource;
import io.github.grebeshok105.codex.client.core.vfx.params.VfxParams;
import io.github.grebeshok105.codex.client.core.vfx.params.VfxParamsLoader;
import io.github.grebeshok105.codex.mechanic.flight.FlightMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

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
 * flight-activation edge: from 0 when the player was on the ground the tick
 * before activation, or past the crouch dip
 * ({@link HomelanderPoseMath#TAKEOFF_AIRBORNE_START_SECONDS}) when flight
 * activated airborne (§7 stage 3).
 *
 * <p>A {@code CLAP} event resets the clap clip clock and plays it once over
 * 1.92 s while {@code clap_w} and the master {@code hl_w} ease in/out (the jem
 * blends it over the hover/boost group, so it works in flight too);
 * {@code CLAP_CANCEL} releases the weights early (§7 stage 7).
 */
final class HomelanderPoseState {

	private static final int MAX_ENTITIES = 64;
	private static final float TICK_SECONDS = 1f / 20f;
	/** {@code vfx/homelander/flight.json} — carries the {@code emfBoost*} keys (§7 stage 4). */
	private static final ResourceLocation FLIGHT_PARAMS = ModId.of("homelander/flight");
	/** Authored HAND CLAP length (windup + hold). */
	private static final float CLAP_SECONDS = 1.92f;
	/** Same ease-in/out as the master weight, so ramp-in and release look symmetric. */
	private static final float CLAP_HALF_LIFE_TICKS = 3f;

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
		/** Synced {@code onGround} read on the previous tick — see {@link #advance}. */
		boolean grounded;
		float active;
		float activePrev;
		float boostWeight;
		float boostWeightPrev;
		float takeoffWeight;
		float takeoffWeightPrev;
		float clapWeight;
		float milkWeight;
		boolean clapPlaying;
		/** Signed forward/strafe speeds (b/t) projected on body yaw from the smoothed velocity. */
		float forward;
		float strafe;
		private double lastX;
		private double lastY;
		private double lastZ;
		private boolean hasLastPos;
		/** Per-tick position delta smoothed with half-life 3 ticks (b/t). */
		private float velX;
		private float velY;
		private float velZ;
		private boolean boostEngaged;

		/**
		 * One client tick of state. The hover clock is never reset (§7 stage 2).
		 * {@code groundedNow} is this tick's synced {@code onGround}; the
		 * activation edge consumes the flag stored from the previous tick, not the
		 * current one: by the time {@link #tick} sees the flight state (end of the
		 * client tick) the client-side TAKEOFF lift has already flipped
		 * {@code onGround} for the local player
		 * ({@code LocalPlayerFlightMixin} applies the min-lift inside
		 * {@code travel} during the entity tick), so the current tick cannot tell
		 * a ground takeoff from an airborne one. Remote entities get
		 * {@code onGround} synced by the move/teleport packets, so the
		 * previous-tick flag is the correct activation-time value on both paths.
		 */
		void advance(boolean flying, boolean groundedNow) {
			boolean activation = flying && !this.flying;
			this.flying = flying;
			activePrev = active;
			active = HomelanderPoseMath.activeWeight(active, flying || clapPlaying, 1f);
			takeoffWeightPrev = takeoffWeight;
			boostWeightPrev = boostWeight;
			if (activation) {
				takeoff.reset(grounded ? 0f : HomelanderPoseMath.TAKEOFF_AIRBORNE_START_SECONDS);
			}
			this.grounded = groundedNow;
			hover.advance(TICK_SECONDS);
			if (!takeoff.finished(HomelanderPoseMath.TAKEOFF_LENGTH_SECONDS)) {
				takeoff.advance(TICK_SECONDS);
			}
			boost.advance(TICK_SECONDS);
			if (clapPlaying) {
				clap.advance(TICK_SECONDS);
				if (clap.finished(CLAP_SECONDS)) {
					clapPlaying = false;
				}
			}
			milk.advance(TICK_SECONDS);
			takeoffWeight = HomelanderPoseMath.takeoffWeight(takeoffWeight, flying, takeoff.time(), 1f);
			clapWeight = HomelanderPoseMath.approach(clapWeight, clapPlaying ? 1f : 0f, CLAP_HALF_LIFE_TICKS, 1f);
		}

		/**
		 * One client tick including the render-velocity update (§7 stage 4):
		 * the raw position delta is smoothed with half-life 3 ticks — remote
		 * players' server-lerped positions jitter the same way and the
		 * smoothing plus hysteresis absorbs it — then projected onto the
		 * body-yaw forward/right axes into {@link #forward}/{@link #strafe}.
		 * The BOOST latch and weight (half-life 4 ticks) follow the signed
		 * forward speed only.
		 */
		void advance(boolean flying, double x, double y, double z, float bodyYawDeg,
				boolean supersonic, float boostEnter, float boostExit, boolean groundedNow) {
			float rawX = hasLastPos ? (float) (x - lastX) : 0f;
			float rawY = hasLastPos ? (float) (y - lastY) : 0f;
			float rawZ = hasLastPos ? (float) (z - lastZ) : 0f;
			lastX = x;
			lastY = y;
			lastZ = z;
			hasLastPos = true;
			velX = HomelanderPoseMath.smoothedVelocity(velX, rawX, 1f);
			velY = HomelanderPoseMath.smoothedVelocity(velY, rawY, 1f);
			velZ = HomelanderPoseMath.smoothedVelocity(velZ, rawZ, 1f);
			forward = HomelanderPoseMath.forwardComponent(velX, velZ, bodyYawDeg);
			strafe = HomelanderPoseMath.strafeComponent(velX, velZ, bodyYawDeg);
			boostEngaged = HomelanderPoseMath.boostEngaged(
					boostEngaged, forward, supersonic, boostEnter, boostExit);
			advance(flying, groundedNow);
			boostWeight = HomelanderPoseMath.boostWeight(
					boostWeight, flying && boostEngaged, 1f);
		}

		/** Master weight interpolated to the render partial tick. */
		float weight(float partial) {
			return activePrev + (active - activePrev) * partial;
		}

		/** TAKEOFF weight interpolated to the render partial tick. */
		float takeoffWeight(float partial) {
			return takeoffWeightPrev + (takeoffWeight - takeoffWeightPrev) * partial;
		}

		/** BOOST weight interpolated to the render partial tick. */
		float boostWeight(float partial) {
			return boostWeightPrev + (boostWeight - boostWeightPrev) * partial;
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
		VfxParams params = VfxParamsLoader.get(FLIGHT_PARAMS);
		float boostEnter = params.number("emfBoostEnter", HomelanderPoseMath.DEFAULT_BOOST_ENTER);
		float boostExit = params.number("emfBoostExit", HomelanderPoseMath.DEFAULT_BOOST_EXIT);
		List<AbstractClientPlayer> players = level.players();
		for (int i = 0; i < players.size(); i++) {
			AbstractClientPlayer player = players.get(i);
			if (!EmfPresentationOwnership.isOwned(player)) {
				continue;
			}
			Entry entry = STATES.get(player.getUUID());
			if (entry == null) {
				entry = new Entry();
				// seed the support flag so a takeoff already active on the first
				// observed tick still resolves the pre-activation ground state
				entry.grounded = player.onGround();
				STATES.put(player.getUUID(), entry);
			}
			// onGround is authoritative for the local player and synced for remote
			// ones (ClientboundMoveEntityPacket/ClientboundTeleportEntityPacket
			// carry it); the entry keeps the previous tick's reading for the edge
			ClientFlightState.State state = ClientFlightState.get(player.getId());
			Vec3 pos = player.position();
			entry.advance(state != null, pos.x, pos.y, pos.z, player.yBodyRot,
					state != null && state.mode() == FlightMode.SUPERSONIC,
					boostEnter, boostExit, player.onGround());
		}
	}

	/** {@code CLAP}: restart the authored clip. Not gated on ownership — a late skin resolve still lands. */
	static void startClap(UUID uuid) {
		Entry entry = STATES.computeIfAbsent(uuid, key -> new Entry());
		entry.clap.reset();
		entry.clapPlaying = true;
	}

	/** {@code CLAP_CANCEL}: the hit was dropped server-side; release the weights early. */
	static void cancelClap(UUID uuid) {
		Entry entry = STATES.get(uuid);
		if (entry != null) {
			entry.clapPlaying = false;
		}
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
		return entry == null ? 0f : entry.boostWeight(partial);
	}

	/**
	 * Directional flight inputs for {@code FlightPoseMath.directional} (§7
	 * stage 5), served to {@code FlightPoseTracker} through
	 * {@link DirectionalPoseSource}: the same smoothed render velocity that
	 * drives the boost latch — projected forward/strafe components plus the
	 * vertical component — and the smoothed boost weight. {@code false}
	 * while the entity has no entry (e.g. the tracker ticks before the pose
	 * state on the first flying tick), so the caller falls back to the
	 * legacy target instead of reading zeros.
	 */
	static boolean fillDirectional(UUID uuid, DirectionalPoseSource.Input out) {
		Entry entry = entryOf(uuid);
		if (entry == null) {
			return false;
		}
		out.forward = entry.forward;
		out.strafe = entry.strafe;
		out.vertical = entry.velY;
		out.boostWeight = entry.boostWeight;
		return true;
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

	/**
	 * Third-person framing provider for {@link ThirdPersonFraming} (§7
	 * stage 6): the world-space offset {@code weight * (bodyCentre -
	 * eyePos)} that shifts the detached camera base from the eye toward
	 * the body centre while the presentation is engaged — equivalent to
	 * {@code lerp(weight, eyePos, bodyCentre)}. {@code null} while the
	 * entity is not an owned player, has no entry, or the master weight
	 * is 0, so standing third-person keeps the vanilla framing exactly.
	 */
	@Nullable
	static Vec3 cameraOffset(Entity entity) {
		if (!(entity instanceof AbstractClientPlayer player)
				|| !EmfPresentationOwnership.isOwned(player)) {
			return null;
		}
		Entry entry = entryOf(player.getUUID());
		if (entry == null) {
			return null;
		}
		float weight = entry.weight(1f);
		if (weight <= 0f) {
			return null;
		}
		float rootTy = VfxParamsLoader.get(FLIGHT_PARAMS)
				.number("hoverRootTy", HomelanderPoseMath.DEFAULT_HOVER_ROOT_TY);
		Vec3 bodyCentre = player.position()
				.add(0.0, HomelanderPoseMath.bodyCentreHeight(rootTy), 0.0);
		return bodyCentre.subtract(player.getEyePosition(1f)).scale(weight);
	}

	static void clearAll() {
		STATES.clear();
		lastUuid = null;
		lastEntry = null;
	}
}
