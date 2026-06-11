package com.example.superheroes.client.hud;

import net.minecraft.client.gui.GuiGraphics;

public final class HudUtil {
	private HudUtil() {
	}

	public static void roundedRectFill(GuiGraphics g, int x, int y, int w, int h, int color) {
		if (w < 4 || h < 4) {
			g.fill(x, y, x + w, y + h, color);
			return;
		}
		g.fill(x + 2, y, x + w - 2, y + h, color);
		g.fill(x, y + 2, x + 2, y + h - 2, color);
		g.fill(x + w - 2, y + 2, x + w, y + h - 2, color);
		g.fill(x + 1, y + 1, x + 2, y + 2, color);
		g.fill(x + w - 2, y + 1, x + w - 1, y + 2, color);
		g.fill(x + 1, y + h - 2, x + 2, y + h - 1, color);
		g.fill(x + w - 2, y + h - 2, x + w - 1, y + h - 1, color);
	}

	public static void roundedRectGradient(GuiGraphics g, int x, int y, int w, int h, int topColor, int bottomColor) {
		if (w < 4 || h < 4) {
			g.fillGradient(x, y, x + w, y + h, topColor, bottomColor);
			return;
		}
		g.fillGradient(x + 2, y, x + w - 2, y + h, topColor, bottomColor);
		g.fillGradient(x, y + 2, x + 2, y + h - 2, topColor, bottomColor);
		g.fillGradient(x + w - 2, y + 2, x + w, y + h - 2, topColor, bottomColor);
		g.fillGradient(x + 1, y + 1, x + 2, y + 2, topColor, bottomColor);
		g.fillGradient(x + w - 2, y + 1, x + w - 1, y + 2, topColor, bottomColor);
		g.fillGradient(x + 1, y + h - 2, x + 2, y + h - 1, topColor, bottomColor);
		g.fillGradient(x + w - 2, y + h - 2, x + w - 1, y + h - 1, topColor, bottomColor);
	}

	public static void roundedRectBorder(GuiGraphics g, int x, int y, int w, int h, int color) {
		if (w < 4 || h < 4) {
			g.fill(x, y, x + w, y + 1, color);
			g.fill(x, y + h - 1, x + w, y + h, color);
			g.fill(x, y, x + 1, y + h, color);
			g.fill(x + w - 1, y, x + w, y + h, color);
			return;
		}
		g.fill(x + 2, y, x + w - 2, y + 1, color);
		g.fill(x + 2, y + h - 1, x + w - 2, y + h, color);
		g.fill(x, y + 2, x + 1, y + h - 2, color);
		g.fill(x + w - 1, y + 2, x + w, y + h - 2, color);
		g.fill(x + 1, y + 1, x + 2, y + 2, color);
		g.fill(x + w - 2, y + 1, x + w - 1, y + 2, color);
		g.fill(x + 1, y + h - 2, x + 2, y + h - 1, color);
		g.fill(x + w - 2, y + h - 2, x + w - 1, y + h - 1, color);
	}

	public static void dropShadow(GuiGraphics g, int x, int y, int w, int h, int offset, int color) {
		roundedRectFill(g, x + offset, y + offset, w, h, color);
	}

	/**
	 * Неоновая стеклянная панель: тень, градиентный фон, рамка, мягкое
	 * внешнее свечение в два прохода и тонкий световой блик сверху.
	 * Раскладку не меняет — рисует ровно в тех же границах, что и
	 * roundedRectGradient + roundedRectBorder.
	 */
	public static void neonPanel(GuiGraphics g, int x, int y, int w, int h,
			int topColor, int bottomColor, int borderColor, int glowColor) {
		// внешнее свечение (от дальнего слоя к ближнему)
		int glowFar = (Math.max(8, ((glowColor >>> 24) / 3)) << 24) | (glowColor & 0x00FFFFFF);
		roundedRectBorder(g, x - 2, y - 2, w + 4, h + 4, glowFar);
		roundedRectBorder(g, x - 1, y - 1, w + 2, h + 2, glowColor);
		dropShadow(g, x, y, w, h, 3, 0x44000000);
		roundedRectGradient(g, x, y, w, h, topColor, bottomColor);
		roundedRectBorder(g, x, y, w, h, borderColor);
		// стеклянный блик по верхней кромке
		int hl = (Math.max(10, ((borderColor >>> 24) / 4)) << 24) | 0x00FFFFFF;
		g.fill(x + 3, y + 1, x + w - 3, y + 2, hl);
	}
}
