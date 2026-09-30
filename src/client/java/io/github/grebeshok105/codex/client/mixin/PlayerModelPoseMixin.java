package io.github.grebeshok105.codex.client.mixin;

import io.github.grebeshok105.codex.client.ClientFlightState;
import io.github.grebeshok105.codex.client.ClientThinkMarkState;
import io.github.grebeshok105.codex.client.core.flight.FlightPresentations;
import io.github.grebeshok105.codex.client.core.render.PlayerModelSuppressions;
import io.github.grebeshok105.codex.client.core.render.SkinResolver;
import io.github.grebeshok105.codex.mechanic.flight.FlightPhase;
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
 *   <li><b>EMF-подмена</b> — пока {@code PlayerModelSuppressions} активна
 *       (Хоумлендер под EMF-моделью), vanilla-модель целиком скрывается через
 *       {@code setAllVisible(false)}.</li>
 * </ul>
 */
@Mixin(PlayerModel.class)
public abstract class PlayerModelPoseMixin<T extends LivingEntity> {
	/**
	 * {@code ModelPart.visible} persists between frames, so the EMF
	 * suppression from the last pass must be lifted before the new pose is
	 * computed — the vanilla model comes back the frame presentation ends.
	 */
	@Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("HEAD"))
	private void superheroes$restoreModelRoot(T entity, float limbSwing, float limbSwingAmount,
			float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
		((PlayerModel<?>) (Object) this).setAllVisible(true);
	}

	@Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
	private void superheroes$heroPose(T entity, float limbSwing, float limbSwingAmount,
			float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
		if (!(entity instanceof AbstractClientPlayer player)) {
			return;
		}
		PlayerModel<?> model = (PlayerModel<?>) (Object) this;

		// --- EMF-подмена: Хоумлендер рисуется HomelanderEmfLayer'ом ---
		if (PlayerModelSuppressions.suppresses(player)) {
			model.setAllVisible(false);
			return;
		}

		// --- статичная поза ног в полёте (для летающих героев без FlightPresentation) ---
		ClientFlightState.State flight = ClientFlightState.get(player.getId());
		if (flight != null && flight.phase() != FlightPhase.IDLE && flight.phase() != FlightPhase.LANDING
				&& !player.isCrouching()
				&& FlightPresentations.of(SkinResolver.heroIdFor(player)).isEmpty()) {
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
