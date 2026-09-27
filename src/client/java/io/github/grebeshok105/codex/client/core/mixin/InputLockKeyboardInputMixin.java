package io.github.grebeshok105.codex.client.core.mixin;

import io.github.grebeshok105.codex.client.core.input.InputLock;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Generic input lock: zero out all movement after the keyboard is polled, so even keys
 * that were already held down can't move the player while a lock reason is active.
 * Registered by hero modules via {@link InputLock} (Pandora death cinematic, …).
 */
@Mixin(KeyboardInput.class)
public abstract class InputLockKeyboardInputMixin {
	@Inject(method = "tick", at = @At("TAIL"))
	private void superheroes$freezeMovement(boolean isSneaking, float movementMultiplier, CallbackInfo ci) {
		if (!InputLock.isLocked()) {
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
