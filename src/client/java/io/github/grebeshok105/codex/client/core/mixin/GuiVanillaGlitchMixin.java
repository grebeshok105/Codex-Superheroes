package io.github.grebeshok105.codex.client.core.mixin;

import io.github.grebeshok105.codex.client.core.HudJitter;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * §5.4 Glitches vanilla UI (HP, food, hotbar) while a hero module's
 * {@link HudGlitchSource} is active — pushes the pose jitter around the whole
 * hotbar/indicator block render. Chat is untouched (not in this method). F3 untouched.
 */
@Mixin(Gui.class)
public abstract class GuiVanillaGlitchMixin {
	@Inject(method = "renderHotbarAndDecorations", at = @At("HEAD"))
	private void superheroes$pushJitter(GuiGraphics graphics, DeltaTracker tracker, CallbackInfo ci) {
		float ramp = HudJitter.ramp();
		if (ramp <= 0.001f) return;
		graphics.pose().pushPose();
		graphics.pose().translate(HudJitter.jitterX(), HudJitter.jitterY(), 0f);
	}

	@Inject(method = "renderHotbarAndDecorations", at = @At("RETURN"))
	private void superheroes$popJitter(GuiGraphics graphics, DeltaTracker tracker, CallbackInfo ci) {
		float ramp = HudJitter.ramp();
		if (ramp <= 0.001f) return;
		graphics.pose().popPose();
	}
}
