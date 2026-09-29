package io.github.grebeshok105.codex.client.core.vfx.pattern;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.grebeshok105.codex.client.core.vfx.VfxEffect;
import io.github.grebeshok105.codex.client.core.vfx.VfxEffectFactory;
import io.github.grebeshok105.codex.client.core.vfx.VfxRenderContext;
import io.github.grebeshok105.codex.client.core.vfx.params.VfxParams;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Expanding ring around an origin (explosions, landings, claps, sonic
 * barrier pops). Radius grows 0 → {@code radius} over {@code durationTicks}
 * while the alpha fades out. The ring lies in the plane perpendicular to
 * {@code normal} — horizontal for the default UP normal. Tuning keys:
 * {@code radius}, {@code durationTicks}, {@code color} ({@code #AARRGGBB}),
 * {@code band} (ring thickness fraction, default 0.15).
 */
public final class ShockwavePattern implements VfxEffect {
	private static final int SEGMENTS = 40;
	private static final Vec3 UP = new Vec3(0, 1, 0);

	private final Vec3 center;
	private final float radius;
	private final int durationTicks;
	private final int argb;
	private final float band;
	private final Vec3 basisU;
	private final Vec3 basisV;
	private final double lift;

	private int age;

	public ShockwavePattern(Vec3 center, float radius, int durationTicks, int argb, float band) {
		this(center, radius, durationTicks, argb, band, UP);
	}

	/** Ring plane perpendicular to {@code normal} (normalized here; degenerate falls back to UP). */
	public ShockwavePattern(Vec3 center, float radius, int durationTicks, int argb, float band, Vec3 normal) {
		this.center = center;
		this.radius = radius;
		this.durationTicks = Math.max(1, durationTicks);
		this.argb = argb;
		this.band = band;
		Vec3 n = normal.lengthSqr() < 1e-6 ? UP : normal.normalize();
		Vec3 u = n.cross(UP);
		if (u.lengthSqr() < 1e-6) {
			u = n.cross(new Vec3(1, 0, 0));
		}
		this.basisU = u.normalize();
		this.basisV = n.cross(basisU).normalize();
		this.lift = 0.02;
	}

	/** Factory reading tuning from the spawn's {@link VfxParams}; {@code scale} multiplies the radius. */
	public static VfxEffectFactory factory() {
		return spawn -> {
			VfxParams p = spawn.params();
			float radius = p.number("radius", 4f) * Math.max(0.1f, spawn.scale());
			int duration = Math.max(1, (int) p.number("durationTicks", 12f));
			int color = p.color("color", 0x60FFFFFF);
			float band = p.number("band", 0.15f);
			return new ShockwavePattern(spawn.origin(), radius, duration, color, band);
		};
	}

	@Override
	public void tick() {
		age++;
	}

	@Override
	public void render(VfxRenderContext ctx) {
		if (ctx.buffers() == null) {
			return;
		}
		float progress = Math.min(1f, (age + ctx.partialTick()) / durationTicks);
		float outer = radius * progress;
		if (outer <= 0f) {
			return;
		}
		float inner = outer * (1f - band);
		float alpha = ((argb >>> 24) & 0xFF) / 255f * (1f - progress);
		if (alpha <= 0f) {
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
		for (int i = 0; i < SEGMENTS; i++) {
			double a0 = Math.PI * 2 * i / SEGMENTS;
			double a1 = Math.PI * 2 * (i + 1) / SEGMENTS;
			Vec3 o0 = offset(a0, outer);
			Vec3 o1 = offset(a1, outer);
			Vec3 i0 = offset(a0, inner);
			Vec3 i1 = offset(a1, inner);
			quad(buf, matrix, o0, o1, i1, i0, r, g, b, alpha);
			quad(buf, matrix, i0, i1, o1, o0, r, g, b, alpha);
		}
		pose.popPose();
	}

	private Vec3 offset(double angle, double ringRadius) {
		return center
				.add(basisU.scale(Math.cos(angle) * ringRadius))
				.add(basisV.scale(Math.sin(angle) * ringRadius))
				.add(0, lift, 0);
	}

	private static void quad(VertexConsumer buf, Matrix4f m, Vec3 a, Vec3 b, Vec3 c, Vec3 d,
			float r, float g, float bl, float alpha) {
		vertex(buf, m, a, r, g, bl, alpha);
		vertex(buf, m, b, r, g, bl, alpha);
		vertex(buf, m, c, r, g, bl, alpha);
		vertex(buf, m, a, r, g, bl, alpha);
		vertex(buf, m, c, r, g, bl, alpha);
		vertex(buf, m, d, r, g, bl, alpha);
	}

	private static void vertex(VertexConsumer buf, Matrix4f m, Vec3 v, float r, float g, float b, float a) {
		buf.addVertex(m, (float) v.x, (float) v.y, (float) v.z).setColor(r, g, b, a);
	}

	@Override
	public boolean done() {
		return age >= durationTicks;
	}
}
