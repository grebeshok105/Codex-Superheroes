package io.github.grebeshok105.codex.client.core.vfx.pattern;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.grebeshok105.codex.client.core.vfx.VfxEffect;
import io.github.grebeshok105.codex.client.core.vfx.VfxRenderContext;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Camera-facing ribbon drawn through a {@link TrailBuffer}. The owning
 * composition pushes anchor points each tick; {@link #finish()} starts a
 * {@code fadeTicks} fade-out after which the effect is done. Alpha also
 * decays toward the oldest retained point so the tail tapers.
 */
public final class TrailPattern implements VfxEffect {
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

		int count = points.size();
		for (int i = 0; i + 1 < count; i++) {
			Vec3 newer = points.get(i);
			Vec3 older = points.get(i + 1);
			Vec3 segment = older.subtract(newer);
			Vec3 toCam = cam.subtract(newer.add(older).scale(0.5));
			Vec3 side = segment.cross(toCam);
			if (side.lengthSqr() < 1e-6) {
				continue;
			}
			side = side.normalize().scale(width * 0.5);
			float alpha = baseAlpha * (1f - (float) i / count);
			quad(buf, matrix, newer, older, side, r, g, b, alpha);
		}
		pose.popPose();
	}

	private static void quad(VertexConsumer buf, Matrix4f m, Vec3 newer, Vec3 older, Vec3 side,
			float r, float g, float b, float alpha) {
		Vec3 n0 = newer.add(side);
		Vec3 n1 = newer.subtract(side);
		Vec3 o0 = older.add(side);
		Vec3 o1 = older.subtract(side);
		vertex(buf, m, n0, r, g, b, alpha);
		vertex(buf, m, n1, r, g, b, alpha);
		vertex(buf, m, o1, r, g, b, alpha);
		vertex(buf, m, n0, r, g, b, alpha);
		vertex(buf, m, o1, r, g, b, alpha);
		vertex(buf, m, o0, r, g, b, alpha);
		// Double-sided so the ribbon stays visible from both faces.
		vertex(buf, m, n1, r, g, b, alpha);
		vertex(buf, m, n0, r, g, b, alpha);
		vertex(buf, m, o0, r, g, b, alpha);
		vertex(buf, m, n1, r, g, b, alpha);
		vertex(buf, m, o0, r, g, b, alpha);
		vertex(buf, m, o1, r, g, b, alpha);
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
