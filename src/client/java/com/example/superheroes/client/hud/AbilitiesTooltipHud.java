package com.example.superheroes.client.hud;

import com.example.superheroes.ability.AbilityIds;
import com.example.superheroes.client.ClientAbilityCooldowns;
import com.example.superheroes.client.ClientAbilityFilter;
import com.example.superheroes.client.ClientHeroState;
import com.example.superheroes.client.ClientMadnessState;
import com.example.superheroes.hero.HeroTheme;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

public final class AbilitiesTooltipHud {
	// Anchored to the RIGHT edge since the v2 HUD redesign; draggable via HUD editor.
	private static final int ANCHOR_RIGHT = 10;
	private static final int ANCHOR_TOP = 70;
	private static final int PANEL_WIDTH = 250;

	private static final int PADDING_X = 12;
	private static final int PADDING_TOP = 8;
	private static final int PADDING_BOTTOM = 10;
	private static final int SECTION_SPACING = 8;
	private static final int ICON_SIZE = 16;
	private static final int SECTION_HEADER_HEIGHT = 13;
	/** Hero-name header plaque at the top of the panel (v4 refresh). */
	private static final int HEADER_HEIGHT = 17;

	private static final int LINE_HEIGHT = 9;
	private static final int NAME_Y_OFFSET = 1;
	private static final int DESC_Y_OFFSET = 11;
	private static final int ABILITY_ROW_BOTTOM_PAD = 3;
	private static final int PASSIVE_ROW_BOTTOM_PAD = 5;
	private static final int MAX_DESC_LINES = 2;
	private static final int MAX_PASSIVE_LINES = 2;

	private static final int ANIM_TICKS = 10;
	private static final int SLIDE_DISTANCE = 12;

	private static float progress = 0f;
	private static float lastProgress = 0f;
	private static boolean userVisible = true;

	private AbilitiesTooltipHud() {
	}

	public static void toggleVisible() {
		userVisible = !userVisible;
	}

	public static boolean isUserVisible() {
		return userVisible;
	}

	public static void tick() {
		lastProgress = progress;
		boolean visible = userVisible && ClientHeroState.data().hasHero();
		float delta = 1f / ANIM_TICKS;
		if (visible) {
			progress = Math.min(1f, progress + delta);
		} else {
			progress = Math.max(0f, progress - delta);
		}
	}

	public static void render(GuiGraphics graphics, DeltaTracker tracker) {
		if ((!userVisible || !ClientHeroState.data().hasHero()) && progress <= 0f) {
			return;
		}
		float ramp = ClientHudGlitch.ramp();
		if (ramp > 0.001f) {
			graphics.pose().pushPose();
			graphics.pose().translate(ClientHudGlitch.jitterX(), ClientHudGlitch.jitterY(), 0f);
			renderInner(graphics, tracker);
			graphics.pose().popPose();
			if (ClientHudGlitch.ghostDouble()) {
				graphics.pose().pushPose();
				graphics.pose().translate(ClientHudGlitch.ghostOffsetX(), 0f, 0f);
				renderInner(graphics, tracker);
				graphics.pose().popPose();
			}
			return;
		}
		renderInner(graphics, tracker);
	}

