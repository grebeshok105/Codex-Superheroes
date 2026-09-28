package io.github.grebeshok105.codex.client.core.vfx.pattern;

import io.github.grebeshok105.codex.client.core.vfx.backend.VfxBackend;
import io.github.grebeshok105.codex.client.core.vfx.params.VfxParams;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * One-shot surface impact: emits the Quasar emitter slightly off the surface
 * along {@code normal} and optionally kicks the screen-space distortion post
 * effect. Tuning keys: {@code surfaceOffset} (blocks, default 0.05),
 * {@code distortionRadius} (0 disables), {@code distortionStrength}.
 */
public final class ImpactPattern {
	private ImpactPattern() {
	}

	public static void spawn(VfxBackend backend, Vec3 pos, Vec3 normal, ResourceLocation emitter,
			VfxParams params) {
		Vec3 at = pos.add(normal.scale(params.number("surfaceOffset", 0.05f)));
		backend.emit(emitter, at);
		float distortionRadius = params.number("distortionRadius", 0f);
		if (distortionRadius > 0f) {
			backend.distortion(at, distortionRadius, params.number("distortionStrength", 0.5f));
		}
	}
}
