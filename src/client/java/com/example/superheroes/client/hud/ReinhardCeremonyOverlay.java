package com.example.superheroes.client.hud;

import com.example.superheroes.client.ClientReinhardCeremonyState;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class ReinhardCeremonyOverlay {
	private ReinhardCeremonyOverlay() {
	}

	public static void render(GuiGraphics graphics, DeltaTracker tracker) {
		float p = ClientReinhardCeremonyState.progress();
		boolean active = ClientReinhardCeremonyState.active();
		if (!active && p <= 0f) {
			return;
		}
		float effective = active ? p : 0f;
		if (effective <= 0.001f) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		int w = mc.getWindow().getGuiScaledWidth();
		int h = mc.getWindow().getGuiScaledHeight();

		// Vignette: dark edges fading to bright center, then full white as progress completes
		float vignetteAlpha = 0.55f * effective;
		int vAlpha = (int) (vignetteAlpha * 255f);
		vAlpha = Math.max(0, Math.min(255, vAlpha));
		int vignette = (vAlpha << 24) | 0x000000;
		graphics.fillGradient(0, 0, w, h / 3, vignette, 0x00000000);
		graphics.fillGradient(0, h - h / 3, w, h, 0x00000000, vignette);

		// White screen brighten — 0% → 80% over 10s
		float white = 0.80f * effective;
		int wAlpha = (int) (white * 255f);
		wAlpha = Math.max(0, Math.min(255, wAlpha));
		int whiteColor = (wAlpha << 24) | 0xFFFFFF;
		graphics.fill(0, 0, w, h, whiteColor);

		// Ominous text near the end
		if (effective > 0.35f) {
			Font font = mc.font;
			Component msg = Component.translatable("ability.superheroes.reinhard_sword_draw.ceremony")
					.withStyle(ChatFormatting.BOLD, ChatFormatting.WHITE);
			int textWidth = font.width(msg);
			int textAlpha = (int) (Math.min(1f, (effective - 0.35f) / 0.4f) * 255f);
			textAlpha = Math.max(0, Math.min(255, textAlpha));
			int textColor = (textAlpha << 24) | 0xFFEFEF;
			graphics.drawString(font, msg, (w - textWidth) / 2, h / 2 - 30, textColor, true);
		}
	}
}
