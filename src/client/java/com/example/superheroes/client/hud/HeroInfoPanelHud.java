package com.example.superheroes.client.hud;

import com.example.superheroes.client.ClientAbilityCooldowns;
import com.example.superheroes.client.ClientAbilityFilter;
import com.example.superheroes.client.ClientHeroState;
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
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public final class HeroInfoPanelHud {
	private static final int BASE_PANEL_W = 220;
	private static final int BASE_PANEL_H = 142;
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
			drawLiveBust(graphics, modelX1, modelY1, modelX2, modelY2, modelScale, mc.player);
		} catch (Exception ignored) {
		}

		// Right side content
		int contentX = x + modelW + HudScaler.scale(4);
		int contentW = panelW - modelW - HudScaler.scale(8);
		int cursorY = y + HudScaler.scale(6);

		// Hero name (no caps-lock shouting, soft theme color)
		Component nameComp = HudUtil.text(Component.translatable("hero.superheroes." + heroId.getPath())
				.copy()).withStyle(ChatFormatting.BOLD);
		graphics.drawString(mc.font, nameComp, contentX, cursorY, theme.heroNameColor(), true);
		cursorY += HudScaler.scale(10);

		// класс угрозы — подзаголовок под именем, как на референсе
		com.example.superheroes.jarvis.JarvisThreatClass threat =
				com.example.superheroes.jarvis.JarvisThreatClass.forHero(heroId);
		int threatColor = threatColor(threat);
		Component threatComp = HudUtil.text("КЛАСС УГРОЗЫ: " + threat.label());
		graphics.drawString(mc.font, threatComp, contentX, cursorY, applyAlpha(threatColor, 235, 1f), true);

		// Creative badge: a small gold star in the free top-left corner (above the bust)
		if (mc.player.getAbilities().instabuild) {
			int starSize = HudScaler.scale(9);
			int starX = x + (modelW - starSize) / 2 + 2;
			int starY = y + HudScaler.scale(5);
			HudIcons.drawPassiveIcon(graphics, starX - 1, starY - 1, starSize + 2,
					HudIcons.PassiveGlyph.STAR, applyAlpha(0xFFFFD24A, 90, 1f));
			HudIcons.drawPassiveIcon(graphics, starX, starY, starSize,
					HudIcons.PassiveGlyph.STAR, 0xFFFFE07A);
		}
		cursorY += HudScaler.scale(12);

		// HP row: ❤ + толстая полоска с плавным градиентом + значение справа
		float hp = lastDisplayedHp + (displayedHp - lastDisplayedHp) * partial;
		float maxHp = mc.player.getMaxHealth();
		String hpText = formatValue(hp) + "/" + formatValue(maxHp);
		drawStatRow(graphics, mc, contentX, cursorY, contentW, "\u2764", 0xFFFF5555,
				maxHp > 0 ? Math.min(1f, hp / maxHp) : 0f,
				0xFFFF7A86, 0xFFB81E32, 0x66FF4455, hpText);
		cursorY += HudScaler.scale(13);

		// Energy row: иконка + золотая градиентная полоска + процент справа
		float energy = lastDisplayedEnergy + (displayedEnergy - lastDisplayedEnergy) * partial;
		float energyMax = ClientHeroState.energyMax();
		int pct = energyMax > 0 ? (int) (energy / energyMax * 100f) : 0;
		drawStatRow(graphics, mc, contentX, cursorY, contentW, null, theme.energyIcon(),
				energyMax > 0 ? Math.min(1f, energy / energyMax) : 0f,
				theme.energyBright(), theme.energyDark(), applyAlpha(theme.energyGlow(), 110, 1f),
				pct + "%");
		HudIcons.drawEnergyIcon(graphics, contentX, cursorY - HudScaler.scale(1), HudScaler.scale(9),
				hudConfig.energyIcon(), theme.energyIcon());
		cursorY += HudScaler.scale(14);

		// Ability readiness list: every visible ability with its status, two columns
		drawReadyList(graphics, mc, contentX, cursorY, contentW, heroId, hudConfig, theme);

		// Пассивки: подпись + ряд чипов-иконок внизу, под бюстом
		drawPassivesRow(graphics, mc, x + HudScaler.scale(8), y + panelH - HudScaler.scale(20),
				panelW - HudScaler.scale(16), heroId, theme);
	}

	/**
	 * Строка статов в стиле референса: иконка слева, толстая полоска со
	 * сглаженным вертикальным градиентом и мягким глоу, значение справа.
	 */
	private static void drawStatRow(GuiGraphics g, Minecraft mc, int x, int y, int w,
			String icon, int iconColor, float pctFill, int bright, int dark, int glow, String valueText) {
		int iconW = HudScaler.scale(11);
		if (icon != null) {
			g.drawString(mc.font, HudUtil.text(icon), x, y, iconColor, true);
		}
		Component valComp = HudUtil.text(valueText);
		int valW = mc.font.width(valComp);
		int barX = x + iconW;
		int barW = w - iconW - valW - HudScaler.scale(5);
		int barH = HudScaler.scale(6);
		int barY = y + HudScaler.scale(1);
		int fillW = (int) (barW * pctFill);
		if (WildShaders.rectReady()) {
			WildRenderer.fill(g, barX, barY, barW, barH, barH / 2f, 0x99000000);
			if (fillW > 2) {
				WildRenderer.panel(g, barX, barY, fillW, barH, barH / 2f,
						bright, dark, 0, 0f, glow, 4f);
			}
		} else {
			g.fill(barX, barY, barX + barW, barY + barH, 0x99000000);
			if (fillW > 0) {
				g.fillGradient(barX, barY, barX + fillW, barY + barH, bright, dark);
			}
		}
		g.drawString(mc.font, valComp, x + w - valW, y, 0xFFE6EAF5, true);
	}

	/** Нижний ряд пассивок: мини-заголовок + тёмные скруглённые чипы с глифами. */
	private static void drawPassivesRow(GuiGraphics g, Minecraft mc, int x, int y, int w,
			ResourceLocation heroId, HeroTheme theme) {
		int count = AbilityDescriptions.passiveCount(heroId);
		if (count <= 0) return;
		Component label = HudUtil.text("ПАССИВКИ");
		g.drawString(mc.font, label, x, y - HudScaler.scale(1), 0xFF8B8FA3, true);
		int chip = HudScaler.scale(13);
		int gap = HudScaler.scale(4);
		int cx = x + mc.font.width(label) + HudScaler.scale(8);
		for (int i = 0; i < count && cx + chip <= x + w; i++) {
			HudUtil.roundedRectFill(g, cx, y - HudScaler.scale(3), chip, chip, 0xCC0A0B12);
			HudUtil.roundedRectBorder(g, cx, y - HudScaler.scale(3), chip, chip,
					applyAlpha(theme.panelBorder(), 150, 1f));
			int glyphSz = chip - HudScaler.scale(5);
			HudIcons.drawPassiveIcon(g, cx + (chip - glyphSz) / 2, y - HudScaler.scale(3) + (chip - glyphSz) / 2,
					glyphSz, PassiveIcons.glyph(heroId, i), 0xFFE8ECF8);
			cx += chip + gap;
		}
	}


	/**
	 * Live "mirror" bust: renders the player with their REAL pose — actual head
	 * yaw/pitch relative to the body, sneaking, swinging, flying — instead of the
	 * vanilla follows-mouse gimmick. The body always faces the viewer; everything
	 * else mirrors what the hero is doing in the world right now.
	 */
	private static void drawLiveBust(GuiGraphics graphics, int x1, int y1, int x2, int y2,
			int scale, net.minecraft.world.entity.LivingEntity entity) {
		float centerX = (x1 + x2) / 2f;
		float centerY = (y1 + y2) / 2f;
		graphics.enableScissor(x1, y1, x2, y2);

		// real head offset relative to the body (clamped so the face stays visible)
		float headDelta = net.minecraft.util.Mth.clamp(
				net.minecraft.util.Mth.wrapDegrees(entity.yHeadRot - entity.yBodyRot), -50f, 50f);
		float headDeltaO = net.minecraft.util.Mth.clamp(
				net.minecraft.util.Mth.wrapDegrees(entity.yHeadRotO - entity.yBodyRotO), -50f, 50f);
		float realPitch = net.minecraft.util.Mth.clamp(entity.getXRot(), -45f, 45f);

		float bodyRot = entity.yBodyRot;
		float bodyRotO = entity.yBodyRotO;
		float yRot = entity.getYRot();
		float xRot = entity.getXRot();
		float headRotO = entity.yHeadRotO;
		float headRot = entity.yHeadRot;

		entity.yBodyRot = 180.0f;
		entity.yBodyRotO = 180.0f;
		entity.setYRot(180.0f + headDelta);
		entity.setXRot(realPitch);
		entity.yHeadRot = 180.0f + headDelta;
		entity.yHeadRotO = 180.0f + headDeltaO;

		org.joml.Quaternionf pose = new org.joml.Quaternionf().rotateZ((float) Math.PI);
		org.joml.Quaternionf camera = new org.joml.Quaternionf().rotateX(8.0f * ((float) Math.PI / 180f));
		pose.mul(camera);
		float entityScale = entity.getScale();
		org.joml.Vector3f translate = new org.joml.Vector3f(0.0f,
				entity.getBbHeight() / 2.0f + MODEL_Y_OFFSET * entityScale, 0.0f);
		InventoryScreen.renderEntityInInventory(graphics, centerX, centerY,
				(float) scale / entityScale, translate, pose, camera, entity);

		entity.yBodyRot = bodyRot;
		entity.yBodyRotO = bodyRotO;
		entity.setYRot(yRot);
		entity.setXRot(xRot);
		entity.yHeadRotO = headRotO;
		entity.yHeadRot = headRot;
		graphics.disableScissor();
	}

	private static void drawReadyList(GuiGraphics g, Minecraft mc, int x, int y, int w,
			ResourceLocation heroId, HeroHudConfig hudConfig, HeroTheme theme) {
		List<ResourceLocation> abilities = ClientAbilityFilter.visibleFor(ClientHeroState.abilities(), heroId);
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
			boolean active = ClientHeroState.data().activeAbilities().contains(aid);

			// ability icon (replaces the old status dot); greyed out while on cooldown
			int iconSz = HudScaler.scale(8);
			int fallback = ready ? (isUlt ? 0xFFFFD24A : 0xFF6BFF8C) : 0xFF8A8FA0;
			AbilityIcons.draw(g, aid, ix, iy, iconSz, fallback);
			if (!ready) {
				g.fill(ix, iy, ix + iconSz, iy + iconSz, 0x88000000);
			} else if (isUlt) {
				float pulse = HudAnimator.pulse(1.2f);
				int haloA = (int) (110 + 90 * pulse);
				HudUtil.roundedRectBorder(g, ix - 1, iy - 1, iconSz + 2, iconSz + 2, (haloA << 24) | 0x00FFD24A);
			}

			// name (truncated)
			String name = Component.translatable(AbilityDescriptions.nameKey(aid)).getString();
			int maxNameW = colW - HudScaler.scale(24);
			if (mc.font.width(name) > maxNameW) {
				String ell = "\u2026";
				name = mc.font.plainSubstrByWidth(name, maxNameW - mc.font.width(ell)) + ell;
			}
			int nameColor = ready ? (isUlt ? 0xFFFFE9B0 : 0xFFCBD2E0) : 0xFF7C8499;
			g.drawString(mc.font, HudUtil.text(name), ix + iconSz + HudScaler.scale(3), iy, nameColor, true);

			// справа в колонке: секунды кулдауна, либо зелёная точка у активного тумблера
			if (!ready) {
				String cdText = cdSeconds(cd);
				int cdW = mc.font.width(HudUtil.text(cdText));
				g.drawString(mc.font, HudUtil.text(cdText), ix + colW - cdW - HudScaler.scale(4), iy, 0xFFB99A6B, true);
			} else if (active) {
				int dotSz = HudScaler.scale(4);
				int dotX = ix + colW - dotSz - HudScaler.scale(5);
				int dotY = iy + HudScaler.scale(2);
				if (WildShaders.rectReady()) {
					WildRenderer.fill(g, dotX - 1, dotY - 1, dotSz + 2, dotSz + 2, (dotSz + 2) / 2f, 0x5532FF7A);
					WildRenderer.fill(g, dotX, dotY, dotSz, dotSz, dotSz / 2f, 0xFF4CFF8C);
				} else {
					g.fill(dotX, dotY, dotX + dotSz, dotY + dotSz, 0xFF4CFF8C);
				}
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
		// затемнённая стеклянная подложка: цвета темы сильно примешаны к чёрному
		HudUtil.neonPanel(g, x, y, w, h,
				applyAlpha(darken(theme.panelTop(), 0.45f), 238, 1f),
				applyAlpha(darken(theme.panelBottom(), 0.4f), 242, 1f),
				applyAlpha(theme.panelBorder(), 235, 1.0f),
				applyAlpha(theme.panelBorder(), 80, 1.0f));
	}

	private static int darken(int argb, float keep) {
		int r = (int) (((argb >> 16) & 0xFF) * keep);
		int gg = (int) (((argb >> 8) & 0xFF) * keep);
		int b = (int) ((argb & 0xFF) * keep);
		return (argb & 0xFF000000) | (r << 16) | (gg << 8) | b;
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

	private static int threatColor(com.example.superheroes.jarvis.JarvisThreatClass threat) {
		return switch (threat) {
			case S -> 0xFFFF4A4A;
			case A -> 0xFFFF7E4A;
			case B -> 0xFFFFB23E;
			case C -> 0xFFFFE07A;
			case D -> 0xFF8CFF9C;
		};
	}

	private static int applyAlpha(int argb, int alpha, float mult) {
		int originalA = (argb >>> 24) & 0xFF;
		int finalA = Math.min(255, Math.max(0, (int) (originalA * (alpha / 255f) * mult)));
		return (finalA << 24) | (argb & 0x00FFFFFF);
	}

}
