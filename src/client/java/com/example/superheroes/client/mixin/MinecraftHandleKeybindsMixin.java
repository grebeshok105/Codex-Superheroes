package com.example.superheroes.client.mixin;

import com.example.superheroes.client.ClientHeroState;
import com.example.superheroes.client.HotbarLockState;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * LOCKED semantics (v3.19): the lock is an "ability mode" switch.
 * - Lock OFF: number keys 1-9 switch hotbar slots as vanilla; ability keys do nothing.
 * - Lock ON: ALL 1-9 hotbar clicks are consumed (slots can't switch via digits);
 *   3/4/5 trigger abilities instead. The mouse wheel always switches slots.
 * Runs at HEAD of handleKeybinds, before vanilla reads the clicks — deterministic,
 * unlike tick-event ordering.
 */
@Mixin(Minecraft.class)
public class MinecraftHandleKeybindsMixin {
	@Inject(method = "handleKeybinds", at = @At("HEAD"))
	private void superheroes$blockHotbarKeys(CallbackInfo ci) {
		Minecraft mc = (Minecraft) (Object) this;
		if (mc.player == null || !ClientHeroState.data().hasHero()) {
			return;
		}
		if (!HotbarLockState.isLocked()) {
			return; // lock off: vanilla slot switching untouched
		}
		for (int i = 0; i < mc.options.keyHotbarSlots.length; i++) {
			while (mc.options.keyHotbarSlots[i].consumeClick()) {
				// lock on: digits never switch slots
			}
		}
	}
}
