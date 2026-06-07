package com.example.superheroes.mixin;

import com.example.superheroes.effect.DoomsdayEffectAdaptationController;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityEffectMixin {
	@Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
			at = @At("HEAD"), cancellable = true)
	private void superheroes$doomsdayEffectAdaptation(MobEffectInstance instance, Entity source,
			CallbackInfoReturnable<Boolean> cir) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (!(self instanceof ServerPlayer player)) return;
		if (!DoomsdayEffectAdaptationController.isTracked(instance.getEffect())) return;
		if (!DoomsdayEffectAdaptationController.onApply(player, instance)) {
			cir.setReturnValue(false);
		}
	}
}