	private static void renderInner(GuiGraphics graphics, DeltaTracker tracker) {
		float partial = tracker.getGameTimeDeltaPartialTick(false);
		float p = lastProgress + (progress - lastProgress) * partial;
		if (p <= 0.001f) {
			return;
		}
		float eased = smoothstep(p);

		ResourceLocation heroId = ClientHeroState.data().heroId();
		if (heroId == null) {
			return;
		}
		List<ResourceLocation> abilities = ClientAbilityFilter.visibleFor(ClientHeroState.abilities(), heroId);
		int passiveCount = AbilityDescriptions.passiveCount(heroId);

		int togglesCount = 0;
		int activesCount = 0;
		for (ResourceLocation id : abilities) {
			AbilityDescriptions.Kind kind = AbilityDescriptions.kindOf(id);
			if (kind == AbilityDescriptions.Kind.TOGGLE) {
				togglesCount++;
			} else {
				activesCount++;
			}
		}

		Minecraft mc = Minecraft.getInstance();
		int contentWidth = PANEL_WIDTH - PADDING_X * 2;
		int abilityTextWidth = contentWidth - ICON_SIZE - 6;
		int passiveTextWidth = contentWidth - 10;

		int panelHeight = PADDING_TOP + HEADER_HEIGHT;
		if (passiveCount > 0) {
			panelHeight += SECTION_HEADER_HEIGHT;
			for (int i = 1; i <= passiveCount; i++) {
				Component name = Component.translatable(AbilityDescriptions.passiveKey(heroId, i));
				panelHeight += passiveRowHeight(mc, name, passiveTextWidth);
			}
			panelHeight += SECTION_SPACING;
		}
		if (activesCount > 0) {
			panelHeight += SECTION_HEADER_HEIGHT;
			for (ResourceLocation id : abilities) {
				if (AbilityDescriptions.kindOf(id) != AbilityDescriptions.Kind.ACTIVE) continue;
				panelHeight += abilityRowHeight(mc, id, abilityTextWidth);
			}
			panelHeight += SECTION_SPACING;
		}
		if (togglesCount > 0) {
			panelHeight += SECTION_HEADER_HEIGHT;
			for (ResourceLocation id : abilities) {
				if (AbilityDescriptions.kindOf(id) != AbilityDescriptions.Kind.TOGGLE) continue;
				panelHeight += abilityRowHeight(mc, id, abilityTextWidth);
			}
			panelHeight += SECTION_SPACING;
		}
		panelHeight = Math.max(panelHeight - SECTION_SPACING, 0) + PADDING_BOTTOM;

		if (panelHeight < 16) {
			return;
		}

		int[] off = HudLayoutManager.offset(HudLayoutManager.TOOLTIPS);
		int xSlide = (int) ((1f - eased) * SLIDE_DISTANCE);
		int x = graphics.guiWidth() - PANEL_WIDTH - ANCHOR_RIGHT + off[0] + xSlide;
		int y = ANCHOR_TOP + off[1];

		int alpha = (int) (eased * 255f);
		if (alpha <= 2) {
			return;
		}

		HeroTheme theme = ClientHeroState.theme();
		drawPanel(graphics, x, y, PANEL_WIDTH, panelHeight, theme, alpha);

		int cursorY = y + PADDING_TOP;
		drawHeroHeader(graphics, mc, x, cursorY, heroId, theme, alpha);
		cursorY += HEADER_HEIGHT;

		if (passiveCount > 0) {
			drawSectionHeader(graphics, mc, x + PADDING_X, cursorY, contentWidth,
					Component.translatable("hud.superheroes.abilities.passives"), theme, alpha);
			cursorY += SECTION_HEADER_HEIGHT;
			for (int i = 1; i <= passiveCount; i++) {
				Component name = Component.translatable(AbilityDescriptions.passiveKey(heroId, i));
				int rowHeight = passiveRowHeight(mc, name, passiveTextWidth);
				drawPassiveRow(graphics, mc, x + PADDING_X, cursorY, name, passiveTextWidth, theme, alpha);
				cursorY += rowHeight;
			}
			cursorY += SECTION_SPACING;
		}

		if (activesCount > 0) {
			drawSectionHeader(graphics, mc, x + PADDING_X, cursorY, contentWidth,
					Component.translatable("hud.superheroes.abilities.active"), theme, alpha);
			cursorY += SECTION_HEADER_HEIGHT;
			for (ResourceLocation id : abilities) {
				if (AbilityDescriptions.kindOf(id) != AbilityDescriptions.Kind.ACTIVE) {
					continue;
				}
				int rowHeight = abilityRowHeight(mc, id, abilityTextWidth);
				drawAbilityRow(graphics, mc, x + PADDING_X, cursorY, contentWidth, id, theme, alpha);
				cursorY += rowHeight;
			}
			cursorY += SECTION_SPACING;
		}

		if (togglesCount > 0) {
			drawSectionHeader(graphics, mc, x + PADDING_X, cursorY, contentWidth,
					Component.translatable("hud.superheroes.abilities.toggle"), theme, alpha);
			cursorY += SECTION_HEADER_HEIGHT;
			for (ResourceLocation id : abilities) {
				if (AbilityDescriptions.kindOf(id) != AbilityDescriptions.Kind.TOGGLE) {
					continue;
				}
				int rowHeight = abilityRowHeight(mc, id, abilityTextWidth);
				drawAbilityRow(graphics, mc, x + PADDING_X, cursorY, contentWidth, id, theme, alpha);
				cursorY += rowHeight;
			}
		}
	}

