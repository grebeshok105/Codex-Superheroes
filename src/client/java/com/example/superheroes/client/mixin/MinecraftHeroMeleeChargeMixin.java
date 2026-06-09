package com.example.superheroes.client.mixin;

import com.example.superheroes.client.ClientHeroState;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftHeroMeleeChargeMixin {
	@Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
	private void superheroes$cancelAttackDuringHeroMeleeCharge(CallbackInfoReturnable<Boolean> cir) {
		if (superheroes$isChargingHeroMelee()) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
	private void superheroes$cancelUseDuringHeroMeleeCharge(CallbackInfo ci) {
		if (superheroes$isChargingHeroMelee()) {
			ci.cancel();
		}
	}

	@Unique
	private static boolean superheroes$isChargingHeroMelee() {
		Minecraft client = Minecraft.getInstance();
		return client.player != null
				&& client.level != null
				&& client.screen == null
				&& ClientHeroState.data().hasHero()
				&& client.options.keyAttack.isDown()
				&& client.options.keyUse.isDown();
	}
}
