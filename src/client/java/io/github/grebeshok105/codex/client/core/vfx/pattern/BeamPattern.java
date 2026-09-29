package io.github.grebeshok105.codex.client.core.vfx.pattern;

import io.github.grebeshok105.codex.client.core.render.BeamLook;
import io.github.grebeshok105.codex.client.core.render.CrossBeamRenderer;
import io.github.grebeshok105.codex.client.core.vfx.VfxRenderContext;
import net.minecraft.world.phys.Vec3;

/**
 * Styled cross-beam draw. Applies the {@link BeamLook#noise()} wobble to the
 * beam endpoint itself, then delegates colors and widths to the
 * {@link CrossBeamRenderer} {@code BeamLook} overload.
 */
public final class BeamPattern {
	private static final double NOISE_SCALE = 0.15;

	private BeamPattern() {
	}

	public static void draw(VfxRenderContext ctx, Vec3 from, Vec3 to, BeamLook look, float intensity) {
		Vec3 end = look.noise() > 0f ? to.add(noiseOffset(to, look.noise())) : to;
		CrossBeamRenderer.draw(ctx.world(), from, end, intensity, look);
	}

	/** Smooth time-varying jitter; deterministic per endpoint so both eyes agree. */
	private static Vec3 noiseOffset(Vec3 to, float noise) {
		long t = System.currentTimeMillis();
		double s = noise * NOISE_SCALE;
		return new Vec3(
				Math.sin(t * 0.031 + to.x * 7.3) * s,
				Math.sin(t * 0.043 + to.y * 5.1) * s,
				Math.sin(t * 0.037 + to.z * 6.7) * s);
	}
}
