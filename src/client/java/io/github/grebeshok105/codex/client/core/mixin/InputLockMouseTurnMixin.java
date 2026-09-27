package io.github.grebeshok105.codex.client.core.mixin;

import io.github.grebeshok105.codex.client.core.input.InputLock;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Generic input lock: hard-lock camera rotation while a lock reason is active — nobody can
 * move the view during a cinematic. Registered by hero modules via {@link InputLock}.
 */
@Mixin(MouseHandler.class)
public abstract class InputLockMouseTurnMixin {
	@Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
	private void superheroes$lockCamera(double sensitivity, CallbackInfo ci) {
		if (InputLock.isLocked()) {
			ci.cancel();
		}
	}
}
