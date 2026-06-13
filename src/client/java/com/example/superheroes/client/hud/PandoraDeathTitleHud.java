package com.example.superheroes.client.hud;

import com.example.superheroes.client.ClientPandoraDeathState;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Pandora revival cut-scene title: shows «Ты думал меня так легко убить?» at the top of the
 * screen while the cut-scene is active (reworked V4 — replaces the old shaderpack overlay).
 */
public final class PandoraDeathTitleHud {
	private static final float SCALE = 1.7f;

	private PandoraDeathTitleHud() {
	}

	public static void render(GuiGraphics graphics, DeltaTracker tracker) {
		if (!ClientPandoraDeathState.active()) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		Font font = mc.font;
		int w = mc.getWindow().getGuiScaledWidth();
		int h = mc.getWindow().getGuiScaledHeight();

		Component msg = Component.translatable("pandora.death.title")
				.withStyle(ChatFormatting.BOLD, ChatFormatting.WHITE);
		int textWidth = font.width(msg);

		// A subtle dark band behind the text for readability over any background.
		int bandTop = (int) (h * 0.10f);
		int bandHeight = (int) (font.lineHeight * SCALE) + 10;
		graphics.fill(0, bandTop - 5, w, bandTop + bandHeight, 0x80000000);

		graphics.pose().pushPose();
		graphics.pose().scale(SCALE, SCALE, 1f);
		float x = (w / SCALE - textWidth) / 2f;
		float y = bandTop / SCALE;
		graphics.drawString(font, msg, (int) x, (int) y, 0xFFFFFFFF, true);
		graphics.pose().popPose();
	}
}
