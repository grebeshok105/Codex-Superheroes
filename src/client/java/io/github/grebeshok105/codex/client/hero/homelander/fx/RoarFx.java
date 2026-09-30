package io.github.grebeshok105.codex.client.hero.homelander.fx;

import java.util.ArrayList;
import java.util.List;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.client.core.vfx.VfxEffect;
import io.github.grebeshok105.codex.client.core.vfx.VfxRenderContext;
import io.github.grebeshok105.codex.client.core.vfx.VfxSpawn;
import io.github.grebeshok105.codex.client.core.vfx.backend.VfxBackend;
import io.github.grebeshok105.codex.client.core.vfx.backend.VfxBackends;
import io.github.grebeshok105.codex.client.core.vfx.params.VfxParams;
import io.github.grebeshok105.codex.client.core.vfx.params.VfxParamsLoader;
import io.github.grebeshok105.codex.client.core.vfx.pattern.CameraImpulse;
import io.github.grebeshok105.codex.client.core.vfx.pattern.ShockwavePattern;
import io.github.grebeshok105.codex.sound.HomelanderSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Homelander's stunning roar ({@code superheroes:homelander/roar}): the
 * two contract sounds
 * ({@code homelander.roar} + {@code homelander.roar.deep} layered exactly as
 * the old server-side pair), then — over the following 1500 ms — a
 * mouth-anchored sound-wave cone: {@link ShockwavePattern} rings and the
 * {@code homelander_roar_wave} emitter spawned in sequence along the look
 * axis, a periodic distortion pulse, dust lifting off the ground and a low
 * proximity-scaled rumble shake. The mouth re-anchors to the source entity
 * every tick so the wave follows the head; the event's origin/target
 * (server-sent mouth and mouth + forward · RADIUS) is the fallback.
 * Tuning lives in {@code vfx/homelander/roar.json}.
 */
public final class RoarFx {
	private static final ResourceLocation PARAMS = ModId.of("homelander/roar");
	private static final ResourceLocation WAVE_EMITTER = ModId.of("homelander_roar_wave");
	private static final ResourceLocation DUST_EMITTER = ModId.of("homelander_roar_dust");

	private RoarFx() {
	}

	public static VfxEffect create(VfxSpawn spawn) {
		return new RoarWaveFx(spawn);
	}

	private static VfxParams params(VfxSpawn spawn) {
		VfxParams p = VfxParamsLoader.get(PARAMS);
		return p == VfxParams.EMPTY ? spawn.params() : p;
	}

	private static final class RoarWaveFx implements VfxEffect {
		private final Entity source;
		private final Vec3 fallbackMouth;
		private final Vec3 fallbackDir;
		private final int durationTicks;
		private final double mouthForward;
		private final int ringIntervalTicks;
		private final double ringSpacing;
		private final float ringRadius;
		private final int ringTicks;
		private final int ringArgb;
		private final float ringBand;
		private final float distortionRadius;
		private final float distortionStrength;
		private final int distortionIntervalTicks;
		private final int dustIntervalTicks;

		private final List<ShockwavePattern> rings = new ArrayList<>();
		private int age;
		private int ringIndex;
		private boolean finished;

		private RoarWaveFx(VfxSpawn spawn) {
			VfxParams p = params(spawn);
			this.source = spawn.source();
			this.fallbackMouth = spawn.origin();
			Vec3 axis = spawn.target().subtract(spawn.origin());
			this.fallbackDir = axis.lengthSqr() > 1e-6 ? axis.normalize() : new Vec3(1, 0, 0);

			this.durationTicks = Math.max(1, (int) p.number("durationTicks", 30f));
			this.mouthForward = p.number("mouthForward", 0.6f);
			this.ringIntervalTicks = Math.max(1, (int) p.number("ringIntervalTicks", 3f));
			this.ringSpacing = p.number("ringSpacing", 1.5f);
			this.ringRadius = p.number("ringRadius", 2.2f);
			this.ringTicks = Math.max(1, (int) p.number("ringTicks", 12f));
			this.ringArgb = p.color("ringColor", 0x70D8ECFF);
			this.ringBand = p.number("ringBand", 0.2f);
			this.distortionRadius = p.number("distortionRadius", 4f);
			this.distortionStrength = p.number("distortionStrength", 0.5f);
			this.distortionIntervalTicks = Math.max(1, (int) p.number("distortionIntervalTicks", 10f));
			this.dustIntervalTicks = Math.max(1, (int) p.number("dustIntervalTicks", 5f));

			playSound(spawn, HomelanderSounds.ROAR,
					p.number("roarVolume", 1.6f), p.number("roarPitch", 1f));
			playSound(spawn, HomelanderSounds.ROAR_DEEP,
					p.number("roarDeepVolume", 1f), p.number("roarDeepPitch", 1f));

			float shakeRadius = p.number("shakeRadius", 16f);
			LocalPlayer self = Minecraft.getInstance().player;
			if (self != null && shakeRadius > 0f) {
				double dist = self.position().distanceTo(fallbackMouth);
				if (dist < shakeRadius) {
					float proximity = 1f - (float) (dist / shakeRadius);
					CameraImpulse.shake(p.number("shakeIntensity", 0.8f) * proximity,
							Math.max(1, (int) p.number("shakeTicks", 24f)));
				}
			}
		}

		private void playSound(VfxSpawn spawn, SoundEvent event, float volume, float pitch) {
			if (source != null) {
				Minecraft.getInstance().getSoundManager().play(new EntityBoundSoundInstance(
						event, SoundSource.PLAYERS, volume, pitch, source,
						source.getRandom().nextLong()));
			} else {
				Vec3 pos = spawn.origin();
				Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(
						event, SoundSource.PLAYERS, volume, pitch, RandomSource.create(),
						pos.x, pos.y, pos.z));
			}
		}

		private Vec3 mouth() {
			return source != null
					? source.getEyePosition().add(source.getViewVector(1f).scale(mouthForward))
					: fallbackMouth;
		}

		private Vec3 dir() {
			return source != null ? source.getViewVector(1f).normalize() : fallbackDir;
		}

		@Override
		public void tick() {
			if (finished) {
				return;
			}
			age++;
			VfxBackend backend = VfxBackends.current();
			Vec3 mouth = mouth();
			Vec3 dir = dir();

			if (age <= durationTicks && age % ringIntervalTicks == 0) {
				Vec3 at = mouth.add(dir.scale(ringIndex * ringSpacing));
				rings.add(new ShockwavePattern(at, ringRadius, ringTicks, ringArgb, ringBand));
				backend.emit(WAVE_EMITTER, at);
				ringIndex++;
			}
			if (distortionRadius > 0f
					&& (age == 1 || age % distortionIntervalTicks == 0)) {
				backend.distortion(mouth, distortionRadius, distortionStrength);
			}
			if (age % dustIntervalTicks == 0) {
				Vec3 feet = source != null ? source.position()
						: fallbackMouth.add(0, -1.5, 0);
				backend.emit(DUST_EMITTER, feet);
			}

			rings.removeIf(ring -> {
				ring.tick();
				return ring.done();
			});
			if (age > durationTicks && rings.isEmpty()) {
				finished = true;
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
		}
	}
}
