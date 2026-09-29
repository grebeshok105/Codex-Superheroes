package io.github.grebeshok105.codex.client.hero.homelander.fx;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.client.core.anim.PlayerAnimator;
import io.github.grebeshok105.codex.client.core.vfx.VfxEffect;
import io.github.grebeshok105.codex.client.core.vfx.VfxRenderContext;
import io.github.grebeshok105.codex.client.core.vfx.VfxSpawn;
import io.github.grebeshok105.codex.client.core.vfx.backend.LightHandle;
import io.github.grebeshok105.codex.client.core.vfx.backend.VfxBackend;
import io.github.grebeshok105.codex.client.core.vfx.backend.VfxBackends;
import io.github.grebeshok105.codex.client.core.vfx.params.VfxParams;
import io.github.grebeshok105.codex.client.core.vfx.params.VfxParamsLoader;
import io.github.grebeshok105.codex.client.core.vfx.pattern.CameraImpulse;
import io.github.grebeshok105.codex.sound.HomelanderSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Homelander's madness-aftermath sun build-up ({@code superheroes:homelander/
 * sun_charge}): a ~200-tick aura around the player — a growing light, rising
 * embers, heat distortion, the BASE {@code sun_charge} animation clip and an
 * entity-bound {@code homelander.sun.charge} sound whose volume and pitch ramp
 * up toward the detonation. Local players inside {@code trembleRadius} get a
 * subtle camera tremble that intensifies with the charge. The event is sent
 * via {@code eventAround} (no source entity), so the effect binds to the
 * nearest player to the origin at spawn time and falls back to the fixed
 * origin when nobody is tracked. Tuning lives in
 * {@code vfx/homelander/sun_charge.json}.
 */
public final class SunChargeFx {
	private static final ResourceLocation PARAMS = ModId.of("homelander/sun_charge");
	private static final ResourceLocation EMBER_EMITTER = ModId.of("homelander_sun_ember");
	private static final ResourceLocation CLIP_SUN_CHARGE = ModId.of("homelander/sun_charge");
	private static final double BIND_RADIUS = 3.0;
	private static final int CLIP_FADE_TICKS = 4;

	private SunChargeFx() {
	}

	public static VfxEffect create(VfxSpawn spawn) {
		return new ChargeFx(spawn);
	}

