package io.github.grebeshok105.codex.client.hero.regulus.render;

import io.github.grebeshok105.codex.client.ClientHeroState;
import io.github.grebeshok105.codex.client.hero.regulus.state.ClientHeartsState;
import io.github.grebeshok105.codex.hero.regulus.RegulusHero;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.Set;

/**
 * Owner-only little-king highlight: a soft golden corner outline around every entity
 * whose network id arrived in {@code HeartsSyncS2CPayload} (no vanilla glowing — only
 * the owner sees where their hearts live). Same camera-facing line-quad technique as
 * {@code IronManEspRenderer}: no depth test, corner segments, gentle alpha pulse.
 */
public final class RegulusHeartsRenderer {
	private static final int GOLD = 0xFFC400;
	private static final double MAX_DISTANCE = 64.0;

	private RegulusHeartsRenderer() {
	}

	public static void register() {
		WorldRenderEvents.AFTER_TRANSLUCENT.register(RegulusHeartsRenderer::render);
	}

	private static void render(WorldRenderContext ctx) {
		Set<Integer> ids = ClientHeartsState.heartEntityIds();
		if (ids.isEmpty()) {
			return;
		}
		// The payload only reaches the owner; belt-and-braces gate in case a view
		// outlives an untransform before the final empty sync lands.
		if (!RegulusHero.ID.equals(ClientHeroState.data().heroId())) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null) {
			return;
		}
		Vec3 cam = ctx.camera().getPosition();
		float partial = ctx.tickCounter().getGameTimeDeltaPartialTick(true);

		PoseStack ps = ctx.matrixStack();
		ps.pushPose();
		ps.translate(-cam.x, -cam.y, -cam.z);
		Matrix4f matrix = ps.last().pose();

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		RenderSystem.disableDepthTest();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

		// Слабый золотой пульс — контур дышит, а не светит постоянно.
		float pulse = 0.55f + 0.2f * Mth.sin(mc.level.getGameTime() * 0.15f);
		BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		int drawn = 0;
		for (int id : ids) {
			Entity e = mc.level.getEntity(id);
			if (e == null || !e.isAlive() || e.distanceToSqr(cam.x, cam.y, cam.z) > MAX_DISTANCE * MAX_DISTANCE) {
				continue;
			}
			drawCorners(bb, matrix, bounds(e, partial), cam, 0.024, GOLD, pulse);
			drawn++;
		}
		if (drawn > 0) {
			BufferUploader.drawWithShader(bb.buildOrThrow());
		}

		RenderSystem.enableCull();
		RenderSystem.disableBlend();
		RenderSystem.enableDepthTest();
		RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
		ps.popPose();
	}

	private static AABB bounds(Entity e, float partial) {
		double ex = Mth.lerp(partial, e.xOld, e.getX());
		double ey = Mth.lerp(partial, e.yOld, e.getY());
		double ez = Mth.lerp(partial, e.zOld, e.getZ());
		return new AABB(
				ex - e.getBbWidth() / 2.0, ey, ez - e.getBbWidth() / 2.0,
				ex + e.getBbWidth() / 2.0, ey + e.getBbHeight(), ez + e.getBbWidth() / 2.0)
				.inflate(0.06);
	}

	private static void drawCorners(BufferBuilder bb, Matrix4f m, AABB box, Vec3 cam,
			double width, int rgb, float a) {
		Vec3[] c = corners(box);
		double ex = Math.min(0.32, (box.maxX - box.minX) * 0.3);
		double ey = Math.min(0.4, (box.maxY - box.minY) * 0.2);
		double ez = Math.min(0.32, (box.maxZ - box.minZ) * 0.3);
		int[][] edges = {
				{0, 1}, {1, 3}, {3, 2}, {2, 0},
				{4, 5}, {5, 7}, {7, 6}, {6, 4},
				{0, 4}, {1, 5}, {2, 6}, {3, 7}
		};
		for (int[] ed : edges) {
			Vec3 p0 = c[ed[0]];
			Vec3 p1 = c[ed[1]];
			Vec3 dir = p1.subtract(p0);
			double len = dir.length();
			if (len < 1e-4) {
				continue;
			}
			Vec3 u = dir.scale(1.0 / len);
			double seg = axisSeg(u, ex, ey, ez);
			seg = Math.min(seg, len * 0.45);
			drawLine(bb, m, p0, p0.add(u.scale(seg)), cam, width, rgb, a);
			drawLine(bb, m, p1, p1.subtract(u.scale(seg)), cam, width, rgb, a);
		}
	}

	private static double axisSeg(Vec3 u, double ex, double ey, double ez) {
		double ax = Math.abs(u.x), ay = Math.abs(u.y), az = Math.abs(u.z);
		if (ay >= ax && ay >= az) {
			return ey;
		}
		return ax >= az ? ex : ez;
	}

	private static Vec3[] corners(AABB b) {
		return new Vec3[]{
				new Vec3(b.minX, b.minY, b.minZ),
				new Vec3(b.maxX, b.minY, b.minZ),
				new Vec3(b.minX, b.minY, b.maxZ),
				new Vec3(b.maxX, b.minY, b.maxZ),
				new Vec3(b.minX, b.maxY, b.minZ),
				new Vec3(b.maxX, b.maxY, b.minZ),
				new Vec3(b.minX, b.maxY, b.maxZ),
				new Vec3(b.maxX, b.maxY, b.maxZ)
		};
	}

	/** Thin camera-facing line-quad between a and b (same shape as IronManEspRenderer). */
	private static void drawLine(BufferBuilder bb, Matrix4f m, Vec3 a, Vec3 b, Vec3 cam,
			double width, int rgb, float alpha) {
		Vec3 dir = b.subtract(a);
		double len = dir.length();
		if (len < 1e-5) {
			return;
		}
		Vec3 u = dir.scale(1.0 / len);
		Vec3 mid = a.add(b).scale(0.5);
		Vec3 toCam = cam.subtract(mid);
		Vec3 side = u.cross(toCam);
		if (side.length() < 1e-5) {
			side = u.cross(new Vec3(0, 1, 0));
			if (side.length() < 1e-5) {
				side = new Vec3(1, 0, 0);
			}
		}
		side = side.normalize().scale(width);
		float r = ((rgb >> 16) & 0xFF) / 255f;
		float g = ((rgb >> 8) & 0xFF) / 255f;
		float bl = (rgb & 0xFF) / 255f;
		Vec3 a0 = a.add(side);
		Vec3 a1 = a.subtract(side);
		Vec3 b0 = b.add(side);
		Vec3 b1 = b.subtract(side);
		vert(bb, m, a0, r, g, bl, alpha);
		vert(bb, m, a1, r, g, bl, alpha);
		vert(bb, m, b1, r, g, bl, alpha);
		vert(bb, m, b0, r, g, bl, alpha);
	}

	private static void vert(BufferBuilder bb, Matrix4f m, Vec3 v, float r, float g, float b, float a) {
		bb.addVertex(m, (float) v.x, (float) v.y, (float) v.z).setColor(r, g, b, a);
	}
}
