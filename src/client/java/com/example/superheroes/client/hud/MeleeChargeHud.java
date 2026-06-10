package com.example.superheroes.client.hud;

import com.example.superheroes.client.ClientMeleeChargeState;
import com.example.superheroes.physics.ImpactChargeRules;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Индикатор заряда удара у прицела: полоса прогресса до Tier 3 и подпись текущего тира.
 */
public final class MeleeChargeHud {
	private static final int BAR_WIDTH = 62;
	private static final int BAR_HEIGHT = 3;

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
		int x = (w - BAR_WIDTH) / 2;
		int y = h / 2 + 14;

		float progress = Math.min(1f, ticks / (float) ImpactChargeRules.CAP_TICKS);
		boolean tier3 = ticks >= ImpactChargeRules.TIER_3_TICKS;
		boolean tier2 = ticks >= ImpactChargeRules.TIER_2_TICKS;
		int fillColor = tier3 ? 0xFFFF4D6A : tier2 ? 0xFF59D8FF : 0xFFE8E8E8;

		graphics.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, 0x90000000);
		graphics.fill(x, y, x + (int) (BAR_WIDTH * progress), y + BAR_HEIGHT, fillColor);

		int tier2X = x + (int) (BAR_WIDTH * (ImpactChargeRules.TIER_2_TICKS / (float) ImpactChargeRules.CAP_TICKS));
		int tier3X = x + (int) (BAR_WIDTH * (ImpactChargeRules.TIER_3_TICKS / (float) ImpactChargeRules.CAP_TICKS));
		graphics.fill(tier2X, y - 1, tier2X + 1, y + BAR_HEIGHT + 1, 0xFFFFFFFF);
		graphics.fill(tier3X, y - 1, tier3X + 1, y + BAR_HEIGHT + 1, 0xFFFFFFFF);

		if (tier2) {
			Component label = Component.translatable(tier3 ? "hud.superheroes.melee_tier.3" : "hud.superheroes.melee_tier.2");
			int textWidth = mc.font.width(label);
			graphics.drawString(mc.font, label, (w - textWidth) / 2, y + BAR_HEIGHT + 4, fillColor, true);
		}
	}
}
