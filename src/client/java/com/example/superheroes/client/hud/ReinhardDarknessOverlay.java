package com.example.superheroes.client.hud;

import com.example.superheroes.client.ClientReinhardDarknessState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Тьма от меча Рейнхарда: основную работу делает ванильная слепота,
 * оверлей лишь добавляет лёгкую мгновенную виньетку по краям экрана,
 * не заливая HUD чёрным, и быстро рассеивается в конце действия.
 */
public final class ReinhardDarknessOverlay {
	private static final long FADE_OUT_MS = 450L;
	private static final int CORE_ALPHA = 60;
	private static final int EDGE_ALPHA = 150;

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
		int core = (int) (CORE_ALPHA * strength);
		int edge = (int) (EDGE_ALPHA * strength);
		int band = h / 3;
		graphics.fill(0, 0, w, h, core << 24);
		graphics.fillGradient(0, 0, w, band, edge << 24, 0);
		graphics.fillGradient(0, h - band, w, h, 0, edge << 24);
	}
}
