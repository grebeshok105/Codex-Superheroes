package com.example.superheroes.client.hud;

import com.example.superheroes.client.ClientMeleeChargeState;
import com.example.superheroes.physics.ImpactChargeRules;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Вертикальный индикатор заряда удара рядом с прицелом.
 */
public final class MeleeChargeHud {
	private static final int GAUGE_W = 7;
	private static final int GAUGE_H = 18;
	private static final int INNER_W = 5;
	private static final int INNER_H = 16;

	private MeleeChargeHud() {
	}

	public static void render(GuiGraphics graphics, DeltaTracker tracker) {
		if (!ClientMeleeChargeState.charging()) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.options.hideGui) return;
		int ticks = ClientMeleeChargeState.chargeTicks();
		if (ticks <= 0) return;

		int w = mc.getWindow().getGuiScaledWidth();
		int h = mc.getWindow().getGuiScaledHeight();
		int gaugeX = w / 2 + 10;
		int gaugeY = h / 2 - 9;

		float progress = Math.min(1f, ticks / (float) ImpactChargeRules.CAP_TICKS);
		boolean tier3 = ticks >= ImpactChargeRules.TIER_3_TICKS;
		boolean tier2 = ticks >= ImpactChargeRules.TIER_2_TICKS;
		int fillColor = tier3 ? 0xFFFF4D6A : tier2 ? 0xFF59D8FF : 0xFFE8E8E8;

		int borderColor = 0x90000000;
		if (tier3) {
			float pulse = (float) Math.sin(Util.getMillis() * (2.0 * Math.PI / 600.0));
			int alpha = (int) (0x90 + 0x30 * pulse);
			borderColor = alpha << 24;
		}

		graphics.fill(gaugeX, gaugeY, gaugeX + GAUGE_W, gaugeY + GAUGE_H, borderColor);

		int innerX = gaugeX + 1;
		int innerBottom = gaugeY + 1 + INNER_H;
		int filledPixels = (int) (INNER_H * progress);
		int fillTop = innerBottom - filledPixels;
		graphics.fill(innerX, fillTop, innerX + INNER_W, innerBottom, fillColor);

		float tier2Frac = ImpactChargeRules.TIER_2_TICKS / (float) ImpactChargeRules.CAP_TICKS;
		float tier3Frac = ImpactChargeRules.TIER_3_TICKS / (float) ImpactChargeRules.CAP_TICKS;
		int notch2Y = innerBottom - (int) (INNER_H * tier2Frac);
		int notch3Y = innerBottom - (int) (INNER_H * tier3Frac);
		graphics.fill(innerX, notch2Y, innerX + INNER_W, notch2Y + 1, 0xFFFFFFFF);
		graphics.fill(innerX, notch3Y, innerX + INNER_W, notch3Y + 1, 0xFFFFFFFF);

		if (tier2) {
			Component label = Component.translatable(
					tier3 ? "hud.superheroes.melee_tier.3" : "hud.superheroes.melee_tier.2"
			);
			int labelX = gaugeX + GAUGE_W + 3;
			int labelY = gaugeY + (GAUGE_H - mc.font.lineHeight) / 2;
			graphics.drawString(mc.font, HudUtil.text(label), labelX, labelY, fillColor, true);
		}
	}
}
