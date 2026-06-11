package com.example.superheroes.client.hud;

import com.example.superheroes.hero.HeroHudConfig;
import net.minecraft.client.gui.GuiGraphics;

public final class HudIcons {
	private HudIcons() {
	}

	public static void drawEnergyIcon(GuiGraphics g, int x, int y, int size, HeroHudConfig.EnergyIconType type, int color) {
		switch (type) {
			case SUN -> drawSun(g, x, y, size, color);
			case LIGHTNING -> drawLightning(g, x, y, size, color);
			case FLAME -> drawFlame(g, x, y, size, color);
			case SKULL -> drawSkull(g, x, y, size, color);
			case SHADOW -> drawShadow(g, x, y, size, color);
			case REACTOR -> drawReactor(g, x, y, size, color);
			case SHIELD -> drawShield(g, x, y, size, color);
			case COSMIC -> drawCosmic(g, x, y, size, color);
			case SWORD -> drawSword(g, x, y, size, color);
			case MAGIC -> drawMagic(g, x, y, size, color);
			case LION -> drawLion(g, x, y, size, color);
			case FIST -> drawFist(g, x, y, size, color);
			case LEAF -> drawLeaf(g, x, y, size, color);
			case SPIRAL -> drawSpiral(g, x, y, size, color);
			case ICE -> drawIce(g, x, y, size, color);
			case BEAST -> drawBeast(g, x, y, size, color);
			default -> drawGeneric(g, x, y, size, color);
		}
	}

	private static void drawSun(GuiGraphics g, int x, int y, int s, int c) {
		int u = Math.max(1, s / 8);
		int cx = x + s / 2;
		int cy = y + s / 2;
		int r = s / 4;
		g.fill(cx - r, cy - r, cx + r, cy + r, c);
		g.fill(cx - u, y, cx + u, y + r, c);
		g.fill(cx - u, cy + r, cx + u, y + s, c);
		g.fill(x, cy - u, x + r, cy + u, c);
		g.fill(cx + r, cy - u, x + s, cy + u, c);
		g.fill(x + u, y + u, x + 2 * u, y + 2 * u, c);
		g.fill(x + s - 2 * u, y + u, x + s - u, y + 2 * u, c);
		g.fill(x + u, y + s - 2 * u, x + 2 * u, y + s - u, c);
		g.fill(x + s - 2 * u, y + s - 2 * u, x + s - u, y + s - u, c);
	}

	private static void drawLightning(GuiGraphics g, int x, int y, int s, int c) {
		int u = Math.max(1, s / 8);
		g.fill(x + 3 * u, y, x + 6 * u, y + u, c);
		g.fill(x + 2 * u, y + u, x + 5 * u, y + 2 * u, c);
		g.fill(x + u, y + 2 * u, x + 4 * u, y + 3 * u, c);
		g.fill(x + 2 * u, y + 3 * u, x + 6 * u, y + 4 * u, c);
		g.fill(x + 3 * u, y + 4 * u, x + 5 * u, y + 5 * u, c);
		g.fill(x + 2 * u, y + 5 * u, x + 4 * u, y + 6 * u, c);
		g.fill(x + u, y + 6 * u, x + 3 * u, y + 7 * u, c);
		g.fill(x + 2 * u, y + 7 * u, x + 3 * u, y + 8 * u, c);
	}

	private static void drawFlame(GuiGraphics g, int x, int y, int s, int c) {
		int u = Math.max(1, s / 8);
		g.fill(x + 3 * u, y, x + 5 * u, y + u, c);
		g.fill(x + 2 * u, y + u, x + 6 * u, y + 3 * u, c);
		g.fill(x + u, y + 3 * u, x + 7 * u, y + 5 * u, c);
		g.fill(x + u, y + 5 * u, x + 7 * u, y + 7 * u, c);
		g.fill(x + 2 * u, y + 7 * u, x + 6 * u, y + 8 * u, c);
	}

