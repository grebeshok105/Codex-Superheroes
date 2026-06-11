package com.example.superheroes.client.hud;

import com.example.superheroes.ModId;
import com.example.superheroes.client.render.WildRenderer;
import com.example.superheroes.client.render.WildShaders;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

/**
 * Примитивы HUD. Основной путь — шейдерный {@link WildRenderer} (SDF-скругления,
 * настоящее мягкое глоу); если шейдеры не скомпилировались, падаем обратно на
 * старую ступенчатую отрисовку через fill.
 */
public final class HudUtil {
	/** Кастомный векторный шрифт HUD (Monocraft, провайдер superheroes:hud). */
	public static final ResourceLocation HUD_FONT = ModId.of("hud");
	private static final Style HUD_STYLE = Style.EMPTY.withFont(HUD_FONT);

	private HudUtil() {
	}

	// ===================== текст =====================

	/** Литеральный текст HUD-шрифтом. */
	public static MutableComponent text(String s) {
		return Component.literal(s).withStyle(HUD_STYLE);
	}

	/** Любой компонент (например translatable) HUD-шрифтом. */
	public static MutableComponent text(Component c) {
		return c.copy().withStyle(HUD_STYLE);
	}

	// ===================== формы =====================

	/** Радиус скругления в стиле Wild Glass: крупный у панелей, аккуратный у мелочи. */
	public static float wildRadius(int w, int h) {
		float r = Math.min(w, h) * 0.22f;
		return Math.max(2f, Math.min(r, 12f));
	}

	public static void roundedRectFill(GuiGraphics g, int x, int y, int w, int h, int color) {
		if (WildShaders.rectReady()) {
			WildRenderer.fill(g, x, y, w, h, wildRadius(w, h), color);
			return;
		}
		legacyRoundedRectFill(g, x, y, w, h, color);
	}

	public static void roundedRectGradient(GuiGraphics g, int x, int y, int w, int h, int topColor, int bottomColor) {
		if (WildShaders.rectReady()) {
			WildRenderer.panel(g, x, y, w, h, wildRadius(w, h), topColor, bottomColor, 0, 0f, 0, 0f);
			return;
		}
		legacyRoundedRectGradient(g, x, y, w, h, topColor, bottomColor);
	}

	public static void roundedRectBorder(GuiGraphics g, int x, int y, int w, int h, int color) {
		if (WildShaders.rectReady()) {
			WildRenderer.border(g, x, y, w, h, wildRadius(w, h), color, 1.1f);
			return;
		}
		legacyRoundedRectBorder(g, x, y, w, h, color);
	}

	public static void dropShadow(GuiGraphics g, int x, int y, int w, int h, int offset, int color) {
		roundedRectFill(g, x + offset, y + offset, w, h, color);
	}

	/**
	 * Неоновая стеклянная панель Wild Glass: дымчатое стекло с градиентом,
	 * тонкая обводка и широкий мягкий ореол цветом героя (шейдерное глоу).
	 * Геометрию не меняет — рисует ровно в тех же границах, что и раньше.
	 */
	public static void neonPanel(GuiGraphics g, int x, int y, int w, int h,
			int topColor, int bottomColor, int borderColor, int glowColor) {
		if (WildShaders.rectReady()) {
			int glowA = Math.min(255, ((glowColor >>> 24) & 0xFF) * 2);
			int glow = (glowA << 24) | (glowColor & 0x00FFFFFF);
			WildRenderer.panel(g, x, y, w, h, wildRadius(w, h),
					topColor, bottomColor, borderColor, 1.1f, glow, 9f);
			return;
		}
		legacyNeonPanel(g, x, y, w, h, topColor, bottomColor, borderColor, glowColor);
	}

	/**
	 * Тонкая неоновая полоска-акцент (можно использовать под заголовком).
	 */
	public static void neonAccentLine(GuiGraphics g, int x, int y, int w, int color) {
		if (WildShaders.rectReady()) {
			WildRenderer.bar(g, x, y, w, 1.2f, color, color);
			return;
		}
		int a = (color >>> 24) & 0xFF;
		int rgb = color & 0x00FFFFFF;
		int soft = (Math.max(8, a / 3) << 24) | rgb;
		g.fill(x, y, x + w, y + 1, color);
		g.fill(x - 1, y, x, y + 1, soft);
		g.fill(x + w, y, x + w + 1, y + 1, soft);
		g.fill(x + 1, y + 1, x + w - 1, y + 2, soft);
	}

	// ===================== legacy fill fallback =====================

	private static boolean isSmall(int w, int h) {
		return Math.min(w, h) < 14;
	}

	private static void legacyRoundedRectFill(GuiGraphics g, int x, int y, int w, int h, int color) {
		if (w < 6 || h < 6) {
			g.fill(x, y, x + w, y + h, color);
			return;
		}
		if (isSmall(w, h)) {
			g.fill(x + 2, y, x + w - 2, y + h, color);
			g.fill(x, y + 2, x + 2, y + h - 2, color);
			g.fill(x + w - 2, y + 2, x + w, y + h - 2, color);
			g.fill(x + 1, y + 1, x + 2, y + 2, color);
			g.fill(x + w - 2, y + 1, x + w - 1, y + 2, color);
			g.fill(x + 1, y + h - 2, x + 2, y + h - 1, color);
			g.fill(x + w - 2, y + h - 2, x + w - 1, y + h - 1, color);
			return;
		}
		g.fill(x + 3, y, x + w - 3, y + h, color);
		g.fill(x, y + 3, x + 3, y + h - 3, color);
		g.fill(x + w - 3, y + 3, x + w, y + h - 3, color);
		g.fill(x + 1, y + 1, x + 3, y + 3, color);
		g.fill(x + w - 3, y + 1, x + w - 1, y + 3, color);
		g.fill(x + 1, y + h - 3, x + 3, y + h - 1, color);
		g.fill(x + w - 3, y + h - 3, x + w - 1, y + h - 1, color);
		g.fill(x + 2, y + 1, x + 3, y + 2, color);
		g.fill(x + w - 3, y + 1, x + w - 2, y + 2, color);
		g.fill(x + 2, y + h - 2, x + 3, y + h - 1, color);
		g.fill(x + w - 3, y + h - 2, x + w - 2, y + h - 1, color);
	}

