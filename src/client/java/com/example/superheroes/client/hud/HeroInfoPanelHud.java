package com.example.superheroes.client.hud;

import com.example.superheroes.client.ClientAbilityCooldowns;
import com.example.superheroes.client.ClientHeroState;
import com.example.superheroes.hero.Hero;
import com.example.superheroes.hero.HeroHudConfig;
import com.example.superheroes.hero.HeroTheme;
import com.example.superheroes.hero.Heroes;
import com.example.superheroes.transform.HeroData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaternionf;

import java.util.List;

public final class HeroInfoPanelHud {
	private static final int BASE_PANEL_W = 220;
	private static final int BASE_PANEL_H = 130;
	private static final int BASE_MARGIN = 8;
	private static final int MODEL_W = 60;

	private static float displayedHp = 0f;
	private static float displayedEnergy = 0f;
	private static float displayedMana = 0f;
	private static float lastDisplayedHp = 0f;
	private static float lastDisplayedEnergy = 0f;
	private static float lastDisplayedMana = 0f;

	private HeroInfoPanelHud() {
	}

	public static void tick() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || !ClientHeroState.data().hasHero()) {
			displayedHp = 0f;
			displayedEnergy = 0f;
			displayedMana = 0f;
			return;
		}
		lastDisplayedHp = displayedHp;
		lastDisplayedEnergy = displayedEnergy;
		lastDisplayedMana = displayedMana;
		displayedHp = HudAnimator.smoothBar(displayedHp, mc.player.getHealth(), 0.15f);
		displayedEnergy = HudAnimator.smoothBar(displayedEnergy, ClientHeroState.data().energy(), 0.2f);
		float manaMax = ClientHeroState.manaMax();
		if (manaMax > 0f) {
			displayedMana = HudAnimator.smoothBar(displayedMana, ClientHeroState.data().mana(), 0.2f);
		}
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
		int panelW = HudScaler.scale(BASE_PANEL_W);
		int panelH = HudScaler.scale(BASE_PANEL_H);
		int x = margin;
		int y = screenH - panelH - margin;
		int modelW = HudScaler.scale(MODEL_W);

		// Panel background
		drawPanelBg(graphics, x, y, panelW, panelH, theme);

		// 3D Character model (left side)
		int modelX1 = x + 2;
		int modelY1 = y + HudScaler.scale(14);
		int modelX2 = x + modelW;
		int modelY2 = y + panelH - HudScaler.scale(28);
		int modelScale = HudScaler.scale(28);
		try {
			int mouseX = (int) mc.mouseHandler.xpos();
			int mouseY = (int) mc.mouseHandler.ypos();
			InventoryScreen.renderEntityInInventoryFollowsMouse(
					graphics, modelX1, modelY1, modelX2, modelY2,
					modelScale, 0.0625f, mouseX, mouseY, mc.player);
		} catch (Exception ignored) {
		}

		// Right side content
		int contentX = x + modelW + HudScaler.scale(4);
		int contentW = panelW - modelW - HudScaler.scale(8);
		int cursorY = y + HudScaler.scale(6);

		// Hero name (styled, no "Form:")
		String heroName = Component.translatable("hero.superheroes." + heroId.getPath()).getString().toUpperCase();
		Component nameComp = Component.literal(heroName).withStyle(ChatFormatting.BOLD);
		graphics.drawString(mc.font, nameComp, contentX, cursorY, theme.heroNameColor(), true);
		cursorY += HudScaler.scale(12);

		// HP bar
		float hp = lastDisplayedHp + (displayedHp - lastDisplayedHp) * partial;
		float maxHp = mc.player.getMaxHealth();
		drawResourceBar(graphics, mc, contentX, cursorY, contentW, hp, maxHp,
				0xFFFF4444, 0xFFCC2222, 0xFFFF6666, "\u2764", null);
		cursorY += HudScaler.scale(14);

		// Energy bar
		float energy = lastDisplayedEnergy + (displayedEnergy - lastDisplayedEnergy) * partial;
		float energyMax = ClientHeroState.energyMax();
		int energyColor = theme.energyBright();
		int energyDark = applyAlpha(energyColor, 200, 0.7f);
		drawResourceBar(graphics, mc, contentX, cursorY, contentW, energy, energyMax,
				energyColor, energyDark, theme.energyIcon(), null, hudConfig.energyIcon());
		cursorY += HudScaler.scale(14);

		// Mana bar (only if hero has mana)
		float manaMax = ClientHeroState.manaMax();
		if (manaMax > 0f) {
			float mana = lastDisplayedMana + (displayedMana - lastDisplayedMana) * partial;
			int manaColor = theme.manaBright() != 0 ? theme.manaBright() : 0xFF4488FF;
			drawResourceBar(graphics, mc, contentX, cursorY, contentW, mana, manaMax,
					manaColor, applyAlpha(manaColor, 200, 0.7f), 0xFF6699FF, "\u2B29", null);
			cursorY += HudScaler.scale(14);
		}

		// Themed energy label + percentage
		Component energyLabel = Component.translatable(hudConfig.energyName());
		int pct = energyMax > 0 ? (int) (energy / energyMax * 100f) : 0;
		graphics.drawString(mc.font, energyLabel, contentX, cursorY, theme.energyIcon(), true);
		Component pctComp = Component.literal(pct + "%").withStyle(ChatFormatting.BOLD);
		int pctW = mc.font.width(pctComp);
		graphics.drawString(mc.font, pctComp, contentX + contentW - pctW, cursorY, theme.energyIcon(), true);
		cursorY += HudScaler.scale(10);

		// Energy mini-bar (segmented)
		drawSegmentedBar(graphics, contentX, cursorY, contentW, HudScaler.scale(4),
				energy / Math.max(1f, energyMax), theme.energyBright(), 8);
		cursorY += HudScaler.scale(8);

		// Ultimate ready indicator
		if (hudConfig.hasUltimate() && hudConfig.ultimateName() != null) {
			List<ResourceLocation> abilities = ClientHeroState.abilities();
			if (!abilities.isEmpty()) {
				ResourceLocation ultId = abilities.get(abilities.size() - 1);
				int cdTicks = ClientAbilityCooldowns.remainingTicks(ultId);
				if (cdTicks <= 0) {
					float pulse = HudAnimator.pulse(1.2f);
					int alpha = (int) (180 + 75 * pulse);
					int color = (alpha << 24) | (theme.energyIcon() & 0x00FFFFFF);
					Component ultReady = Component.literal(hudConfig.ultimateName() + " READY").withStyle(ChatFormatting.BOLD);
					int ultW = mc.font.width(ultReady);
					graphics.drawString(mc.font, ultReady, contentX + (contentW - ultW) / 2, cursorY, color, true);
				}
			}
		}
		cursorY += HudScaler.scale(10);

		// Passives section
		int passiveCount = AbilityDescriptions.passiveCount(heroId);
		if (passiveCount > 0) {
			int passiveY = y + panelH - HudScaler.scale(24);
			int passiveSectionX = x + HudScaler.scale(4);
			int passiveAreaW = panelW - HudScaler.scale(8);

			// "PASSIVES" label
			Component passivesLabel = Component.translatable("hud.superheroes.passives").withStyle(ChatFormatting.BOLD);
			graphics.drawString(mc.font, passivesLabel, passiveSectionX, passiveY, applyAlpha(theme.heroNameColor(), 200, 0.8f), true);
			passiveY += HudScaler.scale(10);

			// Passive icons row
			int maxDisplay = Math.min(passiveCount, 5);
			int iconSize = HudScaler.scale(passiveCount > 5 ? 8 : 10);
			int gap = HudScaler.scale(passiveCount > 5 ? 2 : 4);
			for (int i = 0; i < maxDisplay; i++) {
				int ix = passiveSectionX + i * (iconSize + gap + HudScaler.scale(2));
				HudUtil.roundedRectFill(graphics, ix, passiveY, iconSize, iconSize, 0x44000000);
				HudUtil.roundedRectBorder(graphics, ix, passiveY, iconSize, iconSize, applyAlpha(theme.energyIcon(), 180, 0.6f));
			}
		}
	}

	private static void drawPanelBg(GuiGraphics g, int x, int y, int w, int h, HeroTheme theme) {
		HudUtil.dropShadow(g, x, y, w, h, 3, 0x44000000);
		HudUtil.roundedRectGradient(g, x, y, w, h, applyAlpha(theme.panelTop(), 230, 0.85f), applyAlpha(theme.panelBottom(), 230, 0.85f));
		HudUtil.roundedRectBorder(g, x, y, w, h, applyAlpha(theme.panelBorder(), 220, 0.9f));
		g.fill(x + 3, y + 2, x + w - 3, y + 3, applyAlpha(theme.panelHighlight(), 200, 0.7f));
	}

	private static void drawResourceBar(GuiGraphics g, Minecraft mc, int x, int y, int w, float value, float max,
										   int fillColor, int darkColor, int iconColor,
										   String iconChar, HeroHudConfig.EnergyIconType energyIconType) {
		int barH = HudScaler.scale(8);
		int iconSize = HudScaler.scale(8);
		int textOffset = iconSize + HudScaler.scale(2);

		// Icon
		if (energyIconType != null) {
			HudIcons.drawEnergyIcon(g, x, y, iconSize, energyIconType, iconColor);
		} else if (iconChar != null) {
			g.drawString(mc.font, iconChar, x, y, iconColor, true);
		}

		// Value text
		String valText = formatValue(value) + " / " + formatValue(max);
		Component valComp = Component.literal(valText);
		g.drawString(mc.font, valComp, x + textOffset, y, 0xFFE0E0E0, true);

		// Bar underneath
		int barY = y + HudScaler.scale(9);
		int barW = w - HudScaler.scale(2);
		g.fill(x + textOffset, barY, x + textOffset + barW, barY + HudScaler.scale(3), 0x44000000);
		float pct = max > 0 ? Math.min(1f, value / max) : 0f;
		int fillW = (int) (barW * pct);
		if (fillW > 0) {
			g.fill(x + textOffset, barY, x + textOffset + fillW, barY + HudScaler.scale(3), fillColor);
		}
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

	private static int applyAlpha(int argb, int alpha, float mult) {
		int originalA = (argb >>> 24) & 0xFF;
		int finalA = Math.min(255, Math.max(0, (int) (originalA * (alpha / 255f) * mult)));
		return (finalA << 24) | (argb & 0x00FFFFFF);
	}

}