	private static void drawSkull(GuiGraphics g, int x, int y, int s, int c) {
		int u = Math.max(1, s / 8);
		g.fill(x + 2 * u, y, x + 6 * u, y + u, c);
		g.fill(x + u, y + u, x + 7 * u, y + 5 * u, c);
		g.fill(x + 2 * u, y + 2 * u, x + 3 * u, y + 4 * u, (c & 0xFF000000));
		g.fill(x + 5 * u, y + 2 * u, x + 6 * u, y + 4 * u, (c & 0xFF000000));
		g.fill(x + 2 * u, y + 5 * u, x + 6 * u, y + 6 * u, c);
		g.fill(x + 3 * u, y + 6 * u, x + 5 * u, y + 8 * u, c);
		g.fill(x + 3 * u, y + 7 * u, x + 4 * u, y + 8 * u, (c & 0xFF000000));
	}

	private static void drawShadow(GuiGraphics g, int x, int y, int s, int c) {
		int u = Math.max(1, s / 8);
		g.fill(x + 3 * u, y, x + 5 * u, y + 2 * u, c);
		g.fill(x + u, y + 2 * u, x + 7 * u, y + 4 * u, c);
		g.fill(x + 2 * u, y + 4 * u, x + 6 * u, y + 6 * u, c);
		g.fill(x + 3 * u, y + 6 * u, x + 5 * u, y + 8 * u, c);
	}

	private static void drawReactor(GuiGraphics g, int x, int y, int s, int c) {
		int u = Math.max(1, s / 8);
		g.fill(x + 2 * u, y + u, x + 6 * u, y + 2 * u, c);
		g.fill(x + u, y + 2 * u, x + 7 * u, y + 6 * u, c);
		g.fill(x + 2 * u, y + 6 * u, x + 6 * u, y + 7 * u, c);
		g.fill(x + 3 * u, y + 3 * u, x + 5 * u, y + 5 * u, (c & 0x00FFFFFF) | 0xFF000000);
	}

	private static void drawShield(GuiGraphics g, int x, int y, int s, int c) {
		int u = Math.max(1, s / 8);
		g.fill(x + u, y, x + 7 * u, y + u, c);
		g.fill(x, y + u, x + 8 * u, y + 4 * u, c);
		g.fill(x + u, y + 4 * u, x + 7 * u, y + 6 * u, c);
		g.fill(x + 2 * u, y + 6 * u, x + 6 * u, y + 7 * u, c);
		g.fill(x + 3 * u, y + 7 * u, x + 5 * u, y + 8 * u, c);
	}

	private static void drawCosmic(GuiGraphics g, int x, int y, int s, int c) {
		int u = Math.max(1, s / 8);
		g.fill(x + 3 * u, y, x + 5 * u, y + u, c);
		g.fill(x + u, y + 2 * u, x + 7 * u, y + 3 * u, c);
		g.fill(x + 2 * u, y + u, x + 6 * u, y + 7 * u, c);
		g.fill(x + u, y + 5 * u, x + 7 * u, y + 6 * u, c);
		g.fill(x + 3 * u, y + 7 * u, x + 5 * u, y + 8 * u, c);
	}

	private static void drawSword(GuiGraphics g, int x, int y, int s, int c) {
		int u = Math.max(1, s / 8);
		g.fill(x + 3 * u, y, x + 5 * u, y + 5 * u, c);
		g.fill(x + u, y + 3 * u, x + 7 * u, y + 4 * u, c);
		g.fill(x + 3 * u, y + 5 * u, x + 5 * u, y + 6 * u, c);
		g.fill(x + 2 * u, y + 6 * u, x + 6 * u, y + 7 * u, c);
		g.fill(x + 3 * u, y + 7 * u, x + 5 * u, y + 8 * u, c);
	}

	private static void drawMagic(GuiGraphics g, int x, int y, int s, int c) {
		int u = Math.max(1, s / 8);
		g.fill(x + 3 * u, y, x + 5 * u, y + u, c);
		g.fill(x + 2 * u, y + u, x + 6 * u, y + 2 * u, c);
		g.fill(x + u, y + 2 * u, x + 3 * u, y + 5 * u, c);
		g.fill(x + 5 * u, y + 2 * u, x + 7 * u, y + 5 * u, c);
		g.fill(x + 3 * u, y + 4 * u, x + 5 * u, y + 6 * u, c);
		g.fill(x + 2 * u, y + 6 * u, x + 6 * u, y + 7 * u, c);
		g.fill(x + 3 * u, y + 7 * u, x + 5 * u, y + 8 * u, c);
	}

