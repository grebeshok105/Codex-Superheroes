package com.example.superheroes.client.hud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight stream-friendly debug overlay for the horde system. Toggled via
 * {@code /superheroes horde overlay}; the server pushes pre-formatted text.
 */
public final class HordeDebugOverlay {
	private static final List<String> LINES = new ArrayList<>();
	private static long lastUpdateMs = 0L;
	private static final long STALE_MS = 3000L;

	private HordeDebugOverlay() {
	}

	public static void update(String text) {
		LINES.clear();
		if (text != null && !text.isEmpty()) {
			for (String line : text.split("\n")) {
				if (!line.isEmpty()) {
					LINES.add(line);
				}
			}
		}
		lastUpdateMs = System.currentTimeMillis();
	}

	public static void render(GuiGraphics graphics, DeltaTracker tracker) {
		if (LINES.isEmpty()) {
			return;
		}
		if (System.currentTimeMillis() - lastUpdateMs > STALE_MS) {
			LINES.clear();
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.hideGui) {
			return;
		}
		Font font = mc.font;

		int x = 6;
		int y = 32;
		int width = 0;
		for (String line : LINES) {
			width = Math.max(width, font.width(Component.literal(line)));
		}
		int pad = 4;
		int lineH = font.lineHeight + 1;
		int boxH = LINES.size() * lineH + pad * 2;
		int boxW = width + pad * 2;

		graphics.fill(x - pad, y - pad, x + boxW - pad, y + boxH - pad, 0xA0000000);
		graphics.fill(x - pad, y - pad, x + boxW - pad, y - pad + 1, 0xFFC81E1E);

		int cy = y;
		for (String line : LINES) {
			graphics.drawString(font, Component.literal(line), x, cy, 0xFFFFFF, true);
			cy += lineH;
		}
	}
}
