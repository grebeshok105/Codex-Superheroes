package com.example.superheroes.client.hud;

import com.example.superheroes.client.ClientMadnessState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

public final class EvangelionZoomHud {
	private static final long TOTAL_MS = 10000L;
	private static final float BAR_FRACTION_MAX = 0.18f;

	private EvangelionZoomHud() {
	}

	public static void render(GuiGraphics graphics, DeltaTracker tracker) {
		if (!ClientMadnessState.isReading()) {
			return;
		}
		long now = System.currentTimeMillis();
		long until = ClientMadnessState.readingUntilMs();
		long remaining = until - now;
		if (remaining <= 0L || remaining > TOTAL_MS) {
			return;
		}
		float progress = 1f - (remaining / (float) TOTAL_MS);
		Minecraft mc = Minecraft.getInstance();
		int sw = mc.getWindow().getGuiScaledWidth();
		int sh = mc.getWindow().getGuiScaledHeight();

		int barH = (int) (sh * BAR_FRACTION_MAX * progress);
		if (barH > 0) {
			graphics.fill(0, 0, sw, barH, 0xFF000000);
			graphics.fill(0, sh - barH, sw, sh, 0xFF000000);
		}

		int chromAlpha = (int) (Mth.clamp(progress, 0f, 1f) * 90f);
		if (chromAlpha > 4) {
			int dx = Math.max(1, (int) (progress * 3f));
			int redTint = (chromAlpha << 24) | 0xFF1010;
			int blueTint = (chromAlpha << 24) | 0x1010FF;
			graphics.fill(-dx, 0, sw - dx, sh, redTint);
			graphics.fill(dx, 0, sw + dx, sh, blueTint);
		}

		int vignetteAlpha = (int) (Mth.clamp(progress, 0f, 1f) * 140f);
		if (vignetteAlpha > 8) {
			int color = (vignetteAlpha << 24) | 0x000000;
			int band = Math.max(8, (int) (sh * 0.05f));
			for (int i = 0; i < band; i++) {
				int a = (int) (vignetteAlpha * (1f - i / (float) band));
				if (a <= 0) break;
				int c = (a << 24) | 0x000000;
				graphics.fill(i, 0, i + 1, sh, c);
				graphics.fill(sw - i - 1, 0, sw - i, sh, c);
			}
		}
	}
}
