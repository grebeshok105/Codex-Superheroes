package com.example.superheroes.client.hud;

import com.example.superheroes.ModId;
import com.example.superheroes.ability.AbilityIds;
import com.example.superheroes.client.ClientAbilityCooldowns;
import com.example.superheroes.client.ClientDoomsdayState;
import com.example.superheroes.client.ClientHeroState;
import com.example.superheroes.client.ClientMadnessState;
import com.example.superheroes.client.ClientRemDemonismState;
import com.example.superheroes.client.ClientThanosState;
import com.example.superheroes.client.ModKeys;
import com.example.superheroes.hero.RemHero;
import com.example.superheroes.hero.ThanosHero;
import com.example.superheroes.item.ModItems;
import com.example.superheroes.item.infinity.InfinityStoneType;
import com.example.superheroes.hero.HeroTheme;
import com.example.superheroes.network.ActivateAbilityC2SPayload;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Radial ability wheel: a true donut/pie design.
 * Dark glass annulus split into sectors, hovered sector fills with the hero theme color,
 * ability icons sit at sector centroids, the center hub shows hero + hovered ability info.
 * Selection logic (mouse-look) is unchanged.
 */
public final class RadialMenuHud {
	private static final float DEAD_ZONE = 5f;
	private static final int OUTER_R = 98;
	private static final int INNER_R = 42;
	private static final int HUB_R = 34;

	private static final int COLOR_COOLDOWN = 0xFFFFB45C;

	/** Degrees of mouse-look mapped to on-screen pixels for the virtual cursor. */
	private static final float PX_PER_DEG = INNER_R / DEAD_ZONE;

	private static boolean open;
	private static float startYaw;
	private static float startPitch;
	private static int selected = -1;
	private static float cursorDx;
	private static float cursorDy;

	private RadialMenuHud() {
	}

	public static void clientTick(Minecraft mc) {
		if (mc.player == null || mc.level == null) {
			closeWithoutActivate();
			return;
		}
		boolean down = ModKeys.RADIAL != null && ModKeys.RADIAL.isDown();
		List<ResourceLocation> abilities = visibleAbilities();
		if (down && !open) {
			if (abilities.isEmpty()) {
				return;
			}
			open = true;
			startYaw = mc.player.getYRot();
			startPitch = mc.player.getXRot();
			selected = -1;
			cursorDx = 0f;
			cursorDy = 0f;
			return;
		}
		if (!down && open) {
			closeAndActivate(abilities);
			return;
		}
		if (open) {
			updateSelection(mc, abilities.size());
		}
	}

	private static void updateSelection(Minecraft mc, int n) {
		if (n <= 0) {
			selected = -1;
			return;
		}
		float dyaw = mc.player.getYRot() - startYaw;
		float dpitch = mc.player.getXRot() - startPitch;
		// virtual cursor position (px), clamped to just past the outer rim
		float px = dyaw * PX_PER_DEG;
		float py = dpitch * PX_PER_DEG;
		float mag = (float) Math.sqrt(px * px + py * py);
		float maxR = OUTER_R + 6;
		if (mag > maxR) {
			px *= maxR / mag;
			py *= maxR / mag;
		}
		cursorDx = px;
		cursorDy = py;
		float magSq = dyaw * dyaw + dpitch * dpitch;
		if (magSq < DEAD_ZONE * DEAD_ZONE) {
			// cursor returned to the hub: selection is dropped, release does nothing
			selected = -1;
			return;
		}
		float per = 360f / n;
		double angle = Math.toDegrees(Math.atan2(dpitch, dyaw)) + 90.0 + per / 2.0;
		angle = ((angle % 360.0) + 360.0) % 360.0;
		selected = ((int) Math.floor(angle / per)) % n;
	}

	private static void closeAndActivate(List<ResourceLocation> abilities) {
		int idx = selected;
		open = false;
		selected = -1;
		if (idx >= 0 && idx < abilities.size()) {
			ClientPlayNetworking.send(new ActivateAbilityC2SPayload(abilities.get(idx)));
		}
	}

	private static void closeWithoutActivate() {
		open = false;
		selected = -1;
	}

	public static boolean isOpen() {
		return open;
	}