	private static void drawLion(GuiGraphics g, int x, int y, int s, int c) {
		int u = Math.max(1, s / 8);
		g.fill(x, y + u, x + 8 * u, y + 2 * u, c);
		g.fill(x + u, y + 2 * u, x + 7 * u, y + 6 * u, c);
		g.fill(x + 2 * u, y + 6 * u, x + 6 * u, y + 8 * u, c);
		g.fill(x + 3 * u, y, x + 5 * u, y + u, c);
	}

	private static void drawFist(GuiGraphics g, int x, int y, int s, int c) {
		int u = Math.max(1, s / 8);
		g.fill(x + u, y + u, x + 7 * u, y + 3 * u, c);
		g.fill(x + u, y + 3 * u, x + 7 * u, y + 7 * u, c);
		g.fill(x + 2 * u, y + 7 * u, x + 6 * u, y + 8 * u, c);
	}

	private static void drawLeaf(GuiGraphics g, int x, int y, int s, int c) {
		int u = Math.max(1, s / 8);
		g.fill(x + 4 * u, y, x + 6 * u, y + u, c);
		g.fill(x + 2 * u, y + u, x + 7 * u, y + 3 * u, c);
		g.fill(x + u, y + 3 * u, x + 6 * u, y + 5 * u, c);
		g.fill(x + 2 * u, y + 5 * u, x + 5 * u, y + 7 * u, c);
		g.fill(x + 3 * u, y + 7 * u, x + 4 * u, y + 8 * u, c);
	}

	private static void drawSpiral(GuiGraphics g, int x, int y, int s, int c) {
		int u = Math.max(1, s / 8);
		g.fill(x + 2 * u, y, x + 6 * u, y + u, c);
		g.fill(x + u, y + u, x + 2 * u, y + 3 * u, c);
		g.fill(x + 6 * u, y + u, x + 7 * u, y + 4 * u, c);
		g.fill(x + 3 * u, y + 2 * u, x + 5 * u, y + 3 * u, c);
		g.fill(x + 5 * u, y + 3 * u, x + 6 * u, y + 5 * u, c);
		g.fill(x + 3 * u, y + 5 * u, x + 5 * u, y + 6 * u, c);
		g.fill(x + u, y + 4 * u, x + 2 * u, y + 7 * u, c);
		g.fill(x + 2 * u, y + 7 * u, x + 6 * u, y + 8 * u, c);
		g.fill(x + 6 * u, y + 5 * u, x + 7 * u, y + 7 * u, c);
	}

	private static void drawIce(GuiGraphics g, int x, int y, int s, int c) {
		int u = Math.max(1, s / 8);
		g.fill(x + 3 * u, y, x + 5 * u, y + 8 * u, c);
		g.fill(x, y + 3 * u, x + 8 * u, y + 5 * u, c);
		g.fill(x + u, y + u, x + 2 * u, y + 2 * u, c);
		g.fill(x + 6 * u, y + u, x + 7 * u, y + 2 * u, c);
		g.fill(x + u, y + 6 * u, x + 2 * u, y + 7 * u, c);
		g.fill(x + 6 * u, y + 6 * u, x + 7 * u, y + 7 * u, c);
	}

	private static void drawBeast(GuiGraphics g, int x, int y, int s, int c) {
		int u = Math.max(1, s / 8);
		g.fill(x + u, y, x + 3 * u, y + 2 * u, c);
		g.fill(x + 5 * u, y, x + 7 * u, y + 2 * u, c);
		g.fill(x + u, y + 2 * u, x + 7 * u, y + 5 * u, c);
		g.fill(x + 2 * u, y + 5 * u, x + 6 * u, y + 7 * u, c);
		g.fill(x + 3 * u, y + 7 * u, x + 5 * u, y + 8 * u, c);
	}

	private static void drawGeneric(GuiGraphics g, int x, int y, int s, int c) {
		int u = Math.max(1, s / 8);
		g.fill(x + 2 * u, y + u, x + 6 * u, y + 2 * u, c);
		g.fill(x + u, y + 2 * u, x + 7 * u, y + 6 * u, c);
		g.fill(x + 2 * u, y + 6 * u, x + 6 * u, y + 7 * u, c);
	}
}
