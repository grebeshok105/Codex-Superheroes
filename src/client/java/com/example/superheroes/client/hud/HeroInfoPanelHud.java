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
	private static final int BASE_PANEL_W = 252;
	private static final int BASE_PANEL_H = 156;
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
		// column = icon chip + gap + name + gap + cooldown text ("9.9s")
		int colW = HudScaler.scale(14 + 4) + cachedMaxNameW + HudScaler.scale(6) + mc.font.width("9.9s");
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

		// Hero name — крупнее, в свободном пространстве карточки
		Component nameComp = HudUtil.text(Component.translatable("hero.superheroes." + heroId.getPath())
				.copy()).withStyle(ChatFormatting.BOLD);
		graphics.pose().pushPose();
		graphics.pose().translate(contentX, cursorY, 0);
		graphics.pose().scale(1.3f, 1.3f, 1f);
		graphics.drawString(mc.font, nameComp, 0, 0, theme.heroNameColor(), true);
		graphics.pose().popPose();

		// (Класс угрозы A/B/C убран с карточки по запросу.)

		// Creative badge: a small gold star in the free top-left corner (above the bust)
		if (mc.player.getAbilities().instabuild) {
			int starSize = HudScaler.scale(9);
			int starX = x + (modelW - starSize) / 2 + 2;
			int starY = y + HudScaler.scale(5);
			EmojiIcons.drawWithGlow(graphics, HudIcons.PassiveGlyph.STAR, starX, starY, starSize,
					applyAlpha(0xFFFFD24A, 120, 1f));
		}
		cursorY += HudScaler.scale(15);
		drawDivider(graphics, contentX, cursorY, contentW, theme);
		cursorY += HudScaler.scale(5);

		boolean isIronMan = com.example.superheroes.hero.IronManHero.ID.equals(heroId);
		if (isIronMan) {
			// Железный Человек: HP/энергия живут в J.A.R.V.I.S.-оверлее, поэтому
			// тут вместо полосок — тематический статус костюма и дуг-реактора.
			drawIronManStatus(graphics, mc, contentX, cursorY, contentW, theme, mc.player);
			cursorY += HudScaler.scale(29);
		} else {
			// HP row: ❤ + толстая полоска с плавным градиентом + значение справа
			float hp = lastDisplayedHp + (displayedHp - lastDisplayedHp) * partial;
			float maxHp = mc.player.getMaxHealth();
			int hpPct = maxHp > 0 ? Math.round(hp / maxHp * 100f) : 0;
			drawStatRow(graphics, mc, contentX, cursorY, contentW, HudIcons.PassiveGlyph.HEART,
					maxHp > 0 ? Math.min(1f, hp / maxHp) : 0f,
					0xFFFF8C96, 0xFF9E1428, 0x55FF4455, hpPct + "%");
			cursorY += HudScaler.scale(14);

			// Energy row: иконка + золотая градиентная полоска + процент справа
			float energy = lastDisplayedEnergy + (displayedEnergy - lastDisplayedEnergy) * partial;
			float energyMax = ClientHeroState.energyMax();
			int pct = energyMax > 0 ? (int) (energy / energyMax * 100f) : 0;
			drawStatRow(graphics, mc, contentX, cursorY, contentW, HudIcons.PassiveGlyph.BOLT,
					energyMax > 0 ? Math.min(1f, energy / energyMax) : 0f,
					theme.energyBright(), theme.energyDark(), applyAlpha(theme.energyGlow(), 90, 1f),
					pct + "%");
			cursorY += HudScaler.scale(15);
		}
		drawDivider(graphics, contentX, cursorY, contentW, theme);
		cursorY += HudScaler.scale(5);

		// Ability readiness list: every visible ability with its status, two columns
		drawReadyList(graphics, mc, contentX, cursorY, contentW, heroId, hudConfig, theme);
		// Оставляем ТОЛЬКО полоску внизу — ряд «ПАССИВКИ» с иконками убран со всех героев.
		drawDivider(graphics, x + HudScaler.scale(8), y + panelH - HudScaler.scale(25),
				panelW - HudScaler.scale(16), theme);
	}

	/**
	 * Строка статов в стиле референса: иконка слева, толстая полоска со
	 * сглаженным вертикальным градиентом и мягким глоу, значение справа.
	 */
	private static void drawStatRow(GuiGraphics g, Minecraft mc, int x, int y, int w,
			HudIcons.PassiveGlyph emoji, float pctFill, int bright, int dark, int glow, String valueText) {
		int iconSz = HudScaler.scale(10);
		EmojiIcons.draw(g, emoji, x, y - HudScaler.scale(1), iconSz);
		Component valComp = HudUtil.text(valueText);
		int valW = mc.font.width(valComp);
		int barX = x + iconSz + HudScaler.scale(4);
		int barW = w - iconSz - HudScaler.scale(4) - valW - HudScaler.scale(6);
		int barH = HudScaler.scale(4);
		int barY = y + HudScaler.scale(2);
		int fillW = (int) (barW * pctFill);
		if (WildShaders.rectReady()) {
			// подложка с лёгким градиентом, аккуратная тонкая полоска
			WildRenderer.panel(g, barX, barY, barW, barH, barH / 2f,
					0x70000000, 0xA8000000, 0, 0f, 0, 0f);
			if (fillW > 2) {
				WildRenderer.panel(g, barX, barY, fillW, barH, barH / 2f,
						bright, dark, 0, 0f, glow, 6f);
				// глянцевый блик сверху — дополнительная ступень градиента
				WildRenderer.fill(g, barX + 1, barY + 0.6f, fillW - 2, barH * 0.38f,
						barH * 0.19f, 0x3CFFFFFF);
			}
		} else {
			g.fill(barX, barY, barX + barW, barY + barH, 0x99000000);
			if (fillW > 0) {
				g.fillGradient(barX, barY, barX + fillW, barY + barH, bright, dark);
			}
		}
		g.drawString(mc.font, valComp, x + w - valW, y, 0xFFE6EAF5, true);
	}

	/**
	 * Iron-Man-блок вместо HP/энергии: дуг-реактор + название костюма + строка
	 * статуса с анимированной полоской мощности. Только для Железного Человека.
	 */
	private static void drawIronManStatus(GuiGraphics g, Minecraft mc, int x, int y, int w,
			HeroTheme theme, net.minecraft.world.entity.player.Player player) {
		int gold = 0xFFFFC400;
		int cyan = 0xFF46D8FF;
		// строка 1: реактор + название костюма + индикатор «online»
		int icon = HudScaler.scale(11);
		EmojiIcons.draw(g, HudIcons.PassiveGlyph.REACTOR, x, y - HudScaler.scale(1), icon);
		int suitIdx = com.example.superheroes.client.ClientSuitVariantState.variantFor(player.getUUID());
		String suitName = com.example.superheroes.ability.ironman.IronManSuitVariant.get(suitIdx)
				.name().toUpperCase(java.util.Locale.ROOT);
		g.drawString(mc.font, HudUtil.text(suitName).copy()
				.withStyle(ChatFormatting.BOLD), x + icon + HudScaler.scale(4), y, gold, true);
		// «online» точка справа
		int dotSz = HudScaler.scale(4);
		int dotX = x + w - dotSz - HudScaler.scale(2);
		int dotY = y + HudScaler.scale(1);
		float pulse = HudAnimator.pulse(1.3f);
		if (WildShaders.rectReady()) {
			WildRenderer.fill(g, dotX - 1, dotY - 1, dotSz + 2, dotSz + 2, (dotSz + 2) / 2f,
					applyAlpha(0xFF4CFF8C, (int) (60 + 80 * pulse), 1f));
			WildRenderer.fill(g, dotX, dotY, dotSz, dotSz, dotSz / 2f, 0xFF4CFF8C);
		} else {
			g.fill(dotX, dotY, dotX + dotSz, dotY + dotSz, 0xFF4CFF8C);
		}

		// строка 2: лейбл ARC REACTOR + анимированная полоска мощности
		int row2 = y + HudScaler.scale(14);
		Component label = HudUtil.text("ARC REACTOR");
		g.drawString(mc.font, label, x, row2, 0xFF8FB7C8, true);
		int barX = x + mc.font.width(label) + HudScaler.scale(6);
		int barW = x + w - barX;
		int barH = HudScaler.scale(4);
		int barY = row2 + HudScaler.scale(1);
		float power = 0.78f + 0.18f * (0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 240.0));
		if (barW > 4) {
			if (WildShaders.rectReady()) {
				WildRenderer.panel(g, barX, barY, barW, barH, barH / 2f, 0x70000000, 0xA8000000, 0, 0f, 0, 0f);
				WildRenderer.panel(g, barX, barY, (int) (barW * power), barH, barH / 2f,
						cyan, 0xFF1C6E8C, 0, 0f, applyAlpha(cyan, 90, 1f), 6f);
			} else {
				g.fill(barX, barY, barX + barW, barY + barH, 0x99000000);
				g.fillGradient(barX, barY, barX + (int) (barW * power), barY + barH, cyan, 0xFF1C6E8C);
			}
		}
	}

	/** Тонкая полоска-разделитель между секциями карточки. */
	private static void drawDivider(GuiGraphics g, int x, int y, int w, HeroTheme theme) {
		HudUtil.neonAccentLine(g, x, y, w, applyAlpha(theme.panelBorder(), 60, 1f));
	}

	/** Нижний ряд пассивок: мини-заголовок + тёмные скруглённые чипы с глифами. */
	private static void drawPassivesRow(GuiGraphics g, Minecraft mc, int x, int y, int w,
			ResourceLocation heroId, HeroTheme theme) {
		int count = AbilityDescriptions.passiveCount(heroId);
		if (count <= 0) return;
		Component label = HudUtil.text("ПАССИВКИ");
		// label по центру высоты чипов
		g.drawString(mc.font, label, x, y - HudScaler.scale(3) + (HudScaler.scale(14) - 8) / 2,
				0xFF8B8FA3, true);
		int chip = HudScaler.scale(14);
		int gap = HudScaler.scale(4);
		int cx = x + mc.font.width(label) + HudScaler.scale(8);
		int chipY = y - HudScaler.scale(3);
		for (int i = 0; i < count && cx + chip <= x + w; i++) {
			// квадратный чип; обводка тонкая, иконка с РАВНЫМИ отступами со всех
			// сторон (раньше chip-3 давал 1px слева и 2px справа — чипы стояли криво)
			if (WildShaders.rectReady()) {
				WildRenderer.panel(g, cx, chipY, chip, chip, HudScaler.scale(3) * 0.45f,
						0xA8141A28, 0xC808090F, applyAlpha(theme.panelBorder(), 110, 1f), 0.7f, 0, 0f);
			} else {
				HudUtil.roundedRectFill(g, cx, chipY, chip, chip, 0x96070810);
				HudUtil.roundedRectBorder(g, cx, chipY, chip, chip,
						applyAlpha(theme.panelBorder(), 110, 1f));
			}
			int inset = Math.max(1, HudScaler.scale(1));
			int emojiSz = chip - inset * 2;
			EmojiIcons.draw(g, PassiveIcons.glyph(heroId, i), cx + inset, chipY + inset, emojiSz);
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

	/**
	 * Список способностей: крупные строки на всю свободную высоту карточки.
	 * Каждая иконка сидит РОВНО в квадратном чипе с лёгкой неоновой обводкой
	 * цвета героя — обводка прячет неровные края арта, чипы выравнивают ряд.
	 */
	private static void drawReadyList(GuiGraphics g, Minecraft mc, int x, int y, int w,
			ResourceLocation heroId, HeroHudConfig hudConfig, HeroTheme theme) {
		List<ResourceLocation> abilities = ClientAbilityFilter.visibleFor(ClientHeroState.abilities(), heroId);
		if (abilities.isEmpty()) {
			return;
		}
		int cols = 2;
		int colW = w / cols;
		int rowH = HudScaler.scale(17);
		int chip = HudScaler.scale(14);
		float chipR = HudScaler.scale(3) * 0.45f; // почти квадрат, лишь чуть смягчённые углы
		int maxRows = 4;
		int shown = Math.min(abilities.size(), cols * maxRows);
		for (int i = 0; i < shown; i++) {
			ResourceLocation aid = abilities.get(i);
			int col = i % cols;
			int row = i / cols;
			int ix = x + col * colW;
			int iy = y + row * rowH;
			int cd = ClientAbilityCooldowns.remainingTicks(aid);
			boolean ready = cd <= 0;
			boolean isUlt = hudConfig.hasUltimate() && i == abilities.size() - 1;
			boolean active = ClientHeroState.data().activeAbilities().contains(aid);

			// квадратный чип: тёмное стекло + неоновая обводка цвета героя
			int neon = applyAlpha(isUlt && ready ? 0xFFFFD24A : theme.panelBorder(), ready ? 160 : 70, 1f);
			if (WildShaders.rectReady()) {
				float pulse = isUlt && ready ? HudAnimator.pulse(1.2f) : 0f;
				int glow = ready ? applyAlpha(isUlt ? 0xFFFFD24A : theme.panelBorder(),
						(int) (60 + 60 * pulse), 1f) : 0;
				WildRenderer.panel(g, ix, iy, chip, chip, chipR,
						0xA8141A28, 0xC808090F, neon, 0.7f, glow, ready ? 3.0f : 0f);
			} else {
				HudUtil.roundedRectFill(g, ix, iy, chip, chip, 0xC00B0D14);
				HudUtil.roundedRectBorder(g, ix, iy, chip, chip, neon);
			}

			// иконка-квадрат: заполняет чип почти целиком, строго по центру
			int inset = Math.max(1, HudScaler.scale(1));
			int iconSz = chip - inset * 2;
			int fallback = ready ? (isUlt ? 0xFFFFD24A : 0xFF6BFF8C) : 0xFF8A8FA0;
			AbilityIcons.draw(g, aid, ix + inset, iy + inset, iconSz, fallback);
			if (!ready) {
				if (WildShaders.rectReady()) {
					WildRenderer.fill(g, ix + 1, iy + 1, chip - 2, chip - 2, chipR, 0x8C000000);
				} else {
					g.fill(ix + 1, iy + 1, ix + chip - 1, iy + chip - 1, 0x8C000000);
				}
			}

			// текст по вертикали — по центру чипа
			int textY = iy + (chip - 8) / 2;
			String name = Component.translatable(AbilityDescriptions.nameKey(aid)).getString();
			int maxNameW = colW - chip - HudScaler.scale(10);
			if (mc.font.width(name) > maxNameW) {
				String ell = "\u2026";
				name = mc.font.plainSubstrByWidth(name, maxNameW - mc.font.width(ell)) + ell;
			}
			int nameColor = ready ? (isUlt ? 0xFFFFE9B0 : 0xFFCBD2E0) : 0xFF7C8499;
			g.drawString(mc.font, HudUtil.text(name), ix + chip + HudScaler.scale(4), textY, nameColor, true);

			// справа в колонке: секунды кулдауна, либо зелёная точка у активного тумблера
			if (!ready) {
				String cdText = cdSeconds(cd);
				int cdW = mc.font.width(HudUtil.text(cdText));
				g.drawString(mc.font, HudUtil.text(cdText), ix + colW - cdW - HudScaler.scale(4), textY, 0xFFB99A6B, true);
			} else if (active) {
				int dotSz = HudScaler.scale(4);
				int dotX = ix + colW - dotSz - HudScaler.scale(5);
				int dotY = iy + (chip - dotSz) / 2;
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
				applyAlpha(theme.panelBorder(), 120, 1.0f),
				applyAlpha(theme.panelBorder(), 95, 1.0f));
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
