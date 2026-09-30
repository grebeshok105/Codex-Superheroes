package io.github.grebeshok105.codex.client.hero.homelander.fx;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.client.core.flight.FlightBodyTransform;
import io.github.grebeshok105.codex.client.core.render.BeamLook;
import io.github.grebeshok105.codex.client.core.vfx.VfxChannelEffect;
import io.github.grebeshok105.codex.client.core.vfx.VfxRenderContext;
import io.github.grebeshok105.codex.client.core.vfx.anchor.EyePair;
import io.github.grebeshok105.codex.client.core.vfx.anchor.HumanoidAnchors;
import io.github.grebeshok105.codex.client.core.vfx.backend.LightHandle;
import io.github.grebeshok105.codex.client.core.vfx.backend.VfxBackend;
import io.github.grebeshok105.codex.client.core.vfx.backend.VfxBackends;
import io.github.grebeshok105.codex.client.core.vfx.params.VfxParams;
import io.github.grebeshok105.codex.client.core.vfx.pattern.BeamPattern;
import io.github.grebeshok105.codex.client.core.vfx.pattern.ImpactPattern;
import io.github.grebeshok105.codex.client.core.vfx.pattern.PhaseTimeline;
import io.github.grebeshok105.codex.client.hero.homelander.flight.HomelanderPoseApi;
import io.github.grebeshok105.codex.hero.homelander.ability.EyeLaserPhases;
import io.github.grebeshok105.codex.hero.homelander.effect.HomelanderEffects;
import io.github.grebeshok105.codex.sound.HomelanderSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/**
 * Homelander's eye-laser channel effect ({@code superheroes:homelander/laser}).
 * Two beams leave {@link HumanoidAnchors#eyes} and converge on one end: for
 * the local player the end is their own per-frame raycast (instant aim
 * response), for remote casters the server-sent end lerped over the UPDATE
 * cadence. A {@link PhaseTimeline} (6-tick charge → hold → 8-tick release)
 * scales the draw, and the EMF action lane runs
 * {@code laser_charge → laser_hold → laser_release}.
 *
 * <p>Audio is a single entity-bound loop running for the whole channel —
 * charge, hold and release alike; there are no charge/release one-shots. On
 * release the loop ramps to silence across the release window and stops. The
 * handle is shared per caster entity in {@link #LOOPS}: a re-activation while
 * the previous loop is still fading adopts that handle and ramps it back up,
 * so two loops can never overlap. Tuning lives in
 * {@code vfx/homelander/laser.json}.
 */
public final class EyeLaserChannel implements VfxChannelEffect {
	private static final ResourceLocation IMPACT_EMITTER = ModId.of("homelander_laser_impact");
	private static final ResourceLocation CLIP_CHARGE = ModId.of("homelander/laser_charge");
	private static final ResourceLocation CLIP_HOLD = ModId.of("homelander/laser_hold");
	private static final ResourceLocation CLIP_RELEASE = ModId.of("homelander/laser_release");
	private static final double RANGE = 64.0;
	private static final double CHEST_FRACTION = 0.7;
	/** Remote-end catch-up window — the server's UPDATE cadence (2 ticks). */
	private static final float RETARGET_TICKS = 2f;
	private static final int IMPACT_INTERVAL_TICKS = 3;
	private static final int CLIP_FADE_TICKS = 4;
	/** Ticks for a fresh (or adopted) loop to ramp up to {@code loopVolume}. */
	private static final int LOOP_FADE_IN_TICKS = 3;
	/**
	 * One shared loop handle per caster entity id — the only live loop that
	 * entity ever has. Entries remove themselves when the sound dies, so a
	 * re-START during another channel's fade-out reuses the fading handle and
	 * can never stack a second loop.
	 */
	private static final Map<Integer, LaserLoopSound> LOOPS = new HashMap<>();

	private final Entity source;
	private final @Nullable AbstractClientPlayer player;
	private final boolean local;
	private final VfxParams params;
	private final PhaseTimeline timeline =
			new PhaseTimeline(EyeLaserPhases.CHARGE_TICKS, EyeLaserPhases.RELEASE_TICKS);
	private final BeamLook look;
	private final BeamLook madnessLook;
	private final int lightColor;
	private final float lightRadius;
	private final float lightBrightness;

	private Vec3 previousEnd;
	private Vec3 serverEnd;
	private int retargetAge;
	private int age;
	private int releasedAtAge = -1;
	private boolean holdStarted;
	private @Nullable LaserLoopSound loop;
	private @Nullable LightHandle light;

