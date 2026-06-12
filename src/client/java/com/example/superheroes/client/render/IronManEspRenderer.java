package com.example.superheroes.client.render;

import com.example.superheroes.client.ClientHeroState;
import com.example.superheroes.client.RemoteHeroSkins;
import com.example.superheroes.hero.IronManHero;
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
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Target-ESP Железного Человека (J.A.R.V.I.S. tactical scan), радиус 50 блоков.
 *
 * <p>Логика целей по запросу:
 * <ul>
 *   <li><b>Игроки-герои</b> (трансформированные) — всегда, сквозь стены (wallhack).</li>
 *   <li><b>Враждебные мобы</b> — только в поле зрения и при прямой видимости
 *       (raycast), БЕЗ просвечивания сквозь стены.</li>
 *   <li><b>Железный голем</b> — только если он агрессивен (есть цель/атаковал),
 *       по тем же правилам видимости, что и мобы.</li>
 *   <li>Обычные игроки и мирные мобы — не подсвечиваются.</li>
 * </ul>
 * Акцент — красный (J.A.R.V.I.S. combat), с кратким описанием цели у бокса.
 */
public final class IronManEspRenderer {
	public enum Mode {
		OFF, CORNERS, BOX
	}

	private static Mode mode = Mode.CORNERS;
	private static final double RANGE = 50.0;
	private static final int RED = 0xE2342B;
	private static final int RED_BRIGHT = 0xFF5A4A;
	private static final int GOLD = 0xFFC400;
	// поле зрения для мобов: dot(look, toTarget) должен быть выше порога
	private static final double FOV_DOT = 0.40;

	private IronManEspRenderer() {
	}

	/** Категория цели для ESP. */
	private enum Kind {
		HERO, MOB
	}

	private record Target(LivingEntity entity, Kind kind) {
	}

	public static void register() {
		WorldRenderEvents.AFTER_TRANSLUCENT.register(IronManEspRenderer::render);
	}

	public static Mode mode() {
		return mode;
	}