	private static int passiveRowHeight(Minecraft mc, Component name, int maxWidth) {
		List<FormattedCharSequence> lines = mc.font.split(name, maxWidth);
		int count = Math.max(1, Math.min(MAX_PASSIVE_LINES, lines.size()));
		return count * LINE_HEIGHT + PASSIVE_ROW_BOTTOM_PAD;
	}

	private static int abilityRowHeight(Minecraft mc, ResourceLocation abilityId, int maxTextWidth) {
		Component desc = Component.translatable(AbilityDescriptions.descKey(abilityId));
		List<FormattedCharSequence> lines = mc.font.split(desc, maxTextWidth);
		int descLines = Math.max(1, Math.min(MAX_DESC_LINES, lines.size()));
		return DESC_Y_OFFSET + descLines * LINE_HEIGHT + ABILITY_ROW_BOTTOM_PAD;
	}

	private static void drawPanel(GuiGraphics g, int x, int y, int w, int h, HeroTheme theme, int alpha) {
		int shadowAlpha = Math.min(0x88, alpha / 2);
		HudUtil.dropShadow(g, x, y, w, h, 3, (shadowAlpha << 24) | 0x000000);

		int top = applyAlpha(ClientHudGlitch.tintColor(theme.panelTop()), alpha, 0.55f);
		int bottom = applyAlpha(ClientHudGlitch.tintColor(theme.panelBottom()), alpha, 0.55f);
		HudUtil.roundedRectGradient(g, x, y, w, h, top, bottom);

		int border = applyAlpha(ClientHudGlitch.tintColor(theme.panelBorder()), alpha, 0.9f);
		HudUtil.roundedRectBorder(g, x, y, w, h, border);
		int glow = applyAlpha(ClientHudGlitch.tintColor(theme.panelBorder()), alpha / 4, 1.0f);
		HudUtil.roundedRectBorder(g, x - 1, y - 1, w + 2, h + 2, glow);

		int hi = applyAlpha(ClientHudGlitch.tintColor(theme.panelHighlight()), alpha, 1.0f);
		g.fill(x + 3, y + 2, x + w - 3, y + 3, hi);
	}

	/** v4 refresh: hero name on its own plaque with a soft accent underline. */
	private static void drawHeroHeader(GuiGraphics g, Minecraft mc, int panelX, int y, ResourceLocation heroId,
			HeroTheme theme, int alpha) {
		int x = panelX + PADDING_X;
		Component name = ClientHudGlitch.maybeObfuscate(
				Component.translatable("hero.superheroes." + heroId.getPath()).copy().withStyle(ChatFormatting.BOLD));
		int color = applyAlpha(ClientHudGlitch.tintColor(theme.heroNameColor()), alpha, 1.0f);
		g.drawString(mc.font, name, x, y + 1, color, true);
		// accent underline: bright near the name, fading to the right
		int lineY = y + 12;
		int bright = applyAlpha(ClientHudGlitch.tintColor(theme.heroNameColor()), alpha, 0.85f);
		int faint = applyAlpha(ClientHudGlitch.tintColor(theme.panelBorder()), alpha, 0.25f);
		int nameW = Math.min(mc.font.width(name) + 8, PANEL_WIDTH - PADDING_X * 2);
		g.fill(x, lineY, x + nameW, lineY + 1, bright);
		g.fill(x + nameW, lineY, panelX + PANEL_WIDTH - PADDING_X, lineY + 1, faint);
	}

	private static void drawSectionHeader(GuiGraphics g, Minecraft mc, int x, int y, int width, Component label, HeroTheme theme, int alpha) {
		int color = applyAlpha(ClientHudGlitch.tintColor(theme.heroNameColor()), alpha, 1.0f);
		// vertical accent stripe to the left of the section title (v4 refresh)
		int stripe = applyAlpha(ClientHudGlitch.tintColor(theme.energyIcon()), alpha, 0.9f);
		g.fill(x, y, x + 2, y + 9, stripe);
		Component shown = ClientHudGlitch.maybeObfuscate(
				Component.empty().append(label).withStyle(ChatFormatting.BOLD));
		g.drawString(mc.font, shown, x + 6, y, color, true);
		int line = applyAlpha(ClientHudGlitch.tintColor(theme.panelBorder()), alpha, 0.35f);
		g.fill(x, y + 10, x + width, y + 11, line);
	}

