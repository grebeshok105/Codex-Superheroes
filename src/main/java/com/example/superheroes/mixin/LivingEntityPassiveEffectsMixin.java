package com.example.superheroes.mixin;

import com.example.superheroes.lifecycle.PassiveReconciler;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Feeds {@link PassiveReconciler}: {@code addEffect} reports the instances a hero's
 * {@code applyPassives} declares; {@code onEffectRemoved} reports every removal path
 * (milk, {@code removeAllEffects}, targeted removal, natural expiry) for a deferred re-assert.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityPassiveEffectsMixin {
	@Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
			at = @At("HEAD"))
	private void superheroes$captureDeclaredPassive(MobEffectInstance instance, Entity source,
			CallbackInfoReturnable<Boolean> cir) {
		PassiveReconciler.onAddEffect((LivingEntity) (Object) this, instance);
	}

	@Inject(method = "onEffectRemoved", at = @At("TAIL"))
	private void superheroes$markPassivesDirty(MobEffectInstance instance, CallbackInfo ci) {
		PassiveReconciler.onEffectRemoved((LivingEntity) (Object) this);
	}
}
