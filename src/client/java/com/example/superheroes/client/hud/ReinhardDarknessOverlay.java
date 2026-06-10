package com.example.superheroes.client.hud;

import com.example.superheroes.client.ClientReinhardDarknessState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Мгновенная тьма от меча Рейнхарда: экран жертвы становится чёрным сразу,
 * без плавного появления, и быстро рассеивается в конце действия.
 */
public final class ReinhardDarknessOverlay {
	private static final long FADE_OUT_MS = 450L;

	private ReinhardDarknessOverlay() {
	}

	public static void render(GuiGraphics graphics, DeltaTracker tracker) {
		if (!ClientReinhardDarknessState.active()) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return;
		int w = mc.getWindow().getGuiScaledWidth();
		int h = mc.getWindow().getGuiScaledHeight();

		long remaining = ClientReinhardDarknessState.deadlineMs() - System.currentTimeMillis();
		float strength = remaining >= FADE_OUT_MS ? 1f : Math.max(0f, remaining / (float) FADE_OUT_MS);
		int alpha = (int) (250 * strength);
		graphics.fill(0, 0, w, h, (alpha << 24));
	}
}
