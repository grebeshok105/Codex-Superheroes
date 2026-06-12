package com.example.superheroes.client.hud;

import com.example.superheroes.ability.AbilityIds;
import com.example.superheroes.client.ClientAbilityCooldowns;
import com.example.superheroes.client.ClientAbilityFilter;
import com.example.superheroes.client.ClientHeroState;
import com.example.superheroes.client.ClientThanosState;
import com.example.superheroes.client.ModKeys;
import com.example.superheroes.client.render.WildRenderer;
import com.example.superheroes.client.render.WildShaders;
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
 * Radial ability wheel v2: separated arc segments with gaps, staggered bloom-in,
 * smooth hover growth with a neon rim, a clean EMPTY glass hub (no text), and an
 * info plaque BELOW the ring with the hovered ability name + cooldown.
 * The virtual cursor accumulates mouse-look INCREMENTALLY and is hard-clamped to
 * the wheel radius, so it never "banks up" off-screen distance.
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
	private static float lastYaw;
	private static float lastPitch;
	private static int selected = -1;
	private static float cursorDx;
	private static float cursorDy;
	private static float prevCursorDx;
	private static float prevCursorDy;

	/** Per-sector hover grow progress (0..1), tick-stepped + partial-interpolated. */
	private static final int MAX_SECTORS = 12;
	private static final float[] hoverProgress = new float[MAX_SECTORS];
	private static final float[] lastHoverProgress = new float[MAX_SECTORS];

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
			lastYaw = mc.player.getYRot();
			lastPitch = mc.player.getXRot();
			selected = -1;
			cursorDx = 0f;
			cursorDy = 0f;
			prevCursorDx = 0f;
			prevCursorDy = 0f;
			java.util.Arrays.fill(hoverProgress, 0f);
			java.util.Arrays.fill(lastHoverProgress, 0f);
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
		// INCREMENTAL mouse-look delta since last tick — no accumulation:
		// the cursor itself is clamped to the wheel, so swinging far left never
		// requires swinging just as far right to come back.
		float dyaw = net.minecraft.util.Mth.wrapDegrees(mc.player.getYRot() - lastYaw);
		float dpitch = mc.player.getXRot() - lastPitch;
		lastYaw = mc.player.getYRot();
		lastPitch = mc.player.getXRot();

		prevCursorDx = cursorDx;
		prevCursorDy = cursorDy;
		cursorDx += dyaw * PX_PER_DEG;
		cursorDy += dpitch * PX_PER_DEG;
		float mag = (float) Math.sqrt(cursorDx * cursorDx + cursorDy * cursorDy);
		float maxR = OUTER_R + 6;
		if (mag > maxR) {
			cursorDx *= maxR / mag;
			cursorDy *= maxR / mag;
			mag = maxR;
		}
		if (mag < HUB_R) {
			// cursor returned to the hub: selection is dropped, release does nothing
			selected = -1;
			return;
		}
		float per = 360f / n;
		double angle = Math.toDegrees(Math.atan2(cursorDy, cursorDx)) + 90.0 + per / 2.0;
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
		return ClientAbilityFilter.visibleFor(ClientHeroState.abilities(), ClientHeroState.heroId());
	}

	// Animation state
	private static float openProgress = 0f;
	private static float lastOpenProgress = 0f;
	private static final int OPEN_ANIM_TICKS = 3;

	public static void animTick() {
		lastOpenProgress = openProgress;
		if (open) {
			openProgress = Math.min(1f, openProgress + 1f / OPEN_ANIM_TICKS);
		} else {
			openProgress = Math.max(0f, openProgress - 1f / (OPEN_ANIM_TICKS / 2f));
		}
		for (int i = 0; i < MAX_SECTORS; i++) {
			lastHoverProgress[i] = hoverProgress[i];
			float target = (open && i == selected) ? 1f : 0f;
			float step = 0.34f;
			if (hoverProgress[i] < target) {
				hoverProgress[i] = Math.min(target, hoverProgress[i] + step);
			} else if (hoverProgress[i] > target) {
				hoverProgress[i] = Math.max(target, hoverProgress[i] - step);
			}
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
		// Per-frame cursor/selection update (the 20 Hz tick-only update felt laggy)
		if (open && mc.player != null) {
			updateSelection(mc, n);
		}
		HeroTheme theme = ClientHeroState.theme();
		boolean isThanos = ThanosHero.ID.equals(ClientHeroState.heroId());

		float eased = HudAnimator.smoothstep(anim);

		graphics.pose().pushPose();
		graphics.pose().translate(cx, cy, 0);
		float openScale = 0.92f + 0.08f * eased;
		graphics.pose().scale(openScale, openScale, 1f);
		graphics.pose().translate(-cx, -cy, 0);

		float per = (float) (2 * Math.PI / n);
		// angular gap between segments so arcs read as separate "petals"
		float gap = Math.min(per * 0.16f, (float) Math.toRadians(5.0));

		for (int i = 0; i < n; i++) {
			float mid = i * per - (float) Math.PI / 2f;
			// staggered bloom-in: each petal fades/slides in clockwise
			float stagger = n <= 1 ? 0f : (0.18f * i) / n;
			float petal = HudAnimator.smoothstep(clamp01((anim - stagger) / Math.max(0.001f, 1f - 0.18f)));
			if (petal <= 0.01f) {
				continue;
			}
			float hover = hoverFor(i, partial);
			// hovered petal grows outward; idle petals breathe in slightly while opening
			int grow = Math.round(hover * 7f);
			int r0 = INNER_R + 1 - Math.round(hover * 2f);
			int r1 = OUTER_R - 4 + Math.round((petal - 1f) * 8f) + grow;
			float a0 = mid - per / 2f + gap / 2f;
			float a1 = mid + per / 2f - gap / 2f;

			// petal body: dark glass -> hero color as hover rises
			int bodyA = (int) ((0xB6 + 0x30 * hover) * petal * eased);
			int bodyColor = hover > 0.01f
					? blend(0xFF0A0D18, applyAlpha(theme.panelTop(), 255, 1.35f), hover * 0.85f)
					: 0xFF0A0D18;
			drawWedge(graphics, cx, cy, r0, r1, a0, a1, (bodyA << 24) | (bodyColor & 0xFFFFFF));

			// neon outer rim: idle = faint theme line, hover = bright + glow layer
			int rimA = (int) ((90 + 165 * hover) * petal * eased);
			int rim = applyAlpha(theme.radialBorderActive(), rimA, 1f);
			drawWedge(graphics, cx, cy, r1, r1 + 2, a0, a1, rim);
			if (hover > 0.01f) {
				int glowA = (int) (70 * hover * petal * eased);
				drawWedge(graphics, cx, cy, r1 + 2, r1 + 4, a0, a1, applyAlpha(theme.radialBorderActive(), glowA, 1f));
				int innerRimA = (int) (110 * hover * petal * eased);
				drawWedge(graphics, cx, cy, r0, r0 + 1, a0, a1, applyAlpha(theme.radialBorderActive(), innerRimA, 0.8f));
			}
		}

		// Icons + key labels at petal centroids (drawn after all petals)
		for (int i = 0; i < n; i++) {
			float stagger = n <= 1 ? 0f : (0.18f * i) / n;
			float petal = HudAnimator.smoothstep(clamp01((anim - stagger) / Math.max(0.001f, 1f - 0.18f)));
			if (petal <= 0.05f) {
				continue;
			}
			float hover = hoverFor(i, partial);
			double mid = i * per - Math.PI / 2;
			int iconRadius = (INNER_R + OUTER_R) / 2 + Math.round(hover * 4f);
			int ix = cx + (int) Math.round(Math.cos(mid) * iconRadius);
			int iy = cy + (int) Math.round(Math.sin(mid) * iconRadius);
			ResourceLocation aid = abilities.get(i);
			boolean isSel = i == selected;
			int cdTicks = ClientAbilityCooldowns.remainingTicks(aid);

			int iconSize = 20 + Math.round(hover * 6f);
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

		// Clean empty glass hub — deliberately NO text inside
		drawDisk(graphics, cx, cy, HUB_R, (int) (0xC2 * eased) << 24 | 0x0C101E);
		drawAnnulus(graphics, cx, cy, HUB_R - 1, HUB_R, applyAlpha(theme.radialBorderActive(), (int) (180 * eased), 0.8f));

		// Info plaque BELOW the ring: hovered ability name + status
		drawInfoPlaque(graphics, mc, cx, cy, theme, abilities, eased);

		// Virtual cursor: thin ray from center + glowing dot
		drawCursor(graphics, cx, cy, theme, eased, partial);

		graphics.pose().popPose();
	}

	private static float hoverFor(int i, float partial) {
		if (i < 0 || i >= MAX_SECTORS) {
			return 0f;
		}
		float h = lastHoverProgress[i] + (hoverProgress[i] - lastHoverProgress[i]) * partial;
		return HudAnimator.smoothstep(clamp01(h));
	}

	private static float clamp01(float v) {
		return v < 0f ? 0f : Math.min(v, 1f);
	}

	/** Linear ARGB blend (alpha from a). */
	private static int blend(int a, int b, float t) {
		int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
		int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
		int r = (int) (ar + (br - ar) * t);
		int g = (int) (ag + (bg - ag) * t);
		int bl = (int) (ab + (bb - ab) * t);
		return (a & 0xFF000000) | (r << 16) | (g << 8) | bl;
	}

	private static void drawInfoPlaque(GuiGraphics graphics, Minecraft mc, int cx, int cy, HeroTheme theme,
			List<ResourceLocation> abilities, float eased) {
		if (selected < 0 || selected >= abilities.size()) {
			return;
		}
		ResourceLocation aid = abilities.get(selected);
		Component name = Component.translatable(AbilityDescriptions.nameKey(aid));
		int cd = ClientAbilityCooldowns.remainingTicks(aid);
		Component status = cd > 0 ? cooldownText(cd) : Component.translatable("hud.superheroes.radial.ready");
		int statusColor = cd > 0 ? COLOR_COOLDOWN : 0xFF6BFF8C;

		int nameW = mc.font.width(name);
		int statusW = mc.font.width(status);
		int w = Math.max(nameW, statusW) + 18;
		int h = 26;
		int px = cx - w / 2;
		int py = cy + OUTER_R + 14;
		int bgA = (int) (0xD8 * eased);
		HudUtil.roundedRectFill(graphics, px, py, w, h, (bgA << 24) | 0x0A0D18);
		HudUtil.roundedRectBorder(graphics, px, py, w, h, applyAlpha(theme.radialBorderActive(), (int) (200 * eased), 0.9f));
		graphics.drawCenteredString(mc.font, name, cx, py + 4, theme.radialTextActive());
		graphics.drawCenteredString(mc.font, status, cx, py + 15, statusColor);
	}

	private static void drawCursor(GuiGraphics graphics, int cx, int cy, HeroTheme theme, float eased, float partial) {
		float dx = prevCursorDx + (cursorDx - prevCursorDx) * partial;
		float dy = prevCursorDy + (cursorDy - prevCursorDy) * partial;
		float mag = (float) Math.sqrt(dx * dx + dy * dy);
		if (mag < 2f) {
			return;
		}
		int tipX = cx + Math.round(dx);
		int tipY = cy + Math.round(dy);
		double angle = Math.atan2(dy, dx);
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

	/** Filled circle — gladkij GLSL-disk, fallback na scanlines. */
	private static void drawDisk(GuiGraphics g, int cx, int cy, int r, int color) {
		if (WildShaders.circleReady()) {
			WildRenderer.orb(g, cx, cy, r, color, 0, 0f, 0, 0f);
			return;
		}
		for (int dy = -r; dy <= r; dy++) {
			int span = (int) Math.sqrt((double) r * r - (double) dy * dy);
			g.fill(cx - span, cy + dy, cx + span, cy + dy + 1, color);
		}
	}

	/** Filled ring (annulus) — gladkoe GLSL-kolco, fallback na scanlines. */
	private static void drawAnnulus(GuiGraphics g, int cx, int cy, int r0, int r1, int color) {
		if (WildShaders.circleReady()) {
			float w = Math.max(1f, r1 - r0);
			WildRenderer.ring(g, cx, cy, (r0 + r1) / 2f, w, color, 0, 0f);
			return;
		}
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

	/**
	 * Filled annular sector between angles a0..a1 (radians, screen coords, y-down).
	 * Scans only the sector's bounding box instead of the whole disk — the old
	 * full-disk scan did hundreds of thousands of atan2 calls per frame and lagged.
	 */
	private static void drawWedge(GuiGraphics g, int cx, int cy, int r0, int r1, float a0, float a1, int color) {
		// GLSL: gladkij kolcevoj sektor s AA-kromkami (shejder sam konvertiruet
		// ekrannye ugly -> matematicheskie). Geometrija ta zhe, fallback nizhe.
		if (WildShaders.sectorReady()) {
			WildRenderer.sector(g, cx, cy, r0, r1, a0, a1, color, 0, 0f, 0, 0f);
			return;
		}
		// Bounding box of the sector: corners at a0/a1 (r0 and r1) plus axis
		// crossings (0, 90, 180, 270 deg) at r1 when inside the angular range.
		int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
		int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
		float[] angles = {a0, a1};
		for (float ang : angles) {
			double c = Math.cos(ang), s = Math.sin(ang);
			for (int r : new int[]{r0, r1}) {
				int px = (int) Math.round(c * r);
				int py = (int) Math.round(s * r);
				minX = Math.min(minX, px); maxX = Math.max(maxX, px);
				minY = Math.min(minY, py); maxY = Math.max(maxY, py);
			}
		}
		for (int k = 0; k < 4; k++) {
			double axis = k * Math.PI / 2;
			if (angleInRange(axis, a0, a1) || angleInRange(axis - 2 * Math.PI, a0, a1)) {
				int px = (int) Math.round(Math.cos(axis) * r1);
				int py = (int) Math.round(Math.sin(axis) * r1);
				minX = Math.min(minX, px); maxX = Math.max(maxX, px);
				minY = Math.min(minY, py); maxY = Math.max(maxY, py);
			}
		}
		minX -= 2; maxX += 2; minY -= 2; maxY += 2;
		int yFrom = Math.max(-r1, minY);
		int yTo = Math.min(r1, maxY);
		for (int dy = yFrom; dy <= yTo; dy++) {
			long d2 = (long) dy * dy;
			int xOut = (int) Math.sqrt((double) r1 * r1 - d2);
			int xFrom = Math.max(-xOut, minX);
			int xTo = Math.min(xOut, maxX);
			int runStart = Integer.MIN_VALUE;
			for (int dx = xFrom; dx <= xTo; dx++) {
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
		if (index < ModKeys.SLOT_LABELS.length) {
			return Component.literal(ModKeys.SLOT_LABELS[index]);
		}
		return Component.literal(String.valueOf(index + 1));
	}
}
