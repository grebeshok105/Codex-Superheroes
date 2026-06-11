package com.example.superheroes.client.mixin;

import com.example.superheroes.client.hud.HotbarOverrideHud;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hides vanilla hotbar (HP hearts, food, armor, XP bar, held item tooltip, and the hotbar itself)
 * when a superhero is active — the custom HeroInfoPanelHud + AbilityBarHud replaces everything.
 * Runs at priority 900 so it fires BEFORE GuiVanillaGlitchMixin (default 1000):
 * if we cancel, glitchMixin's HEAD won't push, so we don't need to pop.
 */
@Mixin(value = Gui.class, priority = 900)
public class GuiHotbarMixin {
	@Inject(method = "renderHotbarAndDecorations", at = @At("HEAD"), cancellable = true)
	private void superheroes$hideHotbar(GuiGraphics graphics, DeltaTracker tracker, CallbackInfo ci) {
		if (HotbarOverrideHud.shouldSuppressVanillaHotbar()) {
			ci.cancel();
		}
	}

	@Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
	private void superheroes$hideCrosshairInRadial(GuiGraphics graphics, DeltaTracker tracker, CallbackInfo ci) {
		if (com.example.superheroes.client.hud.RadialMenuHud.isOpen()) {
			ci.cancel();
		}
	}
}
