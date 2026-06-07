package com.example.superheroes.client.mixin;

import com.example.superheroes.ability.AbilityIds;
import com.example.superheroes.client.ClientHeroState;
import com.example.superheroes.effect.ModEffects;
import com.example.superheroes.transform.HeroData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class LocalPlayerFlightMixin {
	private static final double MAX_HORIZONTAL_SPEED = 1.5;
	private static final double MAX_VERTICAL_SPEED = 1.0;
	private static final double ACCEL = 0.12;
	private static final double MIN_SPEED_MUL = 0.5;
	private static final double MADNESS_SPEED_MUL = 1.5;
	private static final double IRON_MAN_BASE_MUL = 0.56;
	private static final double SUPERSONIC_MUL = 2.6;
	private static final double HOMELANDER_NERF_MUL = 0.4;
	private static final double FRICTION_HORIZONTAL = 0.92;
	private static final double FRICTION_VERTICAL = 0.90;

	@Inject(method = "travel", at = @At("HEAD"), cancellable = true)
	private void superheroes$inertialFlight(Vec3 input, CallbackInfo ci) {
		Player self = (Player) (Object) this;
		if (!self.level().isClientSide || !(self instanceof LocalPlayer player)) {
			return;
		}
		HeroData heroData = ClientHeroState.data();
		boolean homelanderFlight = heroData.isActive(AbilityIds.FLIGHT);
		boolean ironFlight = heroData.isActive(AbilityIds.IRON_MAN_FLIGHT);
		boolean supersonic = heroData.isActive(AbilityIds.SUPERSONIC);
		if (!homelanderFlight && !ironFlight && !supersonic) {
			return;
		}

		double speedMul;
		float energyMax = ClientHeroState.energyMax();
		if (energyMax <= 0f) {
			speedMul = 1.0;
		} else {
			double frac = Math.max(0f, Math.min(1f, heroData.energy() / energyMax));
			speedMul = MIN_SPEED_MUL + (1.0 - MIN_SPEED_MUL) * frac;
		}
		if (ModEffects.isMadness(player)) {
			speedMul *= MADNESS_SPEED_MUL;
		} else if (homelanderFlight && !ironFlight && !supersonic) {
			speedMul *= MADNESS_SPEED_MUL;
		}
		if (ironFlight || supersonic) {
			speedMul *= IRON_MAN_BASE_MUL;
			if (supersonic) {
				speedMul *= SUPERSONIC_MUL;
			}
		}
		double maxHorizontal = MAX_HORIZONTAL_SPEED * speedMul;
		double maxVertical = MAX_VERTICAL_SPEED * speedMul;
		double accelMag = ACCEL * speedMul;

		float forward = player.zza;
		float strafe = player.xxa;
		boolean jumping = player.input != null && player.input.jumping;
		boolean sneaking = player.input != null && player.input.shiftKeyDown;
		if (supersonic && forward < 1.0f) {
			forward = 1.0f;
		}

		float yawRad = (float) Math.toRadians(player.getYRot());
		float pitchRad = (float) Math.toRadians(player.getXRot());
		double sinYaw = Math.sin(yawRad);
		double cosYaw = Math.cos(yawRad);
		double sinPitch = Math.sin(pitchRad);
		double cosPitch = Math.cos(pitchRad);

		double inputForwardX = -sinYaw * cosPitch;
		double inputForwardY = -sinPitch;
		double inputForwardZ = cosYaw * cosPitch;

		double inputStrafeX = cosYaw;
		double inputStrafeZ = sinYaw;

		double accelX = inputForwardX * forward + inputStrafeX * strafe;
		double accelY = inputForwardY * forward;
		double accelZ = inputForwardZ * forward + inputStrafeZ * strafe;

		if (jumping) {
			accelY += 1.0;
		}
		if (sneaking) {
			accelY -= 1.0;
		}

		double inputMag = Math.sqrt(accelX * accelX + accelY * accelY + accelZ * accelZ);
		if (inputMag > 1e-4) {
			double scale = accelMag / Math.max(inputMag, 1.0);
			accelX *= scale;
			accelY *= scale;
			accelZ *= scale;
		} else {
			accelX = 0;
			accelY = 0;
			accelZ = 0;
		}

		Vec3 motion = self.getDeltaMovement();
		double mx = motion.x + accelX;
		double my = motion.y + accelY;
		double mz = motion.z + accelZ;

		double horizSq = mx * mx + mz * mz;
		if (horizSq > maxHorizontal * maxHorizontal) {
			double s = maxHorizontal / Math.sqrt(horizSq);
			mx *= s;
			mz *= s;
		}
		if (Math.abs(my) > maxVertical) {
			my = Math.signum(my) * maxVertical;
		}

		boolean noHorizInput = forward == 0 && strafe == 0;
		boolean noVertInput = !jumping && !sneaking && forward == 0;
		if (noHorizInput) {
			mx *= FRICTION_HORIZONTAL;
			mz *= FRICTION_HORIZONTAL;
		}
		if (noVertInput) {
			my *= FRICTION_VERTICAL;
		}

		self.setDeltaMovement(mx, my, mz);
		self.move(MoverType.SELF, self.getDeltaMovement());
		ci.cancel();
	}
}
