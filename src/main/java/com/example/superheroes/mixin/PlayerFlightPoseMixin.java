package com.example.superheroes.mixin;

import com.example.superheroes.ability.AbilityIds;
import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.transform.HeroData;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerFlightPoseMixin {
	@Inject(method = "updatePlayerPose", at = @At("HEAD"), cancellable = true)
	private void superheroes$forceFlightPose(CallbackInfo ci) {
		Player self = (Player) (Object) this;
		HeroData data = self.getAttachedOrCreate(ModAttachments.HERO_DATA);
		if (!data.hasHero()) {
			return;
		}
		if (data.isActive(AbilityIds.FLIGHT)
				|| data.isActive(AbilityIds.IRON_MAN_FLIGHT)
				|| data.isActive(AbilityIds.SUPERSONIC)) {
			if (!self.isFallFlying()) {
				self.startFallFlying();
			}
			if (self.getPose() != Pose.FALL_FLYING) {
				self.setPose(Pose.FALL_FLYING);
			}
			ci.cancel();
		}
	}
}