	private static @Nullable AbstractClientPlayer bindTo(Vec3 origin) {
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null) {
			return null;
		}
		AbstractClientPlayer best = null;
		double bestDist = BIND_RADIUS * BIND_RADIUS;
		for (AbstractClientPlayer player : level.getEntitiesOfClass(
				AbstractClientPlayer.class, AABB.ofSize(origin, BIND_RADIUS * 2, BIND_RADIUS * 2, BIND_RADIUS * 2))) {
			double dist = player.position().distanceToSqr(origin);
			if (dist < bestDist) {
				bestDist = dist;
				best = player;
			}
		}
		return best;
	}

	private static final class ChargeFx implements VfxEffect {
		private final @Nullable AbstractClientPlayer entity;
		private final Vec3 fallback;
		private final int durationTicks;
		private final int emitIntervalTicks;
		private final int distortionIntervalTicks;
		private final int lightRgb;
		private final float lightRadiusStart;
		private final float lightRadiusEnd;
		private final float lightBrightnessStart;
		private final float lightBrightnessEnd;
		private final float distortionRadius;
		private final float distortionStrength;
		private final float trembleRadius;
		private final float trembleMaxIntensity;
		private final int trembleIntervalTicks;

		private int age;
		private @Nullable LightHandle light;
		private @Nullable ChargeSound sound;
		private boolean finished;

		private ChargeFx(VfxSpawn spawn) {
			this.entity = bindTo(spawn.origin());
			this.fallback = spawn.origin();
			VfxParams p = params(spawn);
			this.durationTicks = Math.max(1, (int) p.number("durationTicks", 200f));
			this.emitIntervalTicks = Math.max(1, (int) p.number("emitIntervalTicks", 4f));
			this.distortionIntervalTicks = Math.max(1, (int) p.number("distortionIntervalTicks", 6f));
			this.lightRgb = p.color("lightColor", 0xFFB020) & 0xFFFFFF;
			this.lightRadiusStart = p.number("lightRadiusStart", 4f);
			this.lightRadiusEnd = p.number("lightRadiusEnd", 22f);
			this.lightBrightnessStart = p.number("lightBrightnessStart", 0.4f);
			this.lightBrightnessEnd = p.number("lightBrightnessEnd", 1.6f);
			this.distortionRadius = p.number("distortionRadius", 6f);
			this.distortionStrength = p.number("distortionStrength", 0.5f);
			this.trembleRadius = p.number("trembleRadius", 32f);
			this.trembleMaxIntensity = p.number("trembleMaxIntensity", 0.25f);
			this.trembleIntervalTicks = Math.max(1, (int) p.number("trembleIntervalTicks", 4f));
			if (entity != null) {
				PlayerAnimator.play(entity.getId(), CLIP_SUN_CHARGE,
						PlayerAnimator.Layer.BASE, CLIP_FADE_TICKS);
			}
			this.sound = new ChargeSound(entity, fallback, durationTicks,
					p.number("chargeVolume", 0.9f),
					p.number("chargePitch", 0.9f),
					p.number("chargePitchRise", 0.5f));
			Minecraft.getInstance().getSoundManager().play(sound);
		}

		private static VfxParams params(VfxSpawn spawn) {
			VfxParams p = VfxParamsLoader.get(PARAMS);
			return p == VfxParams.EMPTY ? spawn.params() : p;
		}

		private Vec3 center() {
			return entity != null && !entity.isRemoved()
					? entity.position().add(0, entity.getBbHeight() * 0.5, 0)
					: fallback;
		}

		@Override
		public void tick() {
			if (finished) {
				return;
			}
			age++;
			float t = Math.min(1f, (float) age / durationTicks);
			Vec3 center = center();
			VfxBackend backend = VfxBackends.current();
			if (age % emitIntervalTicks == 0) {
				backend.emit(EMBER_EMITTER, center);
			}
			if (age % distortionIntervalTicks == 0) {
				backend.distortion(center, distortionRadius * (0.4f + 0.6f * t),
						distortionStrength * t);
			}
			float radius = lightRadiusStart + (lightRadiusEnd - lightRadiusStart) * t;
			float brightness = lightBrightnessStart + (lightBrightnessEnd - lightBrightnessStart) * t;
			if (light == null) {
				light = backend.light(center, lightRgb, radius, brightness);
			} else {
				light.move(center);
				light.set(lightRgb, radius, brightness);
			}
			LocalPlayer self = Minecraft.getInstance().player;
			if (self != null && age % trembleIntervalTicks == 0) {
				double dist = self.position().distanceTo(center);
				if (dist < trembleRadius) {
					float proximity = 1f - (float) (dist / trembleRadius);
					CameraImpulse.shake(trembleMaxIntensity * t * proximity,
							trembleIntervalTicks + 1);
				}
			}
			if (age >= durationTicks) {
				finish();
			}
		}

		@Override
		public void render(VfxRenderContext ctx) {
			// Emitters, distortion and the light render themselves through the backend.
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
			if (light != null) {
				light.remove();
				light = null;
			}
			if (sound != null) {
				Minecraft.getInstance().getSoundManager().stop(sound);
				sound = null;
			}
			if (entity != null) {
				PlayerAnimator.stop(entity.getId(), PlayerAnimator.Layer.BASE, CLIP_FADE_TICKS);
			}
		}
	}

	/**
	 * Bound {@code homelander.sun.charge} one-shot that follows the bound entity
	 * and ramps volume + pitch over the charge duration to reinforce the build.
	 */
	private static final class ChargeSound extends AbstractTickableSoundInstance {
		private final @Nullable Entity entity;
		private final Vec3 fixed;
		private final int durationTicks;
		private final float baseVolume;
		private final float basePitch;
		private final float pitchRise;
		private int age;

		private ChargeSound(@Nullable Entity entity, Vec3 fixed, int durationTicks,
				float baseVolume, float basePitch, float pitchRise) {
			super(HomelanderSounds.SUN_CHARGE, SoundSource.PLAYERS, RandomSource.create());
			this.entity = entity;
			this.fixed = fixed;
			this.durationTicks = Math.max(1, durationTicks);
			this.baseVolume = baseVolume;
			this.basePitch = basePitch;
			this.pitchRise = pitchRise;
			this.looping = false;
			this.delay = 0;
			this.attenuation = SoundInstance.Attenuation.LINEAR;
			this.volume = baseVolume * 0.55f;
			this.pitch = basePitch;
			syncPosition();
		}

		@Override
		public void tick() {
			age++;
			if (entity != null) {
				if (entity.isRemoved()) {
					stop();
					return;
				}
				syncPosition();
			}
			float t = Math.min(1f, (float) age / durationTicks);
			this.volume = baseVolume * (0.55f + 0.45f * t);
			this.pitch = basePitch + pitchRise * t;
		}

		private void syncPosition() {
			Entity e = entity;
			if (e != null) {
				this.x = e.getX();
				this.y = e.getY();
				this.z = e.getZ();
			} else {
				this.x = fixed.x;
				this.y = fixed.y;
				this.z = fixed.z;
			}
		}
	}
}
