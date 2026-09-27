package io.github.grebeshok105.codex.client.core.render;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Unified beam renderer: one world-render pass for every registered
 * {@link BeamStyle}. A style's {@link BeamStyle#draw()} decides the look; this
 * class only owns the live-beam list and expiry by {@code lifetimeMs}.
 */
public final class BeamRenderer {
	private static final List<ActiveBeam> BEAMS = new ArrayList<>();

	private BeamRenderer() {
	}

	public static void register() {
		WorldRenderEvents.AFTER_TRANSLUCENT.register(BeamRenderer::render);
	}

	public static void add(ResourceLocation styleId, Vec3 start, Vec3 end) {
		BeamStyle style = BeamStyles.get(styleId);
		if (style == null) {
			return;
		}
		if (style.onAdd() != null) {
			style.onAdd().run();
		}
		BEAMS.add(new ActiveBeam(style, start, end, System.currentTimeMillis()));
	}

	private static void render(WorldRenderContext context) {
		long now = System.currentTimeMillis();
		BEAMS.removeIf(b -> now - b.spawnedAtMs() > b.style().lifetimeMs());
		if (BEAMS.isEmpty()) {
			return;
		}
		for (ActiveBeam b : BEAMS) {
			long ageMs = now - b.spawnedAtMs();
			b.style().draw().draw(context, b.start(), b.end(), ageMs,
					ageMs / (float) b.style().lifetimeMs(), now);
		}
	}

	private record ActiveBeam(BeamStyle style, Vec3 start, Vec3 end, long spawnedAtMs) {
	}
}
