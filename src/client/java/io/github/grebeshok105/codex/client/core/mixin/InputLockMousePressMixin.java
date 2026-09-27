package io.github.grebeshok105.codex.client.core.mixin;

import io.github.grebeshok105.codex.client.core.input.InputLock;
import net.minecraft.client.MouseHandler;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Generic input lock: block all mouse button input (attack, use, picks) while a lock reason
 * is active. Registered by hero modules via {@link InputLock} (Pandora death cinematic, …).
 *
 * <p>Releases must pass through (audit B15): {@code KeyMapping.set(mouseKey, false)} only runs
 * on {@code GLFW_RELEASE} — swallowing it leaves a held button logically pressed after the
 * lock ends (phantom attacking/mining).
 */
@Mixin(MouseHandler.class)
public abstract class InputLockMousePressMixin {
	@Inject(method = "onPress", at = @At("HEAD"), cancellable = true)
	private void superheroes$blockClicks(long window, int button, int action, int mods, CallbackInfo ci) {
		if (InputLock.isLocked() && action != GLFW.GLFW_RELEASE) {
			ci.cancel();
		}
	}
}
