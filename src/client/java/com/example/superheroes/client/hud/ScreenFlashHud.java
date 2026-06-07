package com.example.superheroes.client.hud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class ScreenFlashHud {
	private static final long DURATION_MS = 600L;

	private static volatile long startedAtMs;
	private static volatile int topColor;
	private static volatile int bottomColor;

	private ScreenFlashHud() {
	}

	public static void trigger(boolean activate) {
		startedAtMs = System.currentTimeMillis();
		if (activate) {
			topColor = 0xFFF1C45A;
			bottomColor = 0xFFFF4655;
		} else {
			topColor = 0xFF334466;
			bottomColor = 0xFF050810;
		}
	}

	public static void render(GuiGraphics graphics, DeltaTracker tracker) {
		long started = startedAtMs;
		if (started == 0L) {
			return;
		}
		long elapsed = System.currentTimeMillis() - started;
		if (elapsed >= DURATION_MS) {
			startedAtMs = 0L;
			return;
		}
		float t = elapsed / (float) DURATION_MS;
		float fade = (float) Math.pow(1f - t, 1.7);
		int alphaTop = clampAlpha((int) (((topColor >>> 24) & 0xFF) * fade));
		int alphaBottom = clampAlpha((int) (((bottomColor >>> 24) & 0xFF) * fade * 0.65f));
		int top = (alphaTop << 24) | (topColor & 0x00FFFFFF);
		int bottom = (alphaBottom << 24) | (bottomColor & 0x00FFFFFF);
		Minecraft mc = Minecraft.getInstance();
		int w = mc.getWindow().getGuiScaledWidth();
		int h = mc.getWindow().getGuiScaledHeight();
		graphics.fillGradient(0, 0, w, h, top, bottom);
	}

	private static int clampAlpha(int value) {
		return Math.max(0, Math.min(255, value));
	}
}