	private static List<ResourceLocation> visibleAbilities() {
		List<ResourceLocation> base = ClientHeroState.abilities();
		boolean isDoomsday = ModId.of("doomsday").equals(ClientHeroState.heroId());
		boolean isThanos = ThanosHero.ID.equals(ClientHeroState.heroId());
		boolean isRem = RemHero.ID.equals(ClientHeroState.heroId());
		int doomsdayTier = isDoomsday ? ClientDoomsdayState.tier() : 0;
		boolean remDemonism = isRemDemonismActive();
		java.util.ArrayList<ResourceLocation> out = new java.util.ArrayList<>(base.size());
		for (ResourceLocation id : base) {
			if (!ClientMadnessState.isMadness() && AbilityIds.COUNTER_STRIKE.equals(id)) continue;
			if (isDoomsday && !isDoomsdayUnlocked(id, doomsdayTier)) continue;
			if (isThanos && !isThanosUnlocked(id)) continue;
			if (isRem && !isRemVisible(id, remDemonism)) continue;
			out.add(id);
		}
		return out;
	}

	private static boolean isThanosUnlocked(ResourceLocation id) {
		if (ThanosHero.isSnapAbility(id)) return ClientThanosState.hasAllStones();
		InfinityStoneType req = ThanosHero.getRequiredStoneFor(id);
		if (req == null) return true;
		return ClientThanosState.hasStone(req);
	}

	private static boolean isDoomsdayUnlocked(ResourceLocation id, int tier) {
		if (AbilityIds.DOOMSDAY_SMASH.equals(id)) return tier >= 2;
		if (AbilityIds.DOOMSDAY_ROAR.equals(id)) return tier >= 3;
		if (AbilityIds.DOOMSDAY_BONE_SPIKE.equals(id)) return tier >= 4;
		if (AbilityIds.DOOMSDAY_CHARGE_TACKLE.equals(id)) return tier >= 5;
		if (AbilityIds.DOOMSDAY_BERSERK.equals(id)) return tier >= 6;
		if (AbilityIds.DOOMSDAY_DOOM_GRIP.equals(id)) return tier >= 7;
		return true;
	}

	private static boolean isRemDemonismActive() {
		if (Minecraft.getInstance().player == null) {
			return false;
		}
		return ClientRemDemonismState.isActive(Minecraft.getInstance().player.getUUID());
	}

	private static boolean isRemDemonOnly(ResourceLocation id) {
		return AbilityIds.REM_MORNING_STAR.equals(id)
				|| AbilityIds.REM_MACE_CRATER.equals(id)
				|| AbilityIds.REM_ONI_KICK.equals(id)
				|| AbilityIds.REM_HUMA_ICE_SPIKES.equals(id);
	}

	private static boolean isRemVisible(ResourceLocation id, boolean demonism) {
		if (AbilityIds.REM_ONI_RAGE.equals(id) && demonism) {
			return false;
		}
		return !isRemDemonOnly(id) || demonism;
	}

	// Animation state
	private static float openProgress = 0f;
	private static float lastOpenProgress = 0f;
	private static final int OPEN_ANIM_TICKS = 5;

	public static void animTick() {
		lastOpenProgress = openProgress;
		if (open) {
			openProgress = Math.min(1f, openProgress + 1f / OPEN_ANIM_TICKS);
		} else {
			openProgress = Math.max(0f, openProgress - 1f / (OPEN_ANIM_TICKS / 2f));
		}
	}

