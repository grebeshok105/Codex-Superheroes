package com.example.superheroes.client.hud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

import java.util.Random;

/**
 * §5.5 Cracks-overlay — процедурные «трещины» от краёв экрана к центру,
 * интенсивность зависит от ramp() безумия Регулуса. Линии тонкие,
 * чёрные, лёгкая «кровавая» поднимающаяся компонента.
 *
 * Не трогает чат / F3 / vanilla UI — рисуется отдельным render-pass.
 * Сид фиксирован: трещины не «дёргаются» — растут и темнеют со временем.
 */
public final class CracksOverlayHud {
	private static final int CRACK_COUNT = 12;
	private static final long CRACK_SEED = 0xC2A0BABEL;

	private CracksOverlayHud() {
	}

	public static void render(GuiGraphics graphics, DeltaTracker tracker) {
		float ramp = ClientHudGlitch.ramp();
		if (ramp <= 0.05f) return;
		Minecraft mc = Minecraft.getInstance();
		int sw = mc.getWindow().getGuiScaledWidth();
		int sh = mc.getWindow().getGuiScaledHeight();

		Random rng = new Random(CRACK_SEED);
		int alpha = (int) Mth.clamp(ramp * 220f, 30f, 200f);
		int colorMain = (alpha << 24) | 0x00000000;
		int colorBlood = (Math.min(alpha + 20, 255) << 24) | 0x00220000;

		for (int i = 0; i < CRACK_COUNT; i++) {
			int side = rng.nextInt(4);
			int x0, y0;
			switch (side) {
				case 0 -> { x0 = rng.nextInt(sw); y0 = 0; }
				case 1 -> { x0 = sw - 1; y0 = rng.nextInt(sh); }
				case 2 -> { x0 = rng.nextInt(sw); y0 = sh - 1; }
				default -> { x0 = 0; y0 = rng.nextInt(sh); }
			}
			int len = (int) Mth.lerp(ramp, 18f, 90f) + rng.nextInt(40);
			int segCount = 4 + rng.nextInt(4);
			int x = x0;
			int y = y0;
			float angle = (float) Math.atan2(sh / 2.0 - y0, sw / 2.0 - x0);
			for (int s = 0; s < segCount; s++) {
				float jitter = (rng.nextFloat() - 0.5f) * 0.9f;
				angle += jitter;
				int segLen = Math.max(3, len / segCount);
				int nx = (int) (x + Math.cos(angle) * segLen);
				int ny = (int) (y + Math.sin(angle) * segLen);
				drawLine(graphics, x, y, nx, ny, s == 0 ? colorBlood : colorMain);
				x = nx;
				y = ny;
				if (x < 0 || x >= sw || y < 0 || y >= sh) break;
			}
		}
	}

	private static void drawLine(GuiGraphics g, int x0, int y0, int x1, int y1, int color) {
		int dx = Math.abs(x1 - x0);
		int dy = Math.abs(y1 - y0);
		int sx = x0 < x1 ? 1 : -1;
		int sy = y0 < y1 ? 1 : -1;
		int err = dx - dy;
		int x = x0;
		int y = y0;
		int safety = 0;
		while (safety++ < 200) {
			g.fill(x, y, x + 1, y + 1, color);
			if (x == x1 && y == y1) break;
			int e2 = 2 * err;
			if (e2 > -dy) { err -= dy; x += sx; }
			if (e2 <  dx) { err += dx; y += sy; }
		}
	}
}
