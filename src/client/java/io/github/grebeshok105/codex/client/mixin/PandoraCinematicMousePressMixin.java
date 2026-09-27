package io.github.grebeshok105.codex.client.mixin;

import io.github.grebeshok105.codex.client.ClientPandoraDeathState;
import net.minecraft.client.MouseHandler;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Pandora death cinematic (#10): block ALL mouse button input (attack, use, picks) for the
 * duration — "полностью блокируй ввод с мыши".
 *
 * <p>Releases must pass through (audit B15): {@code KeyMapping.set(mouseKey, false)} only runs
 * on {@code GLFW_RELEASE} — swallowing it leaves a held button logically pressed after the
 * scene (phantom attacking/mining).
 */
@Mixin(MouseHandler.class)
public abstract class PandoraCinematicMousePressMixin {
	@Inject(method = "onPress", at = @At("HEAD"), cancellable = true)
	private void superheroes$blockClicks(long window, int button, int action, int mods, CallbackInfo ci) {
		if (ClientPandoraDeathState.active() && action != GLFW.GLFW_RELEASE) {
			ci.cancel();
		}
	}
}
