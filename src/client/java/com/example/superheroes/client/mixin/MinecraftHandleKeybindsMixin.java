package com.example.superheroes.client.mixin;

import com.example.superheroes.client.ClientHeroState;
import com.example.superheroes.client.HotbarLockState;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Blocks vanilla hotbar slot switching while a hero is active:
 * - keys 3/4/5 are ability binds, so their hotbar clicks are always consumed;
 * - with hotbar lock enabled, all 1-9 keys are consumed.
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
		boolean locked = HotbarLockState.isLocked();
		for (int i = 0; i < mc.options.keyHotbarSlots.length; i++) {
			boolean abilityKey = i == 2 || i == 3 || i == 4; // 3, 4, 5 are ability hotkeys
			if (locked || abilityKey) {
				while (mc.options.keyHotbarSlots[i].consumeClick()) {
					// consumed
				}
			}
		}
	}
}
