package com.example.superheroes.client.hud;

import com.example.superheroes.client.ClientHeroState;
import com.example.superheroes.client.ClientKratosRageState;
import com.example.superheroes.hero.KratosHero;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class SpartanRageHud {
	private static final int BAR_WIDTH = 182;
	private static final int BAR_HEIGHT = 7;
	private static final int Y_OFFSET_FROM_HOTBAR = 28;

	private static final int BAR_BG = 0xCC15050A;
	private static final int BAR_BORDER = 0xFF1C0405;
	private static final int RAGE_DARK = 0xFF640000;
	private static final int RAGE_BRIGHT = 0xFFFF1A1A;
	private static final int RAGE_PEAK = 0xFFFFD040;
	private static final int RAGE_GLOW_RIM = 0x55FF6020;
	private static final int LABEL_DIM = 0xFFAA4030;
	private static final int LABEL_BRIGHT = 0xFFFFE060;

	private SpartanRageHud() {
	}

	public static void render(GuiGraphics graphics, DeltaTracker tracker) {
		if (!ClientHeroState.data().hasHero()) return;
		if (!KratosHero.ID.equals(ClientHeroState.heroId())) return;

		float rage = ClientKratosRageState.rage();
		boolean active = ClientKratosRageState.active();
		if (rage <= 0.001f && !active) return;

		Minecraft mc = Minecraft.getInstance();
		int sw = mc.getWindow().getGuiScaledWidth();
		int sh = mc.getWindow().getGuiScaledHeight();
		int x = sw / 2 - BAR_WIDTH / 2;
		int y = sh - Y_OFFSET_FROM_HOTBAR;

		HudUtil.dropShadow(graphics, x, y, BAR_WIDTH, BAR_HEIGHT, 1, 0x88000000);
		HudUtil.roundedRectFill(graphics, x, y, BAR_WIDTH, BAR_HEIGHT, BAR_BG);
		HudUtil.roundedRectBorder(graphics, x, y, BAR_WIDTH, BAR_HEIGHT, BAR_BORDER);

		float pct = rage / 100f;
		int innerW = BAR_WIDTH - 4;
		int innerH = BAR_HEIGHT - 4;
		int filledW = (int) (innerW * pct);
		int innerX = x + 2;
		int innerY = y + 2;

		boolean full = ClientKratosRageState.isFull();
		long t = mc.level == null ? 0L : mc.level.getGameTime();
		float pulse = full ? (0.7f + 0.3f * (float) Math.sin(t * 0.35)) : 1f;

		int top = full ? mix(RAGE_BRIGHT, RAGE_PEAK, pulse * 0.5f) : RAGE_DARK;
		int bottom = full ? RAGE_PEAK : RAGE_BRIGHT;

		if (filledW > 0) {
			graphics.fillGradient(innerX, innerY, innerX + filledW, innerY + innerH, top, bottom);
			if (active) {
				int rimAlpha = (int) (96 + 96 * Math.sin(t * 0.5));
				int rim = (rimAlpha << 24) | (RAGE_GLOW_RIM & 0x00FFFFFF);
				graphics.fill(innerX, innerY, innerX + filledW, innerY + 1, rim);
				graphics.fill(innerX, innerY + innerH - 1, innerX + filledW, innerY + innerH, rim);
			}
		}

		String label;
		int labelColor;
		if (active) {
			label = Component.translatable("hud.superheroes.spartan_rage.active").getString();
			labelColor = LABEL_BRIGHT;
		} else if (full) {
			label = Component.translatable("hud.superheroes.spartan_rage.ready").getString();
			labelColor = LABEL_BRIGHT;
		} else {
			label = (int) rage + "%";
			labelColor = LABEL_DIM;
		}
		int textW = mc.font.width(label);
		int textY = y - 9;
		graphics.drawString(mc.font, label, x + (BAR_WIDTH - textW) / 2, textY, labelColor, true);
	}

	private static int mix(int a, int b, float t) {
		t = Math.max(0f, Math.min(1f, t));
		int aa = (a >> 24) & 0xFF, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
		int ba = (b >> 24) & 0xFF, br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
		int ra = (int) (aa + (ba - aa) * t);
		int rr = (int) (ar + (br - ar) * t);
		int rg = (int) (ag + (bg - ag) * t);
		int rb = (int) (ab + (bb - ab) * t);
		return (ra << 24) | (rr << 16) | (rg << 8) | rb;
	}
}
