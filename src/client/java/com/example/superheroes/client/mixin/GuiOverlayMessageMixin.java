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
 * Перемещает ванильные overlay-уведомления (action bar: «поблизости нет игроков…»
 * и т.п.) наверх экрана, когда активен геройский HUD — внизу они накладывались
 * на кастомные панели способностей.
 */
@Mixin(Gui.class)
public class GuiOverlayMessageMixin {

	@Inject(method = "renderOverlayMessage", at = @At("HEAD"))
	private void superheroes$moveOverlayUp(GuiGraphics graphics, DeltaTracker tracker, CallbackInfo ci) {
		if (HotbarOverrideHud.shouldSuppressVanillaHotbar()) {
			graphics.pose().pushPose();
			// Ваниль рисует по y = высота - 68; поднимаем сообщение в верхнюю
			// треть экрана (y ≈ 56), где ничего не рисуется.
			graphics.pose().translate(0.0f, -(graphics.guiHeight() - 68 - 56), 0.0f);
		}
	}

	@Inject(method = "renderOverlayMessage", at = @At("RETURN"))
	private void superheroes$moveOverlayUpPop(GuiGraphics graphics, DeltaTracker tracker, CallbackInfo ci) {
		if (HotbarOverrideHud.shouldSuppressVanillaHotbar()) {
			graphics.pose().popPose();
		}
	}
}
