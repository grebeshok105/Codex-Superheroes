package com.example.superheroes.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * Wild HUD renderer: рисует элементы интерфейса через собственные SDF-шейдеры
 * ({@link WildShaders}) — настоящие гладкие скругления, мягкое неоновое глоу с
 * экспоненциальным затуханием (как box-shadow в CSS) и антиалиасинг через
 * fwidth/smoothstep на любом gui scale.
 *
 * Координаты — обычные gui-юниты GuiGraphics; локальные координаты пикселей
 * передаются через UV0, так что fwidth() в шейдере автоматически даёт ширину
 * сглаживания в реальных пикселях фреймбуфера.
 */
public final class WildRenderer {
	public static final float TWO_PI = (float) (Math.PI * 2.0);

	private WildRenderer() {
	}

	// ===================== panels =====================

	/**
	 * Скруглённая панель: градиентная заливка top->bottom, обводка, внешнее глоу.
	 */
	public static void panel(GuiGraphics g, float x, float y, float w, float h, float radius,
			int fillTop, int fillBottom, int borderColor, float borderWidth,
			int glowColor, float glowRadius) {
		ShaderInstance sh = WildShaders.rect();
		if (sh == null || w <= 0 || h <= 0) {
			return;
		}
		float r = Math.min(radius, Math.min(w, h) / 2f);
		float pad = Math.max(glowRadius, 1f);
		setColor(sh, "FillTop", fillTop);
		setColor(sh, "FillBottom", fillBottom);
		setColor(sh, "BorderColor", borderColor);
		setColor(sh, "GlowColor", glowColor);
		sh.safeGetUniform("HalfSize").set(w / 2f, h / 2f);
		sh.safeGetUniform("Radius").set(r);
		sh.safeGetUniform("BorderWidth").set(borderWidth);
		sh.safeGetUniform("GlowRadius").set(glowRadius);
		quadCentered(g, sh, x + w / 2f, y + h / 2f, w / 2f + pad, h / 2f + pad);
	}

	/** Сплошная скруглённая заливка без обводки и глоу. */
	public static void fill(GuiGraphics g, float x, float y, float w, float h, float radius, int color) {
		panel(g, x, y, w, h, radius, color, color, 0, 0f, 0, 0f);
	}

	/** Скруглённая обводка без заливки. */
	public static void border(GuiGraphics g, float x, float y, float w, float h, float radius,
			int color, float width) {
		panel(g, x, y, w, h, radius, 0, 0, color, width, 0, 0f);
	}

	/**
	 * Тонкий бар со скруглёнными концами и светящейся заливкой
	 * (radius = h/2 даёт «капсулу»).
	 */
	public static void bar(GuiGraphics g, float x, float y, float w, float h, int color, int glowColor) {
		panel(g, x, y, w, h, h / 2f, color, color, 0, 0f, glowColor, Math.max(2.5f, h));
	}

	// ===================== circles =====================

	/**
	 * Круглый орб: заливка + кольцо по краю + внешнее глоу.
	 */
	public static void orb(GuiGraphics g, float cx, float cy, float radius,
			int fillColor, int ringColor, float ringWidth, int glowColor, float glowRadius) {
		arc(g, cx, cy, radius, ringWidth, 0f, TWO_PI, fillColor, ringColor, glowColor, glowRadius);
	}

	/** Кольцо (без заливки). */
	public static void ring(GuiGraphics g, float cx, float cy, float radius, float width,
			int color, int glowColor, float glowRadius) {
		arc(g, cx, cy, radius, width, 0f, TWO_PI, 0, color, glowColor, glowRadius);
	}

	/**
	 * Дуговое кольцо. Углы в радианах, 0 = вправо, pi/2 = вверх (CCW, экранный
	 * y-вниз уже учтён в шейдере). Для дуги «по часовой от 12 часов» используй
	 * {@link #clockArc}.
	 */
	public static void arc(GuiGraphics g, float cx, float cy, float radius, float ringWidth,
			float startAngle, float endAngle, int fillColor, int ringColor,
			int glowColor, float glowRadius) {
		ShaderInstance sh = WildShaders.circle();
		if (sh == null || radius <= 0) {
			return;
		}
		float pad = Math.max(glowRadius, 1f);
		setColor(sh, "FillColor", fillColor);
		setColor(sh, "RingColor", ringColor);
		setColor(sh, "GlowColor", glowColor);
		sh.safeGetUniform("Radius").set(radius);
		sh.safeGetUniform("RingWidth").set(ringWidth);
		sh.safeGetUniform("GlowRadius").set(glowRadius);
		sh.safeGetUniform("Arc").set(startAngle, endAngle);
		quadCentered(g, sh, cx, cy, radius + pad, radius + pad);
	}

	/**
	 * Дуга прогресса: начинается сверху (12 часов) и идёт по часовой стрелке.
	 *
	 * @param progress 0..1 — закрашиваемая доля окружности
	 */
	public static void clockArc(GuiGraphics g, float cx, float cy, float radius, float ringWidth,
			float progress, int ringColor, int glowColor, float glowRadius) {
		float span = TWO_PI * Math.max(0f, Math.min(1f, progress));
		if (span <= 0.0001f) {
			return;
		}
		float top = (float) (Math.PI / 2.0);
		// по часовой = в сторону уменьшения угла (CCW-математика)
		arc(g, cx, cy, radius, ringWidth, top - span, top, 0, ringColor, glowColor, glowRadius);
	}

