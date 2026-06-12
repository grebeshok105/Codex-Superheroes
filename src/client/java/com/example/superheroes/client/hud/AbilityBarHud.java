package com.example.superheroes.client.hud;

import com.example.superheroes.client.ClientAbilityCooldowns;
import com.example.superheroes.client.ClientAbilityFilter;
import com.example.superheroes.client.ClientHeroState;
import com.example.superheroes.client.config.SuperheroesClientConfig;
import com.example.superheroes.client.render.WildRenderer;
import com.example.superheroes.client.render.WildShaders;
import com.example.superheroes.hero.Hero;
import com.example.superheroes.hero.HeroHudConfig;
import com.example.superheroes.hero.HeroTheme;
import com.example.superheroes.hero.Heroes;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Панель способностей в стиле Wild: круглые полупрозрачные орбы с неоновым
 * кольцом цвета героя, круговой маской иконки и дуговым кулдауном.
 * Геометрия слотов (размеры/отступы/drag-границы) не менялась.
 */
public final class AbilityBarHud {
	private static final int BASE_SLOT_SIZE = 36;
	private static final int BASE_GAP = 4;
	private static final int BASE_BOTTOM_OFFSET = 40;

	private static final String[] KEYBIND_LABELS = {"Z", "X", "C", "V", "B", "3", "4", "5"};

	private static final int ORB_FILL = 0xB80A0710;
	private static final int ACTIVE_GREEN = 0xFF6BFF8C;
	private static final int ULT_GOLD = 0xFFFFD24A;
	private static final int CD_ORANGE = 0xFFFFA94F;

	private AbilityBarHud() {
	}

	public static void tick() {
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
		List<ResourceLocation> abilities = ClientAbilityFilter.visibleFor(ClientHeroState.abilities(), heroId);
		int n = abilities.size();
		if (n == 0) {
			return;
		}

		int screenW = HudScaler.screenWidth();
		int screenH = HudScaler.screenHeight();
		int slotSize = HudScaler.scale(Math.max(28, Math.min(44, BASE_SLOT_SIZE + (6 - n) * 2)));
		int gap = HudScaler.scale(BASE_GAP);
		int totalWidth = n * slotSize + (n - 1) * gap;
		int[] off = HudLayoutManager.offset(HudLayoutManager.ABILITY_BAR);
		int startX = (screenW - totalWidth) / 2 + off[0];
		int startY = screenH - HudScaler.scale(BASE_BOTTOM_OFFSET) - slotSize + off[1];

		for (int i = 0; i < n; i++) {
			ResourceLocation abilityId = abilities.get(i);
			int sx = startX + i * (slotSize + gap);
			boolean isUlt = hudConfig.hasUltimate() && i == n - 1;
			boolean isActive = ClientHeroState.data().activeAbilities().contains(abilityId);
			int cdRemaining = ClientAbilityCooldowns.remainingTicks(abilityId);
			int cdTotal = ClientAbilityCooldowns.totalTicks(abilityId);

			drawAbilityOrb(graphics, mc, sx, startY, slotSize, abilityId, i, isUlt,
					isActive, cdRemaining, cdTotal, theme);
		}
	}

