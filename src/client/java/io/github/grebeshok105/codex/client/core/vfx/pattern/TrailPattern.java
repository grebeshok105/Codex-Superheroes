package io.github.grebeshok105.codex.client.core.vfx.pattern;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.grebeshok105.codex.client.core.vfx.VfxEffect;
import io.github.grebeshok105.codex.client.core.vfx.VfxRenderContext;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Camera-facing ribbon drawn through a {@link TrailBuffer}. The owning
 * composition pushes anchor points each tick; {@link #finish()} starts a
 * {@code fadeTicks} fade-out after which the effect is done. Alpha also
 * decays toward the oldest retained point so the tail tapers.
 *
 * <p>{@link #soft} is the opt-in profile for a continuous contrail: the
 * stored spine is Catmull-Rom resampled (×3), the width tapers head→tail,
 * the alpha eases out by age, and each sub-segment is drawn as two crossed
 * textured quads through {@code RenderType.eyes} (additive). The default
 * constructor keeps the flat ribbon strip byte-identical.
 */
public final class TrailPattern implements VfxEffect {
	private final TrailBuffer points;
	private final float width;
	private final int argb;
	private final int fadeTicks;
	/** Non-null when the soft profile was opted into. */
	private final @Nullable SoftProfile soft;

	private int fadeLeft = -1;
	private boolean finished;

	public TrailPattern(int capacity, float width, int argb, int fadeTicks) {
		this(capacity, width, argb, fadeTicks, null);
	}

	private TrailPattern(int capacity, float width, int argb, int fadeTicks,
			@Nullable ResourceLocation softTexture) {
		this.points = new TrailBuffer(capacity);
		this.width = width;
		this.argb = argb;
		this.fadeTicks = Math.max(1, fadeTicks);
		this.soft = softTexture == null ? null : new SoftProfile(softTexture, capacity);
	}

	/**
	 * Soft contrail profile through {@code texture}: Catmull-Rom ×3
	 * subdivision, {@code width} tapering head→tail and crossed quads under a
	 * soft gaussian texture.
	 */
	public static TrailPattern soft(int capacity, float width, int argb, int fadeTicks,
			ResourceLocation texture) {
		return new TrailPattern(capacity, width, argb, fadeTicks, texture);
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
		SoftProfile softProfile = soft;
		if (softProfile != null) {
			renderSoft(ctx, softProfile);
			return;
		}
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

	/**
	 * Soft path: resample the stored spine through {@link TrailGeometry} into
	 * preallocated scratch, then draw every sub-segment as two crossed quads
	 * (X cross-section around the tangent) under the gaussian texture.
	 */
	private void renderSoft(VfxRenderContext ctx, SoftProfile softProfile) {
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

		Vector3f[] spine = softProfile.spine;
		int count = TrailGeometry.subdivide(points::get, points.size(), spine);
		if (count < 2) {
			return;
		}
		float last = count - 1;

		Vec3 cam = ctx.camera().getPosition();
		PoseStack pose = ctx.pose();
		pose.pushPose();
		pose.translate(-cam.x, -cam.y, -cam.z);
		VertexConsumer buf = ctx.buffers().getBuffer(RenderType.eyes(softProfile.texture));
		Matrix4f matrix = pose.last().pose();

		Vector3f tangent = softProfile.tangent;
		Vector3f toCam = softProfile.toCam;
		Vector3f sideA = softProfile.sideA;
		Vector3f sideB = softProfile.sideB;
		for (int i = 0; i + 1 < count; i++) {
			Vector3f newer = spine[i];
			Vector3f older = spine[i + 1];
			tangent.set(older).sub(newer);
			if (tangent.lengthSquared() < 1e-8f) {
				continue;
			}
			toCam.set((float) cam.x - 0.5f * (newer.x + older.x),
					(float) cam.y - 0.5f * (newer.y + older.y),
					(float) cam.z - 0.5f * (newer.z + older.z));
			sideA.set(tangent).cross(toCam);
			if (sideA.lengthSquared() < 1e-8f) {
				continue;
			}
			sideA.normalize();
			sideB.set(tangent).cross(sideA).normalize();
			float t0 = i / last;
			float t1 = (i + 1) / last;
			float w0 = width * 0.5f * TrailGeometry.widthScale(t0);
			float w1 = width * 0.5f * TrailGeometry.widthScale(t1);
			float a0 = baseAlpha * TrailGeometry.fadeAlpha(t0);
			float a1 = baseAlpha * TrailGeometry.fadeAlpha(t1);
			texturedQuad(buf, matrix, newer, older, sideA, w0, w1, r, g, b, a0, a1, t0, t1);
			texturedQuad(buf, matrix, newer, older, sideB, w0, w1, r, g, b, a0, a1, t0, t1);
		}
		pose.popPose();
	}

	/**
	 * One double-sided quad across {@code side}: {@code v} runs 0→1 across the
	 * width so the gaussian texture peaks on the spine; {@code u} is the age
	 * fraction along the trail.
	 */
	private static void texturedQuad(VertexConsumer buf, Matrix4f m, Vector3f newer, Vector3f older,
			Vector3f side, float w0, float w1, float r, float g, float b,
			float a0, float a1, float u0, float u1) {
		texturedVertex(buf, m, newer.x + side.x * w0, newer.y + side.y * w0, newer.z + side.z * w0,
				r, g, b, a0, u0, 0f);
		texturedVertex(buf, m, newer.x - side.x * w0, newer.y - side.y * w0, newer.z - side.z * w0,
				r, g, b, a0, u0, 1f);
		texturedVertex(buf, m, older.x - side.x * w1, older.y - side.y * w1, older.z - side.z * w1,
				r, g, b, a1, u1, 1f);
		texturedVertex(buf, m, newer.x + side.x * w0, newer.y + side.y * w0, newer.z + side.z * w0,
				r, g, b, a0, u0, 0f);
		texturedVertex(buf, m, older.x - side.x * w1, older.y - side.y * w1, older.z - side.z * w1,
				r, g, b, a1, u1, 1f);
		texturedVertex(buf, m, older.x + side.x * w1, older.y + side.y * w1, older.z + side.z * w1,
				r, g, b, a1, u1, 0f);
		// Double-sided so the soft quad stays visible from both faces.
		texturedVertex(buf, m, newer.x - side.x * w0, newer.y - side.y * w0, newer.z - side.z * w0,
				r, g, b, a0, u0, 1f);
		texturedVertex(buf, m, newer.x + side.x * w0, newer.y + side.y * w0, newer.z + side.z * w0,
				r, g, b, a0, u0, 0f);
		texturedVertex(buf, m, older.x + side.x * w1, older.y + side.y * w1, older.z + side.z * w1,
				r, g, b, a1, u1, 0f);
		texturedVertex(buf, m, newer.x - side.x * w0, newer.y - side.y * w0, newer.z - side.z * w0,
				r, g, b, a0, u0, 1f);
		texturedVertex(buf, m, older.x + side.x * w1, older.y + side.y * w1, older.z + side.z * w1,
				r, g, b, a1, u1, 0f);
		texturedVertex(buf, m, older.x - side.x * w1, older.y - side.y * w1, older.z - side.z * w1,
				r, g, b, a1, u1, 1f);
	}

	private static void texturedVertex(VertexConsumer buf, Matrix4f m,
			float x, float y, float z, float r, float g, float b, float a, float u, float v) {
		buf.addVertex(m, x, y, z).setColor(r, g, b, a).setUv(u, v);
	}

	/** Preallocated scratch for the soft profile — bounded, never grown per frame. */
	private static final class SoftProfile {
		private final ResourceLocation texture;
		private final Vector3f[] spine;
		private final Vector3f tangent = new Vector3f();
		private final Vector3f toCam = new Vector3f();
		private final Vector3f sideA = new Vector3f();
		private final Vector3f sideB = new Vector3f();

		private SoftProfile(ResourceLocation texture, int storedCapacity) {
			this.texture = texture;
			this.spine = new Vector3f[TrailGeometry.subdividedCapacity(storedCapacity)];
			for (int i = 0; i < spine.length; i++) {
				spine[i] = new Vector3f();
			}
		}
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
