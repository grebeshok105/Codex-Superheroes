package com.example.superheroes.client.mixin;

import com.example.superheroes.client.ClientPandoraDeathState;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Pandora death cinematic (#10): block ALL mouse button input (attack, use, picks) for the
 * duration — "полностью блокируй ввод с мыши".
 */
@Mixin(MouseHandler.class)
public abstract class PandoraCinematicMousePressMixin {
	@Inject(method = "onPress", at = @At("HEAD"), cancellable = true)
	private void superheroes$blockClicks(long window, int button, int action, int mods, CallbackInfo ci) {
		if (ClientPandoraDeathState.active()) {
			ci.cancel();
		}
	}
}
