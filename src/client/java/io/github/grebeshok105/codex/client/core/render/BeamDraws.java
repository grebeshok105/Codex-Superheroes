package io.github.grebeshok105.codex.client.core.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Beam draw bodies, ported byte-identical from the pre-L2 per-hero renderers:
 * {@code laserPair} = old {@code LaserBeamRenderer}, {@code repulsorTracer} = old
 * {@code RepulsorBeamRenderer}, {@code cosmicBeam} = old {@code CosmicBeamRenderer}.
 * The lifetime constants double as the registered {@link BeamStyle} lifetimes.
 */
public final class BeamDraws {
	public static final long LASER_LIFETIME_MS = 220L;
	public static final long REPULSOR_TRAVEL_MS = 70L;
	public static final long REPULSOR_FADE_MS = 220L;
	public static final long REPULSOR_LIFETIME_MS = REPULSOR_TRAVEL_MS + REPULSOR_FADE_MS;
	public static final long COSMIC_LIFETIME_MS = 700L;

	private static final double REPULSOR_HEAD_LEN = 2.4;

	private BeamDraws() {
	}

	/** Homelander eye lasers: two parallel cross-beams from eyeSep-spaced origins. */
	public static void laserPair(WorldRenderContext context, Vec3 start, Vec3 end,
			long ageMs, float ageFrac, long nowMs) {
		float alpha = Math.max(0f, 1f - ageFrac);
		Vec3 dir = end.subtract(start);
		if (dir.lengthSqr() < 1e-6) {
			return;
		}
		dir = dir.normalize();
		Vec3 right = dir.cross(new Vec3(0, 1, 0));
		if (right.lengthSqr() < 1e-6) {
			right = new Vec3(1, 0, 0);
		}
		right = right.normalize();
		double eyeSep = 0.11;
		CrossBeamRenderer.draw(context, start.add(right.scale(-eyeSep)), end, alpha, 0.45f);
		CrossBeamRenderer.draw(context, start.add(right.scale(eyeSep)), end, alpha, 0.45f);
	}

	/** Iron Man repulsor: a tracer head travelling start→end, then an impact ring. */
	public static void repulsorTracer(WorldRenderContext context, Vec3 start, Vec3 end,
			long ageMs, float ageFrac, long nowMs) {
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

		Vec3 dir = end.subtract(start);
		double totalLen = dir.length();
		if (totalLen >= 1e-3) {
			Vec3 unit = dir.scale(1.0 / totalLen);

			double headFrac = Math.min(1.0, ageMs / (double) REPULSOR_TRAVEL_MS);
			Vec3 head = start.add(unit.scale(totalLen * headFrac));
			double trailLen = Math.min(REPULSOR_HEAD_LEN, totalLen * headFrac);
			Vec3 tail = head.subtract(unit.scale(trailLen));

			float fadeAlpha;
			if (ageMs <= REPULSOR_TRAVEL_MS) {
				fadeAlpha = 1f;
			} else {
				fadeAlpha = Math.max(0f, 1f - (ageMs - REPULSOR_TRAVEL_MS) / (float) REPULSOR_FADE_MS);
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

			float pulse = 0.92f + 0.08f * (float) Math.sin(nowMs * 0.022);
			drawCross(buf, matrix, tail, head, side1, side2, 0.30 * pulse, 0.18f, 0.42f, 1f, 0.50f * fadeAlpha);
			drawCross(buf, matrix, tail, head, side1, side2, 0.16 * pulse, 0.30f, 0.65f, 1f, 0.85f * fadeAlpha);
			drawCross(buf, matrix, tail, head, side1, side2, 0.07 * pulse, 0.65f, 0.90f, 1f, 1f * fadeAlpha);
			drawCross(buf, matrix, tail, head, side1, side2, 0.025 * pulse, 1f, 1f, 1f, 1f * fadeAlpha);

			if (ageMs >= REPULSOR_TRAVEL_MS) {
				double ringR = 0.45 + (ageMs - REPULSOR_TRAVEL_MS) / (double) REPULSOR_FADE_MS * 0.6;
				drawImpactQuad(buf, matrix, end, side1, side2, ringR, 0.30f, 0.65f, 1f, 0.5f * fadeAlpha);
			}
		}
		ps.popPose();
	}

	/** Thanos cosmic beam: layered purple cross-beam + impact/muzzle quads. */
	public static void cosmicBeam(WorldRenderContext context, Vec3 start, Vec3 end,
			long ageMs, float ageFrac, long nowMs) {
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

		float fadeIn = Math.min(1f, ageMs / 60f);
		float fadeOut = ageFrac < 0.55f ? 1f : Math.max(0f, 1f - (ageFrac - 0.55f) / 0.45f);
		float alpha = fadeIn * fadeOut;
		if (alpha > 0f) {
			Vec3 dir = end.subtract(start);
			double len = dir.length();
			if (len >= 1e-3) {
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

				float pulse = 0.92f + 0.08f * (float) Math.sin(nowMs * 0.014);
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
}
