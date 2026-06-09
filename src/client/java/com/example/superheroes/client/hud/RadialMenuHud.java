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

public final class RadialMenuHud {
	private static final float DEAD_ZONE = 5f;
	private static final int ITEM_RADIUS = 110;
	private static final int BACKPLATE_RADIUS = 104;
	private static final int SLOT_MIN_WIDTH = 96;
	private static final int SLOT_HEIGHT = 26;
	private static final int SLOT_PADDING_X = 14;
	private static final int CURSOR_RADIUS = 56;

	private static final int COLOR_TEXT_IDLE = 0xFFE3E5F0;
	private static final int COLOR_KEY_IDLE = 0xFF7C8499;
	private static final int COLOR_COOLDOWN = 0xFFFFB45C;
	private static final int COLOR_SHADOW = 0x77000000;

	private static boolean open;
	private static float startYaw;
	private static float startPitch;
	private static int selected = -1;

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
		float magSq = dyaw * dyaw + dpitch * dpitch;
		if (magSq < DEAD_ZONE * DEAD_ZONE) {
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
			if (isRem && isRemDemonOnly(id) && !remDemonism) continue;
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

	public static void render(GuiGraphics graphics, DeltaTracker tracker) {
		if (!open) {
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
		drawBackplate(graphics, cx, cy);
		drawCursor(graphics, mc, cx, cy, theme);
		drawHub(graphics, cx, cy, theme);
		for (int i = 0; i < n; i++) {
			double angle = (i * 2 * Math.PI / n) - Math.PI / 2;
			int x = cx + (int) (Math.cos(angle) * ITEM_RADIUS);
			int y = cy + (int) (Math.sin(angle) * ITEM_RADIUS);
			ResourceLocation aid = abilities.get(i);
			Component name = Component.translatable("ability." + aid.getNamespace() + "." + aid.getPath());
			Component key = keyForSlot(i);
			boolean active = i == selected;
			int cooldownTicks = ClientAbilityCooldowns.remainingTicks(aid);
			int textWidth = mc.font.width(name);
			int slotWidth = Math.max(SLOT_MIN_WIDTH, textWidth + SLOT_PADDING_X * 2);
			int slotX = x - slotWidth / 2;
			int slotY = y - SLOT_HEIGHT / 2;
			boolean swordDrawReady = AbilityIds.REINHARD_SWORD_DRAW.equals(aid)
					&& !ClientHeroState.data().isActive(AbilityIds.REINHARD_SWORD_DRAW)
					&& com.example.superheroes.client.ClientReinhardSwordGateState.ready();
			if (swordDrawReady) {
				drawReadyHalo(graphics, slotX, slotY, slotWidth, SLOT_HEIGHT);
			}
			drawSlot(graphics, slotX, slotY, slotWidth, SLOT_HEIGHT, active, cooldownTicks > 0, theme);
			graphics.drawCenteredString(mc.font, name, x, y - 9,
					cooldownTicks > 0 ? 0xFFA7AAB8 : (active ? theme.radialTextActive() : COLOR_TEXT_IDLE));
			graphics.drawCenteredString(mc.font, cooldownTicks > 0 ? cooldownText(cooldownTicks) : key, x, y + 3,
					cooldownTicks > 0 ? COLOR_COOLDOWN : (active ? theme.radialKeyActive() : COLOR_KEY_IDLE));
			if (isThanos) {
				drawThanosStoneBadge(graphics, mc, aid, x, slotY);
			}
		}
	}

	private static void drawThanosStoneBadge(GuiGraphics graphics, Minecraft mc, ResourceLocation aid,
			int slotCenterX, int slotY) {
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
		int iconY = slotY - badgeSize - 2;
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

	private static void drawHub(GuiGraphics graphics, int cx, int cy, HeroTheme theme) {
		HudUtil.roundedRectFill(graphics, cx - 24, cy - 24, 48, 48, 0xCC080A14);
		HudUtil.roundedRectBorder(graphics, cx - 24, cy - 24, 48, 48, theme.radialBorderIdle());
		graphics.fill(cx - 18, cy - 22, cx + 18, cy - 21, 0x44FFFFFF);
	}

	private static void drawBackplate(GuiGraphics graphics, int cx, int cy) {
		int outer = BACKPLATE_RADIUS + 36;
		graphics.fillGradient(cx - outer, cy - outer, cx + outer, cy + outer, 0x44060814, 0x00040614);
		graphics.fillGradient(cx - BACKPLATE_RADIUS, cy - BACKPLATE_RADIUS,
				cx + BACKPLATE_RADIUS, cy + BACKPLATE_RADIUS, 0x88101422, 0x44050710);
	}

	private static void drawCursor(GuiGraphics graphics, Minecraft mc, int cx, int cy, HeroTheme theme) {
		if (mc.player == null) {
			return;
		}
		float dyaw = mc.player.getYRot() - startYaw;
		float dpitch = mc.player.getXRot() - startPitch;
		float magSq = dyaw * dyaw + dpitch * dpitch;
		boolean inDeadZone = magSq < DEAD_ZONE * DEAD_ZONE;
		if (inDeadZone) {
			return;
		}
		double a = Math.atan2(dpitch, dyaw);
		double r = CURSOR_RADIUS;
		int px = cx + (int) Math.round(r * Math.cos(a));
		int py = cy + (int) Math.round(r * Math.sin(a));
		drawSmoothLine(graphics, cx, cy, px, py, theme.radialGlow());
		graphics.fill(px - 4, py - 4, px + 5, py + 5, COLOR_SHADOW);
		graphics.fill(px - 3, py - 3, px + 4, py + 4, theme.radialBorderActive());
		graphics.fill(px - 2, py - 2, px + 3, py + 3, theme.radialTextActive());
	}

	private static void drawSmoothLine(GuiGraphics graphics, int x0, int y0, int x1, int y1, int color) {
		int dx = x1 - x0;
		int dy = y1 - y0;
		int steps = Math.max(Math.abs(dx), Math.abs(dy));
		if (steps == 0) {
			return;
		}
		float fx = (float) dx / steps;
		float fy = (float) dy / steps;
		for (int i = 24; i < steps - 4; i += 1) {
			int px = x0 + Math.round(fx * i);
			int py = y0 + Math.round(fy * i);
			graphics.fill(px, py, px + 1, py + 1, color);
		}
	}

	private static void drawReadyHalo(GuiGraphics graphics, int x, int y, int width, int height) {
		long now = System.currentTimeMillis();
		float pulse = 0.55f + 0.45f * (float) Math.sin(now / 220.0);
		int alpha = Math.max(70, Math.min(255, (int) (180 * pulse)));
		int glow = (alpha << 24) | 0x00FFD24A;
		int outer = (Math.max(40, alpha / 2) << 24) | 0x00FFB23A;
		HudUtil.roundedRectFill(graphics, x - 6, y - 6, width + 12, height + 12, outer);
		HudUtil.roundedRectFill(graphics, x - 3, y - 3, width + 6, height + 6, glow);
	}

	private static void drawSlot(GuiGraphics graphics, int x, int y, int width, int height,
			boolean selectedSlot, boolean onCooldown, HeroTheme theme) {
		HudUtil.dropShadow(graphics, x, y, width, height, 2, COLOR_SHADOW);
		if (selectedSlot) {
			HudUtil.roundedRectFill(graphics, x - 3, y - 3, width + 6, height + 6, theme.radialGlow());
		}
		int top = onCooldown ? 0xF0121218 : (selectedSlot ? 0xF02A1A14 : theme.panelTop());
		int bottom = onCooldown ? 0xE008080D : (selectedSlot ? 0xE01A0F0A : theme.panelBottom());
		int border = selectedSlot ? theme.radialBorderActive() : theme.radialBorderIdle();
		HudUtil.roundedRectGradient(graphics, x, y, width, height, top, bottom);
		HudUtil.roundedRectBorder(graphics, x, y, width, height, border);
		graphics.fill(x + 3, y + 1, x + width - 3, y + 2, 0x33FFFFFF);
	}

	private static Component cooldownText(int ticks) {
		int tenths = Math.max(1, (int) Math.ceil(ticks / 2.0));
		if (tenths < 100) {
			return Component.literal("CD " + (tenths / 10) + "." + (tenths % 10) + "s");
		}
		return Component.literal("CD " + (int) Math.ceil(ticks / 20.0) + "s");
	}

	private static Component keyForSlot(int index) {
		if (ModKeys.ABILITY_SLOTS != null && index < ModKeys.ABILITY_SLOTS.length) {
			return ModKeys.ABILITY_SLOTS[index].getTranslatedKeyMessage();
		}
		return Component.literal(String.valueOf(index + 1));
	}
}
