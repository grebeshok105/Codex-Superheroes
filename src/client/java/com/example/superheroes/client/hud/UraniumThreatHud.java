package com.example.superheroes.client.hud;

import com.example.superheroes.client.ClientHeroState;
import com.example.superheroes.client.ClientUraniumThreatState;
import com.example.superheroes.hero.HomelanderHero;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Радиационный значок в правом верхнем углу + зелёный tint поверх HUD,
 * пока локальный игрок-Хоумлендер находится в зоне действия урана.
 *
 * Иконка нарисована процедурно через примитивы (3 сектора + центральная точка).
 */
public final class UraniumThreatHud {
	private static final int ICON_SIZE = 28;
	private static final int MARGIN = 12;
	private static final int RADIATION_YELLOW = 0xFFFFE100;
	private static final int RADIATION_BG = 0xCC181818;
	private static final int RADIATION_BORDER = 0xFFFFFFFF;

	private UraniumThreatHud() {
	}

	public static void render(GuiGraphics graphics, DeltaTracker tracker) {
		if (!ClientUraniumThreatState.isSelfThreatened()) {
			return;
		}
		if (!ClientHeroState.data().hasHero()) {
			return;
		}
		ResourceLocation heroId = ClientHeroState.data().heroId();
		if (heroId == null || !HomelanderHero.ID.equals(heroId)) {
			return;
		}

		Minecraft mc = Minecraft.getInstance();
		int screenW = mc.getWindow().getGuiScaledWidth();
		int screenH = mc.getWindow().getGuiScaledHeight();

		// Зелёный пульсирующий тинт поверх всего HUD
		float pulse = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 350.0);
		int tintAlpha = (int) Mth.lerp(pulse, 0x18, 0x35);
		int tint = (tintAlpha << 24) | 0x00FF00;
		graphics.fill(0, 0, screenW, screenH, tint);

		int x = screenW - ICON_SIZE - MARGIN;
		int y = MARGIN;
		drawRadiationIcon(graphics, x, y, ICON_SIZE, pulse);

		int countX = x;
		int countY = y + ICON_SIZE + 2;
		Component label = Component.translatable("hud.superheroes.uranium_threat",
				ClientUraniumThreatState.sourceCount());
		int textW = mc.font.width(label);
		graphics.drawString(mc.font, label, x + ICON_SIZE - textW, countY, 0xFFFF80, true);
	}

	private static void drawRadiationIcon(GuiGraphics g, int x, int y, int size, float pulse) {
		int alpha = (int) Mth.lerp(pulse, 0xC0, 0xFF);
		int yellow = (alpha << 24) | (RADIATION_YELLOW & 0x00FFFFFF);
		int border = (alpha << 24) | (RADIATION_BORDER & 0x00FFFFFF);

		// фон-кружок (квадрат, скруглённый)
		HudUtil.roundedRectFill(g, x, y, size, size, RADIATION_BG);
		HudUtil.roundedRectBorder(g, x, y, size, size, border);

		int cx = x + size / 2;
		int cy = y + size / 2;
		int radius = size / 2 - 3;

		// 3 сектора (трилистник): рисуем псевдо-секторы трапециями из пикселей
		drawSector(g, cx, cy, radius, 0, yellow);
		drawSector(g, cx, cy, radius, 120, yellow);
		drawSector(g, cx, cy, radius, 240, yellow);

		// центральный круг
		int innerR = Math.max(2, size / 8);
		fillCircle(g, cx, cy, innerR, yellow);
	}

	private static void drawSector(GuiGraphics g, int cx, int cy, int radius, int angleDeg, int color) {
		double rad = Math.toRadians(angleDeg);
		double dirX = Math.cos(rad);
		double dirY = Math.sin(rad);
		int innerR = Math.max(3, radius / 2);
		int outerR = radius;
		int halfWidthDeg = 30;
		double halfWidthRad = Math.toRadians(halfWidthDeg);
		double cosHW = Math.cos(halfWidthRad);

		for (int dy = -outerR; dy <= outerR; dy++) {
			for (int dx = -outerR; dx <= outerR; dx++) {
				double dist = Math.sqrt(dx * dx + dy * dy);
				if (dist < innerR || dist > outerR) continue;
				double dot = (dx * dirX + dy * dirY) / dist;
				if (dot < cosHW) continue;
				g.fill(cx + dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
			}
		}
	}

	private static void fillCircle(GuiGraphics g, int cx, int cy, int radius, int color) {
		for (int dy = -radius; dy <= radius; dy++) {
			for (int dx = -radius; dx <= radius; dx++) {
				if (dx * dx + dy * dy <= radius * radius) {
					g.fill(cx + dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
				}
			}
		}
	}
}
