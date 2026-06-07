package com.example.superheroes.mixin;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.effect.FlightController;
import com.example.superheroes.transform.HeroData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityFallFlyingMixin {
	@Shadow
	protected int fallFlyTicks;

	@Inject(method = "updateFallFlying", at = @At("HEAD"), cancellable = true)
	private void superheroes$keepFallFlying(CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (!(self instanceof Player player)) {
			return;
		}
		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
		if (data.hasHero() && FlightController.isFlightActive(data)) {
			player.startFallFlying();
			this.fallFlyTicks++;
			ci.cancel();
		}
	}
}