	// ===================== sectors (radial menu) =====================

	/**
	 * Кольцевой сектор с гладкими кромками, обводкой и внешним глоу.
	 * Углы — экранные радианы (0 = вправо, растут по часовой, y-вниз),
	 * как в существующем RadialMenuHud.
	 */
	public static void sector(GuiGraphics g, float cx, float cy, float innerRadius, float outerRadius,
			float screenA0, float screenA1, int fillColor, int borderColor, float borderWidth,
			int glowColor, float glowRadius) {
		ShaderInstance sh = WildShaders.sector();
		if (sh == null || outerRadius <= 0) {
			return;
		}
		// экранные углы (y-вниз, по часовой) -> математические CCW: a_math = -a_screen
		float a0 = -screenA1;
		float a1 = -screenA0;
		float pad = Math.max(glowRadius, 1f);
		setColor(sh, "FillColor", fillColor);
		setColor(sh, "BorderColor", borderColor);
		setColor(sh, "GlowColor", glowColor);
		sh.safeGetUniform("InnerRadius").set(innerRadius);
		sh.safeGetUniform("OuterRadius").set(outerRadius);
		sh.safeGetUniform("Arc").set(a0, a1);
		sh.safeGetUniform("BorderWidth").set(borderWidth);
		sh.safeGetUniform("GlowRadius").set(glowRadius);
		quadCentered(g, sh, cx, cy, outerRadius + pad, outerRadius + pad);
	}

	// ===================== textured icon in a circle =====================

	/**
	 * Иконка-текстура, обрезанная по кругу (шейдерная маска с AA).
	 *
	 * @param darken 0..1 — затемнение (для кулдауна)
	 */
	public static void iconCircle(GuiGraphics g, ResourceLocation texture, float cx, float cy,
			float size, float maskRadius, float darken) {
		ShaderInstance sh = WildShaders.iconCircle();
		if (sh == null || size <= 0) {
			return;
		}
		flushBatched(g);
		RenderSystem.setShaderTexture(0, texture);
		sh.safeGetUniform("QuadSize").set(size);
		sh.safeGetUniform("MaskRadius").set(maskRadius);
		sh.safeGetUniform("Darken").set(darken);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		// ВАЖНО: глубину выключаем явно. Иначе в 3-м лице (где ванильный crosshair
		// не рисуется и не сбрасывает depth-стейт) эти SDF-квады проваливают depth-тест
		// и весь HUD пропадает. Гарантируем отрисовку во всех ракурсах камеры.
		RenderSystem.disableDepthTest();
		RenderSystem.setShader(() -> sh);
		Matrix4f mat = g.pose().last().pose();
		float half = size / 2f;
		BufferBuilder buf = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		buf.addVertex(mat, cx - half, cy - half, 0f).setUv(0f, 0f);
		buf.addVertex(mat, cx - half, cy + half, 0f).setUv(0f, 1f);
		buf.addVertex(mat, cx + half, cy + half, 0f).setUv(1f, 1f);
		buf.addVertex(mat, cx + half, cy - half, 0f).setUv(1f, 0f);
		BufferUploader.drawWithShader(buf.buildOrThrow());
		RenderSystem.disableBlend();
		RenderSystem.enableDepthTest();
	}

	// ===================== internals =====================

	/**
	 * Квад вокруг центра (cx,cy) с полуразмерами hw,hh; UV0 несёт локальные
	 * координаты в gui-пикселях относительно центра.
	 */
	private static void quadCentered(GuiGraphics g, ShaderInstance sh, float cx, float cy, float hw, float hh) {
		flushBatched(g);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		// глубину выключаем явно — гарантирует отрисовку HUD в 3-м лице (см. iconCircle)
		RenderSystem.disableDepthTest();
		RenderSystem.setShader(() -> sh);
		Matrix4f mat = g.pose().last().pose();
		BufferBuilder buf = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		buf.addVertex(mat, cx - hw, cy - hh, 0f).setUv(-hw, -hh);
		buf.addVertex(mat, cx - hw, cy + hh, 0f).setUv(-hw, hh);
		buf.addVertex(mat, cx + hw, cy + hh, 0f).setUv(hw, hh);
		buf.addVertex(mat, cx + hw, cy - hh, 0f).setUv(hw, -hh);
		BufferUploader.drawWithShader(buf.buildOrThrow());
		RenderSystem.disableBlend();
		RenderSystem.enableDepthTest();
	}

	/** Сбрасывает отложенные батчи GuiGraphics, чтобы сохранить порядок слоёв. */
	private static void flushBatched(GuiGraphics g) {
		g.flush();
	}

	private static void setColor(ShaderInstance sh, String uniform, int argb) {
		float a = ((argb >>> 24) & 0xFF) / 255f;
		float r = ((argb >>> 16) & 0xFF) / 255f;
		float gg = ((argb >>> 8) & 0xFF) / 255f;
		float b = (argb & 0xFF) / 255f;
		sh.safeGetUniform(uniform).set(r, gg, b, a);
	}
}
