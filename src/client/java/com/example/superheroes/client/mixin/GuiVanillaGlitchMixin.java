package com.example.superheroes.client.mixin;

import com.example.superheroes.client.hud.ClientHudGlitch;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * §5.4 Глитчим vanilla UI (HP, food, hotbar) в безумии Регулуса —
 * сдвигаем pose-jitter на время рендера всего блока хотбара/индикаторов.
 * Чат не трогаем (он не в этом методе). F3 не трогаем.
 */
@Mixin(Gui.class)
public abstract class GuiVanillaGlitchMixin {
	@Inject(method = "renderHotbarAndDecorations", at = @At("HEAD"))
	private void superheroes$pushJitter(GuiGraphics graphics, DeltaTracker tracker, CallbackInfo ci) {
		float ramp = ClientHudGlitch.ramp();
		if (ramp <= 0.001f) return;
		graphics.pose().pushPose();
		graphics.pose().translate(ClientHudGlitch.jitterX(), ClientHudGlitch.jitterY(), 0f);
	}

	@Inject(method = "renderHotbarAndDecorations", at = @At("RETURN"))
	private void superheroes$popJitter(GuiGraphics graphics, DeltaTracker tracker, CallbackInfo ci) {
		float ramp = ClientHudGlitch.ramp();
		if (ramp <= 0.001f) return;
		graphics.pose().popPose();
	}
}
