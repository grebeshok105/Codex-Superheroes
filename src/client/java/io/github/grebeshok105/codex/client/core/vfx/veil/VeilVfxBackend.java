package io.github.grebeshok105.codex.client.core.vfx.veil;

import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.light.data.PointLightData;
import foundry.veil.api.client.render.light.renderer.LightRenderHandle;
import foundry.veil.api.quasar.particle.ParticleEmitter;
import foundry.veil.api.quasar.particle.ParticleSystemManager;
import io.github.grebeshok105.codex.client.core.vfx.backend.LightHandle;
import io.github.grebeshok105.codex.client.core.vfx.backend.VfxBackend;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Veil-backed {@link VfxBackend}: Quasar particle emitters, dynamic point
 * lights, and the {@code superheroes:vfx_*} post pipelines (handled by
 * {@link VeilPostEffects}). Instantiated reflectively by {@code VfxBackends}
 * only when Veil is loaded — it is the only class allowed to carry
 * {@code foundry.veil} imports next to {@link VeilPostEffects}, and every
 * call is wrapped so a renderer hiccup degrades silently instead of breaking
 * the client (same boundary style as {@code VeilScorpionFx}).
 */
public final class VeilVfxBackend implements VfxBackend {
	private static final Logger LOGGER = LoggerFactory.getLogger("superheroes-vfx");

	private static final LightHandle NOOP_LIGHT = new LightHandle() {
		@Override
		public void move(Vec3 pos) {
		}

		@Override
		public void set(int rgb, float radius, float brightness) {
		}

		@Override
		public void remove() {
		}
	};

	private static boolean failureLogged;

	/** Resolved by {@code VfxBackends} via {@code Class.forName} — must stay public and no-arg. */
	public VeilVfxBackend() {
	}

	@Override
	public void emit(ResourceLocation emitter, Vec3 pos) {
		try {
			ParticleSystemManager manager = VeilRenderSystem.renderer().getParticleManager();
			ParticleEmitter particleEmitter = manager.createEmitter(emitter);
			if (particleEmitter == null) {
				return;
			}
			particleEmitter.setPosition(pos);
			manager.addParticleSystem(particleEmitter);
		} catch (Throwable t) {
			logOnce(t);
		}
	}

	@Override
	public LightHandle light(Vec3 pos, int rgb, float radius, float brightness) {
		try {
			PointLightData data = new PointLightData()
					.setPosition(pos.x, pos.y, pos.z)
					.setColor(rgb)
					.setRadius(radius)
					.setBrightness(brightness);
			LightRenderHandle<PointLightData> handle =
					VeilRenderSystem.renderer().getLightRenderer().addLight(data);
			return new VeilLightHandle(handle);
		} catch (Throwable t) {
			logOnce(t);
			return NOOP_LIGHT;
		}
	}

	@Override
	public void flash(float intensity, int rgb) {
		VeilPostEffects.flash(intensity, rgb);
	}

	@Override
	public void distortion(Vec3 center, float radius, float strength) {
		VeilPostEffects.distortion(center, radius, strength);
	}

	private static void logOnce(Throwable t) {
		if (!failureLogged) {
			failureLogged = true;
			LOGGER.warn("Veil VFX backend call failed; further failures are silenced", t);
		}
	}

	/** Adapts a Veil {@link LightRenderHandle} to the engine-agnostic {@link LightHandle}. */
	private static final class VeilLightHandle implements LightHandle {
		private LightRenderHandle<PointLightData> handle;

		private VeilLightHandle(LightRenderHandle<PointLightData> handle) {
			this.handle = handle;
		}

		@Override
		public void move(Vec3 pos) {
			try {
				if (handle != null && handle.isValid()) {
					handle.getLightData().setPosition(pos.x, pos.y, pos.z);
					handle.markDirty();
				}
			} catch (Throwable t) {
				logOnce(t);
			}
		}

		@Override
		public void set(int rgb, float radius, float brightness) {
			try {
				if (handle != null && handle.isValid()) {
					handle.getLightData()
							.setColor(rgb)
							.setRadius(radius)
							.setBrightness(brightness);
					handle.markDirty();
				}
			} catch (Throwable t) {
				logOnce(t);
			}
		}

		@Override
		public void remove() {
			try {
				if (handle != null) {
					handle.free();
					handle = null;
				}
			} catch (Throwable t) {
				logOnce(t);
			}
		}
	}
}
