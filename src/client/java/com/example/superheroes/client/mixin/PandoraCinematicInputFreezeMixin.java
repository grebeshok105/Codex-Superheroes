package com.example.superheroes.client.mixin;

import com.example.superheroes.client.ClientPandoraDeathState;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Pandora death cinematic (#7): zero out all movement after the keyboard is polled, so even keys
 * that were already held down can't move the player while everyone is frozen.
 */
@Mixin(KeyboardInput.class)
public abstract class PandoraCinematicInputFreezeMixin {
	@Inject(method = "tick", at = @At("TAIL"))
	private void superheroes$freezeMovement(boolean isSneaking, float movementMultiplier, CallbackInfo ci) {
		if (!ClientPandoraDeathState.active()) {
			return;
		}
		Input self = (Input) (Object) this;
		self.up = false;
		self.down = false;
		self.left = false;
		self.right = false;
		self.jumping = false;
		self.shiftKeyDown = false;
		self.forwardImpulse = 0f;
		self.leftImpulse = 0f;
	}
}
