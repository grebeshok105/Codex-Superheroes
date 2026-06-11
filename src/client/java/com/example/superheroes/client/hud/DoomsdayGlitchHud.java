package com.example.superheroes.client.hud;

import com.example.superheroes.ability.AbilityIds;
import com.example.superheroes.client.ClientDoomsdayState;
import com.example.superheroes.client.ClientHeroState;
import com.example.superheroes.hero.DoomsdayHero;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

public final class DoomsdayGlitchHud {
	private static final RandomSource RNG = RandomSource.create();
	private static final String[] GLYPHS = {
			"Ω", "Ψ", "Σ", "Δ", "Ξ", "Λ", "Φ", "Ϟ",
			"卐", "禁", "死", "魂", "鬼", "邪",
			"ᚠ", "ᚱ", "ᛟ", "ᛞ",
			"☣", "☠", "✶", "✺", "✷"
	};

	private DoomsdayGlitchHud() {
	}

	public static void render(GuiGraphics graphics, DeltaTracker tracker) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.options.hideGui) return;
		if (!isDoomsday()) return;
		float pct = ClientDoomsdayState.glitchPct();
		if (pct <= 0f) return;

		int w = mc.getWindow().getGuiScaledWidth();
		int h = mc.getWindow().getGuiScaledHeight();
		Font font = mc.font;
		int tier = ClientDoomsdayState.tier();

		// Tier badge top-right with chromatic aberration scaled by pct
		String label = "T" + tier + "/7";
		int tx = w - font.width(label) - 8;
		int ty = 6;
		drawAberrated(graphics, font, label, tx, ty, 0xFFD83A1A, pct);

		// Random glyph rain — count scales with pct (max 30 at T7)
		int glyphCount = (int) (30 * pct);
		for (int i = 0; i < glyphCount; i++) {
			if (RNG.nextFloat() > 0.4f) continue;
			int gx = RNG.nextInt(w);
			int gy = RNG.nextInt(h);
			String g = GLYPHS[RNG.nextInt(GLYPHS.length)];
			int alpha = 60 + RNG.nextInt(140);
			int color = (alpha << 24) | (RNG.nextBoolean() ? 0xD83A1A : 0xB23ACC);
			graphics.drawString(font, g, gx, gy, color, false);
		}

		// Scanline / jitter overlay — tinted strips at higher tiers
		if (pct >= 0.5f) {
			int stripCount = (int) (8 * pct);
			for (int i = 0; i < stripCount; i++) {
				int sy = RNG.nextInt(h);
				int sh = 1 + RNG.nextInt(2);
				int a = 30 + RNG.nextInt(40);
				int c = (a << 24) | 0xD83A1A;
				graphics.fill(0, sy, w, sy + sh, c);
			}
		}

		// Tier description bottom-right (keeps the hero panel in the bottom-left clear)
		String tierName = Component.translatable("hero.superheroes.doomsday.tier." + tier).getString();
		int dx = w - font.width(tierName) - 8;
		int dy = h - 22;
		drawAberrated(graphics, font, tierName, dx, dy, 0xFFE7C7A8, pct * 0.5f);
	}

	private static void drawAberrated(GuiGraphics graphics, Font font, String text, int x, int y, int color, float pct) {
		if (pct > 0.05f) {
			int offX = (int) (1 + pct * 2);
			int offY = (int) (pct * 1);
			int redCol = 0xCCFF0040;
			int cyanCol = 0xCC00DDFF;
			graphics.drawString(font, text, x - offX, y + offY, redCol, false);
			graphics.drawString(font, text, x + offX, y - offY, cyanCol, false);
		}
		int jitterX = pct > 0.1f && RNG.nextFloat() < pct ? RNG.nextInt(3) - 1 : 0;
		int jitterY = pct > 0.1f && RNG.nextFloat() < pct ? RNG.nextInt(3) - 1 : 0;
		graphics.drawString(font, text, x + jitterX, y + jitterY, color, false);
	}

	private static boolean isDoomsday() {
		return ClientHeroState.data().hasHero()
				&& DoomsdayHero.ID.equals(ClientHeroState.data().heroId());
	}
}
