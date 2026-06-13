package com.example.superheroes.client.mixin;

import com.example.superheroes.client.ClientPandoraDeathState;
import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Pandora death cinematic (#10): block ALL keyboard input for the duration —
 * "полностью блокируй ввод с клавиатуры". The 9s client safety timeout guarantees the lock
 * always releases even if the END packet is lost.
 */
@Mixin(KeyboardHandler.class)
public abstract class PandoraCinematicKeyboardMixin {
	@Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
	private void superheroes$blockKeys(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
		if (ClientPandoraDeathState.active()) {
			ci.cancel();
		}
	}
}
