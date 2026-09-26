package io.github.grebeshok105.codex.client.mixin;

import io.github.grebeshok105.codex.client.ClientPandoraDeathState;
import net.minecraft.client.KeyboardHandler;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Pandora death cinematic (#10): block ALL keyboard input for the duration —
 * "полностью блокируй ввод с клавиатуры". The 9s client safety timeout guarantees the lock
 * always releases even if the END packet is lost.
 *
 * <p>Releases must pass through (audit B15): {@code KeyMapping.set(key, false)} only runs on
 * {@code GLFW_RELEASE}, so a key held when the cinematic starts and released mid-scene would
 * otherwise stay logically pressed — phantom walking/attacking after the scene.
 */
@Mixin(KeyboardHandler.class)
public abstract class PandoraCinematicKeyboardMixin {
	@Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
	private void superheroes$blockKeys(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
		if (ClientPandoraDeathState.active() && action != GLFW.GLFW_RELEASE) {
			ci.cancel();
		}
	}
}
