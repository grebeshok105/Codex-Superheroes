package com.example.superheroes.client.hud;

import com.example.superheroes.effect.ModEffects;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffectInstance;

public final class SunWindupHud {
	private static final int TOTAL_TICKS = 200;

	private SunWindupHud() {
	}

	public static void render(GuiGraphics graphics, DeltaTracker tracker) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null) {
			return;
		}
		MobEffectInstance instance = player.getEffect(ModEffects.MADNESS_AFTERMATH);
		if (instance == null) {
			return;
		}
		int remaining = instance.getDuration();
		float t = Math.max(0f, Math.min(1f, 1f - (float) remaining / TOTAL_TICKS));
		int w = graphics.guiWidth();
		int h = graphics.guiHeight();
		float ease = t * t;
		int alphaWhite = (int) (ease * 235f);
		int alphaYellow = (int) (ease * 200f);
		int colWhite = (alphaWhite << 24) | 0xFFFFFF;
		int colYellow = (alphaYellow << 24) | 0xFFE680;
		graphics.fill(0, 0, w, h, colYellow);
		if (ease > 0.4f) {
			graphics.fill(0, 0, w, h, colWhite);
		}
		if (ease > 0.92f) {
			int blast = (int) ((ease - 0.92f) / 0.08f * 255f);
			int colBlast = (Math.min(255, blast) << 24) | 0xFFFFFF;
			graphics.fill(0, 0, w, h, colBlast);
		}
	}
}
