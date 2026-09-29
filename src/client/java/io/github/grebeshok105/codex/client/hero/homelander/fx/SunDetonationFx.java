package io.github.grebeshok105.codex.client.hero.homelander.fx;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.client.core.vfx.VfxEffect;
import io.github.grebeshok105.codex.client.core.vfx.VfxRenderContext;
import io.github.grebeshok105.codex.client.core.vfx.VfxSpawn;
import io.github.grebeshok105.codex.client.core.vfx.backend.LightHandle;
import io.github.grebeshok105.codex.client.core.vfx.backend.VfxBackend;
import io.github.grebeshok105.codex.client.core.vfx.backend.VfxBackends;
import io.github.grebeshok105.codex.client.core.vfx.params.VfxParams;
import io.github.grebeshok105.codex.client.core.vfx.params.VfxParamsLoader;
import io.github.grebeshok105.codex.client.core.vfx.pattern.CameraImpulse;
import io.github.grebeshok105.codex.client.core.vfx.pattern.ScreenFlash;
import io.github.grebeshok105.codex.client.core.vfx.pattern.ShockwavePattern;
import io.github.grebeshok105.codex.hero.homelander.vfx.HomelanderVfxIds;
import io.github.grebeshok105.codex.sound.HomelanderSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Homelander's aftermath detonation ({@code superheroes:homelander/
 * sun_detonation}) — and, scaled down by the event's {@code scale}, the
 * madness-flight crash ({@code homelander/madness_crash}): a distance- and
 * line-of-sight-attenuated {@link ScreenFlash}, a core light that fades over
 * {@code coreLightFadeTicks}, two expanding {@link ShockwavePattern} rings, a
 * distortion pulse and ember/debris emitter bursts, plus a camera shake. The
 * {@code homelander.sun.detonate} sting plays once, for the detonation event
 * only — the crash contract has no sound row of its own. Tuning lives in
 * {@code vfx/homelander/sun_detonation.json}.
 */
public final class SunDetonationFx {
	private static final ResourceLocation PARAMS = ModId.of("homelander/sun_detonation");
	private static final ResourceLocation EMBER_EMITTER = ModId.of("homelander_sun_ember");
	private static final ResourceLocation DEBRIS_EMITTER = ModId.of("homelander_sun_debris");

	private SunDetonationFx() {
	}

	public static VfxEffect create(VfxSpawn spawn) {
		return new DetonationFx(spawn);
	}

	private static final class DetonationFx implements VfxEffect {
		private final ShockwavePattern ring;
		private final ShockwavePattern innerRing;
		private final int lightFadeTicks;
		private final int lightRgb;
		private final float lightRadius;
		private final float lightBrightness;
		private final int emberRepeatTicks;
		private final Vec3 center;

		private int age;
		private @Nullable LightHandle light;
		private boolean finished;

		private DetonationFx(VfxSpawn spawn) {
			this.center = spawn.origin();
			VfxParams p = params(spawn);
			float scale = Mth.clamp(spawn.scale(), 0f, 1f);
			float att = attenuation(center) * scale;
			float radius = Math.max(0.1f, scale);

			ScreenFlash.trigger(att * p.number("flashIntensity", 1f),
					p.color("flashColor", 0xFFFFF4E0),
					Math.max(1, (int) p.number("flashTicks", 60f)));

			VfxBackend backend = VfxBackends.current();
			this.lightFadeTicks = Math.max(1, (int) p.number("coreLightFadeTicks", 60f));
			this.lightRgb = p.color("coreLightColor", 0xFFF2CC) & 0xFFFFFF;
			this.lightRadius = p.number("coreLightRadius", 48f) * radius;
			this.lightBrightness = p.number("coreLightBrightness", 2f);
			this.light = backend.light(center, lightRgb, lightRadius, lightBrightness);

			this.ring = new ShockwavePattern(center,
					p.number("ringRadius", 14f) * radius,
					Math.max(1, (int) p.number("ringTicks", 16f)),
					p.color("ringColor", 0x90FFC85A), p.number("ringBand", 0.18f));
			this.innerRing = new ShockwavePattern(center,
					p.number("ring2Radius", 8f) * radius,
					Math.max(1, (int) p.number("ring2Ticks", 10f)),
					p.color("ring2Color", 0xC0FFF4E0), p.number("ring2Band", 0.22f));
			backend.distortion(center, p.number("distortionRadius", 10f) * radius,
					p.number("distortionStrength", 0.8f) * att);

			backend.emit(EMBER_EMITTER, center);
			backend.emit(DEBRIS_EMITTER, center);
			this.emberRepeatTicks = Math.max(0, (int) p.number("emberRepeatTicks", 3f));

			if (att > 0f) {
				CameraImpulse.shake(p.number("shakeIntensity", 1.6f) * att,
						Math.max(1, (int) p.number("shakeTicks", 30f)));
			}
			if (HomelanderVfxIds.SUN_DETONATION.equals(spawn.effect())) {
				float volume = Mth.clamp(p.number("detonateVolume", 2.5f)
						* Math.max(att, 0.15f), 0f, 4f);
				Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(
						HomelanderSounds.SUN_DETONATE, SoundSource.PLAYERS, volume,
						p.number("detonatePitch", 1f), RandomSource.create(),
						center.x, center.y, center.z));
			}
		}

		private static VfxParams params(VfxSpawn spawn) {
			VfxParams p = VfxParamsLoader.get(PARAMS);
			return p == VfxParams.EMPTY ? spawn.params() : p;
		}

		/** Distance + line-of-sight falloff from the camera to the detonation. */
		private static float attenuation(Vec3 center) {
			Minecraft client = Minecraft.getInstance();
			ClientLevel level = client.level;
			LocalPlayer player = client.player;
			if (level == null || player == null) {
				return 0f;
			}
			Vec3 eye = client.gameRenderer.getMainCamera().getPosition();
			boolean los = level.clip(new ClipContext(eye, center,
					ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player))
					.getType() == HitResult.Type.MISS;
			return ScreenFlash.attenuation(eye.distanceTo(center), 160.0, los);
		}

		@Override
		public void tick() {
			if (finished) {
				return;
			}
			age++;
			if (emberRepeatTicks > 0 && age <= emberRepeatTicks * 3 && age % emberRepeatTicks == 0) {
				VfxBackends.current().emit(EMBER_EMITTER, center);
			}
			if (light != null) {
				float fade = Math.max(0f, 1f - (float) age / lightFadeTicks);
				if (fade <= 0f) {
					light.remove();
					light = null;
				} else {
					light.set(lightRgb, lightRadius * fade, lightBrightness * fade);
				}
			}
			ring.tick();
			innerRing.tick();
			if (ring.done() && innerRing.done() && light == null) {
				finished = true;
			}
		}

		@Override
		public void render(VfxRenderContext ctx) {
			ring.render(ctx);
			innerRing.render(ctx);
		}

		@Override
		public boolean done() {
			return finished;
		}

		@Override
		public void cancel() {
			finished = true;
			if (light != null) {
				light.remove();
				light = null;
			}
		}
	}
}