	private static void legacyRoundedRectGradient(GuiGraphics g, int x, int y, int w, int h, int topColor, int bottomColor) {
		if (w < 6 || h < 6) {
			g.fillGradient(x, y, x + w, y + h, topColor, bottomColor);
			return;
		}
		if (isSmall(w, h)) {
			g.fillGradient(x + 2, y, x + w - 2, y + h, topColor, bottomColor);
			g.fillGradient(x, y + 2, x + 2, y + h - 2, topColor, bottomColor);
			g.fillGradient(x + w - 2, y + 2, x + w, y + h - 2, topColor, bottomColor);
			return;
		}
		g.fillGradient(x + 3, y, x + w - 3, y + h, topColor, bottomColor);
		g.fillGradient(x, y + 3, x + 3, y + h - 3, topColor, bottomColor);
		g.fillGradient(x + w - 3, y + 3, x + w, y + h - 3, topColor, bottomColor);
		g.fillGradient(x + 1, y + 1, x + 3, y + 3, topColor, bottomColor);
		g.fillGradient(x + w - 3, y + 1, x + w - 1, y + 3, topColor, bottomColor);
		g.fillGradient(x + 1, y + h - 3, x + 3, y + h - 1, topColor, bottomColor);
		g.fillGradient(x + w - 3, y + h - 3, x + w - 1, y + h - 1, topColor, bottomColor);
	}

	private static void legacyRoundedRectBorder(GuiGraphics g, int x, int y, int w, int h, int color) {
		if (w < 6 || h < 6) {
			g.fill(x, y, x + w, y + 1, color);
			g.fill(x, y + h - 1, x + w, y + h, color);
			g.fill(x, y, x + 1, y + h, color);
			g.fill(x + w - 1, y, x + w, y + h, color);
			return;
		}
		if (isSmall(w, h)) {
			g.fill(x + 2, y, x + w - 2, y + 1, color);
			g.fill(x + 2, y + h - 1, x + w - 2, y + h, color);
			g.fill(x, y + 2, x + 1, y + h - 2, color);
			g.fill(x + w - 1, y + 2, x + w, y + h - 2, color);
			g.fill(x + 1, y + 1, x + 2, y + 2, color);
			g.fill(x + w - 2, y + 1, x + w - 1, y + 2, color);
			g.fill(x + 1, y + h - 2, x + 2, y + h - 1, color);
			g.fill(x + w - 2, y + h - 2, x + w - 1, y + h - 1, color);
			return;
		}
		g.fill(x + 3, y, x + w - 3, y + 1, color);
		g.fill(x + 3, y + h - 1, x + w - 3, y + h, color);
		g.fill(x, y + 3, x + 1, y + h - 3, color);
		g.fill(x + w - 1, y + 3, x + w, y + h - 3, color);
		g.fill(x + 1, y + 2, x + 2, y + 3, color);
		g.fill(x + 2, y + 1, x + 3, y + 2, color);
		g.fill(x + w - 3, y + 1, x + w - 2, y + 2, color);
		g.fill(x + w - 2, y + 2, x + w - 1, y + 3, color);
		g.fill(x + 1, y + h - 3, x + 2, y + h - 2, color);
		g.fill(x + 2, y + h - 2, x + 3, y + h - 1, color);
		g.fill(x + w - 3, y + h - 2, x + w - 2, y + h - 1, color);
		g.fill(x + w - 2, y + h - 3, x + w - 1, y + h - 2, color);
	}

	private static void legacyNeonPanel(GuiGraphics g, int x, int y, int w, int h,
			int topColor, int bottomColor, int borderColor, int glowColor) {
		int baseAlpha = (glowColor >>> 24) & 0xFF;
		int glowRgb = glowColor & 0x00FFFFFF;
		int glowFar = (Math.max(6, baseAlpha / 4) << 24) | glowRgb;
		int glowMid = (Math.max(10, baseAlpha / 3) << 24) | glowRgb;
		int glowNear = (Math.max(16, baseAlpha / 2) << 24) | glowRgb;
		legacyRoundedRectBorder(g, x - 3, y - 3, w + 6, h + 6, glowFar);
		legacyRoundedRectBorder(g, x - 2, y - 2, w + 4, h + 4, glowMid);
		legacyRoundedRectBorder(g, x - 1, y - 1, w + 2, h + 2, glowNear);
		legacyRoundedRectFill(g, x + 3, y + 3, w, h, 0x44000000);
		legacyRoundedRectGradient(g, x, y, w, h, topColor, bottomColor);
		legacyRoundedRectBorder(g, x, y, w, h, borderColor);
		int hl = (Math.max(12, ((borderColor >>> 24) / 3)) << 24) | 0x00FFFFFF;
		g.fill(x + 4, y + 1, x + w - 4, y + 2, hl);
	}
}
