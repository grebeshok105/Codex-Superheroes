package io.github.grebeshok105.codex.client.hero.homelander.fx;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

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
import io.github.grebeshok105.codex.client.core.vfx.pattern.ImpactPattern;
import io.github.grebeshok105.codex.client.core.vfx.pattern.ShockwavePattern;
import io.github.grebeshok105.codex.sound.HomelanderSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Homelander's hand clap impact ({@code superheroes:homelander/clap}): the
 * server sends this event at the EMF {@code hand_clap} clip's contact frame
 * (see {@code HandClapAbility#CONTACT_TICKS}), so the burst fires on the
 * first tick — the {@code homelander.hand_clap} contract sound, a light
 * flash between the hands, an {@link ImpactPattern} burst with a distortion
 * pulse, a {@link ShockwavePattern} ring and a dust cone marching along the
 * event's origin → target axis (the server sends the eye position and the
 * eye + forward · RANGE point captured at cast), plus a proximity-scaled
 * {@link CameraImpulse}. The clip itself is played by the separate
 * {@code homelander/clap_windup} event at cast. Tuning lives in
 * {@code vfx/homelander/clap.json}.
 */
public final class ClapFx {
	private static final ResourceLocation PARAMS = ModId.of("homelander/clap");
	private static final ResourceLocation FLASH_EMITTER = ModId.of("homelander_clap_flash");
	private static final ResourceLocation DUST_EMITTER = ModId.of("homelander_clap_dust");
	private static final Vec3 UP = new Vec3(0, 1, 0);

	private ClapFx() {
	}

	public static VfxEffect create(VfxSpawn spawn) {
		return new ClapBurstFx(spawn);
	}

	private static VfxParams params(VfxSpawn spawn) {
		VfxParams p = VfxParamsLoader.get(PARAMS);
		return p == VfxParams.EMPTY ? spawn.params() : p;
	}

	private static final class ClapBurstFx implements VfxEffect {
		private final Vec3 clapPoint;
		private final Vec3 dir;
		private final Vec3 ground;
		private final int coneSteps;
		private final double coneStep;
		private final int coneIntervalTicks;
		private final int lightFadeTicks;
		private final int lightRgb;
		private final float lightRadius;
		private final float lightBrightness;
		private final float shakeRadius;
		private final float shakeIntensity;
		private final int shakeTicks;
		private final VfxParams params;
		private final List<ShockwavePattern> rings = new ArrayList<>();

		private int burstAge;
		private boolean burst;
		private int emittedConeSteps;
		private @Nullable LightHandle light;
		private boolean finished;

		private ClapBurstFx(VfxSpawn spawn) {
			this.params = params(spawn);
			VfxParams p = params;

			Entity source = spawn.source();

			Vec3 axis = spawn.target().subtract(spawn.origin());
			this.dir = axis.lengthSqr() > 1e-6 ? axis.normalize() : new Vec3(1, 0, 0);
			this.clapPoint = spawn.origin().add(dir.scale(p.number("handForward", 0.55f)))
					.add(0, -p.number("handDrop", 0.25f), 0);
			this.ground = spawn.origin().add(0, -1.4, 0);

			this.coneSteps = Math.max(0, (int) p.number("coneSteps", 10f));
			this.coneStep = p.number("coneStep", 1.5f);
			this.coneIntervalTicks = Math.max(1, (int) p.number("coneIntervalTicks", 1f));
			this.lightFadeTicks = Math.max(1, (int) p.number("flashLightFadeTicks", 8f));
			this.lightRgb = p.color("flashLightColor", 0xEAF6FF) & 0xFFFFFF;
			this.lightRadius = p.number("flashLightRadius", 8f);
			this.lightBrightness = p.number("flashLightBrightness", 1.6f);
			this.shakeRadius = p.number("shakeRadius", 24f);
			this.shakeIntensity = p.number("shakeIntensity", 1.8f);
			this.shakeTicks = Math.max(1, (int) p.number("shakeTicks", 16f));

			float volume = p.number("clapVolume", 2f);
			float pitch = p.number("clapPitch", 1f);
			if (source != null) {
				Minecraft.getInstance().getSoundManager().play(new EntityBoundSoundInstance(
						HomelanderSounds.HAND_CLAP, SoundSource.PLAYERS, volume, pitch,
						source, source.getRandom().nextLong()));
			} else {
				Vec3 pos = spawn.origin();
				Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(
						HomelanderSounds.HAND_CLAP, SoundSource.PLAYERS, volume, pitch,
						RandomSource.create(), pos.x, pos.y, pos.z));
			}
		}

		@Override
		public void tick() {
			if (finished) {
				return;
			}
			// The event already lands at the clip's contact frame — the burst
			// fires on the first tick, no delay param.
			if (!burst) {
				fireBurst();
			}
			burstAge++;
			VfxBackend backend = VfxBackends.current();
			while (emittedConeSteps < coneSteps
					&& burstAge >= (emittedConeSteps + 1) * coneIntervalTicks) {
				emittedConeSteps++;
				backend.emit(DUST_EMITTER, clapPoint.add(dir.scale(emittedConeSteps * coneStep)));
			}
			if (light != null) {
				float fade = Math.max(0f, 1f - (float) burstAge / lightFadeTicks);
				if (fade <= 0f) {
					light.remove();
					light = null;
				} else {
					light.set(lightRgb, lightRadius * fade, lightBrightness * fade);
				}
			}
			rings.removeIf(ring -> {
				ring.tick();
				return ring.done();
			});
			if (emittedConeSteps >= coneSteps && rings.isEmpty() && light == null) {
				finished = true;
			}
		}

		private void fireBurst() {
			burst = true;
			VfxBackend backend = VfxBackends.current();
			this.light = backend.light(clapPoint, lightRgb, lightRadius, lightBrightness);
			ImpactPattern.spawn(backend, clapPoint, dir, FLASH_EMITTER, params);
			backend.emit(DUST_EMITTER, ground);
			rings.add(new ShockwavePattern(clapPoint,
					params.number("ringRadius", 5f),
					Math.max(1, (int) params.number("ringTicks", 10f)),
					params.color("ringColor", 0x80E8F4FF),
					params.number("ringBand", 0.18f)));

			LocalPlayer self = Minecraft.getInstance().player;
			if (self != null && shakeRadius > 0f) {
				double dist = self.position().distanceTo(clapPoint);
				if (dist < shakeRadius) {
					float proximity = 1f - (float) (dist / shakeRadius);
					CameraImpulse.shake(shakeIntensity * proximity, shakeTicks);
				}
			}
		}

		@Override
		public void render(VfxRenderContext ctx) {
			for (ShockwavePattern ring : rings) {
				ring.render(ctx);
			}
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
