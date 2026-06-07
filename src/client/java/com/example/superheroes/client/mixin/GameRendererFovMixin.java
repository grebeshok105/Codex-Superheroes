package com.example.superheroes.client.mixin;

import com.example.superheroes.client.ClientMadnessState;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererFovMixin {
	@Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
	private void superheroes$evangelionZoom(Camera camera, float partial, boolean useSetting, CallbackInfoReturnable<Double> cir) {
		if (!ClientMadnessState.isReading()) {
			return;
		}
		long until = ClientMadnessState.readingUntilMs();
		long now = System.currentTimeMillis();
		long total = 10000L;
		long remaining = until - now;
		if (remaining <= 0L || remaining > total) {
			return;
		}
		float progress = 1f - (remaining / (float) total);
		double zoomFactor = 1.0 - progress * 0.7;
		cir.setReturnValue(cir.getReturnValueD() * zoomFactor);
	}
}
