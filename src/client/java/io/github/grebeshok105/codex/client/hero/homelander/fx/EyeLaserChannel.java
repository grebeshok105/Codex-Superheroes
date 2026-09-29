package io.github.grebeshok105.codex.client.hero.homelander.fx;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.client.core.anim.PlayerAnimator;
import io.github.grebeshok105.codex.client.core.anim.PoseSample;
import io.github.grebeshok105.codex.client.core.flight.FlightBodyTransform;
import io.github.grebeshok105.codex.client.core.flight.FlightPoseTracker;
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
import io.github.grebeshok105.codex.hero.homelander.ability.EyeLaserPhases;
import io.github.grebeshok105.codex.hero.homelander.effect.HomelanderEffects;
import io.github.grebeshok105.codex.sound.HomelanderSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
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

/**
 * Homelander's eye-laser channel effect ({@code superheroes:homelander/laser}).
 * Two beams leave {@link HumanoidAnchors#eyes} and converge on one end: for
 * the local player the end is their own per-frame raycast (instant aim
 * response), for remote casters the server-sent end lerped over the UPDATE
 * cadence. A {@link PhaseTimeline} (6-tick charge → hold → 8-tick release)
 * scales the draw, and the ACTION lane runs
 * {@code laser_charge → laser_hold → laser_release} with the matching
 * entity-bound sounds. Tuning lives in {@code vfx/homelander/laser.json}.
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
		PlayerAnimator.play(source.getId(), CLIP_CHARGE, PlayerAnimator.Layer.ACTION, CLIP_FADE_TICKS);
		playBound(HomelanderSounds.LASER_CHARGE, params.number("chargeVolume", 0.9f),
				params.number("chargePitch", 1f));
	}

	@Override
	public void tick() {
		age++;
		retargetAge++;
		PhaseTimeline.Phase phase = timeline.phaseAt(age, releasedAtAge);
		if (phase == PhaseTimeline.Phase.DONE) {
			stopLoop();
			removeLight();
			PlayerAnimator.stop(source.getId(), PlayerAnimator.Layer.ACTION,
					0, CLIP_CHARGE, CLIP_HOLD);
			return;
		}
		if (phase == PhaseTimeline.Phase.HOLD && !holdStarted) {
			holdStarted = true;
			PlayerAnimator.play(source.getId(), CLIP_HOLD, PlayerAnimator.Layer.ACTION, CLIP_FADE_TICKS);
			loop = new LaserLoopSound(source, params.number("loopVolume", 0.7f),
					params.number("loopPitch", 1f));
			Minecraft.getInstance().getSoundManager().play(loop);
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
		// The WRAP hold loop must be stopped or it keeps looping beside RELEASE.
		PlayerAnimator.stop(source.getId(), PlayerAnimator.Layer.ACTION,
				CLIP_FADE_TICKS, CLIP_HOLD);
		PlayerAnimator.play(source.getId(), CLIP_RELEASE, PlayerAnimator.Layer.ACTION, CLIP_FADE_TICKS);
		playBound(HomelanderSounds.LASER_RELEASE, params.number("releaseVolume", 0.9f),
				params.number("releasePitch", 1f));
		stopLoop();
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
			FlightBodyTransform tilt = FlightPoseTracker.transform(source.getId(), partial);
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
		PlayerAnimator.stop(source.getId(), PlayerAnimator.Layer.ACTION,
				0, CLIP_CHARGE, CLIP_HOLD, CLIP_RELEASE);
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
		PoseSample sample = PlayerAnimator.sample(entityId, partial);
		Vector3f head = sample.rotationDeg().get("head");
		// Sampled rotations are pre-divided by the merged fade weight; scaling
		// back by weight() restores the effective per-bone degrees.
		return head == null ? new Vector3f() : new Vector3f(head).mul(sample.weight());
	}

	private void playBound(SoundEvent sound, float volume, float pitch) {
		Minecraft.getInstance().getSoundManager().play(new EntityBoundSoundInstance(
				sound, SoundSource.PLAYERS, volume, pitch, source, source.getRandom().nextLong()));
	}

	private void stopLoop() {
		if (loop != null) {
			Minecraft.getInstance().getSoundManager().stop(loop);
			loop = null;
		}
	}

	private void removeLight() {
		if (light != null) {
			light.remove();
			light = null;
		}
	}

	/** Entity-bound looping hum while the beam is held; stops with the entity. */
	private static final class LaserLoopSound extends AbstractTickableSoundInstance {
		private final Entity entity;

		private LaserLoopSound(Entity entity, float volume, float pitch) {
			super(HomelanderSounds.LASER_LOOP, SoundSource.PLAYERS, entity.getRandom());
			this.entity = entity;
			this.looping = true;
			this.delay = 0;
			this.volume = volume;
			this.pitch = pitch;
			this.attenuation = SoundInstance.Attenuation.LINEAR;
			syncPosition();
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