	public static void render(GuiGraphics graphics, DeltaTracker tracker) {
		float partial = tracker.getGameTimeDeltaPartialTick(false);
		float anim = lastOpenProgress + (openProgress - lastOpenProgress) * partial;
		if (anim <= 0.001f) {
			return;
		}
		List<ResourceLocation> abilities = visibleAbilities();
		if (abilities.isEmpty()) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		int cx = mc.getWindow().getGuiScaledWidth() / 2;
		int cy = mc.getWindow().getGuiScaledHeight() / 2;
		int n = abilities.size();
		HeroTheme theme = ClientHeroState.theme();
		boolean isThanos = ThanosHero.ID.equals(ClientHeroState.heroId());

		float eased = HudAnimator.smoothstep(anim);

		graphics.pose().pushPose();
		graphics.pose().translate(cx, cy, 0);
		graphics.pose().scale(eased, eased, 1f);
		graphics.pose().translate(-cx, -cy, 0);

		// Soft vignette behind the wheel
		int vigR = OUTER_R + 46;
		int vigA = (int) (0x55 * eased);
		graphics.fillGradient(cx - vigR, cy - vigR, cx + vigR, cy + vigR,
				(vigA << 24) | 0x05070F, 0x00040610);

		// Sector geometry: sector i is centered at angle (i / n) * 2PI - PI/2
		float per = (float) (2 * Math.PI / n);

		// 1) Donut base (dark glass)
		drawAnnulus(graphics, cx, cy, INNER_R, OUTER_R, (int) (0xC8 * eased) << 24 | 0x0A0D18);

		// 2) Hovered wedge in hero color
		if (selected >= 0 && selected < n) {
			float mid = selected * per - (float) Math.PI / 2f;
			int wedgeColor = applyAlpha(theme.panelTop(), (int) (235 * eased), 1.35f);
			drawWedge(graphics, cx, cy, INNER_R + 1, OUTER_R - 1, mid - per / 2f, mid + per / 2f, wedgeColor);
			int rimColor = applyAlpha(theme.radialBorderActive(), (int) (255 * eased), 1f);
			drawWedge(graphics, cx, cy, OUTER_R - 1, OUTER_R + 2, mid - per / 2f, mid + per / 2f, rimColor);
		}

		// 3) Rings
		int ringColor = applyAlpha(theme.radialBorderIdle(), (int) (190 * eased), 0.55f);
		drawAnnulus(graphics, cx, cy, OUTER_R, OUTER_R + 1, ringColor);
		drawAnnulus(graphics, cx, cy, INNER_R - 1, INNER_R, ringColor);

		// 4) Sector dividers
		if (n > 1) {
			int divColor = (int) (0x2E * eased) << 24 | 0xFFFFFF;
			for (int i = 0; i < n; i++) {
				double b = i * per - Math.PI / 2 - per / 2;
				drawRadialLine(graphics, cx, cy, INNER_R, OUTER_R, b, divColor);
			}
		}

		// 5) Icons + key labels at sector centroids
		int iconRadius = (INNER_R + OUTER_R) / 2;
		for (int i = 0; i < n; i++) {
			double mid = i * per - Math.PI / 2;
			int ix = cx + (int) Math.round(Math.cos(mid) * iconRadius);
			int iy = cy + (int) Math.round(Math.sin(mid) * iconRadius);
			ResourceLocation aid = abilities.get(i);
			boolean isSel = i == selected;
			int cdTicks = ClientAbilityCooldowns.remainingTicks(aid);

			int iconSize = isSel ? 20 : 16;
			int half = iconSize / 2;
			int accent = isSel ? theme.radialTextActive() : applyAlpha(theme.energyIcon(), 230, 0.9f);
			AbilityIcons.draw(graphics, aid, ix - half, iy - half - 4, iconSize, accent);
			if (cdTicks > 0) {
				graphics.fill(ix - half, iy - half - 4, ix + half, iy + half - 4, 0x99000000);
				graphics.drawCenteredString(mc.font, cooldownText(cdTicks), ix, iy + half - 1, COLOR_COOLDOWN);
			} else {
				graphics.drawCenteredString(mc.font, keyForSlot(i), ix, iy + half - 1,
						isSel ? theme.radialKeyActive() : 0x99B9BECF);
			}

			if (isThanos) {
				drawThanosStoneBadge(graphics, mc, aid, ix, iy - half - 6);
			}

			// Reinhard sword-draw ready halo (small gold ring around icon)
			boolean swordDrawReady = AbilityIds.REINHARD_SWORD_DRAW.equals(aid)
					&& !ClientHeroState.data().isActive(AbilityIds.REINHARD_SWORD_DRAW)
					&& com.example.superheroes.client.ClientReinhardSwordGateState.ready();
			if (swordDrawReady) {
				float pulse = 0.55f + 0.45f * (float) Math.sin(System.currentTimeMillis() / 220.0);
				int a = Math.max(70, Math.min(255, (int) (200 * pulse)));
				HudUtil.roundedRectBorder(graphics, ix - half - 2, iy - half - 6, iconSize + 4, iconSize + 4,
						(a << 24) | 0x00FFD24A);
			}
		}

		// 6) Center hub
		drawHub(graphics, mc, cx, cy, theme, abilities, eased);

		// 7) Virtual cursor: thin ray from center + glowing dot, so you always
		// see where your mouse-look is pointing and how to cancel (drag to hub).
		drawCursor(graphics, cx, cy, theme, eased);

		graphics.pose().popPose();
	}

