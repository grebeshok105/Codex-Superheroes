package com.example.superheroes.mixin;

import com.example.superheroes.effect.ModEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityHealBlockMixin {
	@Inject(method = "heal", at = @At("HEAD"), cancellable = true)
	private void superheroes$blockHeal(float amount, CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (self.hasEffect(ModEffects.HEAL_BLOCK)) {
			ci.cancel();
		}
	}
}