	public EyeLaserChannel(Entity source, Vec3 target, VfxParams params) {
		this.source = source;
		this.player = source instanceof AbstractClientPlayer p ? p : null;
		this.local = Minecraft.getInstance().player == source;
		this.params = params;
		this.serverEnd = target;
		this.previousEnd = target;
		this.look = new BeamLook(
				params.number("coreWidth", 0.05f),
				params.number("glowWidth", 0.18f),
				params.color("coreColor", 0xFFFFF3E8),
				params.color("glowColor", 0x8CFF3C14),
				params.number("noise", 0.35f));
		float madnessMul = params.number("madnessWidthMul", 2.4f);
		this.madnessLook = new BeamLook(
				look.coreWidth() * madnessMul, look.glowWidth() * madnessMul,
				look.coreArgb(), look.glowArgb(), look.noise());
		this.lightColor = params.color("lightColor", 0xFF6A3C) & 0xFFFFFF;
		this.lightRadius = params.number("lightRadius", 7f);
		this.lightBrightness = params.number("lightBrightness", 0.85f);
		HomelanderPoseApi.playClip(source.getId(), CLIP_CHARGE);
		openLoop();
	}

	@Override
	public void tick() {
		age++;
		retargetAge++;
		PhaseTimeline.Phase phase = timeline.phaseAt(age, releasedAtAge);
		if (phase == PhaseTimeline.Phase.DONE) {
			stopLoop();
			removeLight();
			HomelanderPoseApi.stopClips(source.getId(), CLIP_CHARGE, CLIP_HOLD);
			return;
		}
		if (phase == PhaseTimeline.Phase.HOLD && !holdStarted) {
			holdStarted = true;
			HomelanderPoseApi.playClip(source.getId(), CLIP_HOLD);
		}
		float intensity = timeline.intensity(age, releasedAtAge, 0f);
		if (age % IMPACT_INTERVAL_TICKS == 0 && intensity > 0.05f) {
			Vec3 end = endAt(0f);
			VfxBackend backend = VfxBackends.current();
			ImpactPattern.spawn(backend, end, impactNormal(end), IMPACT_EMITTER, params);
			if (light == null) {
				light = backend.light(end, lightColor, lightRadius, lightBrightness * intensity);
			} else {
				light.move(end);
			}
		}
	}

	@Override
	public void retarget(Vec3 target) {
		previousEnd = endAt(0f);
		serverEnd = target;
		retargetAge = 0;
	}

	@Override
	public void release() {
		if (releasedAtAge >= 0) {
			return;
		}
		releasedAtAge = age;
		// The WRAP hold clip must be stopped or it keeps looping beside RELEASE.
		HomelanderPoseApi.stopClip(source.getId(), CLIP_HOLD);
		HomelanderPoseApi.playClip(source.getId(), CLIP_RELEASE);
		// The loop fades out across the release window and stops itself; it
		// stays registered in LOOPS until then, so a quick re-activation adopts
		// it instead of starting a second one.
		if (loop != null) {
			loop.fadeOut(EyeLaserPhases.RELEASE_TICKS);
			loop = null;
		}
	}

	@Override
	public void render(VfxRenderContext ctx) {
		float partial = ctx.partialTick();
		float intensity = timeline.intensity(age, releasedAtAge, partial);
		if (intensity <= 0f) {
			return;
		}
		boolean madness = source instanceof LivingEntity living && HomelanderEffects.isMadness(living);
		BeamLook activeLook = madness ? madnessLook : look;
		float scaled = intensity * (madness ? params.number("madnessIntensity", 1.3f) : 1f);
		Vec3 end = endAt(partial);
		if (player != null) {
			FlightBodyTransform tilt = HomelanderPoseApi.currentBodyTransform(
					source.getId(), partial).tilt();
			EyePair eyes = HumanoidAnchors.eyes(player, partial, tilt, headAnim(source.getId(), partial));
			BeamPattern.draw(ctx, eyes.left(), end, activeLook, scaled);
			BeamPattern.draw(ctx, eyes.right(), end, activeLook, scaled);
		} else {
			BeamPattern.draw(ctx, source.getEyePosition(), end, activeLook, scaled);
		}
	}

	@Override
	public boolean done() {
		return timeline.phaseAt(age, releasedAtAge) == PhaseTimeline.Phase.DONE;
	}

	@Override
	public void cancel() {
		stopLoop();
		removeLight();
		HomelanderPoseApi.stopClips(source.getId(), CLIP_CHARGE, CLIP_HOLD, CLIP_RELEASE);
	}

	private Vec3 endAt(float partial) {
		if (local) {
			return localBeamEnd(partial);
		}
		float t = Math.min(1f, (retargetAge + partial) / RETARGET_TICKS);
		return previousEnd.lerp(serverEnd, t);
	}