	private static void drawCursor(GuiGraphics graphics, int cx, int cy, HeroTheme theme, float eased) {
		float mag = (float) Math.sqrt(cursorDx * cursorDx + cursorDy * cursorDy);
		if (mag < 2f) {
			return;
		}
		int tipX = cx + Math.round(cursorDx);
		int tipY = cy + Math.round(cursorDy);
		double angle = Math.atan2(cursorDy, cursorDx);
		boolean inHub = mag < INNER_R;
		int rayColor = applyAlpha(inHub ? 0xFF7C8499 : theme.radialBorderActive(), (int) (170 * eased), 1f);
		// ray from hub edge (or center while inside) to the dot
		int rayStart = inHub ? 2 : Math.min((int) mag, HUB_R);
		drawRadialLine(graphics, cx, cy, rayStart, (int) mag - 3, angle, rayColor);
		// glowing dot
		long now = System.currentTimeMillis();
		float pulse = 0.7f + 0.3f * (float) Math.sin(now / 180.0);
		int core = applyAlpha(inHub ? 0xFFB9BECF : theme.radialTextActive(), (int) (255 * eased), 1f);
		int halo = applyAlpha(inHub ? 0xFFB9BECF : theme.radialBorderActive(), (int) (110 * pulse * eased), 1f);
		drawDisk(graphics, tipX, tipY, 4, halo);
		drawDisk(graphics, tipX, tipY, 2, core);
	}

	private static void drawHub(GuiGraphics graphics, Minecraft mc, int cx, int cy, HeroTheme theme,
			List<ResourceLocation> abilities, float eased) {
		drawDisk(graphics, cx, cy, HUB_R, (int) (0xE0 * eased) << 24 | 0x0C101E);
		drawAnnulus(graphics, cx, cy, HUB_R - 1, HUB_R, applyAlpha(theme.radialBorderActive(), (int) (220 * eased), 0.9f));

		ResourceLocation heroId = ClientHeroState.heroId();
		if (heroId != null) {
			Component heroName = Component.translatable("hero.superheroes." + heroId.getPath());
			graphics.drawCenteredString(mc.font, heroName, cx, cy - 14, applyAlpha(theme.heroNameColor(), 220, 0.9f));
		}
		if (selected >= 0 && selected < abilities.size()) {
			ResourceLocation aid = abilities.get(selected);
			Component name = Component.translatable(AbilityDescriptions.nameKey(aid));
			int statusY = drawHubName(graphics, mc, name.getString(), cx, cy, theme);
			int cd = ClientAbilityCooldowns.remainingTicks(aid);
			if (cd > 0) {
				graphics.drawCenteredString(mc.font, cooldownText(cd), cx, statusY, COLOR_COOLDOWN);
			} else {
				graphics.drawCenteredString(mc.font, Component.translatable("hud.superheroes.radial.ready"),
						cx, statusY, 0xFF6BFF8C);
			}
		} else {
			graphics.drawCenteredString(mc.font, Component.translatable("hud.superheroes.radial.hint"),
					cx, cy + 2, 0x887C8499);
		}
	}

