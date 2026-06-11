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
 * Индикатор заряда удара у прицела: стеклянная капсула с плавно растущей
 * градиентной заливкой. Никаких насечек и делений — накопление сглажено,
 * цвет тира перетекает плавным кроссфейдом (тема героя → алый на максимуме).
 */
public final class MeleeChargeHud {
	private static final int GAUGE_W = 8;
	private static final int GAUGE_H = 26;
	private static final int BG_COLOR = 0xE002030A;

	/** Сглаженный прогресс — капсула наполняется плавно, без рывков по тикам. */
	private static float smoothProgress;
	private static boolean wasCharging;

	private MeleeChargeHud() {
	}

	public static void render(GuiGraphics graphics, DeltaTracker tracker) {
		if (!ClientMeleeChargeState.charging()) {
			wasCharging = false;
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.options.hideGui) return;
		int ticks = ClientMeleeChargeState.chargeTicks();
		if (ticks <= 0) return;

		int w = mc.getWindow().getGuiScaledWidth();
		int h = mc.getWindow().getGuiScaledHeight();
		int gaugeX = w / 2 + 12;
		int gaugeY = h / 2 - GAUGE_H / 2;

		float target = Math.min(1f, ticks / (float) ImpactChargeRules.CAP_TICKS);
		if (!wasCharging) {
			smoothProgress = 0f;
			wasCharging = true;
		}
		// экспоненциальное сглаживание по кадрам — очень плавный рост
		smoothProgress += (target - smoothProgress) * 0.18f;
		float progress = smoothProgress;

		// плавный кроссфейд цвета вокруг порогов тиров (никаких резких скачков)
		float t2 = smoothstep(ImpactChargeRules.TIER_2_TICKS - 6, ImpactChargeRules.TIER_2_TICKS + 4, ticks);
		float t3 = smoothstep(ImpactChargeRules.TIER_3_TICKS - 8, ImpactChargeRules.TIER_3_TICKS + 4, ticks);

		HeroTheme theme = ClientHeroState.theme();
		int bright = blend(theme.energyBright(), 0xFFFF4D6A, t3);
		int dark = blend(theme.energyDark(), 0xFF7A1430, t3);
		int glow = blend(theme.energyGlow(), 0x88FF4D6A, t3);

		float pulse = 0.7f + 0.3f * (float) Math.sin(Util.getMillis() * (2.0 * Math.PI / 450.0));
		float glowStrength = 0.35f + 0.65f * t3 * pulse;

		if (WildShaders.rectReady()) {
			// стеклянная капсула-основа с лёгкой каймой темы
			int border = (0x77 << 24) | (theme.panelBorder() & 0x00FFFFFF);
			int haloA = (int) (0x66 * t3 * pulse);
			WildRenderer.panel(graphics, gaugeX, gaugeY, GAUGE_W, GAUGE_H, GAUGE_W / 2f,
					BG_COLOR, BG_COLOR, border, 1f,
					haloA > 6 ? ((haloA << 24) | (bright & 0x00FFFFFF)) : 0,
					haloA > 6 ? 5f : 0f);

			// плавная градиентная заливка снизу вверх
			float filled = (GAUGE_H - 2) * progress;
			if (filled > 2f) {
				float fillY = gaugeY + 1 + (GAUGE_H - 2) - filled;
				WildRenderer.panel(graphics, gaugeX + 1, fillY, GAUGE_W - 2, filled,
						(GAUGE_W - 2) / 2f, bright, dark, 0, 0f,
						(((int) (0x99 * glowStrength)) << 24) | (glow & 0x00FFFFFF), 4f);
			}
		} else {
			// легаси-фолбэк без шейдеров
			graphics.fill(gaugeX, gaugeY, gaugeX + GAUGE_W, gaugeY + GAUGE_H, 0x90000000);
			int filled = (int) ((GAUGE_H - 2) * progress);
			graphics.fill(gaugeX + 1, gaugeY + 1 + (GAUGE_H - 2) - filled,
					gaugeX + GAUGE_W - 1, gaugeY + GAUGE_H - 1, bright);
		}

		if (t2 > 0.01f) {
			boolean tier3 = t3 > 0.5f;
			Component label = HudUtil.text(Component.translatable(
					tier3 ? "hud.superheroes.melee_tier.3" : "hud.superheroes.melee_tier.2"
			)).withStyle(ChatFormatting.BOLD);
			int labelX = gaugeX + GAUGE_W + 4;
			int labelY = gaugeY + (GAUGE_H - mc.font.lineHeight) / 2;
			int alpha = Math.max(20, (int) (255 * t2));
			graphics.drawString(mc.font, label, labelX, labelY,
					(alpha << 24) | (bright & 0x00FFFFFF), true);
		}
	}

	private static float smoothstep(float edge0, float edge1, float x) {
		float t = Math.clamp((x - edge0) / (edge1 - edge0), 0f, 1f);
		return t * t * (3f - 2f * t);
	}

	private static int blend(int a, int b, float t) {
		int aa = (a >>> 24) & 0xFF, ar = (a >>> 16) & 0xFF, ag = (a >>> 8) & 0xFF, ab = a & 0xFF;
		int ba = (b >>> 24) & 0xFF, br = (b >>> 16) & 0xFF, bg = (b >>> 8) & 0xFF, bb = b & 0xFF;
		int ra = (int) (aa + (ba - aa) * t);
		int rr = (int) (ar + (br - ar) * t);
		int rg = (int) (ag + (bg - ag) * t);
		int rb = (int) (ab + (bb - ab) * t);
		return (ra << 24) | (rr << 16) | (rg << 8) | rb;
	}
}
