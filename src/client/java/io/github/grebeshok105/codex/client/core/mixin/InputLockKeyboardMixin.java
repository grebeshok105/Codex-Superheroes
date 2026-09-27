package io.github.grebeshok105.codex.client.core.mixin;

import io.github.grebeshok105.codex.client.core.input.InputLock;
import net.minecraft.client.KeyboardHandler;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Generic input lock: block all keyboard input while a lock reason is active.
 * Registered by hero modules via {@link InputLock} (Pandora death cinematic, …).
 *
 * <p>Releases must pass through (audit B15): {@code KeyMapping.set(key, false)} only runs on
 * {@code GLFW_RELEASE}, so a key held when the lock engages and released mid-lock would
 * otherwise stay logically pressed — phantom walking/attacking after the lock ends.
 */
@Mixin(KeyboardHandler.class)
public abstract class InputLockKeyboardMixin {
	@Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
	private void superheroes$blockKeys(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
		if (InputLock.isLocked() && action != GLFW.GLFW_RELEASE) {
			ci.cancel();
		}
	}
}
