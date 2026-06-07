package com.example.superheroes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class CosmicBeamRenderer {
	private static final long LIFETIME_MS = 700L;
	private static final List<Beam> BEAMS = new ArrayList<>();

	private CosmicBeamRenderer() {
	}

	public static void register() {
		WorldRenderEvents.AFTER_TRANSLUCENT.register(CosmicBeamRenderer::render);
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
		MultiBufferSource consumers = context.consumers();
		if (consumers == null) {
			return;
		}
		Vec3 cam = context.camera().getPosition();
		PoseStack ps = context.matrixStack();
		ps.pushPose();
		ps.translate(-cam.x, -cam.y, -cam.z);
		VertexConsumer buf = consumers.getBuffer(RenderType.lightning());
		Matrix4f matrix = ps.last().pose();

		for (Beam beam : BEAMS) {
			long age = now - beam.spawnedAtMs();
			float ageFrac = age / (float) LIFETIME_MS;
			float fadeIn = Math.min(1f, age / 60f);
			float fadeOut = ageFrac < 0.55f ? 1f : Math.max(0f, 1f - (ageFrac - 0.55f) / 0.45f);
			float alpha = fadeIn * fadeOut;
			if (alpha <= 0f) {
				continue;
			}
			Vec3 start = beam.start();
			Vec3 end = beam.end();
			Vec3 dir = end.subtract(start);
			double len = dir.length();
			if (len < 1e-3) {
				continue;
			}
			Vec3 unit = dir.scale(1.0 / len);
			Vec3 mid = start.add(end).scale(0.5);
			Vec3 toCam = cam.subtract(mid);
			Vec3 side1 = unit.cross(toCam);
			if (side1.length() < 1e-3) {
				side1 = unit.cross(new Vec3(0, 1, 0));
				if (side1.length() < 1e-3) {
					side1 = new Vec3(1, 0, 0);
				}
			}
			side1 = side1.normalize();
			Vec3 side2 = unit.cross(side1).normalize();

			float pulse = 0.92f + 0.08f * (float) Math.sin(now * 0.014);
			drawCross(buf, matrix, start, end, side1, side2, 1.30 * pulse, 0.32f, 0.05f, 0.55f, 0.55f * alpha);
			drawCross(buf, matrix, start, end, side1, side2, 0.85 * pulse, 0.55f, 0.18f, 0.95f, 0.75f * alpha);
			drawCross(buf, matrix, start, end, side1, side2, 0.45 * pulse, 0.78f, 0.45f, 1f, 0.95f * alpha);
			drawCross(buf, matrix, start, end, side1, side2, 0.20 * pulse, 0.95f, 0.85f, 1f, 1f * alpha);
			drawCross(buf, matrix, start, end, side1, side2, 0.07 * pulse, 1f, 1f, 1f, 1f * alpha);

			double impactR = 1.4 + ageFrac * 1.6;
			drawImpactQuad(buf, matrix, end, side1, side2, impactR, 0.55f, 0.18f, 0.95f, 0.55f * alpha);
			drawImpactQuad(buf, matrix, end, side1, side2, impactR * 0.55, 0.95f, 0.75f, 1f, 0.85f * alpha);

			double muzzleR = 1.0 - Math.min(0.9, ageFrac * 1.2);
			if (muzzleR > 0.05) {
				drawImpactQuad(buf, matrix, start, side1, side2, muzzleR, 0.78f, 0.45f, 1f, 0.85f * alpha);
				drawImpactQuad(buf, matrix, start, side1, side2, muzzleR * 0.55, 1f, 1f, 1f, 1f * alpha);
			}
		}
		ps.popPose();
	}

	private static void drawCross(VertexConsumer buf, Matrix4f matrix, Vec3 a, Vec3 b,
			Vec3 s1, Vec3 s2, double width, float r, float g, float bl, float alpha) {
		drawQuad(buf, matrix, a, b, s1, width, r, g, bl, alpha);
		drawQuad(buf, matrix, a, b, s2, width, r, g, bl, alpha);
	}

	private static void drawQuad(VertexConsumer buf, Matrix4f matrix, Vec3 a, Vec3 b, Vec3 side, double width,
			float r, float g, float bl, float alpha) {
		Vec3 sw = side.scale(width);
		Vec3 a0 = a.add(sw);
		Vec3 a1 = a.subtract(sw);
		Vec3 b0 = b.add(sw);
		Vec3 b1 = b.subtract(sw);
		addV(buf, matrix, a0, r, g, bl, alpha);
		addV(buf, matrix, a1, r, g, bl, alpha);
		addV(buf, matrix, b1, r, g, bl, alpha);
		addV(buf, matrix, b0, r, g, bl, alpha);
		addV(buf, matrix, a0, r, g, bl, alpha);
		addV(buf, matrix, b0, r, g, bl, alpha);
		addV(buf, matrix, b1, r, g, bl, alpha);
		addV(buf, matrix, a1, r, g, bl, alpha);
	}

	private static void drawImpactQuad(VertexConsumer buf, Matrix4f matrix, Vec3 c, Vec3 s1, Vec3 s2,
			double r, float red, float green, float blue, float alpha) {
		Vec3 a0 = c.add(s1.scale(r)).add(s2.scale(r));
		Vec3 a1 = c.add(s1.scale(-r)).add(s2.scale(r));
		Vec3 a2 = c.add(s1.scale(-r)).add(s2.scale(-r));
		Vec3 a3 = c.add(s1.scale(r)).add(s2.scale(-r));
		addV(buf, matrix, a0, red, green, blue, alpha);
		addV(buf, matrix, a1, red, green, blue, alpha);
		addV(buf, matrix, a2, red, green, blue, alpha);
		addV(buf, matrix, a3, red, green, blue, alpha);
	}

	private static void addV(VertexConsumer buf, Matrix4f matrix, Vec3 v,
			float r, float g, float b, float alpha) {
		buf.addVertex(matrix, (float) v.x, (float) v.y, (float) v.z)
				.setColor(r, g, b, alpha);
	}

	private record Beam(Vec3 start, Vec3 end, long spawnedAtMs) {
	}
}