	/**
	 * Ability name in the hub: tries one line, then a 2-line wrap, then shrinks
	 * the font so the full name ALWAYS fits inside the hub. Returns the y for
	 * the status line below the name.
	 */
	private static int drawHubName(GuiGraphics graphics, Minecraft mc, String name, int cx, int cy, HeroTheme theme) {
		int maxW = HUB_R * 2 - 10;
		int color = theme.radialTextActive();
		if (mc.font.width(name) <= maxW) {
			graphics.drawCenteredString(mc.font, Component.literal(name), cx, cy - 2, color);
			return cy + 10;
		}
		// 2-line wrap at the most central space
		String l1 = name;
		String l2 = "";
		int bestSplit = -1;
		int bestScore = Integer.MAX_VALUE;
		for (int i = name.indexOf(' '); i >= 0; i = name.indexOf(' ', i + 1)) {
			int score = Math.abs(mc.font.width(name.substring(0, i)) - mc.font.width(name.substring(i + 1)));
			if (score < bestScore) {
				bestScore = score;
				bestSplit = i;
			}
		}
		if (bestSplit > 0) {
			l1 = name.substring(0, bestSplit);
			l2 = name.substring(bestSplit + 1);
		}
		float scale = 1f;
		int widest = Math.max(mc.font.width(l1), mc.font.width(l2));
		if (widest > maxW) {
			scale = Math.max(0.6f, (float) maxW / widest);
		}
		graphics.pose().pushPose();
		graphics.pose().translate(cx, cy - 2, 0);
		graphics.pose().scale(scale, scale, 1f);
		if (l2.isEmpty()) {
			graphics.drawCenteredString(mc.font, Component.literal(l1), 0, -4, color);
		} else {
			graphics.drawCenteredString(mc.font, Component.literal(l1), 0, -9, color);
			graphics.drawCenteredString(mc.font, Component.literal(l2), 0, 1, color);
		}
		graphics.pose().popPose();
		return cy + 12;
	}

	/** Filled circle via horizontal scanlines. */
	private static void drawDisk(GuiGraphics g, int cx, int cy, int r, int color) {
		for (int dy = -r; dy <= r; dy++) {
			int span = (int) Math.sqrt((double) r * r - (double) dy * dy);
			g.fill(cx - span, cy + dy, cx + span, cy + dy + 1, color);
		}
	}

	/** Filled ring (annulus) via horizontal scanlines. */
	private static void drawAnnulus(GuiGraphics g, int cx, int cy, int r0, int r1, int color) {
		for (int dy = -r1; dy <= r1; dy++) {
			long d2 = (long) dy * dy;
			int xOut = (int) Math.sqrt((double) r1 * r1 - d2);
			if (Math.abs(dy) >= r0) {
				g.fill(cx - xOut, cy + dy, cx + xOut, cy + dy + 1, color);
			} else {
				int xIn = (int) Math.sqrt((double) r0 * r0 - d2);
				g.fill(cx - xOut, cy + dy, cx - xIn, cy + dy + 1, color);
				g.fill(cx + xIn, cy + dy, cx + xOut, cy + dy + 1, color);
			}
		}
	}

	/** Filled annular sector between angles a0..a1 (radians, screen coords, y-down). */
	private static void drawWedge(GuiGraphics g, int cx, int cy, int r0, int r1, float a0, float a1, int color) {
		for (int dy = -r1; dy <= r1; dy++) {
			long d2 = (long) dy * dy;
			int xOut = (int) Math.sqrt((double) r1 * r1 - d2);
			int runStart = Integer.MIN_VALUE;
			for (int dx = -xOut; dx <= xOut; dx++) {
				boolean inside = false;
				long dist2 = (long) dx * dx + d2;
				if (dist2 >= (long) r0 * r0 && dist2 <= (long) r1 * r1) {
					double ang = Math.atan2(dy, dx);
					inside = angleInRange(ang, a0, a1);
				}
				if (inside && runStart == Integer.MIN_VALUE) {
					runStart = dx;
				} else if (!inside && runStart != Integer.MIN_VALUE) {
					g.fill(cx + runStart, cy + dy, cx + dx, cy + dy + 1, color);
					runStart = Integer.MIN_VALUE;
				}
			}
			if (runStart != Integer.MIN_VALUE) {
				g.fill(cx + runStart, cy + dy, cx + xOut + 1, cy + dy + 1, color);
			}
		}
	}

