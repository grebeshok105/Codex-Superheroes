package com.example.superheroes.client.hud;

import net.minecraft.client.Minecraft;

public final class HudScaler {
	private static final float BASE_GUI_SCALE = 2.0f;

	private HudScaler() {
	}

	public static float factor() {
		Minecraft mc = Minecraft.getInstance();
		float currentScale = (float) mc.getWindow().getGuiScale();
		if (currentScale <= 0f) {
			return 1f;
		}
		return currentScale / BASE_GUI_SCALE;
	}

	public static int scale(int base) {
		return Math.round(base * factor());
	}

	public static float scale(float base) {
		return base * factor();
	}

	public static int screenWidth() {
		return Minecraft.getInstance().getWindow().getGuiScaledWidth();
	}

	public static int screenHeight() {
		return Minecraft.getInstance().getWindow().getGuiScaledHeight();
	}
}
