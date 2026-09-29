package io.github.grebeshok105.codex.client.hero.homelander.fx;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.client.ClientSessionState;
import io.github.grebeshok105.codex.client.core.anim.PlayerAnimator;
import io.github.grebeshok105.codex.client.core.flight.FlightBodyTransform;
import io.github.grebeshok105.codex.client.core.flight.FlightPoseTracker;
import io.github.grebeshok105.codex.client.core.vfx.VfxEffect;
import io.github.grebeshok105.codex.client.core.vfx.VfxRenderContext;
import io.github.grebeshok105.codex.client.core.vfx.VfxSpawn;
import io.github.grebeshok105.codex.client.core.vfx.anchor.HumanoidAnchors;
import io.github.grebeshok105.codex.client.core.vfx.backend.LightHandle;
import io.github.grebeshok105.codex.client.core.vfx.backend.VfxBackend;
import io.github.grebeshok105.codex.client.core.vfx.backend.VfxBackends;
import io.github.grebeshok105.codex.client.core.vfx.params.VfxParams;
import io.github.grebeshok105.codex.client.core.vfx.params.VfxParamsLoader;
import io.github.grebeshok105.codex.client.core.vfx.pattern.CameraImpulse;
import io.github.grebeshok105.codex.client.core.vfx.pattern.ImpactPattern;
import io.github.grebeshok105.codex.client.core.vfx.pattern.ShockwavePattern;
import io.github.grebeshok105.codex.sound.HomelanderSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Homelander's Iron Fists presentation ({@code superheroes:homelander/
 * iron_fists_*}). The server re-sends ON every aura interval while the ability
 * is active so late-tracking observers still get the effect — {@link
 * #ACTIVE_AURAS} dedupes those resends into one running aura per source entity.
 * ON: a 200-tick aura emitting the hand emitter at both arm anchors (arm
 * pivots rotated ~10px down), the ACTION {@code iron_fists_activate} clip, a
 * one-shot {@code homelander.iron_fists.activate} and an entity-bound {@code
 * homelander.iron_fists.charge} loop. OFF: ends the aura early. HIT: a
 * {@link ShockwavePattern} ring whose radius is the event's {@code scale}
 * (the server sends {@code IronFistsController.SHOCKWAVE_RADIUS}), an {@link
 * ImpactPattern} burst, a proximity-scaled {@link CameraImpulse}, the ACTION
 * {@code iron_fists_strike} clip and {@code homelander.iron_fists.impact}.
 * Tuning: {@code vfx/homelander/iron_fists.json}.
 */
public final class IronFistsFx {
	private static final ResourceLocation PARAMS = ModId.of("homelander/iron_fists");
	private static final ResourceLocation HAND_EMITTER = ModId.of("homelander_iron_fists_hand");
	private static final ResourceLocation IMPACT_EMITTER = ModId.of("homelander_iron_fists_impact");
	private static final ResourceLocation CLIP_ACTIVATE = ModId.of("homelander/iron_fists_activate");
	private static final ResourceLocation CLIP_STRIKE = ModId.of("homelander/iron_fists_strike");
	private static final Vec3 UP = new Vec3(0, 1, 0);
	private static final int CLIP_FADE_TICKS = 4;

	/** Running auras by source entity id — dedupes ON resends; OFF cancels. */
	private static final Map<Integer, HandAuraFx> ACTIVE_AURAS = new ConcurrentHashMap<>();

	static {
		ClientSessionState.register(ACTIVE_AURAS::clear);
	}

	private IronFistsFx() {
	}

	public static VfxEffect activate(VfxSpawn spawn) {
		Entity source = spawn.source();
		if (source != null && ACTIVE_AURAS.containsKey(source.getId())) {
			return null;
		}
		HandAuraFx aura = new HandAuraFx(spawn);
		if (source != null) {
			ACTIVE_AURAS.put(source.getId(), aura);
		}
		return aura;
	}

	public static VfxEffect deactivate(VfxSpawn spawn) {
		Entity source = spawn.source();
		if (source != null) {
			HandAuraFx aura = ACTIVE_AURAS.remove(source.getId());
			if (aura != null) {
				aura.cancel();
			}
		}
		return new DoneFx();
	}

	public static VfxEffect hit(VfxSpawn spawn) {
		return new HitFx(spawn);
	}

	private static VfxParams params(VfxSpawn spawn) {
		VfxParams p = VfxParamsLoader.get(PARAMS);
		return p == VfxParams.EMPTY ? spawn.params() : p;
	}

	private static final class HandAuraFx implements VfxEffect {
		private final @Nullable Entity entity;
		private final int entityId;
		private final Vec3 fallback;
		private final int durationTicks;
		private final int emitIntervalTicks;
		private final float handLateral;
		private final float handUp;
		private final float handForward;
		private final int lightRgb;
		private final float lightRadius;
		private final float lightBrightness;

		private int age;
		private @Nullable LightHandle light;
		private @Nullable ChargeLoopSound loop;
		private boolean finished;

		private HandAuraFx(VfxSpawn spawn) {
			this.entity = spawn.source();
			this.entityId = entity != null ? entity.getId() : -1;
			this.fallback = spawn.origin();
			VfxParams p = params(spawn);
			this.durationTicks = Math.max(1, (int) p.number("durationTicks", 200f));
			this.emitIntervalTicks = Math.max(1, (int) p.number("emitIntervalTicks", 4f));
			this.handLateral = p.number("handLateral", 0.34f);
			this.handUp = p.number("handUp", 1.0f);
			this.handForward = p.number("handForward", 0.15f);
			this.lightRgb = p.colors().containsKey("lightColor")
					? p.color("lightColor", 0) & 0xFFFFFF : -1;
			this.lightRadius = p.number("lightRadius", 5f);
			this.lightBrightness = p.number("lightBrightness", 0.8f);

			float activateVolume = p.number("activateVolume", 1f);
			float activatePitch = p.number("activatePitch", 1f);
			if (entity != null) {
				PlayerAnimator.play(entityId, CLIP_ACTIVATE,
						PlayerAnimator.Layer.ACTION, CLIP_FADE_TICKS);
				Minecraft.getInstance().getSoundManager().play(new EntityBoundSoundInstance(
						HomelanderSounds.IRON_FISTS_ACTIVATE, SoundSource.PLAYERS,
						activateVolume, activatePitch, entity, entity.getRandom().nextLong()));
				this.loop = new ChargeLoopSound(entity,
						p.number("chargeVolume", 0.8f), p.number("chargePitch", 1f));
				Minecraft.getInstance().getSoundManager().play(loop);
			} else {
				Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(
						HomelanderSounds.IRON_FISTS_ACTIVATE, SoundSource.PLAYERS,
						activateVolume, activatePitch, RandomSource.create(),
						fallback.x, fallback.y, fallback.z));
			}
		}

		@Override
		public void tick() {
			if (finished) {
				return;
			}
			age++;
			Vec3 feet = entity != null ? (entity.isRemoved() ? null : entity.position())
					: fallback;
			if (feet == null) {
				finish();
				return;
			}
			float bodyYaw = entity instanceof LivingEntity living
					? living.yBodyRot : (entity != null ? entity.getYRot() : 0f);
			FlightBodyTransform tilt = entityId >= 0
					? FlightPoseTracker.transform(entityId, 1f) : FlightBodyTransform.IDENTITY;
			VfxBackend backend = VfxBackends.current();
			if (age % emitIntervalTicks == 0) {
				backend.emit(HAND_EMITTER, HumanoidAnchors.tiltedPoint(
						feet, bodyYaw, handLateral, handUp, handForward, tilt));
				backend.emit(HAND_EMITTER, HumanoidAnchors.tiltedPoint(
						feet, bodyYaw, -handLateral, handUp, handForward, tilt));
			}
			if (lightRgb >= 0) {
				Vec3 center = HumanoidAnchors.tiltedPoint(
						feet, bodyYaw, 0, handUp, handForward, tilt);
				if (light == null) {
					light = backend.light(center, lightRgb, lightRadius, lightBrightness);
				} else {
					light.move(center);
				}
			}
			if (age >= durationTicks) {
				finish();
			}
		}

		@Override
		public void render(VfxRenderContext ctx) {
			// Emitters and the light render themselves through the backend.
		}

		@Override
		public boolean done() {
			return finished;
		}

		@Override
		public void cancel() {
			finish();
		}

		private void finish() {
			finished = true;
			if (entityId >= 0) {
				ACTIVE_AURAS.remove(entityId, this);
			}
			if (light != null) {
				light.remove();
				light = null;
			}
			if (loop != null) {
				Minecraft.getInstance().getSoundManager().stop(loop);
				loop = null;
			}
		}
	}

	private static final class HitFx implements VfxEffect {
		private final ShockwavePattern ring;

		private HitFx(VfxSpawn spawn) {
			Vec3 center = spawn.origin();
			VfxParams p = params(spawn);
			// The send site packs SHOCKWAVE_RADIUS into scale; ringRadius multiplies it.
			float scale = Math.max(0.1f, spawn.scale());
			this.ring = new ShockwavePattern(center,
					p.number("ringRadius", 1f) * scale,
					Math.max(1, (int) p.number("ringTicks", 10f)),
					p.color("ringColor", 0x80FFE07A),
					p.number("ringBand", 0.18f));
			ImpactPattern.spawn(VfxBackends.current(), center, UP, IMPACT_EMITTER, p);

			float shakeRadius = p.number("shakeRadius", 12f);
			LocalPlayer self = Minecraft.getInstance().player;
			if (self != null && shakeRadius > 0f) {
				double dist = self.position().distanceTo(center);
				if (dist < shakeRadius) {
					float proximity = 1f - (float) (dist / shakeRadius);
					CameraImpulse.shake(p.number("shakeIntensity", 0.8f) * proximity,
							Math.max(1, (int) p.number("shakeTicks", 10f)));
				}
			}

			Entity source = spawn.source();
			if (source != null) {
				PlayerAnimator.play(source.getId(), CLIP_STRIKE,
						PlayerAnimator.Layer.ACTION, CLIP_FADE_TICKS);
			}
			Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(
					HomelanderSounds.IRON_FISTS_IMPACT, SoundSource.PLAYERS,
					p.number("impactVolume", 1f), p.number("impactPitch", 1f),
					RandomSource.create(), center.x, center.y, center.z));
		}

		@Override
		public void tick() {
			ring.tick();
		}

		@Override
		public void render(VfxRenderContext ctx) {
			ring.render(ctx);
		}

		@Override
		public boolean done() {
			return ring.done();
		}

		@Override
		public void cancel() {
			ring.cancel();
		}
	}

	/**
	 * Entity-bound looping {@code homelander.iron_fists.charge} under the aura;
	 * stops when the entity leaves or the aura ends.
	 */
	private static final class ChargeLoopSound extends AbstractTickableSoundInstance {
		private final Entity entity;

		private ChargeLoopSound(Entity entity, float volume, float pitch) {
			super(HomelanderSounds.IRON_FISTS_CHARGE, SoundSource.PLAYERS, RandomSource.create());
			this.entity = entity;
			this.looping = true;
			this.delay = 0;
			this.attenuation = SoundInstance.Attenuation.LINEAR;
			this.volume = volume;
			this.pitch = pitch;
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

	/** Ends after one tick — the effect's real work happened in the factory. */
	private static final class DoneFx implements VfxEffect {
		private boolean done;

		@Override
		public void tick() {
			done = true;
		}

		@Override
		public void render(VfxRenderContext ctx) {
		}

		@Override
		public boolean done() {
			return done;
		}
	}
}
