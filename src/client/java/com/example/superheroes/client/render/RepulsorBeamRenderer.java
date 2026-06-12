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

public final class RepulsorBeamRenderer {
	private static final long TRAVEL_MS = 70L;
	private static final long FADE_MS = 220L;
	private static final long TOTAL_MS = TRAVEL_MS + FADE_MS;
	private static final double TRACER_HEAD_LEN = 2.4;
	private static final List<Tracer> TRACERS = new ArrayList<>();

	private RepulsorBeamRenderer() {
	}

	public static void register() {
		WorldRenderEvents.AFTER_TRANSLUCENT.register(RepulsorBeamRenderer::render);
	}

	public static void add(Vec3 start, Vec3 end) {
		com.example.superheroes.client.ClientRepulsorChargeState.flash();
		TRACERS.add(new Tracer(start, end, System.currentTimeMillis()));
	}

	private static void render(WorldRenderContext context) {
		long now = System.currentTimeMillis();
		Iterator<Tracer> it = TRACERS.iterator();
		while (it.hasNext()) {
			if (now - it.next().spawnedAtMs() > TOTAL_MS) {
				it.remove();
			}
		}
		if (TRACERS.isEmpty()) {
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

		for (Tracer t : TRACERS) {
			long age = now - t.spawnedAtMs();
			Vec3 dir = t.end().subtract(t.start());
			double totalLen = dir.length();
			if (totalLen < 1e-3) {
				continue;
			}
			Vec3 unit = dir.scale(1.0 / totalLen);

			double headFrac = Math.min(1.0, age / (double) TRAVEL_MS);
			Vec3 head = t.start().add(unit.scale(totalLen * headFrac));
			double trailLen = Math.min(TRACER_HEAD_LEN, totalLen * headFrac);
			Vec3 tail = head.subtract(unit.scale(trailLen));

			float fadeAlpha;
			if (age <= TRAVEL_MS) {
				fadeAlpha = 1f;
			} else {
				fadeAlpha = Math.max(0f, 1f - (age - TRAVEL_MS) / (float) FADE_MS);
			}

			Vec3 mid = head.add(tail).scale(0.5);
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

			float pulse = 0.92f + 0.08f * (float) Math.sin(now * 0.022);
			drawCross(buf, matrix, tail, head, side1, side2, 0.30 * pulse, 0.18f, 0.42f, 1f, 0.50f * fadeAlpha);
			drawCross(buf, matrix, tail, head, side1, side2, 0.16 * pulse, 0.30f, 0.65f, 1f, 0.85f * fadeAlpha);
			drawCross(buf, matrix, tail, head, side1, side2, 0.07 * pulse, 0.65f, 0.90f, 1f, 1f * fadeAlpha);
			drawCross(buf, matrix, tail, head, side1, side2, 0.025 * pulse, 1f, 1f, 1f, 1f * fadeAlpha);

			if (age >= TRAVEL_MS) {
				double ringR = 0.45 + (age - TRAVEL_MS) / (double) FADE_MS * 0.6;
				drawImpactQuad(buf, matrix, t.end(), side1, side2, ringR, 0.30f, 0.65f, 1f, 0.5f * fadeAlpha);
			}
		}
		ps.popPose();
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

	private static void addV(VertexConsumer buf, Matrix4f matrix, Vec3 v, float r, float g, float b, float alpha) {
		buf.addVertex(matrix, (float) v.x, (float) v.y, (float) v.z).setColor(r, g, b, alpha);
	}

	private record Tracer(Vec3 start, Vec3 end, long spawnedAtMs) {
	}
}
