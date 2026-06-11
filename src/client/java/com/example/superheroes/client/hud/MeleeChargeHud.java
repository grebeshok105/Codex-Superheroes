package com.example.superheroes.client.hud;

import com.example.superheroes.client.ClientHeroState;
import com.example.superheroes.client.ClientMeleeChargeState;
import com.example.superheroes.client.render.WildRenderer;
import com.example.superheroes.client.render.WildShaders;
import com.example.superheroes.hero.HeroTheme;
import com.example.superheroes.physics.ImpactChargeRules;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Индикатор заряда удара у прицела — неоновая капсула в стиле остального HUD:
 * стеклянная подложка, светящаяся заливка в цветах темы героя, насечки тиров
 * и пульсирующее свечение на максимальном заряде.
 */
public final class MeleeChargeHud {
	private static final int GAUGE_W = 8;
	private static final int GAUGE_H = 26;
	private static final int BG_COLOR = 0xE002030A;

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
		int gaugeX = w / 2 + 12;
		int gaugeY = h / 2 - GAUGE_H / 2;

		float progress = Math.min(1f, ticks / (float) ImpactChargeRules.CAP_TICKS);
		boolean tier3 = ticks >= ImpactChargeRules.TIER_3_TICKS;
		boolean tier2 = ticks >= ImpactChargeRules.TIER_2_TICKS;

		HeroTheme theme = ClientHeroState.theme();
		int bright = tier3 ? 0xFFFF4D6A : theme.energyBright();
		int dark = tier3 ? 0xFF7A1430 : theme.energyDark();
		int glow = tier3 ? 0x88FF4D6A : theme.energyGlow();

		float pulse = tier3
				? 0.7f + 0.3f * (float) Math.sin(Util.getMillis() * (2.0 * Math.PI / 450.0))
				: 1f;

		if (WildShaders.rectReady()) {
			// стеклянная капсула-основа с лёгкой каймой темы
			int border = (0x77 << 24) | (theme.panelBorder() & 0x00FFFFFF);
			WildRenderer.panel(graphics, gaugeX, gaugeY, GAUGE_W, GAUGE_H, GAUGE_W / 2f,
					BG_COLOR, BG_COLOR, border, 1f,
					tier3 ? (((int) (0x66 * pulse)) << 24 | (bright & 0x00FFFFFF)) : 0,
					tier3 ? 5f : 0f);

			// светящаяся заливка снизу вверх
			int filled = (int) ((GAUGE_H - 2) * progress);
			if (filled > 2) {
				int fillY = gaugeY + 1 + (GAUGE_H - 2) - filled;
				WildRenderer.panel(graphics, gaugeX + 1, fillY, GAUGE_W - 2, filled,
						(GAUGE_W - 2) / 2f, bright, dark, 0, 0f,
						(((int) (0x99 * pulse)) << 24) | (glow & 0x00FFFFFF), 4f);
			}

			// насечки тиров
			drawNotch(graphics, gaugeX, gaugeY, ImpactChargeRules.TIER_2_TICKS, tier2, bright);
			drawNotch(graphics, gaugeX, gaugeY, ImpactChargeRules.TIER_3_TICKS, tier3, bright);
		} else {
			// легаси-фолбэк без шейдеров
			graphics.fill(gaugeX, gaugeY, gaugeX + GAUGE_W, gaugeY + GAUGE_H, 0x90000000);
			int filled = (int) ((GAUGE_H - 2) * progress);
			graphics.fill(gaugeX + 1, gaugeY + 1 + (GAUGE_H - 2) - filled,
					gaugeX + GAUGE_W - 1, gaugeY + GAUGE_H - 1, bright);
		}

		if (tier2) {
			Component label = HudUtil.text(Component.translatable(
					tier3 ? "hud.superheroes.melee_tier.3" : "hud.superheroes.melee_tier.2"
			)).withStyle(ChatFormatting.BOLD);
			int labelX = gaugeX + GAUGE_W + 4;
			int labelY = gaugeY + (GAUGE_H - mc.font.lineHeight) / 2;
			graphics.drawString(mc.font, label, labelX, labelY, bright, true);
		}
	}

	private static void drawNotch(GuiGraphics g, int gaugeX, int gaugeY, int tierTicks, boolean reached, int bright) {
		float frac = tierTicks / (float) ImpactChargeRules.CAP_TICKS;
		int y = gaugeY + 1 + (GAUGE_H - 2) - (int) ((GAUGE_H - 2) * frac);
		int color = reached ? (0xEE << 24 | (bright & 0x00FFFFFF)) : 0x66FFFFFF;
		g.fill(gaugeX - 2, y, gaugeX + GAUGE_W + 2, y + 1, color);
	}
}
