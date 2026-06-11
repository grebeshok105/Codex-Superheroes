package com.example.superheroes.client.hud;

import com.example.superheroes.client.ClientReactorState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class ReactorOverlayHud {
	private static final int BAR_WIDTH = 220;
	private static final int BAR_HEIGHT = 14;
	private static final int PANEL_PADDING = 8;
	private static final int COLOR_GOLD = 0xFFFFC400;
	private static final int COLOR_GOLD_DIM = 0xFF7A5A00;
	private static final int COLOR_RED = 0xFFE2342B;
	private static final int COLOR_RED_DIM = 0xFF5A1410;
	private static final int COLOR_PANEL = 0xCC0A0408;
	private static final int COLOR_PANEL_BORDER = 0xFFFFAE00;

	private ReactorOverlayHud() {
	}

	public static void render(GuiGraphics graphics, DeltaTracker tracker) {
		if (!ClientReactorState.active()) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		Font font = mc.font;
		int sw = mc.getWindow().getGuiScaledWidth();
		int sh = mc.getWindow().getGuiScaledHeight();

		boolean hasStock = ClientReactorState.hasStock();
		int progress = ClientReactorState.progress();
		int total = Math.max(1, ClientReactorState.total());
		float pct = Math.min(1f, progress / (float) total);

		String headline = hasStock ? Component.translatable("hud.superheroes.reactor.replacing").getString()
				: Component.translatable("hud.superheroes.reactor.no_stock").getString();
		String hint = hasStock ? Component.translatable("hud.superheroes.reactor.hint").getString() : "";

		int panelW = BAR_WIDTH + PANEL_PADDING * 2;
		int panelH = BAR_HEIGHT + 38;
		int panelX = (sw - panelW) / 2;
		int panelY = sh / 2 + 28;

		HudUtil.roundedRectFill(graphics, panelX, panelY, panelW, panelH, COLOR_PANEL);
		HudUtil.roundedRectBorder(graphics, panelX, panelY, panelW, panelH, hasStock ? COLOR_PANEL_BORDER : COLOR_RED);

		int textY = panelY + 6;
		int headColor = hasStock ? COLOR_GOLD : COLOR_RED;
		double pulse = 0.7 + 0.3 * Math.abs(Math.sin(System.currentTimeMillis() / 280.0));
		int alpha = (int) (255 * pulse);
		int pulsedHead = (alpha << 24) | (headColor & 0x00FFFFFF);
		graphics.drawCenteredString(font, HudUtil.text(headline), sw / 2, textY, pulsedHead);

		int barX = panelX + PANEL_PADDING;
		int barY = panelY + 22;
		HudUtil.roundedRectFill(graphics, barX, barY, BAR_WIDTH, BAR_HEIGHT, 0xFF02030A);
		int filled = (int) (BAR_WIDTH * pct);
		if (filled >= 4) {
			HudUtil.roundedRectGradient(graphics, barX, barY, filled, BAR_HEIGHT,
					hasStock ? COLOR_GOLD : COLOR_RED,
					hasStock ? COLOR_GOLD_DIM : COLOR_RED_DIM);
		}
		HudUtil.roundedRectBorder(graphics, barX, barY, BAR_WIDTH, BAR_HEIGHT, hasStock ? COLOR_GOLD_DIM : COLOR_RED_DIM);

		int tickEvery = 4;
		int tickW = BAR_WIDTH / tickEvery;
		for (int i = 1; i < tickEvery; i++) {
			int tx = barX + tickW * i;
			graphics.fill(tx, barY + 2, tx + 1, barY + BAR_HEIGHT - 2, 0x55000000);
		}

		if (!hint.isEmpty()) {
			graphics.drawCenteredString(font, HudUtil.text(hint), sw / 2, panelY + panelH - 12, 0xFFCCCCCC);
		}

		int seconds = (int) Math.ceil((total - progress) / 20.0);
		String pctText = hasStock ? (seconds + "s") : "—";
		graphics.drawString(font, HudUtil.text(pctText), panelX + panelW - PANEL_PADDING - font.width(HudUtil.text(pctText)), barY + 3, 0xFFFFFFFF, false);
	}
}
