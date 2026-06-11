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

		// класс угрозы — акцент в правом углу строки имени
		com.example.superheroes.jarvis.JarvisThreatClass threat =
				com.example.superheroes.jarvis.JarvisThreatClass.forHero(heroId);
		int threatColor = threatColor(threat);
		Component threatComp = HudUtil.text("УГРОЗА: " + threat.label()).withStyle(ChatFormatting.BOLD);
		int threatW = mc.font.width(threatComp);
		int threatX = contentX + contentW - threatW;
		if (threatX > contentX + mc.font.width(nameComp) + HudScaler.scale(6)) {
			graphics.drawString(mc.font, threatComp, threatX, cursorY, threatColor, true);
			HudUtil.neonAccentLine(graphics, threatX, cursorY + HudScaler.scale(9), threatW,
					applyAlpha(threatColor, 170, 1f));
		}

		// Creative badge: a small gold star in the free top-left corner (above the bust)
		if (mc.player.getAbilities().instabuild) {
			int starSize = HudScaler.scale(9);
			int starX = x + (modelW - starSize) / 2 + 2;
			int starY = cursorY - HudScaler.scale(1);
			// soft glow behind + bright core
			HudIcons.drawPassiveIcon(graphics, starX - 1, starY - 1, starSize + 2,
					HudIcons.PassiveGlyph.STAR, applyAlpha(0xFFFFD24A, 90, 1f));
			HudIcons.drawPassiveIcon(graphics, starX, starY, starSize,
					HudIcons.PassiveGlyph.STAR, 0xFFFFE07A);
		}
		cursorY += HudScaler.scale(12);

		// HP row: heart + numbers + thin bar
		float hp = lastDisplayedHp + (displayedHp - lastDisplayedHp) * partial;
		float maxHp = mc.player.getMaxHealth();
		drawHpRow(graphics, mc, contentX, cursorY, contentW, hp, maxHp, theme);
		cursorY += HudScaler.scale(15);

		// Energy: single themed row — icon + label + percent, segmented bar below
		float energy = lastDisplayedEnergy + (displayedEnergy - lastDisplayedEnergy) * partial;
		float energyMax = ClientHeroState.energyMax();
		int iconSize = HudScaler.scale(9);
		HudIcons.drawEnergyIcon(graphics, contentX, cursorY, iconSize, hudConfig.energyIcon(), theme.energyIcon());
		Component energyLabel = HudUtil.text(Component.translatable(hudConfig.energyName()).copy());
		graphics.drawString(mc.font, energyLabel, contentX + iconSize + HudScaler.scale(3), cursorY,
				soften(theme.energyIcon()), true);
		int pct = energyMax > 0 ? (int) (energy / energyMax * 100f) : 0;
		Component pctComp = HudUtil.text(pct + "%");
		int pctW = mc.font.width(pctComp);
		graphics.drawString(mc.font, pctComp, contentX + contentW - pctW, cursorY, 0xFFD8DCE8, true);
		cursorY += HudScaler.scale(11);

		// Thin 3px energy bar — same style as the HP bar
		int energyBarH = HudScaler.scale(3);
		float energyPct = energyMax > 0 ? Math.min(1f, energy / energyMax) : 0f;
		int energyFillW = (int) (contentW * energyPct);
		if (WildShaders.rectReady()) {
			WildRenderer.fill(graphics, contentX, cursorY, contentW, energyBarH, energyBarH / 2f, 0x66000000);
			if (energyFillW > 1) {
				WildRenderer.bar(graphics, contentX, cursorY, energyFillW, energyBarH,
						theme.energyBright(), applyAlpha(theme.energyGlow(), 150, 1f));
			}
		} else {
			graphics.fill(contentX, cursorY, contentX + contentW, cursorY + energyBarH, 0x44000000);
			if (energyFillW > 0) {
				graphics.fill(contentX, cursorY, contentX + energyFillW, cursorY + energyBarH, theme.energyBright());
			}
		}
		cursorY += HudScaler.scale(7);

		// Ability readiness list: every visible ability with its status, two columns
		drawReadyList(graphics, mc, contentX, cursorY, contentW, heroId, hudConfig, theme);

		// Passives section
		int passiveCount = AbilityDescriptions.passiveCount(heroId);
		if (passiveCount > 0) {
			int passiveY = y + panelH - HudScaler.scale(24);
			int passiveSectionX = x + HudScaler.scale(2);

			Component passivesLabel = HudUtil.text(Component.translatable("hud.superheroes.passives").copy());
			graphics.drawString(mc.font, passivesLabel, passiveSectionX, passiveY,
					applyAlpha(theme.heroNameColor(), 200, 0.8f), true);
			passiveY += HudScaler.scale(10);

			int maxDisplay = Math.min(passiveCount, 6);
			int iconSz = HudScaler.scale(passiveCount > 5 ? 8 : 10);
			int gap = HudScaler.scale(passiveCount > 5 ? 2 : 4);
			for (int i = 0; i < maxDisplay; i++) {
				int ix = passiveSectionX + i * (iconSz + gap);
				if (WildShaders.circleReady()) {
					WildRenderer.orb(graphics, ix + iconSz / 2f, passiveY + iconSz / 2f, iconSz / 2f,
							0x66000000, applyAlpha(theme.energyIcon(), 190, 0.7f), 1.1f,
							applyAlpha(theme.energyIcon(), 70, 1f), 2.5f);
				} else {
					HudUtil.roundedRectFill(graphics, ix, passiveY, iconSz, iconSz, 0x44000000);
					HudUtil.roundedRectBorder(graphics, ix, passiveY, iconSz, iconSz, applyAlpha(theme.energyIcon(), 180, 0.6f));
				}
				int inset = Math.max(1, iconSz / 5);
				HudIcons.drawPassiveIcon(graphics, ix + inset, passiveY + inset, iconSz - inset * 2,
						PassiveIcons.glyph(heroId, i), applyAlpha(theme.energyIcon(), 235, 1f));
			}
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

	private static void drawHpRow(GuiGraphics g, Minecraft mc, int x, int y, int w, float hp, float maxHp, HeroTheme theme) {
		int iconSize = HudScaler.scale(8);
		int textOffset = iconSize + HudScaler.scale(2);
		g.drawString(mc.font, HudUtil.text("\u2764"), x, y, 0xFFFF5555, true);
		String valText = formatValue(hp) + " / " + formatValue(maxHp);
		g.drawString(mc.font, HudUtil.text(valText), x + textOffset, y, 0xFFD8DCE8, true);

		int barY = y + HudScaler.scale(9);
		int barW = w - textOffset - HudScaler.scale(2);
		int barH = HudScaler.scale(3);
		float pctHp = maxHp > 0 ? Math.min(1f, hp / maxHp) : 0f;
		int fillW = (int) (barW * pctHp);
		if (WildShaders.rectReady()) {
			WildRenderer.fill(g, x + textOffset, barY, barW, barH, barH / 2f, 0x66000000);
			if (fillW > 1) {
				WildRenderer.bar(g, x + textOffset, barY, fillW, barH,
						theme.energyBright(), applyAlpha(theme.energyGlow(), 150, 1f));
			}
		} else {
			g.fill(x + textOffset, barY, x + textOffset + barW, barY + barH, 0x44000000);
			if (fillW > 0) {
				g.fill(x + textOffset, barY, x + textOffset + fillW, barY + barH, theme.energyBright());
			}
		}
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

			// soft theme-tinted highlight behind currently active (toggled-on) abilities
			if (active) {
				HudUtil.roundedRectFill(g, ix - HudScaler.scale(2), iy - HudScaler.scale(1),
						colW - HudScaler.scale(2), lineH, applyAlpha(theme.energyBright(), 70, 1f));
			}

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

			// cooldown seconds on the right of the column
			if (!ready) {
				String cdText = cdSeconds(cd);
				int cdW = mc.font.width(HudUtil.text(cdText));
				g.drawString(mc.font, HudUtil.text(cdText), ix + colW - cdW - HudScaler.scale(4), iy, 0xFFB99A6B, true);
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
		HudUtil.neonPanel(g, x, y, w, h,
				applyAlpha(theme.panelTop(), 222, 0.78f),
				applyAlpha(theme.panelBottom(), 226, 0.8f),
				applyAlpha(theme.panelBorder(), 235, 1.0f),
				applyAlpha(theme.panelBorder(), 70, 1.0f));
		g.fill(x + 3, y + 2, x + w - 3, y + 3, applyAlpha(theme.panelHighlight(), 200, 0.7f));
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
