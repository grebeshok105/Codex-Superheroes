package com.example.superheroes.client.mixin;

import com.example.superheroes.client.screen.HudEditScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MC 1.21 blurs the world behind ANY open screen (Menu Background Blurriness).
 * The HUD drag editor must show the world crisp so the player can see exactly
 * where elements land — cancel the blur pass while it is open.
 */
@Mixin(GameRenderer.class)
public class GameRendererBlurMixin {
	@Inject(method = "processBlurEffect", at = @At("HEAD"), cancellable = true)
	private void superheroes$noBlurInHudEditor(float partialTick, CallbackInfo ci) {
		if (Minecraft.getInstance().screen instanceof HudEditScreen) {
			ci.cancel();
		}
	}
}