	private static void drawAbilityOrb(GuiGraphics g, Minecraft mc, int x, int y, int size,
			ResourceLocation abilityId, int index, boolean isUlt,
			boolean isActive, int cdRemaining, int cdTotal, HeroTheme theme) {
		boolean onCd = cdRemaining > 0;
		float cx = x + size / 2f;
		float cy = y + size / 2f;
		// ульта чуть крупнее в пределах того же слота
		float radius = size / 2f - (isUlt ? 0.5f : 2f);
		float ringW = Math.max(1.6f, HudScaler.scale(isUlt ? 2.2f : 1.8f));

		// цвет кольца по состоянию
		int ringColor;
		int glowColor;
		float glowR;
		if (isActive) {
			ringColor = ACTIVE_GREEN;
			glowColor = (0xC0 << 24) | (ACTIVE_GREEN & 0x00FFFFFF);
			glowR = 10f;
		} else if (isUlt) {
			ringColor = ULT_GOLD;
			float pulse = onCd ? 0f : HudAnimator.pulse(1.2f);
			int a = 0x90 + (int) (0x58 * pulse);
			glowColor = (Math.min(0xFF, a) << 24) | (ULT_GOLD & 0x00FFFFFF);
			glowR = 12f;
		} else {
			ringColor = onCd ? 0x99E8C170 : theme.energyIcon();
			float pulse = onCd ? 0f : HudAnimator.pulse(1.5f);
			int a = onCd ? 0x34 : 0x60 + (int) (0x40 * pulse);
			glowColor = (a << 24) | (theme.energyIcon() & 0x00FFFFFF);
			glowR = 8f;
		}

		boolean squareStyle = SuperheroesClientConfig.iconStyle() == SuperheroesClientConfig.IconStyle.SQUARE;
		if (squareStyle && WildShaders.rectReady()) {
			// квадратный стиль: тот же цвет/кольцо/глоу, только скруглённый чип вместо орба
			drawSquareChip(g, mc, x, y, size, cx, cy, abilityId, isUlt, onCd, cdRemaining, cdTotal,
					ringColor, glowColor, glowR, ringW);
		} else if (!squareStyle && WildShaders.circleReady()) {
			// базовый орб: тёмное стекло + кольцо + глоу
			WildRenderer.orb(g, cx, cy, radius, ORB_FILL, ringColor, ringW, glowColor, glowR);

			// иконка с круговой маской (затемняется на кулдауне)
			float iconSize = (radius - ringW) * 2f - 1f;
			ResourceLocation tex = AbilityIcons.texture(abilityId);
			if (tex != null && WildShaders.iconCircleReady()) {
				WildRenderer.iconCircle(g, tex, cx, cy, iconSize, iconSize / 2f, onCd ? 0.55f : 0f);
			} else {
				String badge = isUlt ? "\u2605" : AbilityDescriptions.kindOf(abilityId).badge();
				Component badgeComp = HudUtil.text(badge).withStyle(ChatFormatting.BOLD);
				int badgeW = mc.font.width(badgeComp);
				int badgeColor = onCd ? 0xFF8A8FA0 : ringColor;
				g.drawString(mc.font, badgeComp, (int) (cx - badgeW / 2f), (int) (cy - 4), badgeColor, true);
			}

			// кулдаун: оранжевая дуга по часовой + секунды в центре
			if (onCd) {
				float pct = cdTotal > 0 ? (float) cdRemaining / cdTotal : 0f;
				WildRenderer.clockArc(g, cx, cy, radius, ringW + 0.6f, pct, CD_ORANGE,
						(0x66 << 24) | (CD_ORANGE & 0x00FFFFFF), 4f);
				String cdText = String.format(java.util.Locale.ROOT, "%.1f", cdRemaining / 20f);
				Component cdComp = HudUtil.text(cdText).withStyle(ChatFormatting.BOLD);
				int cdW = mc.font.width(cdComp);
				g.drawString(mc.font, cdComp, (int) (cx - cdW / 2f), (int) (cy - 4), 0xFFFFCB8E, true);
			}
		} else {
			// fallback без шейдеров: старый квадратный слот
			HudUtil.roundedRectFill(g, x, y, size, size, 0xCC0A0B14);
			HudUtil.roundedRectBorder(g, x, y, size, size, ringColor);
			int pad = Math.max(2, HudScaler.scale(2));
			if (AbilityIcons.texture(abilityId) != null) {
				AbilityIcons.draw(g, abilityId, x + pad, y + pad, size - pad * 2, ringColor);
			}
			if (onCd) {
				float cdPct = cdTotal > 0 ? (float) cdRemaining / cdTotal : 0f;
				g.fill(x + 1, y + 1, x + size - 1, y + 1 + (int) (size * cdPct), 0x88000000);
				String cdText = String.format(java.util.Locale.ROOT, "%.1f", cdRemaining / 20f);
				int cdW = mc.font.width(cdText);
				g.drawString(mc.font, HudUtil.text(cdText), x + (size - cdW) / 2, y + size + 2, 0xFFFF9D6E, true);
			}
		}

		// клавиша под орбом
		String key = index < KEYBIND_LABELS.length ? KEYBIND_LABELS[index] : "?";
		Component keyComp = HudUtil.text(key);
		int keyW = mc.font.width(keyComp);
		g.drawString(mc.font, keyComp, (int) (cx - keyW / 2f), y + size + HudScaler.scale(3), 0xCCE8ECF8, true);
	}

	/**
	 * Квадратный стиль иконки: скруглённый чип (тот же fill/кольцо/глоу, что и у орба),
	 * квадратная иконка и кулдаун вертикальной шторкой + секунды. Геометрия слота та же.
	 */
	private static void drawSquareChip(GuiGraphics g, Minecraft mc, int x, int y, int size,
			float cx, float cy, ResourceLocation abilityId, boolean isUlt, boolean onCd,
			int cdRemaining, int cdTotal, int ringColor, int glowColor, float glowR, float ringW) {
		float inset = isUlt ? 0.5f : 2f;
		float px = x + inset;
		float py = y + inset;
		float pw = size - inset * 2f;
		float ph = size - inset * 2f;
		float rad = HudScaler.scale(isUlt ? 5f : 4f);

		// чип: тёмное стекло + неоновая обводка + глоу (как у орба)
		WildRenderer.panel(g, px, py, pw, ph, rad, ORB_FILL, ORB_FILL, ringColor, ringW, glowColor, glowR);

		// квадратная иконка (большая маска => без круговой обрезки), затемняется на кд
		float iconSize = pw - ringW * 2f - 1f;
		ResourceLocation tex = AbilityIcons.texture(abilityId);
		if (tex != null && WildShaders.iconCircleReady()) {
			WildRenderer.iconCircle(g, tex, cx, cy, iconSize, iconSize, onCd ? 0.55f : 0f);
		} else {
			String badge = isUlt ? "\u2605" : AbilityDescriptions.kindOf(abilityId).badge();
			Component badgeComp = HudUtil.text(badge).withStyle(ChatFormatting.BOLD);
			int badgeW = mc.font.width(badgeComp);
			int badgeColor = onCd ? 0xFF8A8FA0 : ringColor;
			g.drawString(mc.font, badgeComp, (int) (cx - badgeW / 2f), (int) (cy - 4), badgeColor, true);
		}

		// кулдаун: вертикальная шторка сверху + секунды по центру
		if (onCd) {
			float pct = cdTotal > 0 ? (float) cdRemaining / cdTotal : 0f;
			int wipeH = (int) (ph * pct);
			if (wipeH > 0) {
				WildRenderer.fill(g, px, py, pw, wipeH, rad, (0x88 << 24));
			}
			String cdText = String.format(java.util.Locale.ROOT, "%.1f", cdRemaining / 20f);
			Component cdComp = HudUtil.text(cdText).withStyle(ChatFormatting.BOLD);
			int cdW = mc.font.width(cdComp);
			g.drawString(mc.font, cdComp, (int) (cx - cdW / 2f), (int) (cy - 4), 0xFFFFCB8E, true);
		}
	}
}
