package com.example.superheroes.client.hud;

import net.minecraft.client.gui.GuiGraphics;

public final class HudUtil {
	private HudUtil() {
	}

	public static void roundedRectFill(GuiGraphics g, int x, int y, int w, int h, int color) {
		if (w < 6 || h < 6) {
			g.fill(x, y, x + w, y + h, color);
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

	public static void roundedRectGradient(GuiGraphics g, int x, int y, int w, int h, int topColor, int bottomColor) {
		if (w < 6 || h < 6) {
			g.fillGradient(x, y, x + w, y + h, topColor, bottomColor);
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

	public static void roundedRectBorder(GuiGraphics g, int x, int y, int w, int h, int color) {
		if (w < 6 || h < 6) {
			g.fill(x, y, x + w, y + 1, color);
			g.fill(x, y + h - 1, x + w, y + h, color);
			g.fill(x, y, x + 1, y + h, color);
			g.fill(x + w - 1, y, x + w, y + h, color);
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

	public static void dropShadow(GuiGraphics g, int x, int y, int w, int h, int offset, int color) {
		roundedRectFill(g, x + offset, y + offset, w, h, color);
	}

	/**
	 * Неоновая стеклянная панель: тень, градиентный фон, рамка, мягкое
	 * внешнее свечение в три прохода и тонкий световой блик сверху.
	 * Раскладку не меняет — рисует ровно в тех же границах, что и
	 * roundedRectGradient + roundedRectBorder.
	 */
	public static void neonPanel(GuiGraphics g, int x, int y, int w, int h,
			int topColor, int bottomColor, int borderColor, int glowColor) {
		int baseAlpha = (glowColor >>> 24) & 0xFF;
		int glowRgb = glowColor & 0x00FFFFFF;
		int glowFar = (Math.max(6, baseAlpha / 4) << 24) | glowRgb;
		int glowMid = (Math.max(10, baseAlpha / 3) << 24) | glowRgb;
		int glowNear = (Math.max(16, baseAlpha / 2) << 24) | glowRgb;
		roundedRectBorder(g, x - 3, y - 3, w + 6, h + 6, glowFar);
		roundedRectBorder(g, x - 2, y - 2, w + 4, h + 4, glowMid);
		roundedRectBorder(g, x - 1, y - 1, w + 2, h + 2, glowNear);
		dropShadow(g, x, y, w, h, 3, 0x44000000);
		roundedRectGradient(g, x, y, w, h, topColor, bottomColor);
		roundedRectBorder(g, x, y, w, h, borderColor);
		int hl = (Math.max(12, ((borderColor >>> 24) / 3)) << 24) | 0x00FFFFFF;
		g.fill(x + 4, y + 1, x + w - 4, y + 2, hl);
	}

	/**
	 * Тонкая неоновая полоска-акцент (можно использовать под заголовком).
	 */
	public static void neonAccentLine(GuiGraphics g, int x, int y, int w, int color) {
		int a = (color >>> 24) & 0xFF;
		int rgb = color & 0x00FFFFFF;
		int soft = (Math.max(8, a / 3) << 24) | rgb;
		g.fill(x, y, x + w, y + 1, color);
		g.fill(x - 1, y, x, y + 1, soft);
		g.fill(x + w, y, x + w + 1, y + 1, soft);
		g.fill(x + 1, y + 1, x + w - 1, y + 2, soft);
	}
}
