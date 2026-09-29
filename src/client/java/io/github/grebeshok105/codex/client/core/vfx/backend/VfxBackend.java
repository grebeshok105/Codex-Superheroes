package io.github.grebeshok105.codex.client.core.vfx.backend;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * The capability seam between Visual Core effects and the rendering engine.
 * The Veil implementation (Quasar emitters, dynamic lights, post pipelines)
 * lives in {@code client/core/vfx/veil/}; {@link FallbackVfxBackend} covers the
 * Veil-free run. Effects call these only — never {@code foundry.veil} types —
 * so the mod keeps working when Veil is absent.
 */
public interface VfxBackend {
	/** Spawns the {@code assets/<ns>/quasar/emitters/<emitter>.json} emitter at {@code pos}. */
	void emit(ResourceLocation emitter, Vec3 pos);

	/** Creates a dynamic point light; move/tune/remove it through the returned handle. */
	LightHandle light(Vec3 pos, int rgb, float radius, float brightness);

	/** Full-screen color flash at {@code intensity} (0..1) — a HUD overlay on fallback. */
	void flash(float intensity, int rgb);

	/** Screen-space heat distortion around {@code center}. */
	void distortion(Vec3 center, float radius, float strength);
}
