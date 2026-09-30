package io.github.grebeshok105.codex.client.core.flight;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.client.ClientFlightState;
import io.github.grebeshok105.codex.client.ClientSessionState;
import io.github.grebeshok105.codex.client.core.anim.PlayerAnimator;
import io.github.grebeshok105.codex.client.core.emf.EmfPresentationOwnership;
import io.github.grebeshok105.codex.client.core.render.SkinResolver;
import io.github.grebeshok105.codex.client.core.vfx.VfxRuntime;
import io.github.grebeshok105.codex.client.core.vfx.VfxSpawn;
import io.github.grebeshok105.codex.client.core.vfx.params.VfxParams;
import io.github.grebeshok105.codex.client.core.vfx.params.VfxParamsLoader;
import io.github.grebeshok105.codex.mechanic.flight.FlightPhase;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-tick driver for continuous flight presentation. For every rendered
 * player whose hero opted in via {@code HeroClientContext.flightPresentation}
 * it (a) steps a smoothed {@link FlightBodyTransform} toward the phase target
 * — {@link #transform} feeds the renderer mixin and the body anchors; (b)
 * crossfades phase clips on {@code PlayerAnimator.Layer.BASE} and fires
 * takeoff/land one-shots on ACTION; (c) runs the looping flight sound whose
 * volume follows {@code horizontalSpeed}; (d) spawns the presentation's
 * trail/boost effects. Velocity comes from the position delta between ticks,
 * which works for remote players too (their positions are server-lerped).
 *
 * <p>Tuning comes from {@code vfx/flight/pose.json} through
 * {@link VfxParamsLoader} — see {@link FlightPoseMath} for the keys plus
 * {@code halfLifeTicks}, {@code loopRefSpeed}, {@code loopVolumeMax}.
 * Entries are dropped — BASE clip faded out, loop sound stopped — when the
 * entity leaves flight, renderable range, or its presentation, so departed
 * players never leave a stale WRAP lane in {@code PlayerAnimator}.
 */
public final class FlightPoseTracker {
	private static final ResourceLocation POSE_PARAMS = ModId.of("flight/pose");
	/** BASE lane crossfade length, per the pilot contract. */
	private static final int CROSSFADE_TICKS = 4;
	/** Fade-in for the ACTION-layer takeoff/land one-shots. */
	private static final int ACTION_FADE_TICKS = 2;

	private static final Map<Integer, Tracked> TRACKED = new ConcurrentHashMap<>();

	static {
		ClientSessionState.register(FlightPoseTracker::reset);
	}

	private FlightPoseTracker() {
	}

	/** Wires the per-tick update; called once from the client bootstrap. */
	public static void init() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
	}

	/**
	 * Interpolated pose for {@code entityId} at {@code partial} ticks —
	 * {@link FlightBodyTransform#IDENTITY} for untracked entities.
	 */
	public static FlightBodyTransform transform(int entityId, float partial) {
		Tracked tracked = TRACKED.get(entityId);
		if (tracked == null) {
			return FlightBodyTransform.IDENTITY;
		}
		return new FlightBodyTransform(
				Mth.lerp(partial, tracked.previous.pitchDeg(), tracked.current.pitchDeg()),
				Mth.lerp(partial, tracked.previous.rollDeg(), tracked.current.rollDeg()));
	}

	public static void tick() {
		Minecraft client = Minecraft.getInstance();
		ClientLevel level = client.level;
		if (level == null) {
			return;
		}
		VfxParams pose = VfxParamsLoader.get(POSE_PARAMS);
		float halfLife = pose.number("halfLifeTicks", FlightPoseMath.DEFAULT_HALF_LIFE_TICKS);
		Set<Integer> alive = new HashSet<>();
		for (Entity entity : level.entitiesForRendering()) {
			if (!(entity instanceof AbstractClientPlayer player)) {
				continue;
			}
			ClientFlightState.State state = ClientFlightState.get(entity.getId());
			if (state == null) {
				continue;
			}
			FlightPresentation presentation =
					FlightPresentations.of(SkinResolver.heroIdFor(player)).orElse(null);
			if (presentation == null) {
				continue;
			}
			alive.add(entity.getId());
			ClientFlightState.markPresentationOwned(entity.getId());
			track(client, player, state, presentation, pose, halfLife);
		}
		for (Iterator<Map.Entry<Integer, Tracked>> it = TRACKED.entrySet().iterator(); it.hasNext();) {
			Map.Entry<Integer, Tracked> entry = it.next();
			if (!alive.contains(entry.getKey())) {
				release(client, entry.getKey(), entry.getValue());
				it.remove();
			}
		}
	}

	private static void track(Minecraft client, AbstractClientPlayer player,
			ClientFlightState.State state, FlightPresentation presentation,
			VfxParams pose, float halfLife) {
		Tracked tracked = TRACKED.computeIfAbsent(player.getId(), id -> new Tracked());
		Vec3 pos = player.position();
		Vec3 velocity = tracked.lastPos != null ? pos.subtract(tracked.lastPos) : Vec3.ZERO;
		float yawRate = tracked.lastPos != null
				? Mth.wrapDegrees(player.getYRot() - tracked.lastYaw) : 0f;
		if (velocity.lengthSqr() > FlightPoseMath.TELEPORT_MIN_DELTA_SQ) {
			// Dimension change / teleport: the delta is relocation, not motion —
			// keep it out of the pose target and the yaw-rate roll.
			velocity = Vec3.ZERO;
			yawRate = 0f;
		}
		tracked.lastPos = pos;
		tracked.lastYaw = player.getYRot();

		FlightPhase phase = state.phase();
		if (phase != tracked.phase) {
			onPhaseChange(client, player, tracked, phase, presentation);
			tracked.phase = phase;
			tracked.trailSpawned = false;
		}

		boolean owned = EmfPresentationOwnership.isOwned(player);
		VfxParams targetParams = pose;
		if (presentation.poseParams() != null && owned) {
			targetParams = mergedPoseParams(tracked, pose, presentation.poseParams());
		}
		tracked.previous = tracked.current;
		tracked.current = FlightPoseMath.step(tracked.current,
				poseTarget(player, phase, velocity, yawRate, targetParams, owned, tracked),
				1f, halfLife);

		updateLoopSound(client, player, tracked, state, phase, presentation, pose);

		if ((phase == FlightPhase.CRUISE || phase == FlightPhase.BOOST)
				&& presentation.trailEffect() != null && !tracked.trailSpawned) {
			tracked.trailSpawned = true;
			spawnEffect(player, presentation.trailEffect());
		}
	}

	/**
	 * {@code pose.json} overlaid with the presentation's {@code poseParams}
	 * (e.g. authored {@code emfBoost*} keys) for EMF-owned players. Memoized
	 * on the tracked entry: both inputs are loader-stable instances between
	 * resource reloads, so the merge allocates only when tuning reloads.
	 */
	private static VfxParams mergedPoseParams(Tracked tracked, VfxParams pose,
			ResourceLocation overlayId) {
		VfxParams overlay = VfxParamsLoader.get(overlayId);
		if (tracked.poseMergeResult == null || tracked.poseMergeBase != pose
				|| tracked.poseMergeOverlay != overlay) {
			tracked.poseMergeResult = pose.withOverrides(overlay);
			tracked.poseMergeBase = pose;
			tracked.poseMergeOverlay = overlay;
		}
		return tracked.poseMergeResult;
	}

	/**
	 * Pose target for the tick (plan §7 stage 5): EMF-owned players whose
	 * presentation exposes live directional inputs through
	 * {@link DirectionalPoseSource} — its own smoothed render velocity plus
	 * boost weight — take the continuous {@link FlightPoseMath#directional}
	 * path, which handles forward/backward/strafe/vertical flight without a
	 * phase switch. Everybody else, and owned players before their first
	 * pose-state tick, keeps the phase-switched {@link FlightPoseMath#target}
	 * unchanged.
	 */
	private static FlightBodyTransform poseTarget(AbstractClientPlayer player,
			FlightPhase phase, Vec3 velocity, float yawRate, VfxParams params,
			boolean owned, Tracked tracked) {
		if (owned && DirectionalPoseSource.fill(player, tracked.directional)) {
			DirectionalPoseSource.Input in = tracked.directional;
			return FlightPoseMath.directional(in.forward, in.strafe, in.vertical,
					yawRate, in.boostWeight, params);
		}
		return FlightPoseMath.target(phase, velocity, yawRate, params);
	}

	private static void onPhaseChange(Minecraft client, AbstractClientPlayer player,
			Tracked tracked, FlightPhase phase, FlightPresentation presentation) {
		ResourceLocation baseClip = switch (phase) {
			case TAKEOFF, HOVER -> presentation.hoverClip();
			case CRUISE -> presentation.cruiseClip();
			case BOOST -> presentation.boostClip();
			default -> null;
		};
		if (!Objects.equals(baseClip, tracked.baseClip)) {
			if (tracked.baseClip != null) {
				PlayerAnimator.stop(player.getId(), PlayerAnimator.Layer.BASE,
						CROSSFADE_TICKS, tracked.baseClip);
			}
			if (baseClip != null) {
				PlayerAnimator.play(player.getId(), baseClip, PlayerAnimator.Layer.BASE, CROSSFADE_TICKS);
			}
			tracked.baseClip = baseClip;
		}
		int entityId = player.getId();
		switch (phase) {
			case TAKEOFF -> {
				if (presentation.takeoffClip() != null) {
					PlayerAnimator.play(entityId, presentation.takeoffClip(),
							PlayerAnimator.Layer.ACTION, ACTION_FADE_TICKS);
				}
				playOneShot(client, player, presentation.takeoffSound());
			}
			case BOOST -> {
				playOneShot(client, player, presentation.boostSound());
				if (presentation.boostEffect() != null) {
					spawnEffect(player, presentation.boostEffect());
				}
			}
			case LANDING -> {
				if (presentation.landClip() != null) {
					PlayerAnimator.play(entityId, presentation.landClip(),
							PlayerAnimator.Layer.ACTION, ACTION_FADE_TICKS);
				}
				playOneShot(client, player, presentation.landSound());
			}
			default -> {
			}
		}
	}

	private static void updateLoopSound(Minecraft client, AbstractClientPlayer player,
			Tracked tracked, ClientFlightState.State state, FlightPhase phase,
			FlightPresentation presentation, VfxParams pose) {
		boolean airborne = phase == FlightPhase.TAKEOFF || phase == FlightPhase.HOVER
				|| phase == FlightPhase.CRUISE || phase == FlightPhase.BOOST;
		if (!airborne) {
			stopLoop(client, tracked);
			return;
		}
		SoundManager sounds = client.getSoundManager();
		if (tracked.loop == null || tracked.loop.isStopped() || !sounds.isActive(tracked.loop)) {
			tracked.loop = new FlightLoopSound(presentation.loopSound(), player);
			sounds.play(tracked.loop);
		}
		float ref = Math.max(0.01f, pose.number("loopRefSpeed", 1.0f));
		float volume = Math.min(1f, Math.max(0f, state.horizontalSpeed()) / ref)
				* pose.number("loopVolumeMax", 0.85f);
		tracked.loop.setVolume(volume);
	}

	private static void playOneShot(Minecraft client, AbstractClientPlayer player, SoundEvent sound) {
		client.getSoundManager().play(new EntityBoundSoundInstance(sound, SoundSource.PLAYERS,
				1f, 1f, player, player.getRandom().nextLong()));
	}

	private static void spawnEffect(AbstractClientPlayer player, ResourceLocation effect) {
		VfxRuntime.spawn(new VfxSpawn(effect, player, player.getId(),
				player.position(), player.position(),
				1f, player.getRandom().nextInt(), VfxParamsLoader.get(effect)));
	}

	private static void release(Minecraft client, int entityId, Tracked tracked) {
		ClientFlightState.unmarkPresentationOwned(entityId);
		if (tracked.baseClip != null) {
			PlayerAnimator.stop(entityId, PlayerAnimator.Layer.BASE,
					CROSSFADE_TICKS, tracked.baseClip);
		}
		stopLoop(client, tracked);
	}

	private static void stopLoop(Minecraft client, Tracked tracked) {
		if (tracked.loop != null) {
			client.getSoundManager().stop(tracked.loop);
			tracked.loop = null;
		}
	}

	/** Session reset (disconnect / world leave): drops every tracked pose. */
	public static void reset() {
		TRACKED.clear();
	}

	private static final class Tracked {
		private Vec3 lastPos;
		private float lastYaw;
		private FlightPhase phase;
		private FlightBodyTransform current = FlightBodyTransform.IDENTITY;
		private FlightBodyTransform previous = FlightBodyTransform.IDENTITY;
		private ResourceLocation baseClip;
		private boolean trailSpawned;
		private FlightLoopSound loop;
		private VfxParams poseMergeBase;
		private VfxParams poseMergeOverlay;
		private VfxParams poseMergeResult;
		private final DirectionalPoseSource.Input directional = new DirectionalPoseSource.Input();
	}

	/**
	 * Entity-bound looping flight sound: follows the player and exposes a
	 * mutable {@link #setVolume} so the tracker can tie loudness to
	 * {@code horizontalSpeed} every tick.
	 */
	private static final class FlightLoopSound extends AbstractTickableSoundInstance {
		private final Entity entity;

		private FlightLoopSound(SoundEvent sound, Entity entity) {
			super(sound, SoundSource.PLAYERS, entity.getRandom());
			this.entity = entity;
			this.looping = true;
			this.delay = 0;
			this.volume = 0f;
			this.attenuation = SoundInstance.Attenuation.LINEAR;
			syncPosition();
		}

		private void setVolume(float volume) {
			this.volume = volume;
		}

		@Override
		public void tick() {
			if (entity.isRemoved()) {
				stop();
				return;
			}
			syncPosition();
		}

		private void syncPosition() {
			this.x = entity.getX();
			this.y = entity.getY();
			this.z = entity.getZ();
		}
	}
}
