package com.example.superheroes.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Generated ability icon textures with graceful fallback to kind badges.
 * Looks for assets/superheroes/textures/gui/abilities/{ability_path}.png (64x64).
 */
public final class AbilityIcons {
	private static final int TEX_SIZE = 64;
	private static final Map<ResourceLocation, ResourceLocation> CACHE = new HashMap<>();
	private static final ResourceLocation MISSING = ResourceLocation.fromNamespaceAndPath("superheroes", "__missing__");

	private AbilityIcons() {
	}

	public static ResourceLocation texture(ResourceLocation abilityId) {
		ResourceLocation cached = CACHE.get(abilityId);
		if (cached != null) {
			return cached == MISSING ? null : cached;
		}
		ResourceLocation tex = ResourceLocation.fromNamespaceAndPath("superheroes",
				"textures/gui/abilities/" + abilityId.getPath() + ".png");
		boolean exists = Minecraft.getInstance().getResourceManager().getResource(tex).isPresent();
		CACHE.put(abilityId, exists ? tex : MISSING);
		return exists ? tex : null;
	}

	/** Draws the icon texture if present, otherwise a kind badge letter. */
	public static void draw(GuiGraphics g, ResourceLocation abilityId, int x, int y, int size, int fallbackColor) {
		ResourceLocation tex = texture(abilityId);
		if (tex != null) {
			RenderSystem.enableBlend();
			g.blit(tex, x, y, size, size, 0f, 0f, TEX_SIZE, TEX_SIZE, TEX_SIZE, TEX_SIZE);
			RenderSystem.disableBlend();
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		String badge = AbilityDescriptions.kindOf(abilityId).badge();
		Component badgeComp = Component.literal(badge).withStyle(ChatFormatting.BOLD);
		int bw = mc.font.width(badgeComp);
		g.drawString(mc.font, badgeComp, x + (size - bw) / 2, y + (size - 8) / 2, fallbackColor, true);
	}
}
