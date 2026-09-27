package io.github.grebeshok105.codex.client.core.mixin;

import io.github.grebeshok105.codex.client.core.FovModifiers;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererFovMixin {
	@Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
	private void superheroes$moduleFov(Camera camera, float partial, boolean useSetting, CallbackInfoReturnable<Double> cir) {
		cir.setReturnValue(FovModifiers.applyAll(camera, partial, cir.getReturnValueD()));
	}
}
