package com.example.superheroes.client.mixin;

import com.example.superheroes.client.ClientFlightState;
import com.example.superheroes.client.ClientHeroState;
import com.example.superheroes.effect.ModEffects;
import com.example.superheroes.flight.FlightAbilityState;
import com.example.superheroes.flight.FlightMode;
import com.example.superheroes.flight.FlightProfiles;
import com.example.superheroes.flight.FlightTuning;
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
	@Inject(method = "travel", at = @At("HEAD"), cancellable = true)
	private void superheroes$inertialFlight(Vec3 input, CallbackInfo ci) {
		Player self = (Player) (Object) this;
		if (!self.level().isClientSide || !(self instanceof LocalPlayer player)) {
			return;
		}
		HeroData heroData = ClientHeroState.data();
		FlightMode mode = FlightAbilityState.activeMode(heroData);
		ClientFlightState.State state = ClientFlightState.get(player.getId());
		if (state != null) {
			mode = state.mode();
		}
		if (mode == null) {
			return;
		}

		FlightTuning tuning = FlightProfiles.tuning(mode, heroData.energy(), ClientHeroState.energyMax(), ModEffects.isMadness(player));
		double maxHorizontal = tuning.maxHorizontalSpeed();
		double maxVertical = tuning.maxVerticalSpeed();
		double accelMag = tuning.acceleration();

		float forward = player.zza;
		float strafe = player.xxa;
		boolean jumping = player.input != null && player.input.jumping;
		boolean sneaking = player.input != null && player.input.shiftKeyDown;
		if (tuning.forcesForward() && forward < 1.0f) {
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
			mx *= tuning.horizontalFriction();
			mz *= tuning.horizontalFriction();
		}
		if (noVertInput) {
			my *= tuning.verticalFriction();
		}

		self.setDeltaMovement(mx, my, mz);
		self.move(MoverType.SELF, self.getDeltaMovement());
		ci.cancel();
	}

}