	public static void cycleMode() {
		mode = switch (mode) {
			case OFF -> Mode.CORNERS;
			case CORNERS -> Mode.BOX;
			case BOX -> Mode.OFF;
		};
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null) {
			mc.player.displayClientMessage(Component.translatable("hud.superheroes.esp.mode",
					Component.translatable("hud.superheroes.esp.mode." + mode.name().toLowerCase(java.util.Locale.ROOT))), true);
		}
	}

	private static void render(WorldRenderContext ctx) {
		if (mode == Mode.OFF) {
			return;
		}
		if (!ClientHeroState.data().hasHero() || !IronManHero.ID.equals(ClientHeroState.data().heroId())) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		Player self = mc.player;
		if (self == null || mc.level == null) {
			return;
		}
		float partial = ctx.tickCounter().getGameTimeDeltaPartialTick(true);
		Vec3 cam = ctx.camera().getPosition();
		Vec3 eye = self.getEyePosition(partial);
		Vec3 look = self.getViewVector(partial).normalize();

		List<LivingEntity> raw = mc.level.getEntitiesOfClass(LivingEntity.class,
				self.getBoundingBox().inflate(RANGE),
				e -> e != self && e.isAlive() && !e.isSpectator());
		if (raw.isEmpty()) {
			return;
		}

		List<Target> heroes = new ArrayList<>();
		List<Target> mobs = new ArrayList<>();
		for (LivingEntity e : raw) {
			double ex = Mth.lerp(partial, e.xOld, e.getX());
			double ey = Mth.lerp(partial, e.yOld, e.getY());
			double ez = Mth.lerp(partial, e.zOld, e.getZ());
			if (e instanceof Player p) {
				// только трансформированные игроки-герои; обычные — нет
				if (RemoteHeroSkins.get(p.getUUID()) != null) {
					heroes.add(new Target(e, Kind.HERO));
				}
				continue;
			}
			boolean hostile = e instanceof Enemy;
			boolean angryGolem = e instanceof IronGolem golem && golem.getTarget() != null;
			if (!hostile && !angryGolem) {
				continue;
			}
			// мобы/голем — только в поле зрения И при прямой видимости (без wallhack)
			Vec3 center = new Vec3(ex, ey + e.getBbHeight() * 0.5, ez);
			Vec3 toE = center.subtract(eye);
			double d = toE.length();
			if (d < 1e-3 || d > RANGE) {
				continue;
			}
			if (look.dot(toE.scale(1.0 / d)) < FOV_DOT) {
				continue;
			}
			if (!hasLineOfSight(mc, self, eye, center)) {
				continue;
			}
			mobs.add(new Target(e, Kind.MOB));
		}
		if (heroes.isEmpty() && mobs.isEmpty()) {
			return;
		}

		PoseStack ps = ctx.matrixStack();
		ps.pushPose();
		ps.translate(-cam.x, -cam.y, -cam.z);
		Matrix4f matrix = ps.last().pose();

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

		// --- проход 1: мобы/голем — С тестом глубины (прячутся за стенами) ---
		if (!mobs.isEmpty()) {
			RenderSystem.enableDepthTest();
			BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
			for (Target t : mobs) {
				emitBox(ctx, bb, matrix, t.entity(), cam, partial);
			}
			BufferUploader.drawWithShader(bb.buildOrThrow());
		}

		// --- проход 2: игроки-герои — БЕЗ теста глубины (сквозь стены) ---
		if (!heroes.isEmpty()) {
			RenderSystem.disableDepthTest();
			BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
			for (Target t : heroes) {
				emitBox(ctx, bb, matrix, t.entity(), cam, partial);
			}
			BufferUploader.drawWithShader(bb.buildOrThrow());
		}

		RenderSystem.enableCull();
		RenderSystem.disableBlend();
		RenderSystem.enableDepthTest();
		RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
		ps.popPose();

		drawLabels(ctx, heroes, cam, partial, mc.font);
		drawLabels(ctx, mobs, cam, partial, mc.font);
	}

	/** Прямая видимость до точки (raycast по блокам-коллайдерам). */
	private static boolean hasLineOfSight(Minecraft mc, Player self, Vec3 eye, Vec3 target) {
		BlockHitResult hit = mc.level.clip(new ClipContext(eye, target,
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, self));
		return hit.getType() == HitResult.Type.MISS
				|| hit.getLocation().distanceToSqr(target) < 1.0;
	}

	/** Рисует рамку (углы/бокс) + HP-полосу для одной цели, акцент — красный. */
	private static void emitBox(WorldRenderContext ctx, BufferBuilder bb, Matrix4f matrix, LivingEntity e, Vec3 cam, float partial) {
		double ex = Mth.lerp(partial, e.xOld, e.getX());
		double ey = Mth.lerp(partial, e.yOld, e.getY());
		double ez = Mth.lerp(partial, e.zOld, e.getZ());
		AABB box = new AABB(
				ex - e.getBbWidth() / 2.0, ey, ez - e.getBbWidth() / 2.0,
				ex + e.getBbWidth() / 2.0, ey + e.getBbHeight(), ez + e.getBbWidth() / 2.0)
				.inflate(0.08);
		float hpFrac = Mth.clamp(e.getHealth() / Math.max(1f, e.getMaxHealth()), 0f, 1f);
		if (mode == Mode.BOX) {
			drawBoxEdges(bb, matrix, box, cam, 0.018, RED_BRIGHT, 0.9f);
		} else {
			drawCorners(bb, matrix, box, cam, 0.024, RED_BRIGHT, 0.95f);
		}
		drawHealthBar(bb, matrix, ctx, box, hpFrac);
	}

	// ===================== labels (J.A.R.V.I.S. описание цели) ===========
	private static void drawLabels(WorldRenderContext ctx, List<Target> targets, Vec3 cam, float partial, Font font) {
		PoseStack ps = ctx.matrixStack();
		Font.DisplayMode seeThrough = Font.DisplayMode.SEE_THROUGH;
		for (Target t : targets) {
			LivingEntity e = t.entity();
			double ex = Mth.lerp(partial, e.xOld, e.getX());
			double ey = Mth.lerp(partial, e.yOld, e.getY());
			double ez = Mth.lerp(partial, e.zOld, e.getZ());
			double dist = Math.sqrt(e.distanceToSqr(cam.x, cam.y, cam.z));
			if (dist > RANGE) {
				continue;
			}
			float hpFrac = Mth.clamp(e.getHealth() / Math.max(1f, e.getMaxHealth()), 0f, 1f);
			// верхняя строка — имя (для героя: имя героя), нижняя — дескриптор J.A.R.V.I.S.
			String name = displayName(e).toUpperCase(java.util.Locale.ROOT);
			String desc = descriptor(e, t.kind()) + "  //  " + (int) dist + "M  "
					+ Math.round(hpFrac * 100f) + "%";

			ps.pushPose();
			ps.translate(ex - cam.x, ey + e.getBbHeight() + 0.6 - cam.y, ez - cam.z);
			ps.mulPose(ctx.camera().rotation());
			ps.scale(-0.025f, -0.025f, 0.025f);
			Matrix4f m = ps.last().pose();
			int wName = font.width(name);
			int wDesc = font.width(desc);
			// имя — ярко-красное
			font.drawInBatch(Component.literal(name), -wName / 2f, -10f, 0xFF000000 | (RED_BRIGHT & 0xFFFFFF), false,
					m, ctx.consumers(), seeThrough, 0x90000000, 0xF000F0);
			// дескриптор — золотой, мельче по смыслу (J.A.R.V.I.S. readout)
			font.drawInBatch(Component.literal(desc), -wDesc / 2f, 1f, 0xFF000000 | (GOLD & 0xFFFFFF), false,
					m, ctx.consumers(), seeThrough, 0x90000000, 0xF000F0);
			ps.popPose();
		}
	}

	private static String displayName(LivingEntity e) {
		if (e instanceof Player p) {
			ResourceLocation heroId = RemoteHeroSkins.get(p.getUUID());
			if (heroId != null) {
				String key = "hero.superheroes." + heroId.getPath();
				Component c = Component.translatable(key);
				String s = c.getString();
				if (!s.equals(key)) {
					return p.getName().getString() + " / " + s;
				}
			}
		}
		return e.getName().getString();
	}

	private static String descriptor(LivingEntity e, Kind kind) {
		if (kind == Kind.HERO) {
			return "HERO UNIT";
		}
		if (e instanceof IronGolem) {
			return "SENTRY // HOSTILE";
		}
		return "HOSTILE";
	}

	// ===================== geometry =====================
	private static void drawBoxEdges(BufferBuilder bb, Matrix4f m, AABB box, Vec3 cam, double width, int rgb, float a) {
		Vec3[] c = corners(box);
		// 12 рёбер
		int[][] edges = {
				{0, 1}, {1, 3}, {3, 2}, {2, 0}, // низ
				{4, 5}, {5, 7}, {7, 6}, {6, 4}, // верх
				{0, 4}, {1, 5}, {2, 6}, {3, 7}  // стойки
		};
		for (int[] ed : edges) {
			drawLine(bb, m, c[ed[0]], c[ed[1]], cam, width, rgb, a);
		}
	}

	private static void drawCorners(BufferBuilder bb, Matrix4f m, AABB box, Vec3 cam, double width, int rgb, float a) {
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

	/** Тонкая камера-ориентированная линия-квад между a и b. */
	private static void drawLine(BufferBuilder bb, Matrix4f m, Vec3 a, Vec3 b, Vec3 cam, double width, int rgb, float alpha) {
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

	private static void drawHealthBar(BufferBuilder bb, Matrix4f m, WorldRenderContext ctx, AABB box, float hpFrac) {
		Vec3 right = vec(ctx.camera().getLeftVector()).scale(-1);
		Vec3 up = vec(ctx.camera().getUpVector());
		if (right.lengthSqr() < 1e-6) {
			right = new Vec3(1, 0, 0);
		}
		double cx = (box.minX + box.maxX) / 2.0;
		double cy = box.maxY + 0.32;
		double cz = (box.minZ + box.maxZ) / 2.0;
		Vec3 center = new Vec3(cx, cy, cz);
		double halfW = 0.5;
		double halfH = 0.05;
		// фон
		quadBillboard(bb, m, center, right, up, halfW + 0.03, halfH + 0.02, 0.05f, 0.05f, 0.06f, 0.7f);
		// заполнение
		int col = healthColor(hpFrac);
		float r = ((col >> 16) & 0xFF) / 255f, g = ((col >> 8) & 0xFF) / 255f, b = (col & 0xFF) / 255f;
		double fillW = (halfW * 2) * hpFrac;
		Vec3 leftEdge = center.subtract(right.scale(halfW));
		Vec3 fillCenter = leftEdge.add(right.scale(fillW / 2.0));
		quadBillboard(bb, m, fillCenter, right, up, fillW / 2.0, halfH, r, g, b, 0.9f);
	}

	private static void quadBillboard(BufferBuilder bb, Matrix4f m, Vec3 center, Vec3 right, Vec3 up,
			double hw, double hh, float r, float g, float b, float a) {
		Vec3 rw = right.scale(hw);
		Vec3 uh = up.scale(hh);
		Vec3 p0 = center.subtract(rw).add(uh);
		Vec3 p1 = center.subtract(rw).subtract(uh);
		Vec3 p2 = center.add(rw).subtract(uh);
		Vec3 p3 = center.add(rw).add(uh);
		vert(bb, m, p0, r, g, b, a);
		vert(bb, m, p1, r, g, b, a);
		vert(bb, m, p2, r, g, b, a);
		vert(bb, m, p3, r, g, b, a);
	}

	private static Vec3 vec(org.joml.Vector3f v) {
		return new Vec3(v.x(), v.y(), v.z());
	}

	private static int healthColor(float hp) {
		if (hp > 0.5f) {
			return lerpColor(0xFFD34A, 0x57FF6B, (hp - 0.5f) * 2f);
		}
		return lerpColor(0xFF3030, 0xFFD34A, hp * 2f);
	}

	private static int lerpColor(int c0, int c1, float t) {
		t = Mth.clamp(t, 0f, 1f);
		int r = Math.round(((c0 >> 16) & 0xFF) + (((c1 >> 16) & 0xFF) - ((c0 >> 16) & 0xFF)) * t);
		int g = Math.round(((c0 >> 8) & 0xFF) + (((c1 >> 8) & 0xFF) - ((c0 >> 8) & 0xFF)) * t);
		int b = Math.round((c0 & 0xFF) + ((c1 & 0xFF) - (c0 & 0xFF)) * t);
		return (r << 16) | (g << 8) | b;
	}

	private static void vert(BufferBuilder bb, Matrix4f m, Vec3 v, float r, float g, float b, float a) {
		bb.addVertex(m, (float) v.x, (float) v.y, (float) v.z).setColor(r, g, b, a);
	}
}
