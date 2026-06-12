package com.example.superheroes.client.mixin;

import com.example.superheroes.client.ClientFlightState;
import com.example.superheroes.client.ClientThinkMarkState;
import com.example.superheroes.flight.FlightPhase;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Кастомные позы модели игрока:
 * <ul>
 *   <li><b>«Think, Mark!»</b> — обе руки вытянуты вперёд (захват/рывок Омнимена).</li>
 *   <li><b>Полёт</b> — ноги фиксируются в ровной «крейсерской» позе, чтобы они не
 *       болтались/не доигрывали анимацию падения при взлёте из прыжка.</li>
 * </ul>
 */
@Mixin(PlayerModel.class)
public abstract class PlayerModelPoseMixin<T extends LivingEntity> {
	@Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
	private void superheroes$heroPose(T entity, float limbSwing, float limbSwingAmount,
			float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
		if (!(entity instanceof AbstractClientPlayer player)) {
			return;
		}
		PlayerModel<?> model = (PlayerModel<?>) (Object) this;

		// --- статичная поза ног в полёте (для всех летающих героев) ---
		ClientFlightState.State flight = ClientFlightState.get(player.getId());
		if (flight != null && flight.phase() != FlightPhase.IDLE && flight.phase() != FlightPhase.LANDING
				&& !player.isCrouching()) {
			// ноги прямые, вместе, слегка отведены назад — стабильная «полётная» поза,
			// перекрывает остаточный limbSwing от прыжка/падения
			float back = -0.22f;
			model.rightLeg.xRot = back;
			model.leftLeg.xRot = back;
			model.rightLeg.yRot = 0f;
			model.leftLeg.yRot = 0f;
			model.rightLeg.zRot = 0.04f;
			model.leftLeg.zRot = -0.04f;
			model.rightPants.copyFrom(model.rightLeg);
			model.leftPants.copyFrom(model.leftLeg);
		}

		// --- поза «Think, Mark!» (руки вперёд) ---
		if (ClientThinkMarkState.isActive(player.getUUID())) {
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
}
