package com.example.superheroes.client.hud;

import com.example.superheroes.client.ClientHeroState;
import com.example.superheroes.hero.IronManHero;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class JarvisOverlayHud {
	private static final int COLOR_GOLD = 0xFFFFC400;
	private static final int COLOR_GOLD_SOFT = 0x66FFC400;
	private static final int COLOR_GOLD_BRIGHT = 0xCCFFB000;
	private static final int COLOR_RED_SOFT = 0x44E2342B;
	private static final int CORNER_SIZE = 28;

	private JarvisOverlayHud() {
	}

	public static void render(GuiGraphics graphics, DeltaTracker tracker) {
		if (!ClientHeroState.data().hasHero()) {
			return;
		}
		ResourceLocation heroId = ClientHeroState.data().heroId();
		if (!IronManHero.ID.equals(heroId)) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		int sw = mc.getWindow().getGuiScaledWidth();
		int sh = mc.getWindow().getGuiScaledHeight();
		long t = System.currentTimeMillis();
		double pulse = 0.5 + 0.5 * Math.sin(t / 500.0);
		int alpha = 0x33 + (int) (0x55 * pulse);
		int pulseColor = (alpha << 24) | (COLOR_GOLD & 0x00FFFFFF);

		drawCorner(graphics, 4, 4, false, false, pulseColor);
		drawCorner(graphics, sw - CORNER_SIZE - 4, 4, true, false, pulseColor);
		drawCorner(graphics, 4, sh - CORNER_SIZE - 4, false, true, pulseColor);
		drawCorner(graphics, sw - CORNER_SIZE - 4, sh - CORNER_SIZE - 4, true, true, pulseColor);

		drawSideRails(graphics, sw, sh, t);
		drawTopBanner(graphics, sw, t);
		drawCenterReticle(graphics, sw, sh, t);
	}

	private static void drawCorner(GuiGraphics g, int x, int y, boolean flipX, boolean flipY, int color) {
		int armLong = CORNER_SIZE;
		int armShort = 10;
		int t = 1;
		int x0 = x;
		int y0 = y;
		g.fill(x0, y0 + (flipY ? armLong - t : 0), x0 + armLong, y0 + (flipY ? armLong - t : 0) + t, color);
		g.fill(flipX ? x0 + armLong - t : x0, y0 + (flipY ? armLong - armShort : 0),
				flipX ? x0 + armLong : x0 + t, y0 + (flipY ? armLong : armShort), color);

		int dotX = flipX ? x0 + armLong - 4 : x0 + 2;
		int dotY = flipY ? y0 + armLong - 4 : y0 + 2;
		g.fill(dotX, dotY, dotX + 2, dotY + 2, COLOR_GOLD_BRIGHT);

		int hexX = flipX ? x0 + armLong - 14 : x0 + 8;
		int hexY = flipY ? y0 + armLong - 14 : y0 + 8;
		drawHex(g, hexX, hexY, color);

		int hex2X = flipX ? x0 + armLong - 22 : x0 + 16;
		int hex2Y = flipY ? y0 + armLong - 8 : y0 + 2;
		drawHex(g, hex2X, hex2Y, COLOR_RED_SOFT);

		int barLen = 10;
		int barOffset = 14;
		if (flipX) {
			g.fill(x0 + armLong - t, y0 + (flipY ? armLong - barOffset - 1 : barOffset), x0 + armLong,
					y0 + (flipY ? armLong - barOffset : barOffset + 1), COLOR_GOLD_SOFT);
			g.fill(x0 + armLong - barLen, y0 + (flipY ? armLong - barOffset - 5 : barOffset + 4), x0 + armLong - barLen + barLen,
					y0 + (flipY ? armLong - barOffset - 4 : barOffset + 5), COLOR_GOLD_SOFT);
		} else {
			g.fill(x0, y0 + (flipY ? armLong - barOffset - 1 : barOffset), x0 + t,
					y0 + (flipY ? armLong - barOffset : barOffset + 1), COLOR_GOLD_SOFT);
			g.fill(x0, y0 + (flipY ? armLong - barOffset - 5 : barOffset + 4), x0 + barLen,
					y0 + (flipY ? armLong - barOffset - 4 : barOffset + 5), COLOR_GOLD_SOFT);
		}
	}

	private static void drawHex(GuiGraphics g, int x, int y, int color) {
		g.fill(x + 1, y, x + 5, y + 1, color);
		g.fill(x, y + 1, x + 1, y + 5, color);
		g.fill(x + 5, y + 1, x + 6, y + 5, color);
		g.fill(x + 1, y + 5, x + 5, y + 6, color);
	}

	private static void drawSideRails(GuiGraphics g, int sw, int sh, long t) {
		int alpha = 0x33 + (int) (0x33 * (0.5 + 0.5 * Math.sin(t / 700.0)));
		int color = (alpha << 24) | (COLOR_GOLD & 0x00FFFFFF);
		int top = 60;
		int bottom = sh - 60;
		int rightX = sw - 3;
		g.fill(2, top, 3, bottom, color);
		g.fill(rightX, top, rightX + 1, bottom, color);
		int step = 14;
		for (int yy = top; yy <= bottom; yy += step) {
			g.fill(2, yy, 6, yy + 1, COLOR_GOLD_SOFT);
			g.fill(rightX - 3, yy, rightX + 1, yy + 1, COLOR_GOLD_SOFT);
		}
	}

	private static void drawTopBanner(GuiGraphics g, int sw, long t) {
		int alpha = 0x66 + (int) (0x44 * (0.5 + 0.5 * Math.sin(t / 600.0)));
		int color = (alpha << 24) | (COLOR_GOLD & 0x00FFFFFF);
		int cx = sw / 2;
		int y = 4;
		g.fill(cx - 60, y, cx - 6, y + 1, color);
		g.fill(cx + 6, y, cx + 60, y + 1, color);
		g.fill(cx - 60, y + 1, cx - 59, y + 4, color);
		g.fill(cx + 59, y + 1, cx + 60, y + 4, color);
		Component label = Component.literal("J.A.R.V.I.S.");
		Minecraft mc = Minecraft.getInstance();
		int textWidth = mc.font.width(label);
		g.drawString(mc.font, label, cx - textWidth / 2, y + 6, color, false);
	}

	private static void drawCenterReticle(GuiGraphics g, int sw, int sh, long t) {
		double cx = sw / 2.0;
		double cy = sh / 2.0;
		int rOuter = 18;
		int rInner = 11;
		int dotsOuter = 24;
		int dotsInner = 12;
		double rotateOuter = (t / 2200.0) % (Math.PI * 2);
		double rotateInner = -(t / 1400.0) % (Math.PI * 2);

		for (int i = 0; i < dotsOuter; i++) {
			double a = rotateOuter + (Math.PI * 2 * i) / dotsOuter;
			boolean major = i % 4 == 0;
			int s = major ? 2 : 1;
			int dx = (int) Math.round(cx + Math.cos(a) * rOuter - s / 2.0);
			int dy = (int) Math.round(cy + Math.sin(a) * rOuter - s / 2.0);
			int c = major ? COLOR_GOLD_BRIGHT : COLOR_GOLD_SOFT;
			g.fill(dx, dy, dx + s, dy + s, c);
		}
		for (int i = 0; i < dotsInner; i++) {
			double a = rotateInner + (Math.PI * 2 * i) / dotsInner;
			int s = 1;
			int dx = (int) Math.round(cx + Math.cos(a) * rInner - s / 2.0);
			int dy = (int) Math.round(cy + Math.sin(a) * rInner - s / 2.0);
			g.fill(dx, dy, dx + s, dy + s, COLOR_GOLD_SOFT);
		}

		int icx = (int) Math.round(cx);
		int icy = (int) Math.round(cy);
		int gap = 5;
		int len = 4;
		g.fill(icx - gap - len, icy, icx - gap, icy + 1, COLOR_GOLD_BRIGHT);
		g.fill(icx + gap, icy, icx + gap + len, icy + 1, COLOR_GOLD_BRIGHT);
		g.fill(icx, icy - gap - len, icx + 1, icy - gap, COLOR_GOLD_BRIGHT);
		g.fill(icx, icy + gap, icx + 1, icy + gap + len, COLOR_GOLD_BRIGHT);

		int diag = 22;
		g.fill(icx - diag, icy - diag, icx - diag + 4, icy - diag + 1, COLOR_GOLD_SOFT);
		g.fill(icx - diag, icy - diag, icx - diag + 1, icy - diag + 4, COLOR_GOLD_SOFT);
		g.fill(icx + diag - 4, icy - diag, icx + diag, icy - diag + 1, COLOR_GOLD_SOFT);
		g.fill(icx + diag - 1, icy - diag, icx + diag, icy - diag + 4, COLOR_GOLD_SOFT);
		g.fill(icx - diag, icy + diag - 1, icx - diag + 4, icy + diag, COLOR_GOLD_SOFT);
		g.fill(icx - diag, icy + diag - 4, icx - diag + 1, icy + diag, COLOR_GOLD_SOFT);
		g.fill(icx + diag - 4, icy + diag - 1, icx + diag, icy + diag, COLOR_GOLD_SOFT);
		g.fill(icx + diag - 1, icy + diag - 4, icx + diag, icy + diag, COLOR_GOLD_SOFT);
	}
}
