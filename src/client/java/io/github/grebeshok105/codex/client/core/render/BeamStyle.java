package io.github.grebeshok105.codex.client.core.render;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * One registered beam look: the wire-level style id, how long a beam lives, the
 * per-beam draw body, and an optional add-time side effect (e.g. the repulsor
 * charge flash). Draw parameters inside the body are byte-identical ports of the
 * pre-L2 per-hero renderers.
 */
public record BeamStyle(ResourceLocation id, long lifetimeMs, Draw draw, @Nullable Runnable onAdd) {
	public BeamStyle(ResourceLocation id, long lifetimeMs, Draw draw) {
		this(id, lifetimeMs, draw, null);
	}

	@FunctionalInterface
	public interface Draw {
		void draw(WorldRenderContext context, Vec3 start, Vec3 end, long ageMs, float ageFrac, long nowMs);
	}
}
