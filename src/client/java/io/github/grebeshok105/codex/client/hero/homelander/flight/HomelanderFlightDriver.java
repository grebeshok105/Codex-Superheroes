package io.github.grebeshok105.codex.client.hero.homelander.flight;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.client.ClientFlightState;
import io.github.grebeshok105.codex.client.ClientSessionState;
import io.github.grebeshok105.codex.client.core.flight.FlightPresentation;
import io.github.grebeshok105.codex.client.core.flight.FlightPresentations;
import io.github.grebeshok105.codex.client.core.flight.LandingSoundScale;
import io.github.grebeshok105.codex.client.core.flight.SpeedRingGate;
import io.github.grebeshok105.codex.client.core.render.SkinResolver;
import io.github.grebeshok105.codex.client.core.vfx.VfxRuntime;
import io.github.grebeshok105.codex.client.core.vfx.VfxSpawn;
import io.github.grebeshok105.codex.client.core.vfx.params.VfxParams;
import io.github.grebeshok105.codex.client.core.vfx.params.VfxParamsLoader;
import io.github.grebeshok105.codex.client.hero.homelander.emf.EmfPlaybackState;
import io.github.grebeshok105.codex.client.hero.homelander.emf.HomelanderEmfRuntime;
import io.github.grebeshok105.codex.client.hero.homelander.flight.HomelanderFlightMachine.Input;
import io.github.grebeshok105.codex.client.hero.homelander.flight.HomelanderFlightMachine.Phase;
import io.github.grebeshok105.codex.client.hero.homelander.flight.HomelanderFlightMachine.ServerPhase;
import io.github.grebeshok105.codex.mechanic.flight.FlightPhase;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-tick driver for Homelander's EMF flight presentation — the EMF-era
 * replacement for {@code core.flight.FlightPoseTracker}. For every rendered
 * Homelander player with a synced {@link ClientFlightState} it feeds the
 * {@link HomelanderFlightMachine} the server phase + forward speed, writes
 * clip weight targets into the entity's {@link HomelanderEmfRuntime} (the
 * expressions then blend {@code takeoff}/{@code hover}/{@code boost}), layers
 * the procedural lean, and runs the loop/one-shot sounds + trail/boost
 * effects from the registered {@link FlightPresentation}.
 *
 * <p>Velocity comes from the position delta between ticks (works for remote
 * players — positions are server-lerped). A clip set through
 * {@link HomelanderPoseApi#playClip} suppresses flight-loop weights so the
 * per-channel sum stays ≤ 1.
 */
public final class HomelanderFlightDriver {
	private static final ResourceLocation POSE_PARAMS = ModId.of("flight/pose");
	private static final String CLIP_TAKEOFF = "takeoff";
	private static final String CLIP_HOVER = "hover";
	private static final String CLIP_BOOST = "boost";

	private static final Map<Integer, Tracked> TRACKED = new ConcurrentHashMap<>();

	static {
		ClientSessionState.register(HomelanderFlightDriver::reset);
	}

	private HomelanderFlightDriver() {
	}

	/** END_CLIENT_TICK hook — registered by {@code HomelanderClientModule}. */
	public static void tick(Minecraft client) {
		ClientLevel level = client.level;
		if (level == null) {
			return;
		}
		VfxParams pose = VfxParamsLoader.get(POSE_PARAMS);
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
			if (presentation == null || !HomelanderEmfRuntime.available()) {
				continue;
			}
			alive.add(entity.getId());
			ClientFlightState.markPresentationOwned(entity.getId());
			track(client, player, state, presentation, pose);
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
			ClientFlightState.State state, FlightPresentation presentation, VfxParams pose) {
		Tracked tracked = TRACKED.computeIfAbsent(player.getId(), id -> new Tracked());
		HomelanderEmfRuntime runtime = HomelanderEmfRuntime.of(player);

		Vec3 pos = player.position();
		Vec3 velocity = tracked.lastPos != null ? pos.subtract(tracked.lastPos) : Vec3.ZERO;
		tracked.lastPos = pos;
		double yawRad = Math.toRadians(player.getYRot());
		double forwardSpeed = -velocity.x * Math.sin(yawRad) + velocity.z * Math.cos(yawRad);
		double strafeSpeed = -velocity.x * Math.cos(yawRad) - velocity.z * Math.sin(yawRad);
		double verticalSpeed = velocity.y;

		FlightPhase serverPhase = state.phase();
		if (serverPhase != tracked.serverPhase) {
			onPhaseChange(client, player, tracked, serverPhase, presentation, pose, velocity);
			tracked.serverPhase = serverPhase;
			tracked.trailSpawned = false;
		}
		Phase phase = tracked.machine.update(
				new Input(toServerPhase(serverPhase), forwardSpeed),
				runtime.playback().oneShotFinished(CLIP_TAKEOFF));
		if (phase == Phase.TAKEOFF && tracked.visualPhase != Phase.TAKEOFF) {
			runtime.playback().restart(CLIP_TAKEOFF);
		}
		tracked.visualPhase = phase;

		// Action clips played through HomelanderPoseApi suppress flight loops
		// so per-channel weights stay ≤ 1.
		EmfPlaybackState playback = runtime.playback();
		float suppress = 1f - maxActionWeight(playback);
		playback.setTargetWeight(CLIP_TAKEOFF, phase == Phase.TAKEOFF ? suppress : 0f);
		playback.setTargetWeight(CLIP_HOVER, phase == Phase.HOVER ? suppress : 0f);
		playback.setTargetWeight(CLIP_BOOST, phase == Phase.BOOST ? suppress : 0f);

		// Procedural lean: forward pitch, strafe bank, small vertical pitch.
		HomelanderFlightLean.Targets lean = tracked.lean.target(
				forwardSpeed, strafeSpeed, verticalSpeed, playback.weight(CLIP_BOOST));
		runtime.setLeanTargets(lean.pitchRad(), lean.rollRad(), lean.yPx());

		updateLoopSound(client, player, tracked, state, serverPhase, presentation, pose);

		if ((serverPhase == FlightPhase.CRUISE || serverPhase == FlightPhase.BOOST)
				&& presentation.trailEffect() != null && !tracked.trailSpawned) {
			tracked.trailSpawned = true;
			spawnEffect(player, presentation.trailEffect());
		}

		if ((serverPhase == FlightPhase.CRUISE || serverPhase == FlightPhase.BOOST)
				&& presentation.speedRingEffect() != null
				&& SpeedRingGate.shouldFire(tracked.prevVelocity, velocity, pose,
						tracked.lastSpeedRingTick, client.level.getGameTime())) {
			tracked.lastSpeedRingTick = client.level.getGameTime();
			spawnEffect(player, presentation.speedRingEffect(), velocity, 1f);
		}
		tracked.prevVelocity = velocity;
	}

	private static float maxActionWeight(EmfPlaybackState playback) {
		float max = 0f;
		for (String clip : HomelanderEmfRuntime.clipNames()) {
			if (!HomelanderPoseApi.FLIGHT_CLIPS.contains(clip)) {
				max = Math.max(max, playback.weight(clip));
			}
		}
		return max;
	}

	private static ServerPhase toServerPhase(FlightPhase phase) {
		return switch (phase) {
			case TAKEOFF -> ServerPhase.TAKEOFF;
			case HOVER, CRUISE -> ServerPhase.AIRBORNE;
			case BOOST -> ServerPhase.BOOST;
			case LANDING -> ServerPhase.LANDING;
			default -> ServerPhase.NONE;
		};
	}

	private static void onPhaseChange(Minecraft client, AbstractClientPlayer player,
			Tracked tracked, FlightPhase phase, FlightPresentation presentation,
			VfxParams pose, Vec3 velocity) {
		switch (phase) {
			case TAKEOFF -> playOneShot(client, player, presentation.takeoffSound());
			case BOOST -> {
				playOneShot(client, player, presentation.boostSound());
				if (presentation.boostEffect() != null) {
					spawnEffect(player, presentation.boostEffect(), velocity, 1f);
				}
			}
			case LANDING -> playLandingSound(client, player, presentation.landSound(),
					tracked.prevVelocity, pose);
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

	/**
	 * Landing thump scaled by approach speed: {@code prevVelocity} still
	 * carries the descent/dive vector because the touchdown tick's own
	 * position delta is already ~0. Keys in {@code flight/pose.json}:
	 * {@code landRefSpeed} (b/t mapped to full impact) plus the
	 * {@link LandingSoundScale} keys.
	 */
	private static void playLandingSound(Minecraft client, AbstractClientPlayer player,
			SoundEvent sound, Vec3 approachVelocity, VfxParams pose) {
		double impact = Math.max(Math.abs(approachVelocity.y),
				Math.hypot(approachVelocity.x, approachVelocity.z));
		LandingSoundScale scale = LandingSoundScale.of(pose);
		float factor = (float) LandingSoundScale.speedFactor(
				impact, pose.number("landRefSpeed", 1.2f));
		client.getSoundManager().play(new EntityBoundSoundInstance(sound, SoundSource.PLAYERS,
				scale.volume(factor), scale.pitch(factor), player, player.getRandom().nextLong()));
	}

	private static void spawnEffect(AbstractClientPlayer player, ResourceLocation effect) {
		spawnEffect(player, effect, player.position(), 1f);
	}

	private static void spawnEffect(AbstractClientPlayer player, ResourceLocation effect,
			Vec3 target, float scale) {
		VfxRuntime.spawn(new VfxSpawn(effect, player, player.getId(),
				player.position(), target,
				scale, player.getRandom().nextInt(), VfxParamsLoader.get(effect)));
	}

	private static void release(Minecraft client, int entityId, Tracked tracked) {
		ClientFlightState.unmarkPresentationOwned(entityId);
		HomelanderEmfRuntime runtime = HomelanderEmfRuntime.peek(entityId);
		if (runtime != null) {
			for (String clip : HomelanderEmfRuntime.clipNames()) {
				runtime.playback().setTargetWeight(clip, 0f);
			}
			runtime.setLeanTargets(0, 0, 0);
			HomelanderEmfRuntime.release(entityId);
		}
		stopLoop(client, tracked);
	}

	private static void stopLoop(Minecraft client, Tracked tracked) {
		if (tracked.loop != null) {
			client.getSoundManager().stop(tracked.loop);
			tracked.loop = null;
		}
	}

	/** Session reset (disconnect / world leave): drops every tracked player. */
	public static void reset() {
		TRACKED.clear();
		HomelanderEmfRuntime.clearAll();
	}

	private static final class Tracked {
		private Vec3 lastPos;
		private Vec3 prevVelocity = Vec3.ZERO;
		private FlightPhase serverPhase;
		private Phase visualPhase = Phase.IDLE;
		private final HomelanderFlightMachine machine = new HomelanderFlightMachine();
		private final HomelanderFlightLean lean = new HomelanderFlightLean(
				HomelanderFlightLean.Params.defaults());
		private boolean trailSpawned;
		private long lastSpeedRingTick = -1;
		private FlightLoopSound loop;
	}

	/**
	 * Entity-bound looping flight sound: follows the player and exposes a
	 * mutable {@link #setVolume} so the driver can tie loudness to
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
