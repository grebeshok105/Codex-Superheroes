package com.example.superheroes.client.mixin;

import com.example.superheroes.client.ClientPandoraDeathState;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Pandora death cinematic (#7, #10): hard-lock camera rotation — nobody can move the view while
 * the cinematic plays. Pandora's POV is already the killer's (server-side setCamera).
 */
@Mixin(MouseHandler.class)
public abstract class PandoraCinematicMouseTurnMixin {
	@Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
	private void superheroes$lockCamera(double sensitivity, CallbackInfo ci) {
		if (ClientPandoraDeathState.active()) {
			ci.cancel();
		}
	}
}
