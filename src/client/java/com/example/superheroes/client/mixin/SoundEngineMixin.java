package com.example.superheroes.client.mixin;

import com.example.superheroes.client.ClientPandoraDeathState;
import com.example.superheroes.client.ClientReinhardTimeSlowState;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {
	@Inject(method = "play", at = @At("HEAD"), cancellable = true)
	private void superheroes$muteLightningDuringPandora(SoundInstance instance, CallbackInfo ci) {
		// Pandora's cinematic lightning strikes WITHOUT sound (#9) — only the bolts are silent,
		// her giggle (a superheroes: sound) still plays.
		if (ClientPandoraDeathState.active()
				&& instance.getLocation().getPath().contains("lightning")) {
			ci.cancel();
		}
	}

	@Inject(method = "play", at = @At("HEAD"), cancellable = true)
	private void superheroes$muteDuringTimeSlow(SoundInstance instance, CallbackInfo ci) {
		if (!ClientReinhardTimeSlowState.active()) return;
		SoundSource source = instance.getSource();
		if (source == SoundSource.MUSIC || source == SoundSource.MASTER || source == SoundSource.VOICE) return;
		String soundPath = instance.getLocation().toString();
		if (soundPath.contains("superheroes:reinhard")) return;
		ci.cancel();
	}
}
