package io.github.grebeshok105.codex.client.core.vfx.veil;

import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.post.PostPipeline;
import foundry.veil.api.client.render.post.PostProcessingManager;
import io.github.grebeshok105.codex.ModId;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Drives the {@code superheroes:vfx_flash} and {@code superheroes:vfx_distortion}
 * post pipelines (assets at {@code assets/superheroes/pinwheel/post/}). A call
 * arms the pipeline for {@link #FADE_TICKS} client ticks and pushes its
 * uniforms; the tick handler decays {@code uIntensity} to zero and then
 * removes the pipeline, so an effect is active only while its intensity is
 * positive.
 */
final class VeilPostEffects {
	private static final Logger LOGGER = LoggerFactory.getLogger("superheroes-vfx");
	private static final ResourceLocation FLASH = ModId.of("vfx_flash");
	private static final ResourceLocation DISTORTION = ModId.of("vfx_distortion");
	/** Matches {@code FallbackVfxBackend}'s flash duration so both backends fade alike. */
	private static final float FADE_TICKS = 6f;

	private static float flashPeak;
	private static int flashRgb;
	private static float flashTicksLeft;

	private static Vec3 distortionCenter;
	private static float distortionRadius;
	private static float distortionStrength;
	private static float distortionTicksLeft;

	private static boolean tickRegistered;
	private static boolean failureLogged;

	private VeilPostEffects() {
	}

	static void flash(float intensity, int rgb) {
		try {
			if (intensity <= 0f) {
				return;
			}
			if (intensity >= flashPeak || flashTicksLeft <= 0f) {
				flashPeak = Math.min(intensity, 1f);
				flashRgb = rgb & 0xFFFFFF;
			}
			flashTicksLeft = FADE_TICKS;
			ensureTick();
			apply();
		} catch (Throwable t) {
			logOnce(t);
		}
	}

	static void distortion(Vec3 center, float radius, float strength) {
		try {
			if (strength <= 0f) {
				return;
			}
			distortionCenter = center;
			distortionRadius = radius;
			distortionStrength = strength;
			distortionTicksLeft = FADE_TICKS;
			ensureTick();
			apply();
		} catch (Throwable t) {
			logOnce(t);
		}
	}

	private static void ensureTick() {
		if (!tickRegistered) {
			tickRegistered = true;
			ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
		}
	}

	private static void tick() {
		boolean active = flashTicksLeft > 0f || distortionTicksLeft > 0f;
		if (flashTicksLeft > 0f) {
			flashTicksLeft--;
		}
		if (distortionTicksLeft > 0f) {
			distortionTicksLeft--;
		}
		// One last apply() after the fade hits zero so the pipeline is removed.
		if (active) {
			apply();
		}
	}

	private static void apply() {
		PostProcessingManager manager = VeilRenderSystem.renderer().getPostProcessingManager();
		applyFlash(manager, flashTicksLeft > 0f ? flashPeak * (flashTicksLeft / FADE_TICKS) : 0f);
		applyDistortion(manager, distortionTicksLeft > 0f ? distortionTicksLeft / FADE_TICKS : 0f);
	}

	private static void applyFlash(PostProcessingManager manager, float intensity) {
		if (intensity <= 0f) {
			manager.remove(FLASH);
			return;
		}
		manager.add(FLASH);
		PostPipeline pipeline = manager.getPipeline(FLASH);
		if (pipeline == null) {
			return;
		}
		pipeline.getUniformSafe("uIntensity").setFloat(intensity);
		pipeline.getUniformSafe("uColor").setVector(
				((flashRgb >> 16) & 0xFF) / 255f,
				((flashRgb >> 8) & 0xFF) / 255f,
				(flashRgb & 0xFF) / 255f);
	}

	private static void applyDistortion(PostProcessingManager manager, float intensity) {
		if (intensity <= 0f || distortionCenter == null) {
			manager.remove(DISTORTION);
			return;
		}
		manager.add(DISTORTION);
		PostPipeline pipeline = manager.getPipeline(DISTORTION);
		if (pipeline == null) {
			return;
		}
		pipeline.getUniformSafe("uIntensity").setFloat(intensity);
		pipeline.getUniformSafe("uCenter").setVector(
				(float) distortionCenter.x, (float) distortionCenter.y, (float) distortionCenter.z);
		pipeline.getUniformSafe("uRadius").setFloat(distortionRadius);
		pipeline.getUniformSafe("uStrength").setFloat(distortionStrength);
	}

	private static void logOnce(Throwable t) {
		if (!failureLogged) {
			failureLogged = true;
			LOGGER.warn("Veil post-effect call failed; further failures are silenced", t);
		}
	}
}