	private static void drawPassiveRow(GuiGraphics g, Minecraft mc, int x, int y, Component name, int maxTextWidth, HeroTheme theme, int alpha) {
		int nameColor = applyAlpha(ClientHudGlitch.tintColor(0xFFE8E9F2), alpha, 1.0f);
		int bulletColor = applyAlpha(ClientHudGlitch.tintColor(theme.energyIcon()), alpha, 1.0f);
		g.drawString(mc.font, Component.literal("▸ ").withStyle(ChatFormatting.BOLD), x, y + NAME_Y_OFFSET, bulletColor, true);

		Component shown = ClientHudGlitch.maybeObfuscate(name);
		List<FormattedCharSequence> lines = mc.font.split(shown, maxTextWidth);
		int count = Math.min(MAX_PASSIVE_LINES, lines.size());
		for (int i = 0; i < count; i++) {
			FormattedCharSequence line = lines.get(i);
			int lineY = y + NAME_Y_OFFSET + i * LINE_HEIGHT;
			g.drawString(mc.font, line, x + 10, lineY, nameColor, true);
		}
	}

	private static void drawAbilityRow(GuiGraphics g, Minecraft mc, int x, int y, int width, ResourceLocation abilityId, HeroTheme theme, int alpha) {
		AbilityDescriptions.Kind kind = AbilityDescriptions.kindOf(abilityId);
		boolean glitchSecret = AbilityIds.COUNTER_STRIKE.equals(abilityId) && !ClientMadnessState.isMadness();
		boolean isActive = !glitchSecret && ClientHeroState.data().activeAbilities().contains(abilityId);
		int cooldownTicks = glitchSecret ? 0 : ClientAbilityCooldowns.remainingTicks(abilityId);

		int iconBg = applyAlpha(0xFF0A0B14, alpha, 1.0f);
		int themeColor = kind == AbilityDescriptions.Kind.TOGGLE ? theme.heroNameColor() : theme.energyIcon();
		int activeBorder = applyAlpha(ClientHudGlitch.tintColor(0xFF6BFF8C), alpha, 1.0f);
		int cdBorder = applyAlpha(ClientHudGlitch.tintColor(0xFFFF8866), alpha, 1.0f);
		int normalBorder = applyAlpha(ClientHudGlitch.tintColor(themeColor), alpha, 1.0f);
		int iconBorder = isActive ? activeBorder : (cooldownTicks > 0 ? cdBorder : normalBorder);
		int badgeX = x + ClientHudGlitch.badgeJitterX();
		int badgeY = y + ClientHudGlitch.badgeJitterY();
		HudUtil.roundedRectFill(g, badgeX, badgeY, ICON_SIZE, ICON_SIZE, iconBg);
		HudUtil.roundedRectBorder(g, badgeX, badgeY, ICON_SIZE, ICON_SIZE, iconBorder);
		if (glitchSecret) {
			Component badge = Component.literal("?").withStyle(ChatFormatting.OBFUSCATED, ChatFormatting.BOLD);
			g.drawCenteredString(mc.font, badge, badgeX + ICON_SIZE / 2, badgeY + (ICON_SIZE - 8) / 2, iconBorder);
		} else {
			// real ability icon inside the tile; tiny A/T marker in the corner
			AbilityIcons.draw(g, abilityId, badgeX + 1, badgeY + 1, ICON_SIZE - 2, iconBorder);
			g.pose().pushPose();
			g.pose().translate(badgeX + ICON_SIZE - 3.5f, badgeY + ICON_SIZE - 4.5f, 0f);
			g.pose().scale(0.6f, 0.6f, 1f);
			g.drawCenteredString(mc.font, Component.literal(kind.badge()).withStyle(ChatFormatting.BOLD), 0, 0, iconBorder);
			g.pose().popPose();
		}

		int statusBadgeX = x + width;
		int statusBadgeY = y + NAME_Y_OFFSET;
		Component statusText = null;
		int statusColor = 0;
		if (!glitchSecret) {
			if (cooldownTicks > 0) {
				int seconds = (cooldownTicks + 19) / 20;
				statusText = Component.literal(seconds + "с").withStyle(ChatFormatting.BOLD);
				statusColor = applyAlpha(ClientHudGlitch.tintColor(0xFFFF9D6E), alpha, 1.0f);
			} else if (isActive && kind == AbilityDescriptions.Kind.TOGGLE) {
				statusText = Component.translatable("ability.superheroes.status.on").withStyle(ChatFormatting.BOLD);
				statusColor = applyAlpha(ClientHudGlitch.tintColor(0xFF6BFF8C), alpha, 1.0f);
			} else if (kind == AbilityDescriptions.Kind.TOGGLE) {
				statusText = Component.translatable("ability.superheroes.status.off").withStyle(ChatFormatting.BOLD);
				statusColor = applyAlpha(ClientHudGlitch.tintColor(0xFF8E94A8), alpha, 1.0f);
			} else if (kind == AbilityDescriptions.Kind.ACTIVE) {
				statusText = Component.translatable("ability.superheroes.status.ready").withStyle(ChatFormatting.BOLD);
				statusColor = applyAlpha(ClientHudGlitch.tintColor(0xFFB6D4FF), alpha, 1.0f);
			}
		}
		int statusWidth = statusText == null ? 0 : (mc.font.width(statusText) + 4);
		if (statusText != null) {
			int sx = statusBadgeX - statusWidth + 2;
			g.drawString(mc.font, statusText, sx, statusBadgeY, statusColor, true);
		}

		int textX = x + ICON_SIZE + 6;
		int maxTextWidth = width - ICON_SIZE - 6 - statusWidth;
		int baseNameColor = isActive ? 0xFF6BFF8C : (cooldownTicks > 0 ? 0xFFFFD0AE : 0xFFF4F5FC);
		int nameColor = applyAlpha(ClientHudGlitch.tintColor(baseNameColor), alpha, 1.0f);
		Component name = glitchSecret
				? Component.literal("????????").withStyle(ChatFormatting.OBFUSCATED, ChatFormatting.BOLD)
				: Component.translatable(AbilityDescriptions.nameKey(abilityId)).withStyle(ChatFormatting.BOLD);
		Component nameShown = glitchSecret ? name : ClientHudGlitch.maybeObfuscate(ellipsize(mc, name, maxTextWidth));
		g.drawString(mc.font, glitchSecret ? ellipsize(mc, name, maxTextWidth) : nameShown, textX, y + NAME_Y_OFFSET, nameColor, true);

		int descColor = applyAlpha(ClientHudGlitch.tintColor(0xFFA2A6B8), alpha, 1.0f);
		Component desc = glitchSecret
				? Component.literal("????????????????????").withStyle(ChatFormatting.OBFUSCATED)
				: ClientHudGlitch.maybeObfuscate(Component.translatable(AbilityDescriptions.descKey(abilityId)));

		List<FormattedCharSequence> descLines = mc.font.split(desc, maxTextWidth);
		int lineCount = Math.min(MAX_DESC_LINES, descLines.size());
		for (int i = 0; i < lineCount; i++) {
			FormattedCharSequence line = descLines.get(i);
			int lineY = y + DESC_Y_OFFSET + i * LINE_HEIGHT;
			g.drawString(mc.font, line, textX, lineY, descColor, true);
		}

		if (cooldownTicks > 0) {
			int barX = textX;
			int barY = y + DESC_Y_OFFSET + lineCount * LINE_HEIGHT - 1;
			int barW = Math.max(20, maxTextWidth - 2);
			int barH = 2;
			int rawTotal = ClientAbilityCooldowns.totalTicks(abilityId);
			int total = rawTotal > 0 ? rawTotal : Math.max(20, cooldownTicks);
			float frac = Math.max(0f, Math.min(1f, 1f - (cooldownTicks / (float) total)));
			int trackBg = applyAlpha(0xFF1A1B25, alpha, 1.0f);
			int barFg = applyAlpha(ClientHudGlitch.tintColor(0xFFFF9D6E), alpha, 1.0f);
			g.fill(barX, barY, barX + barW, barY + barH, trackBg);
			g.fill(barX, barY, barX + (int) (barW * frac), barY + barH, barFg);
		}
	}

	private static Component ellipsize(Minecraft mc, Component component, int maxWidth) {
		String text = component.getString();
		if (mc.font.width(component) <= maxWidth) {
			return component;
		}
		String trimmed = mc.font.plainSubstrByWidth(text, Math.max(0, maxWidth - mc.font.width("…")));
		net.minecraft.network.chat.Style style = component.getStyle();
		return Component.literal(trimmed + "…").setStyle(style);
	}

	private static int applyAlpha(int argb, int alpha, float mult) {
		int originalA = (argb >>> 24) & 0xFF;
		int finalA = Math.min(255, Math.max(0, (int) (originalA * (alpha / 255f) * mult)));
		return (finalA << 24) | (argb & 0x00FFFFFF);
	}

	private static float smoothstep(float x) {
		float c = Math.max(0f, Math.min(1f, x));
		return c * c * (3f - 2f * c);
	}
}
