package com.example.superheroes.client.hud;

import com.example.superheroes.ModId;
import com.example.superheroes.client.ClientHeroState;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class LowResourceVignetteHud {
	private static final float WARN_THRESHOLD = 0.25f;
	private static final float CRITICAL_THRESHOLD = 0.10f;
	private static final ResourceLocation VIGNETTE = ModId.of("textures/gui/vignette.png");

	private LowResourceVignetteHud() {
	}

	public static void render(GuiGraphics graphics, DeltaTracker tracker) {
		if (!ClientHeroState.data().hasHero()) {
			return;
		}
		ResourceLocation heroId = ClientHeroState.data().heroId();
		if (heroId != null && ("homelander".equals(heroId.getPath())
				|| "sung_jinwoo".equals(heroId.getPath())
				|| "doomsday".equals(heroId.getPath()))) {
			return;
		}
		float energyMax = ClientHeroState.energyMax();
		float manaMax = ClientHeroState.manaMax();
		float energyPct = energyMax <= 0f ? 1f : ClientHeroState.data().energy() / energyMax;
		float manaPct = manaMax <= 0f ? 1f : ClientHeroState.data().mana() / manaMax;
		float worst = Math.min(energyPct, manaPct);
		if (worst >= WARN_THRESHOLD) {
			return;
		}
		float intensity = 1f - (worst / WARN_THRESHOLD);
		boolean critical = worst < CRITICAL_THRESHOLD;
		float pulse = critical ? (0.65f + 0.35f * (float) Math.abs(Math.sin(System.currentTimeMillis() / 180.0))) : 1f;
		float alpha = Math.min(1f, intensity * pulse * 0.85f);
		Minecraft mc = Minecraft.getInstance();
		int w = mc.getWindow().getGuiScaledWidth();
		int h = mc.getWindow().getGuiScaledHeight();
		float r = critical ? 1.00f : 0.95f;
		float g = critical ? 0.27f : 0.82f;
		float b = critical ? 0.33f : 0.42f;
		RenderSystem.enableBlend();
		RenderSystem.setShaderColor(r, g, b, alpha);
		graphics.blit(VIGNETTE, 0, 0, 0f, 0f, w, h, w, h);
		RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
	}
}