	/** The local caster's own per-frame raycast — instant aim response. */
	private Vec3 localBeamEnd(float partial) {
		Minecraft client = Minecraft.getInstance();
		ClientLevel level = client.level;
		LocalPlayer localPlayer = client.player;
		if (localPlayer == null || level == null) {
			return serverEnd;
		}
		Vec3 eye = localPlayer.getEyePosition(partial);
		Vec3 dir = localPlayer.getViewVector(partial);
		Vec3 end = eye.add(dir.scale(RANGE));
		BlockHitResult blockHit = level.clip(new ClipContext(
				eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, localPlayer));
		Vec3 entitySearchEnd = blockHit.getType() == HitResult.Type.BLOCK ? blockHit.getLocation() : end;
		AABB box = localPlayer.getBoundingBox().expandTowards(dir.scale(RANGE)).inflate(1.0);
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(
				level, localPlayer, eye, entitySearchEnd, box,
				e -> e instanceof LivingEntity && e.isAlive() && e != localPlayer && !e.isSpectator());
		if (hit != null && hit.getEntity() instanceof LivingEntity target) {
			return new Vec3(target.getX(), target.getY() + target.getBbHeight() * CHEST_FRACTION,
					target.getZ());
		}
		return entitySearchEnd;
	}

	/** Beam direction reversed — the impact's outward surface normal. */
	private Vec3 impactNormal(Vec3 end) {
		Vec3 towardEye = source.getEyePosition().subtract(end);
		return towardEye.lengthSqr() < 1e-6 ? new Vec3(0, 1, 0) : towardEye.normalize();
	}

	private static Vector3f headAnim(int entityId, float partial) {
		return HomelanderPoseApi.currentHeadAnglesDeg(entityId);
	}

	/**
	 * Adopts the caster's still-live loop (e.g. a previous channel mid-fade) or
	 * starts a fresh one, then ramps it up to {@code loopVolume}. The shared
	 * {@link #LOOPS} handle is what guarantees a restart can never double up.
	 */
	private void openLoop() {
		float volume = params.number("loopVolume", 0.7f);
		LaserLoopSound existing = LOOPS.get(source.getId());
		if (existing != null && existing.isStopped()) {
			LOOPS.remove(source.getId());
			existing = null;
		}
		if (existing != null) {
			loop = existing;
		} else {
			loop = new LaserLoopSound(source, params.number("loopPitch", 1f));
			LOOPS.put(source.getId(), loop);
			Minecraft.getInstance().getSoundManager().play(loop);
		}
		loop.rampTo(volume, LOOP_FADE_IN_TICKS);
	}

	/** Hard stop — the abort path (eviction/session reset), not the release fade. */
	private void stopLoop() {
		if (loop != null) {
			Minecraft.getInstance().getSoundManager().stop(loop);
			LOOPS.remove(source.getId(), loop);
			loop = null;
		}
	}

	private void removeLight() {
		if (light != null) {
			light.remove();
			light = null;
		}
	}

	/**
	 * Entity-bound looping hum that runs for the whole channel. Volume ramps
	 * apply per tick: {@link #rampTo} fades a fresh (or adopted mid-fade)
	 * handle up, {@link #fadeOut} ramps to silence over the release window and
	 * then stops. A dead instance removes itself from {@link #LOOPS}, which is
	 * the only way a still-registered handle ever leaves the map.
	 */
	private static final class LaserLoopSound extends AbstractTickableSoundInstance {
		private final Entity entity;
		private int rampTicks = -1;
		private int rampTotal = 1;
		private float fadeFrom;
		private float fadeTarget;
		private boolean stopAfterRamp;

		private LaserLoopSound(Entity entity, float pitch) {
			super(HomelanderSounds.LASER_LOOP, SoundSource.PLAYERS, entity.getRandom());
			this.entity = entity;
			this.looping = true;
			this.delay = 0;
			this.volume = 0f;
			this.pitch = pitch;
			this.attenuation = SoundInstance.Attenuation.LINEAR;
			syncPosition();
		}

		/** Ramps volume toward {@code target} over {@code ticks}; cancels a pending fade-out. */
		private void rampTo(float target, int ticks) {
			this.fadeFrom = this.volume;
			this.fadeTarget = target;
			this.rampTotal = Math.max(1, ticks);
			this.rampTicks = 0;
			this.stopAfterRamp = false;
		}

		/** Ramps to silence over {@code ticks}, then stops for good. */
		private void fadeOut(int ticks) {
			rampTo(0f, ticks);
			this.stopAfterRamp = true;
		}

		@Override
		public void tick() {
			if (entity.isRemoved()) {
				stop();
				LOOPS.remove(entity.getId(), this);
				return;
			}
			syncPosition();
			if (rampTicks >= 0) {
				float t = Math.min(1f, ++rampTicks / (float) rampTotal);
				this.volume = fadeFrom + (fadeTarget - fadeFrom) * t;
				if (t >= 1f) {
					rampTicks = -1;
					if (stopAfterRamp) {
						stop();
						LOOPS.remove(entity.getId(), this);
					}
				}
			}
		}

		private void syncPosition() {
			this.x = entity.getX();
			this.y = entity.getY();
			this.z = entity.getZ();
		}
	}
}
