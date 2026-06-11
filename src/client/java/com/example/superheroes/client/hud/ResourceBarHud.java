package com.example.superheroes.client.hud;

import com.example.superheroes.client.ClientHeroState;
import com.example.superheroes.client.render.WildRenderer;
import com.example.superheroes.client.render.WildShaders;
import com.example.superheroes.hero.HeroTheme;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class ResourceBarHud {
	private static final int X = 12;
	private static final int Y = 12;
	private static final int PANEL_WIDTH = 260;
	private static final int PANEL_HEIGHT_DUAL = 70;
	private static final int PANEL_HEIGHT_SOLO = 48;
	private static final int BAR_WIDTH = 162;
	private static final int BAR_HEIGHT = 10;
	private static final int BAR_X_OFFSET = 38;
	private static final int ICON_SIZE = 18;
	private static final int VALUE_GAP = 6;
	private static final int HERO_BADGE_SIZE = 14;

	private static final int SHADOW = 0x66000000;
	private static final int BAR_BG = 0xFF02030A;
	private static final int BAR_INNER_SHADOW = 0xAA000000;

	private static final int VALUE_COLOR = 0xFFEFEFF7;
	private static final int VALUE_DIM = 0xFF8B8FA0;

	private ResourceBarHud() {
	}

	public static void render(GuiGraphics graphics, DeltaTracker tracker) {
		if (!ClientHeroState.data().hasHero()) {
			return;
		}
		float ramp = ClientHudGlitch.ramp();
		if (ramp > 0.001f) {
			graphics.pose().pushPose();
			graphics.pose().translate(ClientHudGlitch.jitterX(), ClientHudGlitch.jitterY(), 0f);
			renderInner(graphics, tracker);
			graphics.pose().popPose();
			if (ClientHudGlitch.ghostDouble()) {
				int gx = ClientHudGlitch.ghostOffsetX();
				graphics.pose().pushPose();
				graphics.pose().translate(gx, 0f, 0f);
				renderInner(graphics, tracker);
				graphics.pose().popPose();
			}
			return;
		}
		renderInner(graphics, tracker);
	}

	private static void renderInner(GuiGraphics graphics, DeltaTracker tracker) {
		HeroTheme theme = ClientHeroState.theme();
		int themePanelTop = ClientHudGlitch.tintColor(theme.panelTop());
		int themePanelBottom = ClientHudGlitch.tintColor(theme.panelBottom());
		int themePanelBorder = ClientHudGlitch.tintColor(theme.panelBorder());
		int themePanelHighlight = ClientHudGlitch.tintColor(theme.panelHighlight());
		int themeHeroNameColor = ClientHudGlitch.tintColor(theme.heroNameColor());
		int themeEnergyIcon = ClientHudGlitch.tintColor(theme.energyIcon());
		int themeEnergyDark = ClientHudGlitch.tintColor(theme.energyDark());
		int themeEnergyBright = ClientHudGlitch.tintColor(theme.energyBright());
		int themeEnergyGlow = ClientHudGlitch.tintColor(theme.energyGlow());
		int themeManaIcon = ClientHudGlitch.tintColor(theme.manaIcon());
		int themeManaDark = ClientHudGlitch.tintColor(theme.manaDark());
		int themeManaBright = ClientHudGlitch.tintColor(theme.manaBright());
		int themeManaGlow = ClientHudGlitch.tintColor(theme.manaGlow());
		float energyMax = ClientHeroState.energyMax();
		float manaMax = ClientHeroState.manaMax();
		float energy = ClientHeroState.data().energy();
		float mana = ClientHeroState.data().mana();
		float energyPct = energyMax <= 0f ? 0f : Math.min(1f, energy / energyMax);
		float manaPct = manaMax <= 0f ? 0f : Math.min(1f, mana / manaMax);
		boolean showMana = manaMax > 0f;

		Minecraft mc = Minecraft.getInstance();
		ResourceLocation heroId = ClientHeroState.data().heroId();
		Component heroName = HudUtil.text(Component.translatable("hero." + heroId.getNamespace() + "." + heroId.getPath()).copy());

		int panelHeight = showMana ? PANEL_HEIGHT_DUAL : PANEL_HEIGHT_SOLO;
		HudUtil.neonPanel(graphics, X, Y, PANEL_WIDTH, panelHeight,
				themePanelTop, themePanelBottom, themePanelBorder,
				(0x6A << 24) | (themePanelBorder & 0x00FFFFFF));

		graphics.drawString(mc.font, heroName, X + 12, Y + 6, themeHeroNameColor, true);
		HudUtil.neonAccentLine(graphics, X + 12, Y + 18, PANEL_WIDTH - 24, (themePanelBorder & 0x00FFFFFF) | 0x44000000);

		int row1Y = Y + 26;
		drawIcon(graphics, X + 12, row1Y - 4, themeEnergyIcon, "E");
		drawBar(graphics, mc, X + BAR_X_OFFSET, row1Y, energyPct, themeEnergyDark, themeEnergyBright, themeEnergyGlow);
		drawValue(graphics, mc, X + BAR_X_OFFSET + BAR_WIDTH + VALUE_GAP, row1Y - 1, energy, energyMax);

		if (showMana) {
			int row2Y = Y + 48;
			drawIcon(graphics, X + 12, row2Y - 4, themeManaIcon, "M");
			drawBar(graphics, mc, X + BAR_X_OFFSET, row2Y, manaPct, themeManaDark, themeManaBright, themeManaGlow);
			drawValue(graphics, mc, X + BAR_X_OFFSET + BAR_WIDTH + VALUE_GAP, row2Y - 1, mana, manaMax);
		}

		drawHeroBadge(graphics, theme, themeEnergyIcon, themeManaIcon, themeEnergyDark);
	}

	private static void drawHeroBadge(GuiGraphics g, HeroTheme theme, int energyIcon, int manaIcon, int energyDark) {
		int bx = X + PANEL_WIDTH - HERO_BADGE_SIZE - 6 + ClientHudGlitch.badgeJitterX();
		int by = Y + 3 + ClientHudGlitch.badgeJitterY();
		HudUtil.roundedRectFill(g, bx, by, HERO_BADGE_SIZE, HERO_BADGE_SIZE, 0xFF0A0408);
		HudUtil.roundedRectBorder(g, bx, by, HERO_BADGE_SIZE, HERO_BADGE_SIZE, energyIcon);
		int cx = bx + HERO_BADGE_SIZE / 2;
		int cy = by + HERO_BADGE_SIZE / 2;
		g.fill(cx - 3, cy - 4, cx + 3, cy - 3, manaIcon);
		g.fill(cx - 4, cy - 3, cx - 2, cy + 1, manaIcon);
		g.fill(cx + 2, cy - 3, cx + 4, cy + 1, manaIcon);
		g.fill(cx - 1, cy - 1, cx, cy, energyDark);
		g.fill(cx + 1, cy - 1, cx + 2, cy, energyDark);
		g.fill(cx - 3, cy + 2, cx + 3, cy + 3, energyIcon);
		g.fill(cx - 1, cy + 3, cx + 1, cy + 4, energyIcon);
	}

	private static void drawIcon(GuiGraphics g, int x, int y, int color, String letter) {
		if (WildShaders.circleReady()) {
			WildRenderer.orb(g, x + ICON_SIZE / 2f, y + ICON_SIZE / 2f, ICON_SIZE / 2f - 0.5f,
					0xE60A0B14, color, 1.3f, ((0x55 << 24) | (color & 0x00FFFFFF)), 3.5f);
		} else {
			HudUtil.roundedRectFill(g, x, y, ICON_SIZE, ICON_SIZE, 0xFF0A0B14);
			HudUtil.roundedRectBorder(g, x, y, ICON_SIZE, ICON_SIZE, color);
		}
		g.drawCenteredString(Minecraft.getInstance().font, HudUtil.text(letter).withStyle(ChatFormatting.BOLD),
				x + ICON_SIZE / 2, y + (ICON_SIZE - 8) / 2, color);
	}

	private static void drawBar(GuiGraphics g, Minecraft mc, int x, int y, float pct, int dark, int bright, int glow) {
		int filled = (int) (BAR_WIDTH * pct);
		if (WildShaders.rectReady()) {
			// тёмная стеклянная капсула-основа
			WildRenderer.panel(g, x, y, BAR_WIDTH, BAR_HEIGHT, BAR_HEIGHT / 2f,
					BAR_BG, BAR_BG, 0x55000000, 1f, 0, 0f);
			if (filled > 2) {
				// светящаяся заливка со скруглёнными концами
				WildRenderer.panel(g, x + 1, y + 1, Math.max(filled - 2, BAR_HEIGHT - 2), BAR_HEIGHT - 2,
						(BAR_HEIGHT - 2) / 2f, bright, dark, 0, 0f, glow, 4f);
			}
			return;
		}
		HudUtil.roundedRectFill(g, x, y, BAR_WIDTH, BAR_HEIGHT, BAR_BG);
		g.fill(x + 1, y + 1, x + BAR_WIDTH - 1, y + 2, BAR_INNER_SHADOW);
		if (filled >= 4) {
			HudUtil.roundedRectGradient(g, x, y, filled, BAR_HEIGHT, bright, dark);
			g.fill(x + 1, y + 1, x + filled - 1, y + 2, 0x55FFFFFF);
			if (filled - 8 > 0) {
				g.fill(x + filled - 8, y, x + filled, y + BAR_HEIGHT, glow);
			}
		} else if (filled > 0) {
			g.fillGradient(x, y, x + filled, y + BAR_HEIGHT, bright, dark);
		}
		HudUtil.roundedRectBorder(g, x, y, BAR_WIDTH, BAR_HEIGHT, 0x55000000);
	}

	private static void drawValue(GuiGraphics g, Minecraft mc, int x, int y, float value, float max) {
		String cur = String.valueOf((int) value);
		String tot = "/" + (int) max;
		g.drawString(mc.font, HudUtil.text(cur), x, y, VALUE_COLOR, true);
		g.drawString(mc.font, HudUtil.text(tot), x + mc.font.width(HudUtil.text(cur)), y, VALUE_DIM, true);
	}
}
