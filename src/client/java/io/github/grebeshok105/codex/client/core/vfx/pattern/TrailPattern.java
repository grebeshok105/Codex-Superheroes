package io.github.grebeshok105.codex.client.core.vfx.pattern;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.grebeshok105.codex.client.core.vfx.VfxEffect;
import io.github.grebeshok105.codex.client.core.vfx.VfxRenderContext;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Soft vapor ribbon drawn through a {@link TrailBuffer}. The owning
 * composition pushes anchor points each tick; {@link #finish()} starts a
 * {@code fadeTicks} fade-out after which the effect is done.
 *
 * <p>Rendering is deliberately blur-first so the trail reads as one
 * continuous contrail rather than a chain of quads: the raw control points
 * are resampled with a Catmull-Rom spline ({@value #SUBDIV} samples per
 * segment), and every cross-section is a vertical alpha gradient — a bright
 * spine flanked by fully transparent edges — drawn twice: a wide faint halo
 * pass plus a narrow core pass. Alpha also decays toward the oldest retained
 * point with a smoothstep so the tail tapers without a visible cutoff.
 */
public final class TrailPattern implements VfxEffect {
	/** Spline samples emitted between each pair of control points. */
	private static final int SUBDIV = 5;
	/** Halo width multiplier relative to the core ribbon width. */
	private static final float HALO_WIDTH_MUL = 3.0f;
	/** Halo alpha relative to the core alpha. */
	private static final float HALO_ALPHA_SCALE = 0.35f;

	private final TrailBuffer points;
	private final float width;
	private final int argb;
	private final int fadeTicks;

	private int fadeLeft = -1;
	private boolean finished;

	public TrailPattern(int capacity, float width, int argb, int fadeTicks) {
		this.points = new TrailBuffer(capacity);
		this.width = width;
		this.argb = argb;
		this.fadeTicks = Math.max(1, fadeTicks);
	}

	public void push(Vec3 point) {
		points.push(point);
	}

	public int size() {
		return points.size();
	}

	/** Begins the release fade; idempotent. */
	public void finish() {
		if (fadeLeft < 0) {
			fadeLeft = fadeTicks;
		}
	}

	@Override
	public void tick() {
		if (fadeLeft > 0) {
			fadeLeft--;
			if (fadeLeft == 0) {
				finished = true;
			}
		}
	}

	@Override
	public void render(VfxRenderContext ctx) {
		if (ctx.buffers() == null || points.size() < 2) {
			return;
		}
		float fade = fadeLeft < 0 ? 1f : (float) fadeLeft / fadeTicks;
		float baseAlpha = ((argb >>> 24) & 0xFF) / 255f * fade;
		if (baseAlpha <= 0f) {
			return;
		}
		float r = ((argb >>> 16) & 0xFF) / 255f;
		float g = ((argb >>> 8) & 0xFF) / 255f;
		float b = (argb & 0xFF) / 255f;

		Vec3 cam = ctx.camera().getPosition();
		PoseStack pose = ctx.pose();
		pose.pushPose();
		pose.translate(-cam.x, -cam.y, -cam.z);
		VertexConsumer buf = ctx.buffers().getBuffer(RenderType.lightning());
		Matrix4f matrix = pose.last().pose();

		int samples = 1 + (points.size() - 1) * SUBDIV;
		for (int s = 0; s < samples; s++) {
			Vec3 here = sample(s);
			if (s + 1 >= samples) {
				break;
			}
			Vec3 next = sample(s + 1);
			Vec3 segment = next.subtract(here);
			Vec3 toCam = cam.subtract(here.add(next).scale(0.5));
			Vec3 side = segment.cross(toCam);
			if (side.lengthSqr() < 1e-8) {
				continue;
			}
			side = side.normalize();
			// Smoothstep tail taper: full alpha near the head, easing to zero.
			float t = 1f - (float) s / samples;
			float alpha = baseAlpha * t * t * (3f - 2f * t);
			pass(buf, matrix, here, next, side, width * HALO_WIDTH_MUL, r, g, b,
					alpha * HALO_ALPHA_SCALE);
			pass(buf, matrix, here, next, side, width, r, g, b, alpha);
		}
		pose.popPose();
	}

	/** Catmull-Rom sample {@code s} of the smoothed polyline (newest → oldest). */
	private Vec3 sample(int s) {
		int i = s / SUBDIV;
		float t = (s % SUBDIV) / (float) SUBDIV;
		Vec3 p1 = points.get(i);
		Vec3 p2 = points.get(i + 1);
		Vec3 p0 = i > 0 ? points.get(i - 1) : p1;
		Vec3 p3 = i + 2 < points.size() ? points.get(i + 2) : p2;
		return catmullRom(p0, p1, p2, p3, t);
	}

	private static Vec3 catmullRom(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, float t) {
		double t2 = t * t;
		double t3 = t2 * t;
		return new Vec3(
				0.5 * (2 * p1.x + (-p0.x + p2.x) * t + (2 * p0.x - 5 * p1.x + 4 * p2.x - p3.x) * t2
						+ (-p0.x + 3 * p1.x - 3 * p2.x + p3.x) * t3),
				0.5 * (2 * p1.y + (-p0.y + p2.y) * t + (2 * p0.y - 5 * p1.y + 4 * p2.y - p3.y) * t2
						+ (-p0.y + 3 * p1.y - 3 * p2.y + p3.y) * t3),
				0.5 * (2 * p1.z + (-p0.z + p2.z) * t + (2 * p0.z - 5 * p1.z + 4 * p2.z - p3.z) * t2
						+ (-p0.z + 3 * p1.z - 3 * p2.z + p3.z) * t3));
	}

	/**
	 * One gradient strip between two samples: two quads, spine → edge alpha
	 * falls to zero so the ribbon has no hard outline.
	 */
	private static void pass(VertexConsumer buf, Matrix4f m, Vec3 a, Vec3 b, Vec3 side,
			float stripWidth, float r, float g, float bl, float alpha) {
		if (alpha <= 0f) {
			return;
		}
		Vec3 half = side.scale(stripWidth * 0.5);
		Vec3 aL = a.add(half);
		Vec3 aR = a.subtract(half);
		Vec3 bL = b.add(half);
		Vec3 bR = b.subtract(half);
		// Left gradient half (edge α=0 → spine α=alpha).
		strip(buf, m, aL, bL, a, b, r, g, bl, alpha, true);
		// Right gradient half (spine → edge).
		strip(buf, m, a, b, aR, bR, r, g, bl, alpha, false);
	}

	private static void strip(VertexConsumer buf, Matrix4f m, Vec3 e0, Vec3 e1, Vec3 s0, Vec3 s1,
			float r, float g, float b, float alpha, boolean edgeFirst) {
		float aE = edgeFirst ? 0f : alpha;
		float aS = edgeFirst ? alpha : 0f;
		vertex(buf, m, e0, r, g, b, aE);
		vertex(buf, m, e1, r, g, b, aE);
		vertex(buf, m, s1, r, g, b, aS);
		vertex(buf, m, e0, r, g, b, aE);
		vertex(buf, m, s1, r, g, b, aS);
		vertex(buf, m, s0, r, g, b, aS);
		// Double-sided so the ribbon stays visible from both faces.
		vertex(buf, m, s0, r, g, b, aS);
		vertex(buf, m, s1, r, g, b, aS);
		vertex(buf, m, e1, r, g, b, aE);
		vertex(buf, m, s0, r, g, b, aS);
		vertex(buf, m, e1, r, g, b, aE);
		vertex(buf, m, e0, r, g, b, aE);
	}

	private static void vertex(VertexConsumer buf, Matrix4f m, Vec3 v, float r, float g, float b, float a) {
		buf.addVertex(m, (float) v.x, (float) v.y, (float) v.z).setColor(r, g, b, a);
	}

	@Override
	public boolean done() {
		return finished;
	}

	@Override
	public void cancel() {
		finished = true;
	}
}
