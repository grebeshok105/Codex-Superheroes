package io.github.grebeshok105.codex.client.core.mixin;

import io.github.grebeshok105.codex.client.core.audio.ClientSoundFilters;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {
	@Inject(method = "play", at = @At("HEAD"), cancellable = true)
	private void superheroes$muteFiltered(SoundInstance instance, CallbackInfo ci) {
		if (ClientSoundFilters.isMuted(instance)) {
			ci.cancel();
		}
	}
}
