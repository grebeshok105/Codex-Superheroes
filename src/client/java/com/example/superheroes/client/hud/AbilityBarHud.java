package com.example.superheroes.client.hud;

import com.example.superheroes.ModId;
import com.example.superheroes.ability.AbilityIds;
import com.example.superheroes.client.ClientAbilityCooldowns;
import com.example.superheroes.client.ClientDoomsdayState;
import com.example.superheroes.client.ClientHeroState;
import com.example.superheroes.client.ClientRemDemonismState;
import com.example.superheroes.client.ClientThanosState;
import com.example.superheroes.client.ModKeys;
import com.example.superheroes.hero.Hero;
import com.example.superheroes.hero.HeroHudConfig;
import com.example.superheroes.hero.HeroTheme;
import com.example.superheroes.hero.Heroes;
import com.example.superheroes.hero.RemHero;
import com.example.superheroes.hero.ThanosHero;
import com.example.superheroes.item.infinity.InfinityStoneType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public final class AbilityBarHud {
	private static final int BASE_SLOT_SIZE = 36;
	private static final int BASE_GAP = 4;
	private static final int BASE_BOTTOM_OFFSET = 40;

	private static final String[] KEYBIND_LABELS = {"Z", "X", "C", "V", "B", "3", "4", "5"};

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
		List<ResourceLocation> abilities = filterByState(ClientHeroState.abilities(), heroId);
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

			drawAbilitySlot(graphics, mc, sx, startY, slotSize, abilityId, i, isUlt,
					isActive, cdRemaining, cdTotal, theme, hudConfig);
		}
	}

	private static void drawAbilitySlot(GuiGraphics g, Minecraft mc, int x, int y, int size,
										 ResourceLocation abilityId, int index, boolean isUlt,
										 boolean isActive, int cdRemaining, int cdTotal,
										 HeroTheme theme, HeroHudConfig hudConfig) {
		// Slot background
		int bgColor = 0xCC0A0B14;
		HudUtil.roundedRectFill(g, x, y, size, size, bgColor);

		// Border color based on state
		int borderColor;
		if (isActive) {
			borderColor = 0xFF6BFF8C;
		} else if (cdRemaining > 0) {
			borderColor = 0xFFFF8866;
		} else {
			borderColor = theme.energyIcon();
		}
		HudUtil.roundedRectBorder(g, x, y, size, size, borderColor);

		// Ability icon: generated texture if present, fallback to badge letter
		int pad = Math.max(2, HudScaler.scale(2));
		if (AbilityIcons.texture(abilityId) != null) {
			AbilityIcons.draw(g, abilityId, x + pad, y + pad, size - pad * 2, borderColor);
		} else {
			String badge = isUlt ? "\u2605" : AbilityDescriptions.kindOf(abilityId).badge();
			Component badgeComp = Component.literal(badge).withStyle(ChatFormatting.BOLD);
			int badgeW = mc.font.width(badgeComp);
			g.drawString(mc.font, badgeComp, x + (size - badgeW) / 2, y + (size - 8) / 2 - 2, borderColor, true);
		}

		// Cooldown overlay
		if (cdRemaining > 0) {
			float cdPct = 1.0f - (cdTotal > 0 ? (float) cdRemaining / cdTotal : 0f);
			int overlayH = (int) (size * (1f - cdPct));
			g.fill(x + 1, y + 1, x + size - 1, y + 1 + overlayH, 0x88000000);

			// CD text
			float cdSeconds = cdRemaining / 20f;
			String cdText = String.format(java.util.Locale.ROOT, "%.1f", cdSeconds);
			Component cdComp = Component.literal(cdText);
			int cdW = mc.font.width(cdComp);
			g.drawString(mc.font, cdComp, x + (size - cdW) / 2, y + size + 2, 0xFFFF9D6E, true);

			// Circular progress indicator (simplified as corner highlights)
			drawCooldownArc(g, x, y, size, cdPct, borderColor);
		} else {
			// Ready glow
			float pulse = HudAnimator.pulse(1.5f);
			int glowAlpha = (int) (pulse * 40);
			if (glowAlpha > 5) {
				HudUtil.roundedRectFill(g, x - 1, y - 1, size + 2, size + 2, (glowAlpha << 24) | (borderColor & 0x00FFFFFF));
			}
		}

		// Slot number top-left
		Component numComp = Component.literal(String.valueOf(index + 1));
		g.drawString(mc.font, numComp, x + 2, y + 1, 0xAAFFFFFF, true);

		// Keybind label bottom
		String key = index < KEYBIND_LABELS.length ? KEYBIND_LABELS[index] : "?";
		Component keyComp = Component.literal(key);
		int keyW = mc.font.width(keyComp);
		int keyY = y + size + (cdRemaining > 0 ? 12 : 2);
		g.drawString(mc.font, keyComp, x + (size - keyW) / 2, keyY, 0x99FFFFFF, true);

	}

	private static void drawCooldownArc(GuiGraphics g, int x, int y, int size, float progress, int color) {
		int arcColor = (0xCC << 24) | (color & 0x00FFFFFF);
		int thickness = Math.max(1, HudScaler.scale(2));

		// Top edge
		int topFillW = (int) (size * Math.min(1f, progress * 4f));
		if (topFillW > 0) {
			g.fill(x, y - thickness, x + topFillW, y, arcColor);
		}
		// Right edge
		if (progress > 0.25f) {
			int rightFillH = (int) (size * Math.min(1f, (progress - 0.25f) * 4f));
			g.fill(x + size, y, x + size + thickness, y + rightFillH, arcColor);
		}
		// Bottom edge
		if (progress > 0.5f) {
			int bottomFillW = (int) (size * Math.min(1f, (progress - 0.5f) * 4f));
			g.fill(x + size - bottomFillW, y + size, x + size, y + size + thickness, arcColor);
		}
		// Left edge
		if (progress > 0.75f) {
			int leftFillH = (int) (size * Math.min(1f, (progress - 0.75f) * 4f));
			g.fill(x - thickness, y + size - leftFillH, x, y + size, arcColor);
		}
	}

	private static List<ResourceLocation> filterByState(List<ResourceLocation> base, ResourceLocation heroId) {
		if (ModId.of("doomsday").equals(heroId)) {
			int tier = ClientDoomsdayState.tier();
			ArrayList<ResourceLocation> out = new ArrayList<>(base.size());
			for (ResourceLocation id : base) {
				if (isDoomsdayUnlocked(id, tier)) out.add(id);
			}
			return out;
		}
		if (ThanosHero.ID.equals(heroId)) {
			ArrayList<ResourceLocation> out = new ArrayList<>(base.size());
			for (ResourceLocation id : base) {
				if (isThanosUnlocked(id)) out.add(id);
			}
			return out;
		}
		if (RemHero.ID.equals(heroId)) {
			boolean demonism = Minecraft.getInstance().player != null && ClientRemDemonismState.isActive(Minecraft.getInstance().player.getUUID());
			ArrayList<ResourceLocation> out = new ArrayList<>(base.size());
			for (ResourceLocation id : base) {
				if (isRemVisible(id, demonism)) out.add(id);
			}
			return out;
		}
		return base;
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

	private static boolean isRemVisible(ResourceLocation id, boolean demonism) {
		if (AbilityIds.REM_ONI_RAGE.equals(id) && demonism) return false;
		boolean demonOnly = AbilityIds.REM_MORNING_STAR.equals(id)
				|| AbilityIds.REM_MACE_CRATER.equals(id)
				|| AbilityIds.REM_ONI_KICK.equals(id)
				|| AbilityIds.REM_HUMA_ICE_SPIKES.equals(id);
		return !demonOnly || demonism;
	}
}
