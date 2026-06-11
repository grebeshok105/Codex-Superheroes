package com.example.superheroes.client.hud;

import com.example.superheroes.client.ClientAbilityCooldowns;
import com.example.superheroes.client.ClientHeroState;
import com.example.superheroes.hero.Hero;
import com.example.superheroes.hero.HeroHudConfig;
import com.example.superheroes.hero.HeroTheme;
import com.example.superheroes.hero.Heroes;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public final class HeroInfoPanelHud {
	private static final int BASE_PANEL_W = 220;
	private static final int BASE_PANEL_H = 130;
	private static final int BASE_MARGIN = 8;
	private static final int MODEL_W = 60;
	// Bust view: zoomed to the face, visible down to just below the shoulders
	private static final int MODEL_SCALE = 62;
	private static final float MODEL_Y_OFFSET = 0.62f;

	/** Cache: widest ability name per hero (font widths are stable per session). */
	private static ResourceLocation cachedWidthHero = null;
	private static int cachedMaxNameW = 0;

	private static float displayedHp = 0f;
	private static float displayedEnergy = 0f;
	private static float lastDisplayedHp = 0f;
	private static float lastDisplayedEnergy = 0f;

	private HeroInfoPanelHud() {
	}

	/**
	 * Panel width auto-fits the longest ability name of the current hero so the
	 * two-column ready-list never clips text. Mirrored by HudEditScreen.bounds().
	 */
	public static int panelWidth() {
		int base = HudScaler.scale(BASE_PANEL_W);
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || !ClientHeroState.data().hasHero()) {
			return base;
		}
		ResourceLocation heroId = ClientHeroState.data().heroId();
		if (!heroId.equals(cachedWidthHero)) {
			int maxW = 0;
			for (ResourceLocation aid : ClientHeroState.abilities()) {
				String name = Component.translatable(AbilityDescriptions.nameKey(aid)).getString();
				maxW = Math.max(maxW, mc.font.width(name));
			}
			cachedWidthHero = heroId;
			cachedMaxNameW = maxW;
		}
		// column = dot + gap + name + gap + cooldown text ("9.9s")
		int colW = HudScaler.scale(3 + 3) + cachedMaxNameW + HudScaler.scale(6) + mc.font.width("9.9s");
		int needed = HudScaler.scale(MODEL_W) + HudScaler.scale(8) + colW * 2 + HudScaler.scale(4);
		return Math.max(base, Math.min(needed, HudScaler.scale(330)));
	}

	public static void tick() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || !ClientHeroState.data().hasHero()) {
			displayedHp = 0f;
			displayedEnergy = 0f;
			return;
		}
		lastDisplayedHp = displayedHp;
		lastDisplayedEnergy = displayedEnergy;
		displayedHp = HudAnimator.smoothBar(displayedHp, mc.player.getHealth(), 0.15f);
		displayedEnergy = HudAnimator.smoothBar(displayedEnergy, ClientHeroState.data().energy(), 0.2f);
	}

	public static void render(GuiGraphics graphics, DeltaTracker tracker) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || !ClientHeroState.data().hasHero()) {
			return;
		}
		if (mc.options.hideGui) {
			return;
		}

		ResourceLocation heroId = ClientHeroState.data().heroId();
		Hero hero = Heroes.get(heroId);
		if (hero == null) {
			return;
		}

		HeroTheme theme = ClientHeroState.theme();
		HeroHudConfig hudConfig = hero.getHudConfig();
		float partial = tracker.getGameTimeDeltaPartialTick(false);

		int screenH = HudScaler.screenHeight();
		int margin = HudScaler.scale(BASE_MARGIN);
		int panelW = panelWidth();
		int panelH = HudScaler.scale(BASE_PANEL_H);
		int[] off = HudLayoutManager.offset(HudLayoutManager.HERO_PANEL);
		int x = margin + off[0];
		int y = screenH - panelH - margin + off[1];
		int modelW = HudScaler.scale(MODEL_W);

		// Panel background
		drawPanelBg(graphics, x, y, panelW, panelH, theme);

		// 3D Character bust (left side, zoomed to face/shoulders)
		int modelX1 = x + 2;
		int modelY1 = y + HudScaler.scale(14);
		int modelX2 = x + modelW;
		int modelY2 = y + panelH - HudScaler.scale(28);
		int modelScale = HudScaler.scale(MODEL_SCALE);
		try {
			int mouseX = (int) mc.mouseHandler.xpos();
			int mouseY = (int) mc.mouseHandler.ypos();
			InventoryScreen.renderEntityInInventoryFollowsMouse(
					graphics, modelX1, modelY1, modelX2, modelY2,
					modelScale, MODEL_Y_OFFSET, mouseX, mouseY, mc.player);
		} catch (Exception ignored) {
		}

		// Right side content
		int contentX = x + modelW + HudScaler.scale(4);
		int contentW = panelW - modelW - HudScaler.scale(8);
		int cursorY = y + HudScaler.scale(6);

		// Hero name (no caps-lock shouting, soft theme color)
		Component nameComp = Component.translatable("hero.superheroes." + heroId.getPath())
				.copy().withStyle(ChatFormatting.BOLD);
		graphics.drawString(mc.font, nameComp, contentX, cursorY, theme.heroNameColor(), true);
		cursorY += HudScaler.scale(12);

		// HP row: heart + numbers + thin bar
		float hp = lastDisplayedHp + (displayedHp - lastDisplayedHp) * partial;
		float maxHp = mc.player.getMaxHealth();
		drawHpRow(graphics, mc, contentX, cursorY, contentW, hp, maxHp);
		cursorY += HudScaler.scale(15);

		// Energy: single themed row — icon + label + percent, segmented bar below
		float energy = lastDisplayedEnergy + (displayedEnergy - lastDisplayedEnergy) * partial;
		float energyMax = ClientHeroState.energyMax();
		int iconSize = HudScaler.scale(9);
		HudIcons.drawEnergyIcon(graphics, contentX, cursorY, iconSize, hudConfig.energyIcon(), theme.energyIcon());
		Component energyLabel = Component.translatable(hudConfig.energyName());
		graphics.drawString(mc.font, energyLabel, contentX + iconSize + HudScaler.scale(3), cursorY,
				soften(theme.energyIcon()), true);
		int pct = energyMax > 0 ? (int) (energy / energyMax * 100f) : 0;
		Component pctComp = Component.literal(pct + "%");
		int pctW = mc.font.width(pctComp);
		graphics.drawString(mc.font, pctComp, contentX + contentW - pctW, cursorY, 0xFFD8DCE8, true);
		cursorY += HudScaler.scale(11);

		drawSegmentedBar(graphics, contentX, cursorY, contentW, HudScaler.scale(5),
				energy / Math.max(1f, energyMax), theme.energyBright(), 10);
		cursorY += HudScaler.scale(9);

		// Ability readiness list: every visible ability with its status, two columns
		drawReadyList(graphics, mc, contentX, cursorY, contentW, heroId, hudConfig, theme);

		// Passives section
		int passiveCount = AbilityDescriptions.passiveCount(heroId);
		if (passiveCount > 0) {
			int passiveY = y + panelH - HudScaler.scale(24);
			int passiveSectionX = x + HudScaler.scale(4);

			Component passivesLabel = Component.translatable("hud.superheroes.passives");
			graphics.drawString(mc.font, passivesLabel, passiveSectionX, passiveY,
					applyAlpha(theme.heroNameColor(), 200, 0.8f), true);
			passiveY += HudScaler.scale(10);

			int maxDisplay = Math.min(passiveCount, 5);
			int iconSz = HudScaler.scale(passiveCount > 5 ? 8 : 10);
			int gap = HudScaler.scale(passiveCount > 5 ? 2 : 4);
			for (int i = 0; i < maxDisplay; i++) {
				int ix = passiveSectionX + i * (iconSz + gap + HudScaler.scale(2));
				HudUtil.roundedRectFill(graphics, ix, passiveY, iconSz, iconSz, 0x44000000);
				HudUtil.roundedRectBorder(graphics, ix, passiveY, iconSz, iconSz, applyAlpha(theme.energyIcon(), 180, 0.6f));
			}
		}
	}

	private static void drawHpRow(GuiGraphics g, Minecraft mc, int x, int y, int w, float hp, float maxHp) {
		int iconSize = HudScaler.scale(8);
		int textOffset = iconSize + HudScaler.scale(2);
		g.drawString(mc.font, "\u2764", x, y, 0xFFFF5555, true);
		String valText = formatValue(hp) + " / " + formatValue(maxHp);
		g.drawString(mc.font, Component.literal(valText), x + textOffset, y, 0xFFD8DCE8, true);

		int barY = y + HudScaler.scale(9);
		int barW = w - textOffset - HudScaler.scale(2);
		int barH = HudScaler.scale(3);
		g.fill(x + textOffset, barY, x + textOffset + barW, barY + barH, 0x44000000);
		float pctHp = maxHp > 0 ? Math.min(1f, hp / maxHp) : 0f;
		int fillW = (int) (barW * pctHp);
		if (fillW > 0) {
			g.fill(x + textOffset, barY, x + textOffset + fillW, barY + barH, 0xFFFF4444);
		}
	}

	private static void drawReadyList(GuiGraphics g, Minecraft mc, int x, int y, int w,
			ResourceLocation heroId, HeroHudConfig hudConfig, HeroTheme theme) {
		List<ResourceLocation> abilities = ClientHeroState.abilities();
		if (abilities.isEmpty()) {
			return;
		}
		int cols = 2;
		int colW = w / cols;
		int lineH = HudScaler.scale(9);
		int maxRows = 4;
		int shown = Math.min(abilities.size(), cols * maxRows);
		for (int i = 0; i < shown; i++) {
			ResourceLocation aid = abilities.get(i);
			int col = i % cols;
			int row = i / cols;
			int ix = x + col * colW;
			int iy = y + row * lineH;
			int cd = ClientAbilityCooldowns.remainingTicks(aid);
			boolean ready = cd <= 0;
			boolean isUlt = hudConfig.hasUltimate() && i == abilities.size() - 1;

			// status dot
			int dotSize = HudScaler.scale(3);
			int dotColor = ready ? (isUlt ? 0xFFFFD24A : 0xFF6BFF8C) : 0xFF8A8FA0;
			if (ready && isUlt) {
				float pulse = HudAnimator.pulse(1.2f);
				dotColor = ((int) (170 + 85 * pulse) << 24) | 0x00FFD24A;
			}
			g.fill(ix, iy + HudScaler.scale(2), ix + dotSize, iy + HudScaler.scale(2) + dotSize, dotColor);

			// name (truncated)
			String name = Component.translatable(AbilityDescriptions.nameKey(aid)).getString();
			int maxNameW = colW - HudScaler.scale(24);
			if (mc.font.width(name) > maxNameW) {
				String ell = "\u2026";
				name = mc.font.plainSubstrByWidth(name, maxNameW - mc.font.width(ell)) + ell;
			}
			int nameColor = ready ? (isUlt ? 0xFFFFE9B0 : 0xFFCBD2E0) : 0xFF7C8499;
			g.drawString(mc.font, Component.literal(name), ix + dotSize + HudScaler.scale(3), iy, nameColor, true);

			// cooldown seconds on the right of the column
			if (!ready) {
				String cdText = cdSeconds(cd);
				int cdW = mc.font.width(cdText);
				g.drawString(mc.font, Component.literal(cdText), ix + colW - cdW - HudScaler.scale(4), iy, 0xFFB99A6B, true);
			}
		}
	}

	private static String cdSeconds(int ticks) {
		float s = ticks / 20f;
		if (s < 10f) {
			return String.format(java.util.Locale.ROOT, "%.1fs", s);
		}
		return ((int) Math.ceil(s)) + "s";
	}

	private static void drawPanelBg(GuiGraphics g, int x, int y, int w, int h, HeroTheme theme) {
		HudUtil.dropShadow(g, x, y, w, h, 3, 0x44000000);
		HudUtil.roundedRectGradient(g, x, y, w, h, applyAlpha(theme.panelTop(), 230, 0.85f), applyAlpha(theme.panelBottom(), 230, 0.85f));
		HudUtil.roundedRectBorder(g, x, y, w, h, applyAlpha(theme.panelBorder(), 220, 0.9f));
		g.fill(x + 3, y + 2, x + w - 3, y + 3, applyAlpha(theme.panelHighlight(), 200, 0.7f));
	}

	private static void drawSegmentedBar(GuiGraphics g, int x, int y, int w, int h, float pct, int color, int segments) {
		int segW = (w - (segments - 1)) / segments;
		for (int i = 0; i < segments; i++) {
			int sx = x + i * (segW + 1);
			float segStart = (float) i / segments;
			float segEnd = (float) (i + 1) / segments;
			if (pct >= segEnd) {
				g.fill(sx, y, sx + segW, y + h, color);
			} else if (pct > segStart) {
				float segPct = (pct - segStart) / (segEnd - segStart);
				g.fill(sx, y, sx + (int) (segW * segPct), y + h, color);
				g.fill(sx + (int) (segW * segPct), y, sx + segW, y + h, 0x33FFFFFF);
			} else {
				g.fill(sx, y, sx + segW, y + h, 0x33FFFFFF);
			}
		}
	}

	private static String formatValue(float v) {
		if (v >= 1000f) {
			return String.format(java.util.Locale.ROOT, "%,.0f", v);
		}
		if (v == Math.floor(v)) {
			return Integer.toString((int) v);
		}
		return String.format(java.util.Locale.ROOT, "%.1f", v);
	}

	private static int soften(int argb) {
		return applyAlpha(argb, 235, 1f);
	}

	private static int applyAlpha(int argb, int alpha, float mult) {
		int originalA = (argb >>> 24) & 0xFF;
		int finalA = Math.min(255, Math.max(0, (int) (originalA * (alpha / 255f) * mult)));
		return (finalA << 24) | (argb & 0x00FFFFFF);
	}

}
