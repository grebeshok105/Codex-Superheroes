package com.example.superheroes.client.mixin;

import com.example.superheroes.client.ClientThinkMarkState;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Поза «Think, Mark!»: обе руки вытянуты вперёд — Омнимен держит цель
 * перед собой на протяжении захвата и рывка.
 */
@Mixin(PlayerModel.class)
public abstract class PlayerModelPoseMixin<T extends LivingEntity> {
	@Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
	private void superheroes$thinkMarkPose(T entity, float limbSwing, float limbSwingAmount,
			float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
		if (!(entity instanceof AbstractClientPlayer player)
				|| !ClientThinkMarkState.isActive(player.getUUID())) {
			return;
		}
		PlayerModel<?> model = (PlayerModel<?>) (Object) this;
		float forward = -(float) (Math.PI / 2.0) + player.getXRot() * ((float) Math.PI / 180f) * 0.6f;
		model.rightArm.xRot = forward;
		model.leftArm.xRot = forward;
		model.rightArm.yRot = -0.10f;
		model.leftArm.yRot = 0.10f;
		model.rightArm.zRot = 0.04f;
		model.leftArm.zRot = -0.04f;
		model.rightSleeve.copyFrom(model.rightArm);
		model.leftSleeve.copyFrom(model.leftArm);
	}
}
