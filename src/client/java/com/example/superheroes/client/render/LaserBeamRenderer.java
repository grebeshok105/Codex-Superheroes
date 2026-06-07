package com.example.superheroes.client.render;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class LaserBeamRenderer {
	private static final long LIFETIME_MS = 220L;
	private static final List<Beam> BEAMS = new ArrayList<>();

	private LaserBeamRenderer() {
	}

	public static void register() {
		WorldRenderEvents.AFTER_TRANSLUCENT.register(LaserBeamRenderer::render);
	}

	public static void add(Vec3 start, Vec3 end) {
		BEAMS.add(new Beam(start, end, System.currentTimeMillis()));
	}

	private static void render(WorldRenderContext context) {
		long now = System.currentTimeMillis();
		Iterator<Beam> it = BEAMS.iterator();
		while (it.hasNext()) {
			if (now - it.next().spawnedAtMs() > LIFETIME_MS) {
				it.remove();
			}
		}
		if (BEAMS.isEmpty()) {
			return;
		}
		for (Beam beam : BEAMS) {
			float age = (now - beam.spawnedAtMs()) / (float) LIFETIME_MS;
			float alpha = Math.max(0f, 1f - age);
			Vec3 start = beam.start();
			Vec3 end = beam.end();
			Vec3 dir = end.subtract(start);
			if (dir.lengthSqr() < 1e-6) {
				continue;
			}
			dir = dir.normalize();
			Vec3 right = dir.cross(new Vec3(0, 1, 0));
			if (right.lengthSqr() < 1e-6) {
				right = new Vec3(1, 0, 0);
			}
			right = right.normalize();
			double eyeSep = 0.11;
			Vec3 leftEye = start.add(right.scale(-eyeSep));
			Vec3 rightEye = start.add(right.scale(eyeSep));
			BeamRenderer.draw(context, leftEye, end, alpha, 0.45f);
			BeamRenderer.draw(context, rightEye, end, alpha, 0.45f);
		}
	}

	private record Beam(Vec3 start, Vec3 end, long spawnedAtMs) {
	}
}
