package com.example.superheroes.client.mixin;

import com.example.superheroes.client.hud.HudLayoutManager;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies the user's drag offset to the vanilla status effect icons (top-right).
 */
@Mixin(Gui.class)
public class GuiEffectsMixin {
	@Inject(method = "renderEffects", at = @At("HEAD"))
	private void superheroes$pushEffectsOffset(GuiGraphics graphics, DeltaTracker tracker, CallbackInfo ci) {
		int[] off = HudLayoutManager.offset(HudLayoutManager.EFFECTS);
		graphics.pose().pushPose();
		graphics.pose().translate(off[0], off[1], 0);
	}

	@Inject(method = "renderEffects", at = @At("RETURN"))
	private void superheroes$popEffectsOffset(GuiGraphics graphics, DeltaTracker tracker, CallbackInfo ci) {
		graphics.pose().popPose();
	}
}