	private static boolean angleInRange(double ang, float a0, float a1) {
		double twoPi = 2 * Math.PI;
		double norm = ((ang - a0) % twoPi + twoPi) % twoPi;
		double width = ((a1 - a0) % twoPi + twoPi) % twoPi;
		return norm <= width;
	}

	private static void drawRadialLine(GuiGraphics g, int cx, int cy, int r0, int r1, double angle, int color) {
		double cos = Math.cos(angle);
		double sin = Math.sin(angle);
		for (int r = r0; r <= r1; r++) {
			int px = cx + (int) Math.round(cos * r);
			int py = cy + (int) Math.round(sin * r);
			g.fill(px, py, px + 1, py + 1, color);
		}
	}

	private static int applyAlpha(int argb, int alpha, float mult) {
		int originalA = (argb >>> 24) & 0xFF;
		int finalA = Math.min(255, Math.max(0, (int) (originalA * (alpha / 255f) * mult)));
		return (finalA << 24) | (argb & 0x00FFFFFF);
	}

	private static void drawThanosStoneBadge(GuiGraphics graphics, Minecraft mc, ResourceLocation aid,
			int slotCenterX, int topY) {
		ItemStack stack;
		int color;
		boolean owned;
		if (ThanosHero.isSnapAbility(aid)) {
			stack = new ItemStack(ModItems.INFINITY_GAUNTLET);
			color = 0xFFFFD24A;
			owned = ClientThanosState.hasAllStones();
		} else {
			InfinityStoneType type = ThanosHero.getRequiredStoneFor(aid);
			if (type == null) {
				return;
			}
			stack = stoneStackFor(type);
			color = type.getColor();
			owned = ClientThanosState.hasStone(type);
		}
		if (stack == null || stack.isEmpty()) {
			return;
		}
		int badgeSize = 20;
		int iconX = slotCenterX - 8;
		int iconY = topY - badgeSize - 2;
		int bx = slotCenterX - badgeSize / 2;
		int by = iconY - 2;
		HudUtil.roundedRectFill(graphics, bx, by, badgeSize, badgeSize, 0xCC080A14);
		HudUtil.roundedRectBorder(graphics, bx, by, badgeSize, badgeSize, color);
		long now = System.currentTimeMillis();
		float pulse = 0.55f + 0.45f * (float) Math.sin(now / 280.0);
		int glowAlpha = (int) (160 * pulse);
		int glow = ((Math.max(40, glowAlpha) & 0xFF) << 24) | (color & 0x00FFFFFF);
		HudUtil.roundedRectBorder(graphics, bx - 1, by - 1, badgeSize + 2, badgeSize + 2, glow);
		RenderSystem.enableBlend();
		graphics.renderItem(stack, iconX, iconY);
		RenderSystem.disableBlend();
		if (!owned) {
			graphics.fill(bx + 1, by + 1, bx + badgeSize - 1, by + badgeSize - 1, 0xB0000814);
			graphics.drawCenteredString(mc.font, Component.literal("\u2715"),
					slotCenterX, by + 6, 0xFFE03030);
		}
	}

	private static ItemStack stoneStackFor(InfinityStoneType type) {
		return switch (type) {
			case POWER -> new ItemStack(ModItems.POWER_STONE);
			case SPACE -> new ItemStack(ModItems.SPACE_STONE);
			case REALITY -> new ItemStack(ModItems.REALITY_STONE);
			case SOUL -> new ItemStack(ModItems.SOUL_STONE);
			case TIME -> new ItemStack(ModItems.TIME_STONE);
			case MIND -> new ItemStack(ModItems.MIND_STONE);
		};
	}

	private static Component cooldownText(int ticks) {
		int tenths = Math.max(1, (int) Math.ceil(ticks / 2.0));
		if (tenths < 100) {
			return Component.literal((tenths / 10) + "." + (tenths % 10) + "s");
		}
		return Component.literal((int) Math.ceil(ticks / 20.0) + "s");
	}

	private static Component keyForSlot(int index) {
		if (ModKeys.ABILITY_SLOTS != null && index < ModKeys.ABILITY_SLOTS.length) {
			return ModKeys.ABILITY_SLOTS[index].getTranslatedKeyMessage();
		}
		return Component.literal(String.valueOf(index + 1));
	}
}
