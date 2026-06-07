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
}
